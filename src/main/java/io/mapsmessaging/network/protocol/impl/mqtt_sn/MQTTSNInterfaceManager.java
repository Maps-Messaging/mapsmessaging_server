/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
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
import io.mapsmessaging.network.protocol.ProtocolMessageTransformation;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.MQTT_SNProtocol;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.*;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.MQTT_SNProtocolV2;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.PacketFactoryV2;
import io.mapsmessaging.network.protocol.transformation.TransformationManager;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SelectionKey;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

// The protocol is MQTT_SN so it makes sense
@java.lang.SuppressWarnings("squid:S00101")
public class MQTTSNInterfaceManager implements SelectorCallback {

  private final Logger logger;
  private final SelectorTask selectorTask;
  private final EndPoint endPoint;
  private final UDPSessionManager<MQTT_SNProtocol> currentSessions;
  private final PacketFactory[] packetFactory;
  private final AdvertiserTask advertiserTask;
  private final byte gatewayId;
  private final RegisteredTopicConfiguration registeredTopicConfiguration;
  private final ProtocolMessageTransformation transformation;

  private final boolean enablePortChanges;
  private final boolean enableAddressChanges;
  private final boolean advertiseGateway;

  private final MqttSnConfig mqttSnConfig;


  public MQTTSNInterfaceManager(byte gatewayId, SelectorTask selectorTask, EndPoint endPoint) {
    logger = LoggerFactory.getLogger("MQTT-SN Protocol on " + endPoint.getName());
    this.gatewayId = gatewayId;
    this.selectorTask = selectorTask;
    advertiserTask = null;
    this.endPoint = endPoint;
    mqttSnConfig = (MqttSnConfig) endPoint.getConfig().getProtocolConfig("mqtt-sn");
    long timeout = mqttSnConfig.getIdleSessionTimeout();
    enablePortChanges = mqttSnConfig.isEnablePortChanges();
    enableAddressChanges = mqttSnConfig.isEnableAddressChanges();
    advertiseGateway = mqttSnConfig.isAdvertiseGateway();
    currentSessions = new UDPSessionManager<>(timeout);
    packetFactory = new PacketFactory[2];
    packetFactory[0] = new PacketFactory();
    packetFactory[1] = new PacketFactoryV2();
    transformation = TransformationManager.getInstance().getTransformation(
        endPoint.getProtocol(),
        endPoint.getName(),
        "mqtt-sn",
        "<registered>"
    );

    registeredTopicConfiguration = new RegisteredTopicConfiguration(mqttSnConfig);
  }

  public MQTTSNInterfaceManager(InterfaceInformation info, EndPoint endPoint, byte gatewayId) throws IOException {
    logger = LoggerFactory.getLogger("MQTT-SN Protocol on " + endPoint.getName());
    this.endPoint = endPoint;
    this.gatewayId = gatewayId;
    mqttSnConfig = (MqttSnConfig) endPoint.getConfig().getProtocolConfig("mqtt-sn");
    long timeout = mqttSnConfig.getIdleSessionTimeout();
    enablePortChanges = mqttSnConfig.isEnablePortChanges();
    enableAddressChanges = mqttSnConfig.isEnableAddressChanges();
    advertiseGateway = mqttSnConfig.isAdvertiseGateway();

    currentSessions = new UDPSessionManager<>(timeout);
    packetFactory = new PacketFactory[2];
    packetFactory[0] = new PacketFactory();
    packetFactory[1] = new PacketFactoryV2();

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
        "mqtt-sn",
        "<registered>"
    );
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
    UDPSessionState<MQTT_SNProtocol> state = currentSessions.getState(packet.getFromAddress());
    if(state == null && enablePortChanges){
      state = lookupByPacket(packet);
    }

    if (state != null && state.getContext() != null) {
      MQTT_SNProtocol protocol = state.getContext();
      // OK we have an existing protocol, so simply hand over the packet for processing
      protocol.processPacket(packet);
    } else {
      int offset = 0;
      if (packet.get(0) == 1) {
        offset = 2;
      }
      int version = -1;
      boolean isConnect = packet.get(1 + offset) == MQTT_SNPacket.CONNECT;
      if (isConnect && (packet.get(2 + offset) & 0b11111000) == 0) {
        version = packet.get(3 + offset);
      }
      //
      // OK so this is either a new connection request or an admin request
      //
      PacketFactory factory = packetFactory[0];
      if (version == 2) {
        factory = packetFactory[1];
      }

      try {
        processIncomingPacket(packet, factory);
      } catch (IOException ioException) {
        logger.log( ServerLogMessages.MQTT_SN_EXCEPTION_RASIED, ioException);
      }
    }
    selectorTask.register(SelectionKey.OP_READ);
    return true;
  }

  private UDPSessionState<MQTT_SNProtocol> lookupByPacket(Packet packet) throws IOException {
    byte type = packet.get(1);
    if(type == MQTT_SNPacket.PINGREQ){
      for (PacketFactory factory : packetFactory) {
        MQTT_SNPacket mqttMsg = factory.parseFrame(packet);
        packet.position(0);
        if (mqttMsg instanceof PingRequest) {
          PingRequest pingRequest = (PingRequest) mqttMsg;
          if (pingRequest.getClientId() != null) {
            UDPSessionState<MQTT_SNProtocol> state = currentSessions.findAndUpdate(pingRequest.getClientId(), packet.getFromAddress(), enableAddressChanges);
            if (state != null) {
              state.getContext().setAddressKey(packet.getFromAddress());
              return state;
            }
          }
        }
      }
    }
    return null;
  }

  private void processIncomingPacket(Packet packet, PacketFactory factory) throws IOException {
    int len = packet.available();
    MQTT_SNPacket mqttSn = factory.parseFrame(packet);

    if (mqttSn instanceof Connect) {
      // Cool, so we have a new connect, so let's create a new protocol Impl and add it into our list
      // of current sessions
      UDPFacadeEndPoint facade = new UDPFacadeEndPoint(endPoint, packet.getFromAddress(), endPoint.getServer());
      MQTT_SNProtocol impl = new MQTT_SNProtocol(this, facade, packet.getFromAddress(), selectorTask, registeredTopicConfiguration, (Connect) mqttSn, mqttSnConfig);
      UDPSessionState<MQTT_SNProtocol> state = new UDPSessionState<>(impl);
      state.setClientIdentifier( ((Connect) mqttSn).getClientId());
      currentSessions.addState(packet.getFromAddress(), state);
      facade.updateReadBytes(len);
      facade.updateWriteBytes(len);
    } else if (mqttSn instanceof io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.Connect) {
      // Cool, so we have a new connect, so let's create a new protocol Impl and add it into our list
      // of current sessions
      UDPFacadeEndPoint facade = new UDPFacadeEndPoint(endPoint, packet.getFromAddress(), endPoint.getServer());
      io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.Connect connectV2 = (io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.Connect) mqttSn;
      MQTT_SNProtocol impl = new MQTT_SNProtocolV2(this, facade, packet.getFromAddress(), selectorTask, registeredTopicConfiguration, connectV2, mqttSnConfig);
      UDPSessionState<MQTT_SNProtocol> state = new UDPSessionState<>(impl);
      state.setClientIdentifier(connectV2.getClientId());
      currentSessions.addState(packet.getFromAddress(), state);
      facade.updateReadBytes(len);
      facade.updateWriteBytes(len);
    } else if (mqttSn instanceof SearchGateway) {
      handleSearch(packet);
    } else if (mqttSn instanceof Publish) {
      handlePublish(packet, (Publish) mqttSn);
    } else if (mqttSn instanceof Advertise) {
      handleAdvertise(packet, (Advertise) mqttSn);
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
    if (publish.getQoS().equals(QualityOfService.MQTT_SN_REGISTERED)) {
      String topic = registeredTopicConfiguration.getTopic(packet.getFromAddress(), publish.getTopicId());
      if (topic != null) {
        logger.log(ServerLogMessages.MQTT_SN_REGISTERED_EVENT, topic);
        publishRegisteredTopic(topic, publish);
      } else {
        logger.log(ServerLogMessages.MQTT_SN_REGISTERED_EVENT_NOT_FOUND, packet.getFromAddress(), publish.getTopicId());
      }
    } else {
      logger.log(ServerLogMessages.MQTT_SN_INVALID_QOS_PACKET_DETECTED, packet.getFromAddress(), publish.getQoS());
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
