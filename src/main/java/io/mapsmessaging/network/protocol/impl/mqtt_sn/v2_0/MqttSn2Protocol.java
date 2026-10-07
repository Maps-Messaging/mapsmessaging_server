/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0;

import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionContextBuilder;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.api.SubscriptionContextBuilder;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.DestinationMode;
import io.mapsmessaging.engine.destination.MessageOverrides;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.transformers.ParsedMessage;
import io.mapsmessaging.config.protocol.impl.MqttSnConfig;
import io.mapsmessaging.dto.rest.protocol.ProtocolInformationDTO;
import io.mapsmessaging.dto.rest.protocol.impl.MqttSnProtocolInformation;
import io.mapsmessaging.network.ProtocolClientConnection;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.impl.SelectorTask;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.MQTTSNInterfaceManager;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.*;
import lombok.Getter;
import lombok.Setter;

import javax.security.auth.Subject;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Independent MQTT-SN 2.0 server protocol adapter.
 *
 * <p>Does not inherit the 1.2 protocol, packet factory, state engine or
 * listener hierarchy. Implements the base transport/session boundary only.
 * AUTH, WILL, sleeping clients and QoS2 require their own v2 state-machine
 * work before full conformance can be claimed.</p>
 */
public final class MqttSn2Protocol extends Protocol {

  private final MQTTSNInterfaceManager manager;
  private final SelectorTask selectorTask;
  private final SocketAddress address;
  private final AtomicInteger aliasSequence = new AtomicInteger();
  private final Map<String, Integer> topicAliases = new HashMap<>();
  private final Map<Integer, String> aliasTopics = new HashMap<>();
  @Getter
  @Setter
  private volatile Session session;
  private volatile boolean closed;
  private final MqttSn2Lifecycle lifecycle = new MqttSn2Lifecycle();
  private String clientIdentifier = "waiting";

  public MqttSn2Protocol(MQTTSNInterfaceManager manager, EndPoint endpoint,
      SocketAddress address, SelectorTask selectorTask, MqttSnConfig config) {
    super(endpoint, address, config);
    this.manager = manager;
    this.address = address;
    this.selectorTask = selectorTask;
  }

  public String getVersion() {
    return "2.0";
  }

  public String getName() {
    return "MQTT_SN";
  }

  @Override
  public String getSessionId() {
    return session == null ? clientIdentifier : session.getName();
  }

  @Override
  public Subject getSubject() {
    return session == null ? new Subject() : session.getSecurityContext().getSubject();
  }

  @Override
  public ProtocolInformationDTO getInformation() {
    MqttSnProtocolInformation information = new MqttSnProtocolInformation();
    updateInformation(information);
    if (session != null) {
      information.setSessionInfo(session.getSessionInformation());
    }
    return information;
  }

  @Override
  public boolean processPacket(Packet packet) throws IOException {
    ByteBuffer input = packet.getRawBuffer().asReadOnlyBuffer();
    MqttSn2PacketDecoder.Decoded decoded = MqttSn2PacketDecoder.decode(input);
    lifecycle.checkAllowed(decoded.type());
    if (decoded.type() == MqttSn2PacketType.CONNECT) {
      connect((MqttSn2ConnectCodec.Connect) decoded.content());
      return true;
    }
    if (session == null) {
      throw new IOException("MQTT-SN 2.0 packet before completed CONNECT");
    }
    switch (decoded.type()) {
      case PINGREQ -> send(MqttSn2FrameCodec.encode(MqttSn2PacketType.PINGRESP,
          ByteBuffer.allocate(2).putShort((short) ((Integer) decoded.content()).intValue()).flip()), null);
      case SUBSCRIBE -> subscribe((MqttSn2SubscriptionCodec.Request) decoded.content());
      case UNSUBSCRIBE -> unsubscribe((MqttSn2SubscriptionCodec.Request) decoded.content());
      case REGISTER -> register((MqttSn2RegisterCodec.Register) decoded.content());
      case PUBLISH, PUBWOS -> publish((MqttSn2PublishCodec.Publish) decoded.content());
      case DISCONNECT -> close();
      default -> throw new IOException("Unsupported MQTT-SN 2.0 state transition: " + decoded.type());
    }
    return true;
  }

  public void start(ByteBuffer connectFrame) throws IOException {
    MqttSn2PacketDecoder.Decoded decoded = MqttSn2PacketDecoder.decode(connectFrame);
    if (decoded.type() != MqttSn2PacketType.CONNECT) {
      throw new IOException("Expected MQTT-SN 2.0 CONNECT");
    }
    connect((MqttSn2ConnectCodec.Connect) decoded.content());
  }

  private void connect(MqttSn2ConnectCodec.Connect request) throws IOException {
    if (lifecycle.state() != MqttSn2Lifecycle.State.NEW || session != null) {
      throw new IOException("Duplicate MQTT-SN 2.0 CONNECT");
    }
    if (request.will() || request.authentication() || endPoint.getConfig().getSaslConfig() != null) {
      // Fail closed until the dedicated v2 AUTH/WILL exchanges are wired.
      // Never create a broker session before completing negotiation.
      throw new IOException("MQTT-SN 2.0 WILL/AUTH negotiation not yet available");
    }
    lifecycle.begin(request.authentication() || endPoint.getConfig().getSaslConfig() != null, request.will());
    clientIdentifier = request.clientIdentifier().isEmpty()
        ? java.util.UUID.randomUUID().toString() : request.clientIdentifier();
    setKeepAlive(request.keepAliveSeconds() * 1000L);
    SessionContextBuilder builder = new SessionContextBuilder(
        clientIdentifier, new ProtocolClientConnection(this));
    builder.setResetState(request.cleanStart());
    builder.setPersistentSession(!request.cleanStart());
    builder.setReceiveMaximum(1);
    builder.setSessionExpiry(0);
    SessionManager.getInstance().createAsync(builder.build(), this).whenComplete((created, failure) -> {
      if (failure != null || created == null) {
        try {
          close();
        } catch (IOException ignored) {
          // Session establishment has already failed.
        }
        return;
      }
      try {
        if (closed || lifecycle.state() != MqttSn2Lifecycle.State.ESTABLISHING) {
          SessionManager.getInstance().close(created, false);
          return;
        }
        setSession(created);
        created.login();
        ByteBuffer response = MqttSn2ConnAckCodec.encode(new MqttSn2ConnAckCodec.ConnAck(
            created.isRestored(), request.packetIdentifier(), 0, null, null, null, null,
            request.clientIdentifier().isEmpty() ? clientIdentifier : ""));
        send(response, created::resumeState);
        lifecycle.connected();
        setConnected(true);
      } catch (IOException | RuntimeException e) {
        try {
          close();
        } catch (IOException ignored) {
          // Cleanup after failed session negotiation.
        }
      }
    });
  }

  private void subscribe(MqttSn2SubscriptionCodec.Request request) throws IOException {
    if (request.topicType() != 3) {
      throw new IOException("MQTT-SN 2.0 subscription topic alias not registered");
    }
    QualityOfService qos = qos(request.maximumQos());
    SubscriptionContextBuilder builder =
        new SubscriptionContextBuilder(request.topic(), qos.getClientAcknowledgement());
    builder.setReceiveMaximum(1);
    builder.setQos(qos);
    session.addSubscription(builder.build());
    int alias = topicAliases.computeIfAbsent(request.topic(), topic -> aliasSequence.incrementAndGet());
    aliasTopics.put(alias, request.topic());
    send(MqttSn2ReplyCodec.encodeSubAck(new MqttSn2ReplyCodec.SubAck(
        0, alias, request.packetIdentifier(), 0)), null);
  }

  private void unsubscribe(MqttSn2SubscriptionCodec.Request request) throws IOException {
    String topic = request.topicType() == 3
        ? request.topic() : aliasTopics.get(request.topicAlias());
    if (topic == null) {
      throw new IOException("Unknown subscription topic alias");
    }
    session.removeSubscription(topic);
    send(MqttSn2AckCodec.encode(new MqttSn2AckCodec.Ack(
        MqttSn2PacketType.UNSUBACK, request.packetIdentifier(), null)), null);
  }

  private void register(MqttSn2RegisterCodec.Register request) {
    int alias = topicAliases.computeIfAbsent(request.topicName(), topic -> aliasSequence.incrementAndGet());
    aliasTopics.put(alias, request.topicName());
    send(MqttSn2RegAckCodec.encode(new MqttSn2RegAckCodec.RegAck(
        0, alias, request.packetIdentifier(), 0)), null);
  }

  private void publish(MqttSn2PublishCodec.Publish publish) throws IOException {
    if (publish.qos() == 2) {
      throw new IOException("MQTT-SN 2.0 QoS2 transaction integration pending");
    }
    String topic = publish.topicType() == 3 ? publish.topicName() : aliasTopics.get(publish.topicAlias());
    if (topic == null || (topic.startsWith("$")
        && !topic.toLowerCase(java.util.Locale.ROOT)
            .startsWith(DestinationMode.SCHEMA.getNamespace()))) {
      throw new IOException("Invalid or unknown MQTT-SN 2.0 publication topic");
    }
    MessageBuilder builder = new MessageBuilder();
    builder.setOpaqueData(publish.payload()).setRetain(publish.retained())
        .setQoS(qos(publish.qos())).setTransformation(getProtocolMessageTransformation());

    // Resolve through the client's Session, never the server-wide publish API:
    // destination discovery and authorisation must retain the session identity.
    session.findDestination(topic, DestinationType.TOPIC).whenComplete((destination, failure) -> {
      boolean succeeded = false;
      if (failure == null && destination != null) {
        try {
          Message message = MessageOverrides.createMessageBuilder(
              getProtocolConfig().getMessageDefaults(), builder).build();
          destination.storeMessage(message);
          succeeded = true;
        } catch (IOException error) {
          // Send an error acknowledgement where one is required.
        }
      }
      if (!publish.withoutSession() && publish.qos() == 1) {
        send(MqttSn2AckCodec.encode(new MqttSn2AckCodec.Ack(
            MqttSn2PacketType.PUBACK, publish.packetIdentifier(), succeeded ? null : 0x80)), null);
      }
    });
  }

  private static QualityOfService qos(int value) throws IOException {
    return switch (value) {
      case 0 -> QualityOfService.AT_MOST_ONCE;
      case 1 -> QualityOfService.AT_LEAST_ONCE;
      case 2 -> QualityOfService.EXACTLY_ONCE;
      default -> throw new IOException("Invalid MQTT-SN 2.0 QoS");
    };
  }

  private void send(ByteBuffer wire, Runnable completion) {
    selectorTask.push(new MqttSn2OutboundPacket(wire, address, completion));
    sentMessage();
  }

  @Override
  public void sendMessage(MessageEvent event) {
    ParsedMessage parsed = parseOutboundMessage(event);
    if (parsed == null) return;
    int qosLevel = parsed.getMessage().getQualityOfService().getLevel();
    if (qosLevel != 0) {
      // The v2 protocol state machine must own QoS1/2 Packet Identifiers,
      // retransmission and acknowledgement callbacks (Block 2). Never
      // silently downgrade a subscription's delivery guarantee to QoS0.
      throw new UnsupportedOperationException(
          "MQTT-SN 2.0 outbound QoS" + qosLevel + " is not enabled");
    }
    MqttSn2PublishCodec.Publish publish = new MqttSn2PublishCodec.Publish(
        false, 0, false, parsed.getMessage().isRetain(), 3, 0,
        parsed.getDestinationName(), 0, parsed.getMessage().getOpaqueData());
    send(MqttSn2PublishCodec.encode(publish), event.getCompletionTask());
  }

  @Override
  public void close() throws IOException {
    if (closed) return;
    closed = true;
    lifecycle.close();
    if (session != null && !session.isClosed()) {
      SessionManager.getInstance().close(session, false);
    }
    manager.close(address);
    super.close();
  }
}
