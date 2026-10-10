/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.network.protocol.impl.mqtt_sn;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.config.protocol.impl.MqttSnConfig;
import io.mapsmessaging.engine.destination.MessageOverrides;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.InterfaceInformation;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.impl.SelectorCallback;
import io.mapsmessaging.network.io.impl.SelectorTask;
import io.mapsmessaging.network.io.impl.udp.UDPFacadeEndPoint;
import io.mapsmessaging.network.io.impl.udp.session.UDPSessionManager;
import io.mapsmessaging.network.io.impl.udp.session.UDPSessionState;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.MQTT_SNProtocol;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.*;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.MqttSn2Protocol;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.MqttSn2HmacProtectionSession;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.MqttSn2ProtectionPolicy;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.MqttSn2ProtectionSession;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.protection.MqttSn2CryptoProviders;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.MqttSn2IndexedCounterStore;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.KeyStoreProtectionKeyResolver;
import io.mapsmessaging.configuration.ConfigurationProperties;
import java.util.Map;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.MqttSn2ProtectionVerifier;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.MqttSn2FrameCodec;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.MqttSn2GatewayCodec;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.MqttSn2PacketType;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.MqttSn2PublishCodec;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.MqttSn2OutboundPacket;
import io.mapsmessaging.network.protocol.transformation.ProtocolMessageTransformation;
import io.mapsmessaging.network.protocol.transformation.TransformationManager;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.channels.SelectionKey;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

// The protocol is MQTT_SN so it makes sense
@java.lang.SuppressWarnings("squid:S00101")
public class MQTTSNInterfaceManager implements SelectorCallback {

  private static final String PROTOCOL_NAME = "mqtt-sn";


  private final Logger logger;
  private final SelectorTask selectorTask;
  private final EndPoint endPoint;
  private final UDPSessionManager<Protocol> currentSessions;
  private final PacketFactory[] packetFactory;
  private final AdvertiserTask advertiserTask;
  private final byte gatewayId;
  private final RegisteredTopicConfiguration registeredTopicConfiguration;
  private final ProtocolMessageTransformation transformation;

  private final boolean enablePortChanges;
  private final boolean enableAddressChanges;
  private final boolean advertiseGateway;

  private final MqttSnConfig mqttSnConfig;
  private volatile MqttSn2ProtectionPolicy protectionSession;
  private MqttSn2IndexedCounterStore ownedCounterStore;

  /** Opt-in secured MQTT-SN 2.0 endpoint; requires durable counter source and key resolver. */
  public void configureProtection(MqttSn2ProtectionPolicy policy) {
    java.util.Objects.requireNonNull(policy, "policy");
    if (!policy.hasDurableReplayStore()) {
      throw new IllegalArgumentException("Secured MQTT-SN endpoint requires durable replay tracking");
    }
    protectionSession = policy;
  }

  /**
   * Configure HMAC protection with symmetric keys resolved via Authentication
   * library's ServiceLoader-selected keystore manager. Mapping keys use
   * lowercase hexadecimal sender ID followed by ':' and the numeric scheme.
   */
  public synchronized void configureKeystoreHmacProtection(
      ConfigurationProperties keyStoreConfiguration,
      Map<String, String> senderSchemeAliases,
      char[] keyEntryPassword,
      Path counterDirectory,
      byte[] localSenderIdentifier,
      int outboundScheme) throws IOException {
    if (ownedCounterStore != null || protectionSession != null) {
      throw new IllegalStateException("Protection is already configured on this endpoint");
    }
    KeyStoreProtectionKeyResolver resolver = new KeyStoreProtectionKeyResolver(
        keyStoreConfiguration, senderSchemeAliases, keyEntryPassword);
    // Fail startup if the local sender cannot obtain a suitable key.
    byte[] key = resolver.resolve(localSenderIdentifier, outboundScheme);
    try {
      if (key.length < 16) throw new IOException("Outbound HMAC key is too short");
    } finally {
      java.util.Arrays.fill(key, (byte) 0);
    }
    configureHmacProtection(resolver, counterDirectory, localSenderIdentifier, outboundScheme);
  }

  /**
   * Configure ServiceLoader-backed HMAC or AEAD protection using Authentication
   * library SecretKey entries and durable counter storage.
   */
  public synchronized void configureKeystoreProtection(
      ConfigurationProperties keyStoreConfiguration,
      Map<String, String> senderSchemeAliases,
      char[] entryPassword,
      Path counterDirectory,
      byte[] localSenderIdentifier,
      int outboundScheme,
      java.util.Set<Integer> acceptedSchemes) throws IOException {
    if (ownedCounterStore != null || protectionSession != null) {
      throw new IllegalStateException("Protection is already configured");
    }
    KeyStoreProtectionKeyResolver resolver = new KeyStoreProtectionKeyResolver(
        keyStoreConfiguration, senderSchemeAliases, entryPassword);
    MqttSn2CryptoProviders providers = new MqttSn2CryptoProviders();
    java.util.Objects.requireNonNull(acceptedSchemes, "acceptedSchemes");
    if (!acceptedSchemes.contains(outboundScheme)) {
      throw new IllegalArgumentException("Outbound scheme must be explicitly permitted");
    }
    for (int scheme : acceptedSchemes) providers.require(scheme);
    // Validate the outbound key before acquiring storage or accepting traffic.
    javax.crypto.SecretKey outgoingKey = resolver.resolveSecretKey(localSenderIdentifier, outboundScheme);
    if (outgoingKey == null) throw new IOException("Missing outbound protection key");
    // Fail endpoint configuration, not the first production packet, if the
    // provider cannot use this secret key or the configured key length.
    try {
      int nonceLength = outboundScheme >= 0x40 && outboundScheme <= 0x45 ? 13
          : outboundScheme >= 0x46 && outboundScheme <= 0x49 ? 12 : 0;
      providers.require(outboundScheme).protect(outboundScheme, outgoingKey,
          new byte[nonceLength], new byte[] {1}, new byte[] {1});
    } catch (java.security.GeneralSecurityException error) {
      throw new IOException("Outbound protection key and scheme are incompatible", error);
    }
    MqttSn2IndexedCounterStore counters = new MqttSn2IndexedCounterStore(counterDirectory);
    try {
      configureProtection(new MqttSn2ProtectionSession(
          providers, resolver::resolveSecretKey, counters, acceptedSchemes,
          outboundScheme, localSenderIdentifier));
      ownedCounterStore = counters;
    } catch (RuntimeException | Error failure) {
      try { counters.close(); } catch (IOException closeFailure) {
        failure.addSuppressed(closeFailure);
      }
      throw failure;
    }
  }

  /** Configure an indexed persistent protection store in a dedicated directory. */
  public synchronized void configureHmacProtection(MqttSn2ProtectionVerifier.KeyResolver keys,
      Path counterDirectory, byte[] localSenderIdentifier, int scheme) throws IOException {
    if (ownedCounterStore != null || protectionSession != null) {
      throw new IllegalStateException("Protection is already configured on this endpoint");
    }
    // Validate all inputs before constructing/publishing an empty durable store.
    if (localSenderIdentifier == null || localSenderIdentifier.length != 8
        || (scheme != 0 && scheme != 1)) {
      throw new IllegalArgumentException("Invalid protection sender identifier or HMAC scheme");
    }
    java.util.Objects.requireNonNull(keys, "keys");
    MqttSn2IndexedCounterStore counters = new MqttSn2IndexedCounterStore(counterDirectory);
    try {
      configureProtection(new MqttSn2ProtectionSession(
          new MqttSn2CryptoProviders(),
          (sender, incomingScheme) -> {
            if (incomingScheme != 0 && incomingScheme != 1) {
              throw new IOException("Unsupported HMAC scheme");
            }
            byte[] key = keys.resolve(sender, incomingScheme);
            if (key == null || key.length < 16) throw new IOException("Invalid HMAC key");
            return new javax.crypto.spec.SecretKeySpec(key,
                incomingScheme == 0 ? "HmacSHA256" : "HmacSHA3-256");
          }, counters, java.util.Set.of(scheme), scheme, localSenderIdentifier));
      ownedCounterStore = counters;
    } catch (RuntimeException | Error failure) {
      try {
        counters.close();
      } catch (IOException closeFailure) {
        failure.addSuppressed(closeFailure);
      }
      throw failure;
    }
  }


  public MQTTSNInterfaceManager(byte gatewayId, SelectorTask selectorTask, EndPoint endPoint) {
    logger = LoggerFactory.getLogger("MQTT-SN Protocol on " + endPoint.getName());
    this.gatewayId = gatewayId;
    this.selectorTask = selectorTask;
    advertiserTask = null;
    this.endPoint = endPoint;
    mqttSnConfig = (MqttSnConfig) endPoint.getConfig().getProtocolConfig(PROTOCOL_NAME);
    long timeout = mqttSnConfig.getIdleSessionTimeout();
    enablePortChanges = mqttSnConfig.isEnablePortChanges();
    enableAddressChanges = mqttSnConfig.isEnableAddressChanges();
    advertiseGateway = mqttSnConfig.isAdvertiseGateway();
    currentSessions = new UDPSessionManager<>(timeout);
    packetFactory = new PacketFactory[1];
    packetFactory[0] = new PacketFactory();
    transformation = TransformationManager.getInstance().getTransformation(
        endPoint.getProtocol(),
        endPoint.getName(),
        PROTOCOL_NAME,
        "<registered>"
    );

    registeredTopicConfiguration = new RegisteredTopicConfiguration(mqttSnConfig);
  }

  /** Resolves a configured MQTT-SN predefined topic for the independent v2 adapter. */
  public String resolvePredefinedTopic(SocketAddress address, int topicId) {
    return registeredTopicConfiguration.getTopic(address, topicId);
  }

  /** Reverse lookup for configured predefined aliases, respecting address scoping. */
  public int resolvePredefinedAlias(SocketAddress address, String name) {
    return registeredTopicConfiguration.getRegisteredTopicAliasType(address, name);
  }

  public MQTTSNInterfaceManager(InterfaceInformation info, EndPoint endPoint, byte gatewayId) throws IOException {
    logger = LoggerFactory.getLogger("MQTT-SN Protocol on " + endPoint.getName());
    this.endPoint = endPoint;
    this.gatewayId = gatewayId;
    mqttSnConfig = (MqttSnConfig) endPoint.getConfig().getProtocolConfig(PROTOCOL_NAME);
    long timeout = mqttSnConfig.getIdleSessionTimeout();
    enablePortChanges = mqttSnConfig.isEnablePortChanges();
    enableAddressChanges = mqttSnConfig.isEnableAddressChanges();
    advertiseGateway = mqttSnConfig.isAdvertiseGateway();

    currentSessions = new UDPSessionManager<>(timeout);
    packetFactory = new PacketFactory[1];
    packetFactory[0] = new PacketFactory();

    selectorTask = new SelectorTask(this, endPoint.getConfig().getEndPointConfig(), endPoint.isUDP());
    selectorTask.register(SelectionKey.OP_READ);
    if (startAdvertiseTask(info)) {
      AdvertiserTask tmp = null;
      try {
        tmp = new AdvertiserTask(gatewayId, endPoint, info, info.getBroadcast(), mqttSnConfig.getAdvertiseInterval());
      } catch (UncheckedIOException e) {
        logger.log(ServerLogMessages.MQTT_SN_EXCEPTION_RASIED, e);
        // unable to run the advertiser task on this endpoint
      }
      advertiserTask = tmp;
    } else {
      advertiserTask = null;
    }
    registeredTopicConfiguration = new RegisteredTopicConfiguration(mqttSnConfig);
    transformation = TransformationManager.getInstance().getTransformation(
        endPoint.getProtocol(),
        endPoint.getName(),
        PROTOCOL_NAME,
        "<registered>"
    );
    initializeConfiguredProtection();
  }

  /** Initialize explicitly configured protection; never expose secret configuration through REST. */
  private void initializeConfiguredProtection() throws IOException {
    ConfigurationProperties protection = mqttSnConfig.getProtectionConfiguration();
    if (protection == null || !protection.getBooleanProperty("enabled", false)) return;
    if (!(protection.get("keyStore") instanceof ConfigurationProperties keyStore)
        || !(protection.get("senderAliases") instanceof ConfigurationProperties aliasesConfig)) {
      throw new IOException("MQTT-SN protection keyStore and senderAliases are required");
    }
    String passwordEnvironment = protection.getProperty("keyEntryPasswordEnv");
    if (passwordEnvironment == null || passwordEnvironment.isBlank()) {
      throw new IOException("Missing key entry password environment variable name");
    }
    String passwordValue = System.getenv(passwordEnvironment);
    if (passwordValue == null || passwordValue.isEmpty()) {
      throw new IOException("MQTT-SN key entry password environment variable is unset");
    }
    String senderValue = protection.getProperty("senderIdentifier");
    byte[] sender;
    try {
      sender = java.util.HexFormat.of().parseHex(senderValue);
    } catch (IllegalArgumentException | NullPointerException invalid) {
      throw new IOException("Invalid MQTT-SN sender identifier", invalid);
    }
    if (sender.length != 8) throw new IOException("Sender identifier must contain eight octets");
    String directory = protection.getProperty("counterDirectory");
    String accepted = protection.getProperty("acceptedSchemes");
    if (directory == null || directory.isBlank() || accepted == null || accepted.isBlank()) {
      throw new IOException("Missing protection counter directory or scheme policy");
    }
    java.util.Set<Integer> schemes = new java.util.HashSet<>();
    try {
      for (String item : accepted.split(",")) schemes.add(Integer.decode(item.trim()));
    } catch (NumberFormatException invalid) {
      throw new IOException("Invalid protection scheme list", invalid);
    }
    Map<String, String> aliases = new java.util.HashMap<>();
    for (var entry : aliasesConfig.getMap().entrySet()) {
      aliases.put(entry.getKey().toString().toLowerCase(java.util.Locale.ROOT),
          entry.getValue().toString());
    }
    char[] entryPassword = passwordValue.toCharArray();
    try {
      configureKeystoreProtection(keyStore, aliases, entryPassword, Path.of(directory),
          sender, protection.getIntProperty("outboundScheme", -1), schemes);
    } finally {
      java.util.Arrays.fill(entryPassword, (char) 0);
    }
  }

  private boolean startAdvertiseTask(InterfaceInformation info) throws SocketException {
    return advertiseGateway && info.getBroadcast() != null && !info.isLoopback();
  }

  @Override
  public boolean processPacket(Packet packet) throws IOException {
    // OK, we have received a packet, lets find out if we have an existing context for it
    if (packet.getFromAddress() == null) {
      return true; // Ignoring packet since unknown client
    }
    UDPSessionState<Protocol> state = currentSessions.getState(packet.getFromAddress());
    if (state == null && enablePortChanges) {
      state = lookupByPacket(packet);
    }

    try {
      if (state != null && state.getContext() != null) {
        state.getContext().processPacket(packet);
      } else {
        MqttSnVersionDetector.Version version =
            MqttSnVersionDetector.detect(packet.getRawBuffer().asReadOnlyBuffer());
        if (version == MqttSnVersionDetector.Version.V2_0 || isProtectedV2(packet)) {
          acceptV2Connection(packet);
        } else if (version == MqttSnVersionDetector.Version.V1_2) {
          processIncomingPacket(packet, packetFactory[0]);
        } else {
          processUnconnectedDiscovery(packet);
        }
      }
    } catch (IOException e) {
      logger.log(ServerLogMessages.MQTT_SN_EXCEPTION_RASIED, e);
    }
    selectorTask.register(SelectionKey.OP_READ);
    return true;
  }

  private static boolean isProtectedV2(Packet packet) {
    ByteBuffer data = packet.getRawBuffer().asReadOnlyBuffer();
    if (data.remaining() < 2) return false;
    int offset = Byte.toUnsignedInt(data.get(data.position())) == 1 ? 3 : 1;
    return data.remaining() > offset
        && Byte.toUnsignedInt(data.get(data.position() + offset)) == 0xFF;
  }

  private void acceptV2Connection(Packet packet) throws IOException {
    ByteBuffer incoming = packet.getRawBuffer().asReadOnlyBuffer();
    MqttSn2ProtectionPolicy policy = protectionSession;
    if (isProtectedV2(packet)) {
      if (policy == null) {
        throw new IOException("Protected MQTT-SN 2.0 CONNECT without endpoint policy");
      }
      incoming = policy.receive(incoming);
    } else if (policy != null) {
      throw new IOException("Unprotected MQTT-SN 2.0 CONNECT on secured endpoint");
    }
    MqttSn2FrameCodec.Frame initial = MqttSn2FrameCodec.decode(incoming);
    if (initial.type() == MqttSn2PacketType.PUBWOS) {
      publishWithoutV2Connection(packet.getFromAddress(), MqttSn2PublishCodec.decode(initial));
      return;
    }
    var connect = io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.MqttSn2ConnectCodec.decode(initial);
    UDPFacadeEndPoint facade = new UDPFacadeEndPoint(
        endPoint, packet.getFromAddress(), endPoint.getServer());
    MqttSn2Protocol protocol = new MqttSn2Protocol(this, facade,
        packet.getFromAddress(), selectorTask, mqttSnConfig);
    if (policy != null) protocol.configureProtection(policy);
    UDPSessionState<Protocol> state = new UDPSessionState<>(protocol);
    state.setClientIdentifier(connect.clientIdentifier());
    currentSessions.addState(packet.getFromAddress(), state);
    try {
      protocol.start(incoming.asReadOnlyBuffer());
    } catch (IOException | RuntimeException error) {
      currentSessions.deleteState(packet.getFromAddress());
      protocol.close();
      throw error;
    }
    facade.updateReadBytes(packet.available());
  }

  private void processUnconnectedDiscovery(Packet packet) throws IOException {
    ByteBuffer wire = packet.getRawBuffer().asReadOnlyBuffer();
    if (!wire.hasRemaining()) {
      throw new IOException("Empty MQTT-SN datagram");
    }
    int first = Byte.toUnsignedInt(wire.get(wire.position()));
    int typeOffset = first == 1 ? 3 : 1;
    if (wire.remaining() > typeOffset) {
      int type = Byte.toUnsignedInt(wire.get(wire.position() + typeOffset));
      // MQTT-SN 1.2 SEARCHGW (0x01) is always exactly three octets.
      // A longer packet with type 0x01 is not a valid 1.2 discovery
      // request and must not be accepted as a legacy fallback for a
      // malformed MQTT-SN 2.0 CONNECT.
      if (type == 0x01 && wire.remaining() != 3) {
        throw new IOException("Malformed MQTT-SN 2.0 CONNECT or 1.2 SEARCHGW");
      }
    }
    MqttSn2FrameCodec.Frame frame;
    try {
      frame = MqttSn2FrameCodec.decode(wire);
    } catch (IOException malformedV2) {
      // The packet may belong to the legacy MQTT-SN 1.2 parser.
      processIncomingPacket(packet, packetFactory[0]);
      return;
    }
    if (frame.type() == MqttSn2PacketType.SEARCHGW) {
      MqttSn2GatewayCodec.decodeSearchGateway(frame);
      ByteBuffer response = MqttSn2GatewayCodec.encodeGatewayInfo(
          new MqttSn2GatewayCodec.GatewayInfo(Byte.toUnsignedInt(gatewayId), new byte[0]));
      selectorTask.push(new MqttSn2OutboundPacket(response, packet.getFromAddress(), null));
      return;
    }
    if (frame.type() == MqttSn2PacketType.PUBWOS) {
      if (protectionSession != null) {
        throw new IOException("Unprotected PUBWOS rejected by secured MQTT-SN 2.0 endpoint");
      }
      MqttSn2PublishCodec.Publish publish = MqttSn2PublishCodec.decode(frame);
      // Established sessions were dispatched before arriving here. Only
      // sessionless PUBWOS is valid on this unconnected path; predefined
      // aliases are resolved against the gateway's configured topic table.
      publishWithoutV2Connection(packet.getFromAddress(), publish);
      return;
    }
    processIncomingPacket(packet, packetFactory[0]);
  }

  private void publishWithoutV2Connection(SocketAddress sender, MqttSn2PublishCodec.Publish publish)
      throws IOException {
    if (!publish.withoutSession() || publish.qos() != 0) {
      throw new IOException("Only QoS 0 PUBWOS can publish without a virtual connection");
    }
    String topic = publish.topicType() == 3 ? publish.topicName()
        : publish.topicType() == 1
            ? resolvePredefinedTopic(sender, publish.topicAlias()) : null;
    if (topic == null || topic.isEmpty() || topic.indexOf('+') >= 0 || topic.indexOf('#') >= 0
        || topic.startsWith("$")) {
      throw new IOException("Invalid or unconfigured PUBWOS topic");
    }
    MessageBuilder builder = new MessageBuilder();
    builder.setOpaqueData(publish.payload()).setQoS(QualityOfService.AT_MOST_ONCE)
        .setRetain(publish.retained()).storeOffline(publish.retained())
        .setTransformation(transformation);
    Message message = MessageOverrides.createMessageBuilder(
        mqttSnConfig.getMessageDefaults(), builder).build();
    SessionManager.getInstance().publish(topic, message).whenComplete((result, failure) -> {
      if (failure != null) logger.log(ServerLogMessages.MQTT_SN_EXCEPTION_RASIED, failure);
    });
  }

  private UDPSessionState<Protocol> lookupByPacket(Packet packet) throws IOException {
    byte type = packet.get(1);
    if(type == MQTT_SNPacket.PINGREQ){
      for (PacketFactory factory : packetFactory) {
        MQTT_SNPacket mqttMsg = factory.parseFrame(packet);
        packet.position(0);
        if (mqttMsg instanceof PingRequest pingRequest
            && pingRequest.getClientId() != null) {
          UDPSessionState<Protocol> state = currentSessions.findAndUpdate(pingRequest.getClientId(), packet.getFromAddress(), enableAddressChanges);
          if (state != null) {
            if (state.getContext() instanceof MQTT_SNProtocol oldProtocol) {
              oldProtocol.setAddressKey(packet.getFromAddress());
            }
            return state;
          }
        }
      }
    }
    return null;
  }

  private void processIncomingPacket(Packet packet, PacketFactory factory) throws IOException {
    int len = packet.available();
    MQTT_SNPacket mqttSn = factory.parseFrame(packet);

    if (mqttSn instanceof Connect matchedConnect) {
      // Cool, so we have a new connect, so let's create a new protocol Impl and add it into our list
      // of current sessions
      UDPFacadeEndPoint facade = new UDPFacadeEndPoint(endPoint, packet.getFromAddress(), endPoint.getServer());
      MQTT_SNProtocol impl = new MQTT_SNProtocol(this, facade, packet.getFromAddress(), selectorTask, registeredTopicConfiguration, matchedConnect, mqttSnConfig);
      UDPSessionState<Protocol> state = new UDPSessionState<>(impl);
      state.setClientIdentifier( (matchedConnect).getClientId());
      currentSessions.addState(packet.getFromAddress(), state);
      facade.updateReadBytes(len);
      facade.updateWriteBytes(len);
    } else if (mqttSn instanceof SearchGateway) {
      handleSearch(packet);
    } else if (mqttSn instanceof Publish matchedPublish) {
      handlePublish(packet, matchedPublish);
    } else if (mqttSn instanceof Advertise matchedAdvertise) {
      handleAdvertise(packet, matchedAdvertise);
    } else if (mqttSn instanceof ConnAck || mqttSn instanceof io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.ConnAck) {
      Packet error = new Packet(32, false);
      mqttSn.packFrame(error);
      error.setFromAddress(packet.getFromAddress());
      error.flip();
      endPoint.sendPacket(error);
    } else {
      packet.flip();
    }
  }

  private void handleSearch(Packet packet) throws IOException {
    // This is a client asking for information about existing gateways on the network
    GatewayInfo gatewayInfo = new GatewayInfo(gatewayId);
    Packet gwInfo = new Packet(3, false);
    gatewayInfo.packFrame(gwInfo);
    gwInfo.setFromAddress(packet.getFromAddress());
    endPoint.sendPacket(gwInfo);
  }

  private void handlePublish(Packet packet, Publish publish) throws IOException {
    if (!publish.getQoS().equals(QualityOfService.MQTT_SN_REGISTERED)) {
      logger.log(ServerLogMessages.MQTT_SN_INVALID_QOS_PACKET_DETECTED, packet.getFromAddress(), publish.getQoS());
      return;
    }

    String topic = null;
    if (publish.getTopicIdType() == MQTT_SNPacket.TOPIC_PRE_DEFINED_ID) {
      topic = registeredTopicConfiguration.getTopic(packet.getFromAddress(), publish.getTopicId());
    } else if (publish.getTopicIdType() == MQTT_SNPacket.TOPIC_SHORT_NAME) {
      topic = new String(new byte[]{
          (byte) ((publish.getTopicId() >>> 8) & 0xff),
          (byte) (publish.getTopicId() & 0xff)
      }, java.nio.charset.StandardCharsets.US_ASCII);
    }

    if (topic != null) {
      logger.log(ServerLogMessages.MQTT_SN_REGISTERED_EVENT, topic);
      publishRegisteredTopic(topic, publish);
    } else {
      logger.log(ServerLogMessages.MQTT_SN_REGISTERED_EVENT_NOT_FOUND, packet.getFromAddress(), publish.getTopicId());
    }
  }

  private void handleAdvertise(Packet packet, Advertise advertise) {
    logger.log(ServerLogMessages.MQTT_SN_GATEWAY_DETECTED, advertise.getGatewayId(), packet.getFromAddress().toString());
  }

  private void publishRegisteredTopic(String topic, Publish publish) throws IOException {
    MessageBuilder messageBuilder =  new MessageBuilder();
    if (publish.retain()) {
      messageBuilder.storeOffline(true)
          .setRetain(true)
          .setTransformation(transformation)
          .setQoS(QualityOfService.AT_LEAST_ONCE); // Store for Retain
    } else {
      messageBuilder.storeOffline(false)
          .setRetain(false)
          .setTransformation(transformation)
          .setQoS(QualityOfService.AT_MOST_ONCE); // Always for these events
    }
    messageBuilder.setOpaqueData(publish.getMessage());
    try {
      Message message = MessageOverrides.createMessageBuilder(mqttSnConfig.getMessageDefaults(), messageBuilder).build();
      SessionManager.getInstance().publish(topic, message).get(1, TimeUnit.MINUTES);
    } catch (ExecutionException | InterruptedException | TimeoutException e) {
      Thread.currentThread().interrupt();
      throw new IOException(e);
    }
  }

  @Override
  public void close() {
    if (advertiserTask != null) {
      advertiserTask.stop();
    }
    currentSessions.close();
    MqttSn2IndexedCounterStore counters = ownedCounterStore;
    if (counters != null) {
      try {
        counters.close();
      } catch (IOException failure) {
        logger.log(ServerLogMessages.MQTT_SN_EXCEPTION_RASIED, failure);
      }
      ownedCounterStore = null;
    }
  }

  @Override
  public String getName() {
    return "MQTT_SN";
  }

  @Override
  public String getSessionId() {
    return "";
  }

  @Override
  public String getVersion() {
    return "2.0";
  }

  @Override
  public EndPoint getEndPoint() {
    return endPoint;
  }

  public void close(SocketAddress remoteClient) {
    currentSessions.deleteState(remoteClient);
  }

}
