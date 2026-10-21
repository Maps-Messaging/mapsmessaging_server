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

package io.mapsmessaging.network.protocol.impl.mqtt;

import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.api.SubscriptionContextBuilder;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.transformers.InterServerTransformation;
import io.mapsmessaging.api.transformers.ParsedMessage;
import io.mapsmessaging.dto.rest.analytics.StatisticsConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.MqttConfigDTO;
import io.mapsmessaging.dto.rest.protocol.ProtocolInformationDTO;
import io.mapsmessaging.dto.rest.protocol.impl.MqttProtocolInformation;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.logging.ThreadContext;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.ServerPacket;
import io.mapsmessaging.network.io.impl.SelectorTask;
import io.mapsmessaging.network.protocol.EndOfBufferException;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.impl.mqtt.listeners.PacketListener;
import io.mapsmessaging.network.protocol.impl.mqtt.listeners.PacketListenerFactory;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.*;
import io.mapsmessaging.network.protocol.impl.mqtt5.ConnectCompletionTimeout;
import io.mapsmessaging.selector.operators.ParserExecutor;
import io.mapsmessaging.utilities.filtering.NamespaceFilters;
import io.mapsmessaging.utilities.threads.SimpleTaskScheduler;
import lombok.Getter;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.security.auth.Subject;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


@java.lang.SuppressWarnings("DuplicatedBlocks")
public class MQTTProtocol extends Protocol implements RemoteSubscriptionProtocol {

  private final Logger logger;
  private final PacketFactory packetFactory;
  private final PacketListenerFactory packetListenerFactory;
  private final SelectorTask selectorTask;
  private final MqttKeepAliveManager keepAliveManager;
  @Getter
  private final PacketIdManager packetIdManager;
  private record PubRelGate(io.mapsmessaging.storage.delivery.PacketIdStore.Delivery delivery,
                            java.util.concurrent.CompletableFuture<Void> future, java.util.concurrent.atomic.AtomicBoolean ready) {}
  private final java.util.Map<Integer, PubRelGate> pubRelAcknowledgements = new java.util.concurrent.ConcurrentHashMap<>();
  private final long maxBufferSize;

  @Getter
  private final MqttConfigDTO mqttConfig;

  private volatile boolean closed;
  @Getter
  private Session session;

  @Getter
  private final RemoteSubscriptionTracker remoteSubscriptionTracker;

  @Getter
  private volatile String connectionSessionId;

  private ScheduledFuture<?> connectionTimeOut = null;
  private volatile boolean rejectingConnection;

  public MQTTProtocol(EndPoint endPoint) throws IOException {
    super(endPoint, endPoint.getConfig().getProtocolConfig("mqtt"));
    System.err.println("MQTT 3.1.1 Protocol on " + endPoint.getName());
    logger = LoggerFactory.getLogger("MQTT 3.1.1 Protocol on " + endPoint.getName());
    ThreadContext.put("endpoint", endPoint.getName());
    ThreadContext.put("protocol", getName());
    ThreadContext.put("version", getVersion());
    logger.log(ServerLogMessages.MQTT_START);
    mqttConfig = (MqttConfigDTO) protocolConfig;
    maxBufferSize =  mqttConfig.getMaximumBufferSize();
    keepAlive = mqttConfig.getMaxServerKeepAlive() * 1000L;
    keepAliveManager = new MqttKeepAliveManager();
    selectorTask = new SelectorTask(this, endPoint.getConfig().getEndPointConfig());
    packetListenerFactory = new PacketListenerFactory();
    packetFactory = new PacketFactory(this);
    closed = false;
    packetIdManager = new PacketIdManager();
    remoteSubscriptionTracker = new RemoteSubscriptionTracker(packetIdManager);
  }

  public MQTTProtocol(EndPoint endPoint, Packet packet) throws IOException {
    this(endPoint);
    processPacket(packet);
    selectorTask.getReadTask().pushOutstandingData(packet);
  }

  @Override
  public void close() throws IOException {
    if (closed) {
      return;
    }
    closed = true;
    pubRelAcknowledgements.values().forEach(pending -> pending.future().completeExceptionally(new IOException("Delivery session is closed")));
    pubRelAcknowledgements.clear();
    remoteSubscriptionTracker.close();
    packetIdManager.close();
    if(selectorTask.isOpen()){
      selectorTask.close();
    }
    if(session != null && !session.isClosed()){
      SessionManager.getInstance().close(session, false);
    }
    super.close();
  }

  @Override
  public Subject getSubject() {
    if(session != null) {
      return session.getSecurityContext().getSubject();
    }
    return new Subject();
  }

  @Override
  public void connect(@NonNull @NotNull String sessionId, String username, String password ) throws IOException {
    connect(sessionId, username, password, null);
  }

  public void connect(@NonNull @NotNull String sessionId, String username, String password, Publish willMsg) throws IOException {
    Connect connect = new Connect();
    connect.setSessionId(sessionId);
    connectionSessionId = sessionId;
    connect.setCleanSession(false);
    connect.setKeepAlive((int) keepAlive);
    if (username != null) {
      connect.setUsername(username);
      connect.setPassword(password.trim().toCharArray());
    }
    if(willMsg != null) {
      connect.setWillMsg(willMsg.getPayload());
      connect.setWillRetain(willMsg.isRetain());
      connect.setWillQOS(willMsg.getQos());
      connect.setWillFlag(true);
      connect.setWillTopic(willMsg.getDestinationName());
    }
    connectionTimeOut = SimpleTaskScheduler.getInstance().schedule(new ConnectCompletionTimeout(this), 1, TimeUnit.MINUTES);
    writeFrame(connect);
    registerRead();
    completedConnection();
  }


  @Override
  public void setConnected(boolean connected) {
    super.setConnected(connected);
    if (connectionTimeOut != null) {
      connectionTimeOut.cancel(true);
      connectionTimeOut = null;
    }
  }



  @Override
  public void subscribeRemote(@NonNull @NotNull String resource, @NonNull @NotNull String mappedResource, @NonNull @NotNull QualityOfService qos, @Nullable ParserExecutor parser, @Nullable InterServerTransformation transformer, StatisticsConfigDTO statistics, Map<String, Object> linkProperties) throws IOException {
    super.subscribeRemote(resource,mappedResource, qos, parser, transformer,statistics, linkProperties);
    Subscribe subscribe = new Subscribe();
    subscribe.setMessageId(remoteSubscriptionTracker.register(List.of(resource)));
    subscribe.getSubscriptionList().add(new SubscriptionInfo(resource, qos));
    writeFrame(subscribe);
  }

  @Override
  public void unsubscribeRemote(@NonNull @NotNull String resource){
    Unsubscribe unsubscribe = new Unsubscribe(List.of(resource));
    unsubscribe.setMessageId(packetIdManager.nextControlPacketIdentifier());
    writeFrame(unsubscribe);
  }

  @Override
  public void subscribeLocal(@NonNull @NotNull String resource, @NonNull @NotNull String mappedResource, @NonNull @NotNull QualityOfService qos, @Nullable String selector, @Nullable InterServerTransformation transformer, @Nullable NamespaceFilters namespaceFilters, StatisticsConfigDTO statistics, Map<String, Object> linkProperties)
      throws IOException {
    super.subscribeLocal(resource, mappedResource, qos, selector, transformer, namespaceFilters, statistics, linkProperties);
    SubscriptionContextBuilder builder = createSubscriptionContextBuilder(resource, selector, qos, 1024);
    session.addSubscription(builder.build());
  }

  @Override
  public void unsubscribeLocal(@NonNull @NotNull String resource){
    session.removeSubscription(resource);
  }

  @Override
  public String getVersion() {
    return "3.1.1";
  }

  public void registerRead() throws IOException {
    selectorTask.register(SelectionKey.OP_READ);
  }

  @Override
  public String getSessionId() {
    if (session == null) {
      return "waiting";
    }
    return session.getName();
  }

  @Override
  public void setSession(Session session) {
    if (this.session != session) pubRelAcknowledgements.clear();
    this.session = session;
    try {
      packetIdManager.bindStore(session.getOrCreatePacketIdStore(65535));
    } catch (IOException | java.io.UncheckedIOException error) {
      try { endPoint.close(); } catch (IOException closeError) { error.addSuppressed(closeError); }
      if (error instanceof java.io.UncheckedIOException unchecked) {
        throw unchecked;
      }
      throw new java.io.UncheckedIOException((IOException) error);
    }
    completedConnection();
  }

  /** Called after CONNACK and before waking engine deliveries already accepted at PUBREC. */
  public boolean isDeliverySessionActive(Session candidate) {
    return !closed && session == candidate && candidate != null && !candidate.isClosed();
  }

  public boolean isPubRelReady(int packetId) {
    PubRelGate gate = pubRelAcknowledgements.get(packetId);
    return gate != null && gate.ready().get();
  }

  public void completePubRel(int packetId) {
    pubRelAcknowledgements.remove(packetId);
  }

  /** PUBREL is safe only once its exact broker interest has been durably acknowledged. */
  public java.util.concurrent.CompletableFuture<Void> sendPubRelAfterBrokerAck(int packetId) {
    Session activeSession = session;
    if (!isDeliverySessionActive(activeSession)) {
      return java.util.concurrent.CompletableFuture.failedFuture(new IOException("Delivery session is closed"));
    }
    io.mapsmessaging.storage.delivery.PacketIdStore.Delivery delivery = packetIdManager.getDelivery(packetId);
    if (delivery == null) {
      return java.util.concurrent.CompletableFuture.completedFuture(null);
    }
    PubRelGate gate;
    for (;;) {
      PubRelGate existing = pubRelAcknowledgements.get(packetId);
      if (existing != null && existing.delivery().equals(delivery)) {
        if (existing.ready().get() && isDeliverySessionActive(activeSession)) writeFrame(new io.mapsmessaging.network.protocol.impl.mqtt.packet.PubRel(packetId));
        return existing.future();
      }
      gate = new PubRelGate(delivery, new java.util.concurrent.CompletableFuture<>(), new java.util.concurrent.atomic.AtomicBoolean());
      if (existing == null) {
        if (pubRelAcknowledgements.putIfAbsent(packetId, gate) == null) break;
      } else if (pubRelAcknowledgements.replace(packetId, existing, gate)) {
        existing.future().complete(null);
        break;
      }
    }
    PubRelGate ownedGate = gate;
    try {
      activeSession.ackRecoveredDelivery(delivery).whenComplete((ignored, error) -> {
        try {
          if (pubRelAcknowledgements.get(packetId) != ownedGate || !delivery.equals(packetIdManager.getDelivery(packetId)) || !packetIdManager.isAwaitingPubComp(packetId)) {
            pubRelAcknowledgements.remove(packetId, ownedGate);
            ownedGate.future().complete(null);
            return;
          }
          if (error == null && !isDeliverySessionActive(activeSession)) error = new IOException("Delivery session is closed");
          if (error == null) {
            ownedGate.ready().set(true);
            writeFrame(new io.mapsmessaging.network.protocol.impl.mqtt.packet.PubRel(packetId));
            ownedGate.future().complete(null);
            return;
          }
        } catch (RuntimeException callbackError) {
          error = callbackError;
        }
        ownedGate.ready().set(false);
        ownedGate.future().completeExceptionally(error);
        if (!closed && session == activeSession) {
          try { endPoint.close(); } catch (IOException closeError) { error.addSuppressed(closeError); }
        }
      });
    } catch (RuntimeException error) {
      ownedGate.future().completeExceptionally(error);
      if (!closed && session == activeSession) {
        try { endPoint.close(); } catch (IOException closeError) { error.addSuppressed(closeError); }
      }
    }
    return ownedGate.future();
  }

  public java.util.concurrent.CompletableFuture<Void> recoverOutgoingDeliveries() {
    Session activeSession = session;
    if (!isDeliverySessionActive(activeSession)) {
      return java.util.concurrent.CompletableFuture.failedFuture(new IOException("Delivery session is closed"));
    }
    java.util.List<java.util.concurrent.CompletableFuture<Void>> recovered = new java.util.ArrayList<>();
    for (int packetId : packetIdManager.getAwaitingPubCompPacketIds()) recovered.add(sendPubRelAfterBrokerAck(packetId));
    return java.util.concurrent.CompletableFuture.allOf(recovered.toArray(java.util.concurrent.CompletableFuture[]::new));
  }

  public boolean processPacket(Packet packet) throws IOException {
    if (rejectingConnection) {
      packet.position(packet.limit());
      return true;
    }
    int pos = packet.position();
    try {
      boolean resume = false;
      while (packet.hasRemaining()) {
        resume = handleMQTTEvent(packet);
        pos = packet.position();
        if (rejectingConnection) {
          packet.position(packet.limit());
          return true;
        }
      }
      if (resume) {
        registerRead();
      }
    } catch (EndOfBufferException eobe) {
      packet.position(pos);
      registerRead();
      return false;
    } catch (MalformedException | IOException | java.io.UncheckedIOException e) {
      logger.log(ServerLogMessages.MALFORMED, e, e.getMessage());
      endPoint.close();
    }
    return true;
  }

  protected boolean handleMQTTEvent(Packet packet) throws MalformedException, EndOfBufferException {

    MQTTPacket mqtt = packetFactory.parseFrame(packet);
    if (mqtt != null) {
      if (logger.isInfoEnabled()) {
        logger.log(ServerLogMessages.RECEIVE_PACKET, mqtt);
      }
      EndPoint.totalReceived.increment();
      PacketListener packetListener = packetListenerFactory.getListener(mqtt.getControlPacketId());
      MQTTPacket response = packetListener.handlePacket(mqtt, session, endPoint, this);
      if (mqtt instanceof Connect connect && !connect.hasSupportedProtocolLevel()) {
        rejectingConnection = true;
      }
      if (response != null) {
        EndPoint.totalSent.increment();
        if (logger.isInfoEnabled()) {
          logger.log(ServerLogMessages.RESPONSE_PACKET, response);
        }
        selectorTask.push(response);
      }
      return packetListener.resumeRead();
    }
    return true;
  }

  @Override
  public void sendKeepAlive() {
    Session activeSession = session;
    if (closed || activeSession == null || activeSession.isClosed()) {
      return;
    }
    try {
      ThreadContext.put("session", activeSession.getName());
      ThreadContext.put("protocol", getName());
      ThreadContext.put("endpoint", endPoint.getName());
      ThreadContext.put("version", getVersion());
      long now = System.currentTimeMillis();
      MqttKeepAliveManager.Action action = endPoint.isClient()
          ? keepAliveManager.checkClient(now, endPoint.getLastWrite(), keepAlive)
          : keepAliveManager.checkServer(now, endPoint.getLastRead(), keepAlive);
      if (action == MqttKeepAliveManager.Action.SEND_PING) {
        logger.log(ServerLogMessages.MQTT_KEEPALIVE_TIMOUT, keepAlive);
        writeFrame(new PingReq());
      } else if (action == MqttKeepAliveManager.Action.DISCONNECT) {
        logger.log(ServerLogMessages.MQTT_DISCONNECT_TIMEOUT);
        closeAfterKeepAliveTimeout(activeSession);
      }
    } finally {
      ThreadContext.clearMap();
    }
  }

  private void closeAfterKeepAliveTimeout(Session activeSession) {
    closed = true;
    try {
      if (selectorTask.isOpen()) {
        selectorTask.close();
      }
    } catch (RuntimeException exception) {
      logger.log(ServerLogMessages.END_POINT_CLOSE_EXCEPTION, exception);
    }
    try {
      SessionManager.getInstance().closeAsync(activeSession, false).whenComplete((closedSession, exception) -> {
        if (exception != null) {
          logger.log(ServerLogMessages.SESSION_CLOSE_EXCEPTION, exception);
        }
      });
    } catch (RuntimeException exception) {
      logger.log(ServerLogMessages.SESSION_CLOSE_EXCEPTION, exception);
    }
    try {
      super.close();
    } catch (IOException | RuntimeException exception) {
      logger.log(ServerLogMessages.END_POINT_CLOSE_EXCEPTION, exception);
    }
  }

  @Override
  public long getKeepAliveTaskInterval() {
    return keepAliveManager.getCheckInterval(keepAlive, endPoint.isClient());
  }

  public void pingResponseReceived() {
    keepAliveManager.pingResponseReceived();
  }

  public String getName() {
    return "MQTT";
  }

  @Override
  public void sendMessage(@NotNull @NonNull MessageEvent messageEvent) {
    try {
      sendDelivery(messageEvent);
    } catch (java.io.UncheckedIOException error) {
      try { endPoint.close(); } catch (IOException closeError) { error.addSuppressed(closeError); }
    }
  }

  private void sendDelivery(MessageEvent messageEvent) {
    Message msg = messageEvent.getMessage();
    SubscriptionContext subInfo = messageEvent.getSubscription().getContext();
    if (subInfo == null) {
      packetIdManager.releaseSendSlot(messageEvent.getSubscription());
      messageEvent.getCompletionTask().run();
      return;
    }
    if (reconcileRecoveredDelivery(messageEvent.getSubscription(), msg.getIdentifier(), messageEvent.getCompletionTask())) {
      return;
    }
    QualityOfService qos = subInfo.getQualityOfService();
    Integer existingId = packetIdManager.getPacketIdentifier(messageEvent.getSubscription(), msg.getIdentifier());
    if (existingId != null && packetIdManager.getPacketQos(existingId) > 0) qos = QualityOfService.getInstance(packetIdManager.getPacketQos(existingId));

    ParsedMessage parsedMessage = parseOutboundMessage(messageEvent, subInfo);
    if(parsedMessage == null) {
      packetIdManager.discard(messageEvent.getSubscription(), msg.getIdentifier());
      packetIdManager.releaseSendSlot(messageEvent.getSubscription());
      return;
    }

    int packetId = 0;
    if (qos.isSendPacketId()) {
      packetId = packetIdManager.nextPacketIdentifier(messageEvent.getSubscription(), msg.getIdentifier(), qos.getLevel());
    }
    String topicName = parsedMessage.getDestinationName();
    Publish publish = new Publish(msg.isRetain(), parsedMessage.getMessage().getOpaqueData(), qos, packetId, topicName,
        qos.isSendPacketId() && packetIdManager.isExistingPacketId(packetId));
    publish.setCallback(messageEvent.getCompletionTask());
    writeFrame(publish);
  }

  private boolean reconcileRecoveredDelivery(io.mapsmessaging.api.SubscribedEventManager subscription, long messageId, Runnable completion) {
    if (packetIdManager.consumeCompletedRecovery(subscription, messageId)) {
      subscription.ackReceived(messageId);
      packetIdManager.releaseSendSlot(subscription);
      completion.run();
      return true;
    }
    Integer existingId = packetIdManager.getPacketIdentifier(subscription, messageId);
    if (existingId == null || !packetIdManager.isAwaitingPubComp(existingId)) {
      return false;
    }
    packetIdManager.nextPacketIdentifier(subscription, messageId);
    sendPubRelAfterBrokerAck(existingId).thenRun(completion);
    return true;
  }

  public void writeFrame(ServerPacket frame) {
    if(this.endPoint.isClosed()){
      try {
        close();
      } catch (IOException e) {
        // Ignore here
      }
    }
    sentMessage();
    selectorTask.push(frame);
    logger.log(ServerLogMessages.PUSH_WRITE, frame);
  }

  public long getMaximumBufferSize() {
    return maxBufferSize;
  }


  @Override
  public ProtocolInformationDTO getInformation() {
    MqttProtocolInformation information = new MqttProtocolInformation();
    updateInformation(information);
    Session currentSession = session;
    if (currentSession != null) {
      information.setSessionInfo(currentSession.getSessionInformation());
    }
    return information;
  }
}
