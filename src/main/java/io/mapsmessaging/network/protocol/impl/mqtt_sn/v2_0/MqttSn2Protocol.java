/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionContextBuilder;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.api.Transaction;
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
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.ProtocolClientConnection;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.impl.SelectorTask;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.MQTTSNInterfaceManager;
import io.mapsmessaging.network.protocol.sasl.SaslAuthenticationMechanism;
import io.mapsmessaging.utilities.threads.SimpleTaskScheduler;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.*;
import lombok.Getter;
import lombok.Setter;

import javax.security.auth.Subject;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.security.sasl.Sasl;

/**
 * Independent MQTT-SN 2.0 server protocol adapter.
 *
 * <p>Does not inherit the 1.2 protocol, packet factory, state engine or
 * listener hierarchy. Implements the CSD01 flows on the shared session API.</p>
 */
public final class MqttSn2Protocol extends Protocol {

  private static final Logger AUTH_LOGGER = LoggerFactory.getLogger(MqttSn2Protocol.class);

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
  private final MqttSn2OutgoingDeliveryManager outgoing = new MqttSn2OutgoingDeliveryManager();
  private final Map<Integer, Transaction> incomingQos2 = new HashMap<>();
  private final java.util.Set<Integer> incomingQos2Pending = new java.util.HashSet<>();
  private final java.util.ArrayDeque<MessageEvent> sleepingQueue = new java.util.ArrayDeque<>();
  private String clientIdentifier = "waiting";
  private MqttSn2ConnectCodec.Connect connectRequest;
  private SaslAuthenticationMechanism sasl;
  private byte[] saslFinalResponse;
  private ScheduledFuture<?> retryTask;
  private ScheduledFuture<?> sleepTask;
  private boolean cleanWillOnClose;
  private int defaultAwakeMessages = 0xFFFF;

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
    if (decoded.type() == MqttSn2PacketType.AUTH) {
      authenticate((MqttSn2ControlCodec.Auth) decoded.content());
      return true;
    }
    if (session == null) {
      throw new IOException("MQTT-SN 2.0 packet before completed CONNECT");
    }
    switch (decoded.type()) {
      case PINGREQ -> ping((Integer) decoded.content());
      case PUBACK, PUBREC, PUBCOMP -> acknowledge((MqttSn2AckCodec.Ack) decoded.content());
      case PUBREL -> receivePubRel((MqttSn2AckCodec.Ack) decoded.content());
      case SLEEPREQ -> sleep((MqttSn2SleepCodec.SleepRequest) decoded.content());
      case SUBSCRIBE -> subscribe((MqttSn2SubscriptionCodec.Request) decoded.content());
      case UNSUBSCRIBE -> unsubscribe((MqttSn2SubscriptionCodec.Request) decoded.content());
      case REGISTER -> register((MqttSn2RegisterCodec.Register) decoded.content());
      case PUBLISH, PUBWOS -> publish((MqttSn2PublishCodec.Publish) decoded.content());
      case DISCONNECT -> disconnect((MqttSn2DisconnectCodec.Disconnect) decoded.content());
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
    var saslConfig = endPoint.getConfig().getSaslConfig();
    if (saslConfig != null && !request.authentication()) {
      sendConnectFailure(request, 0x87);
      return;
    }
    if (request.authentication() && saslConfig == null) {
      sendConnectFailure(request, 0x8C);
      return;
    }
    lifecycle.begin(request.authentication(), request.will());
    connectRequest = request;
    clientIdentifier = request.clientIdentifier().isEmpty()
        ? java.util.UUID.randomUUID().toString() : request.clientIdentifier();
    defaultAwakeMessages = request.defaultAwakeMessages() == null ? 0xFFFF : request.defaultAwakeMessages();
    setKeepAlive(request.keepAliveSeconds() * 1000L);
    if (request.authentication()) {
      if (!saslConfig.getMechanism().equals(request.authenticationMethod())) {
        sendConnectFailure(request, 0x8C);
        return;
      }
      try {
        Map<String, String> props = new HashMap<>();
        props.put(Sasl.QOP, "auth");
        sasl = new SaslAuthenticationMechanism(saslConfig.getMechanism(), saslConfig.getRealmName(),
            "mqtt-sn", props, endPoint.getConfig());
        byte[] response = sasl.challenge(request.authenticationData());
        if (sasl.complete()) {
          saslFinalResponse = response == null ? null : response.clone();
          lifecycle.authenticated(request.will());
          establishSession();
        } else {
          send(MqttSn2ControlCodec.encodeAuth(new MqttSn2ControlCodec.Auth(
              request.packetIdentifier(), 0x18, request.authenticationMethod(), response)), null);
        }
      } catch (IOException failure) {
        AUTH_LOGGER.log(ServerLogMessages.MQTT_SN_AUTHENTICATION_FAILED, failure);
        sendConnectFailure(request, 0x87);
      }
      return;
    }
    if (request.will()) lifecycle.willAccepted();
    establishSession();
  }

  private void authenticate(MqttSn2ControlCodec.Auth auth) throws IOException {
    if (sasl == null || connectRequest == null || auth.packetIdentifier() != connectRequest.packetIdentifier()
        || !connectRequest.authenticationMethod().equals(auth.mechanism()) || auth.reasonCode() != 0x18) {
      throw new IOException("Unexpected MQTT-SN 2.0 AUTH exchange");
    }
    try {
      byte[] response = sasl.challenge(auth.data());
      if (sasl.complete()) {
        saslFinalResponse = response == null ? null : response.clone();
        lifecycle.authenticated(connectRequest.will());
        establishSession();
      } else {
        send(MqttSn2ControlCodec.encodeAuth(new MqttSn2ControlCodec.Auth(
            auth.packetIdentifier(), 0x18, auth.mechanism(), response)), null);
      }
    } catch (IOException failure) {
      AUTH_LOGGER.log(ServerLogMessages.MQTT_SN_AUTHENTICATION_FAILED, failure);
      sendConnectFailure(connectRequest, 0x87);
    }
  }

  private void establishSession() throws IOException {
    MqttSn2ConnectCodec.Connect request = connectRequest;
    if (lifecycle.state() == MqttSn2Lifecycle.State.NEGOTIATING_WILL) lifecycle.willAccepted();
    SessionContextBuilder builder = new SessionContextBuilder(
        clientIdentifier, new ProtocolClientConnection(this));
    builder.setResetState(request.cleanStart());
    long expiry = request.sessionExpirySeconds() == null ? 0 : request.sessionExpirySeconds();
    builder.setPersistentSession(expiry > 0);
    builder.setReceiveMaximum(1);
    builder.setSessionExpiry(expiry);
    if (sasl != null && sasl.getUsername() != null) {
      builder.setUsername(sasl.getUsername());
      builder.isAuthorized(true);
    }
    if (request.will()) {
      String willTopic = request.willTopicType() == 3 ? request.willTopicName()
          : request.willTopicType() == 1 ? manager.resolvePredefinedTopic(address, request.willTopicAlias()) : null;
      if (willTopic == null) throw new IOException("Unknown or unsupported CONNECT Will Topic alias");
      MessageBuilder will = new MessageBuilder().setOpaqueData(request.willPayload())
          .setRetain(request.willRetained()).setQoS(qos(request.willQos()))
          .setTransformation(getProtocolMessageTransformation());
      builder.setWillTopic(willTopic).setWillMessage(
          MessageOverrides.createMessageBuilder(getProtocolConfig().getMessageDefaults(), will).build());
    }
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
          SessionManager.getInstance().close(created, true);
          return;
        }
        setSession(created);
        created.login();
        ByteBuffer response = MqttSn2ConnAckCodec.encode(new MqttSn2ConnAckCodec.ConnAck(
            created.isRestored(), request.packetIdentifier(), 0, request.sessionExpirySeconds(), null,
            request.authentication() ? request.authenticationMethod() : null, saslFinalResponse,
            request.clientIdentifier().isEmpty() ? clientIdentifier : ""));
        send(response, created::resumeState);
        lifecycle.connected();
        setConnected(true);
        if (sasl != null) {
          sasl.close();
          sasl = null;
        }
      } catch (IOException | RuntimeException e) {
        try {
          close();
        } catch (IOException ignored) {
          // Cleanup after failed session negotiation.
        }
      }
    });
  }

  private void sendConnectFailure(MqttSn2ConnectCodec.Connect request, int reason) {
    send(MqttSn2ConnAckCodec.encode(new MqttSn2ConnAckCodec.ConnAck(false,
        request.packetIdentifier(), reason, null, null, null, null, request.clientIdentifier())), () -> {
      try { close(); } catch (IOException ignored) { }
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
    String topic = publish.topicType() == 3 ? publish.topicName() : aliasTopics.get(publish.topicAlias());
    if (topic == null || (topic.startsWith("$")
        && !topic.toLowerCase(java.util.Locale.ROOT)
            .startsWith(DestinationMode.SCHEMA.getNamespace()))) {
      throw new IOException("Invalid or unknown MQTT-SN 2.0 publication topic");
    }
    MessageBuilder builder = new MessageBuilder();
    builder.setOpaqueData(publish.payload()).setRetain(publish.retained())
        .setQoS(qos(publish.qos())).setTransformation(getProtocolMessageTransformation());

    if (publish.qos() == 2) {
      synchronized (incomingQos2) {
        if (incomingQos2Pending.contains(publish.packetIdentifier())) {
          // The original asynchronous destination lookup has not finished.
          // Never begin another transaction or acknowledge an unpersisted packet.
          return;
        }
        if (qos2Transaction(publish.packetIdentifier()) != null) {
          send(MqttSn2AckCodec.encode(new MqttSn2AckCodec.Ack(
              MqttSn2PacketType.PUBREC, publish.packetIdentifier(), null)), null);
          return;
        }
        incomingQos2Pending.add(publish.packetIdentifier());
      }
    }
    // Resolve through the client's Session, never the server-wide publish API:
    // destination discovery and authorisation must retain the session identity.
    session.findDestination(topic, DestinationType.TOPIC).whenComplete((destination, failure) -> {
      boolean succeeded = false;
      try {
        if (!closed && failure == null && destination != null) {
          Message message = MessageOverrides.createMessageBuilder(
              getProtocolConfig().getMessageDefaults(), builder).build();
          if (publish.qos() == 2) {
            Transaction transaction = session.startTransaction(
                qos2TransactionName(publish.packetIdentifier()));
            try {
              transaction.add(destination, message);
              synchronized (incomingQos2) {
                incomingQos2.put(publish.packetIdentifier(), transaction);
              }
            } catch (IOException | RuntimeException error) {
              session.closeTransaction(transaction);
              throw error;
            }
          } else {
            destination.storeMessage(message);
          }
          succeeded = true;
        }
      } catch (IOException | RuntimeException error) {
        // Failure is reported by the appropriate negative acknowledgement.
      } finally {
        if (publish.qos() == 2) {
          synchronized (incomingQos2) {
            incomingQos2Pending.remove(publish.packetIdentifier());
          }
        }
      }
      if (!closed && !publish.withoutSession() && publish.qos() > 0) {
        MqttSn2PacketType type = publish.qos() == 1
            ? MqttSn2PacketType.PUBACK : MqttSn2PacketType.PUBREC;
        send(MqttSn2AckCodec.encode(new MqttSn2AckCodec.Ack(
            type, publish.packetIdentifier(), succeeded ? null : 0x80)), null);
      }
    });
  }

  private void receivePubRel(MqttSn2AckCodec.Ack ack) throws IOException {
    Transaction transaction;
    synchronized (incomingQos2) {
      if (incomingQos2Pending.contains(ack.packetIdentifier())) {
        throw new IOException("PUBREL received before PUBREC transaction is ready");
      }
      transaction = qos2Transaction(ack.packetIdentifier());
      if (transaction == null) {
        send(MqttSn2AckCodec.encode(new MqttSn2AckCodec.Ack(
            MqttSn2PacketType.PUBCOMP, ack.packetIdentifier(), 0x92)), null);
        return;
      }
      incomingQos2.remove(ack.packetIdentifier());
    }
    if (transaction != null) {
      try {
        transaction.commit();
      } finally {
        session.closeTransaction(transaction);
      }
    }
    send(MqttSn2AckCodec.encode(new MqttSn2AckCodec.Ack(
        MqttSn2PacketType.PUBCOMP, ack.packetIdentifier(), null)), null);
  }

  private Transaction qos2Transaction(int packetIdentifier) {
    synchronized (incomingQos2) {
      Transaction transaction = incomingQos2.get(packetIdentifier);
      return transaction == null && session != null
          ? session.getTransaction(qos2TransactionName(packetIdentifier)) : transaction;
    }
  }

  private String qos2TransactionName(int packetIdentifier) {
    return session.getName() + ":" + packetIdentifier;
  }

  private void acknowledge(MqttSn2AckCodec.Ack ack) throws IOException {
    ByteBuffer next = outgoing.acknowledge(ack, System.currentTimeMillis());
    if (retryTask != null) retryTask.cancel(false);
    if (next != null) {
      send(next, null);
    }
    if (outgoing.hasInFlightDelivery()) {
      scheduleRetry();
    }
    if (lifecycle.state() == MqttSn2Lifecycle.State.AWAKE && !outgoing.hasInFlightDelivery()) {
      drainSleepingQueue();
    }
  }

  private void scheduleRetry() {
    if (retryTask != null) retryTask.cancel(false);
    retryTask = SimpleTaskScheduler.getInstance().schedule(() -> {
      if (closed) return;
      ByteBuffer retry = outgoing.retryExpired(System.currentTimeMillis());
      if (retry != null) {
        send(retry, null);
        scheduleRetry();
      } else if (outgoing.isRetryExhausted()) {
        try { close(); } catch (IOException ignored) { }
      } else if (outgoing.hasInFlightDelivery()) {
        scheduleRetry();
      }
    }, 10, TimeUnit.SECONDS);
  }

  private void sleep(MqttSn2SleepCodec.SleepRequest request) throws IOException {
    lifecycle.sleep();
    send(MqttSn2SleepCodec.encodeResponse(new MqttSn2SleepCodec.SleepResponse(
        request.packetIdentifier(), null, 0)), null);
    if (sleepTask != null) sleepTask.cancel(false);
    sleepTask = SimpleTaskScheduler.getInstance().schedule(() -> {
      try { close(); } catch (IOException ignored) { }
    }, Math.max(1L, request.durationSeconds() * 1_500L), TimeUnit.MILLISECONDS);
  }

  private void ping(int packetIdentifier) throws IOException {
    if (lifecycle.state() == MqttSn2Lifecycle.State.ASLEEP) {
      lifecycle.wake();
      if (sleepTask != null) sleepTask.cancel(false);
      if (defaultAwakeMessages == 0) {
        lifecycle.sleepAgain();
        send(MqttSn2ControlCodec.encodePingResponse(packetIdentifier, queueSize()), null);
      } else {
        wakePacketIdentifier = packetIdentifier;
        awakeBudget = defaultAwakeMessages;
        drainSleepingQueue();
      }
      return;
    }
    send(MqttSn2ControlCodec.encodePingResponse(packetIdentifier, null), null);
  }

  private int wakePacketIdentifier;
  private int awakeBudget;

  private int queueSize() {
    synchronized (sleepingQueue) { return Math.min(0xFFFF, sleepingQueue.size()); }
  }

  private void drainSleepingQueue() {
    int sent = 0;
    while (sent < awakeBudget && !outgoing.hasInFlightDelivery()) {
      MessageEvent event;
      synchronized (sleepingQueue) { event = sleepingQueue.pollFirst(); }
      if (event == null) break;
      awakeBudget--;
      sent++;
      deliver(event);
    }
    if (!outgoing.hasInFlightDelivery()) {
      try {
        lifecycle.sleepAgain();
        send(MqttSn2ControlCodec.encodePingResponse(wakePacketIdentifier, queueSize()), null);
      } catch (IOException ignored) { }
    }
  }

  private void disconnect(MqttSn2DisconnectCodec.Disconnect disconnect) throws IOException {
    cleanWillOnClose = disconnect.reasonCode() == null || disconnect.reasonCode() == 0;
    close();
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
    if (lifecycle.state() == MqttSn2Lifecycle.State.ASLEEP) {
      synchronized (sleepingQueue) { sleepingQueue.addLast(event); }
      return;
    }
    deliver(event);
  }

  private void deliver(MessageEvent event) {
    ParsedMessage parsed = parseOutboundMessage(event);
    if (parsed == null) return;
    int qosLevel = parsed.getMessage().getQualityOfService().getLevel();
    if (qosLevel == 0) {
      MqttSn2PublishCodec.Publish publish = new MqttSn2PublishCodec.Publish(
          false, 0, false, parsed.getMessage().isRetain(), 3, 0,
          parsed.getDestinationName(), 0, parsed.getMessage().getOpaqueData());
      send(MqttSn2PublishCodec.encode(publish), event.getCompletionTask());
      return;
    }
    ByteBuffer frame = outgoing.enqueue(parsed.getDestinationName(), parsed.getMessage().getOpaqueData(),
        qosLevel, parsed.getMessage().isRetain(), event.getCompletionTask(), System.currentTimeMillis());
    if (frame != null) {
      send(frame, null);
      scheduleRetry();
    }
  }

  @Override
  public void close() throws IOException {
    if (closed) return;
    closed = true;
    lifecycle.close();
    if (retryTask != null) retryTask.cancel(false);
    if (sleepTask != null) sleepTask.cancel(false);
    if (sasl != null) sasl.close();
    if (session != null && !session.isClosed()) {
      SessionManager.getInstance().close(session, cleanWillOnClose);
    }
    manager.close(address);
    super.close();
  }
}
