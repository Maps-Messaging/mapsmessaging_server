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

package io.mapsmessaging.network.protocol.impl.mqtt5;

import io.mapsmessaging.api.*;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.TypedData;
import io.mapsmessaging.api.transformers.InterServerTransformation;
import io.mapsmessaging.api.transformers.ParsedMessage;
import io.mapsmessaging.config.protocol.impl.MqttConfig;
import io.mapsmessaging.dto.rest.analytics.StatisticsConfigDTO;
import io.mapsmessaging.dto.rest.config.auth.SaslConfigDTO;
import io.mapsmessaging.dto.rest.protocol.ProtocolInformationDTO;
import io.mapsmessaging.dto.rest.protocol.impl.MqttV5ProtocolInformation;
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
import io.mapsmessaging.network.protocol.impl.mqtt.MqttKeepAliveManager;
import io.mapsmessaging.network.protocol.impl.mqtt.PacketIdManager;
import io.mapsmessaging.network.protocol.impl.mqtt.RemoteSubscriptionProtocol;
import io.mapsmessaging.network.protocol.impl.mqtt.RemoteSubscriptionTracker;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MalformedException;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.SubscriptionInfo;
import io.mapsmessaging.network.protocol.impl.mqtt5.listeners.PacketListenerFactory5;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.*;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.*;
import io.mapsmessaging.selector.operators.ParserExecutor;
import io.mapsmessaging.utilities.collections.NaturalOrderedLongList;
import io.mapsmessaging.utilities.collections.bitset.BitSetFactory;
import io.mapsmessaging.utilities.collections.bitset.BitSetFactoryImpl;
import io.mapsmessaging.utilities.filtering.NamespaceFilters;
import io.mapsmessaging.utilities.threads.SimpleTaskScheduler;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.security.auth.Subject;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

// Between MQTT 3/4 and 5 there is duplicate code base, yes this is by design
@java.lang.SuppressWarnings("DuplicatedBlocks")
public class MQTT5Protocol extends Protocol implements RemoteSubscriptionProtocol {

  private final Logger logger;
  private final PacketFactory5 packetFactory;
  private final SelectorTask selectorTask;
  private final MqttKeepAliveManager keepAliveManager;

  @Getter
  private final PacketListenerFactory5 packetListenerFactory;
  @Getter
  private final PacketIdManager packetIdManager;
  private record PubRelGate(io.mapsmessaging.storage.delivery.PacketIdStore.Delivery delivery,
                            java.util.concurrent.CompletableFuture<Void> future, java.util.concurrent.atomic.AtomicBoolean ready) {}
  private final java.util.Map<Integer, PubRelGate> pubRelAcknowledgements = new java.util.concurrent.ConcurrentHashMap<>();
  @Getter
  private final NaturalOrderedLongList clientOutstanding;
  @Getter
  private final TopicAliasMapping clientTopicAliasMapping;
  @Getter
  private final TopicAliasMapping serverTopicAliasMapping;

  @Getter
  private volatile boolean closed;

  @Getter
  private final int serverReceiveMaximum;
  @Getter
  private final int clientReceiveMaximum;
  @Getter
  private final int minimumKeepAlive;

  @Getter
  private Session session;
  @Getter
  @Setter
  private AuthenticationContext authenticationContext;
  @Getter
  @Setter
  private long maxBufferSize;
  @Getter
  @Setter
  private boolean isClosing;
  @Getter
  @Setter
  private boolean sendProblemInformation;
  @Getter
  private final MqttConfig mqttConfig;

  @Getter
  private final RemoteSubscriptionTracker remoteSubscriptionTracker;

  @Getter
  private volatile String connectionSessionId;

  private ScheduledFuture<?> connectionTimeOut = null;


  public MQTT5Protocol(EndPoint endPoint) throws IOException {
    super(endPoint, endPoint.getConfig().getProtocolConfig("mqtt"));
    logger = LoggerFactory.getLogger("MQTT 5.0 Protocol on " + endPoint.getName());

    isClosing = false;
    ThreadContext.put("endpoint", endPoint.getName());
    ThreadContext.put("protocol", getName());
    ThreadContext.put("version", getVersion());

    logger.log(ServerLogMessages.MQTT5_INITIALISATION);
    clientTopicAliasMapping = new TopicAliasMapping("Client");
    serverTopicAliasMapping = new TopicAliasMapping("Server");
    mqttConfig = (MqttConfig)protocolConfig;
    maxBufferSize = mqttConfig.getMaximumBufferSize();
    serverReceiveMaximum = mqttConfig.getServerReceiveMaximum();
    clientReceiveMaximum = mqttConfig.getClientReceiveMaximum();
    int clientMaximumTopicAlias = mqttConfig.getClientMaximumTopicAlias();
    clientTopicAliasMapping.setMaximum(clientMaximumTopicAlias);
    int serverMaximumTopicAlias = mqttConfig.getServerMaximumTopicAlias();
    serverTopicAliasMapping.setMaximum(serverMaximumTopicAlias);
    keepAlive = mqttConfig.getMaxServerKeepAlive() * 1000L;
    minimumKeepAlive = mqttConfig.getMinServerKeepAlive();
    keepAliveManager = new MqttKeepAliveManager();
    selectorTask = new SelectorTask(this, endPoint.getConfig().getEndPointConfig());
    packetListenerFactory = new PacketListenerFactory5();
    packetFactory = new PacketFactory5(this);
    closed = false;
    packetIdManager = new PacketIdManager();
    remoteSubscriptionTracker = new RemoteSubscriptionTracker(packetIdManager);
    BitSetFactory bitsetFactory = new BitSetFactoryImpl(DefaultConstants.BITSET_BLOCK_SIZE);
    clientOutstanding = new NaturalOrderedLongList(0, bitsetFactory);
    SaslConfigDTO saslConfig = endPoint.getConfig().getSaslConfig();
    if( saslConfig!= null){
      authenticationContext = new AuthenticationContext(saslConfig.getMechanism(), saslConfig.getRealmName(), getName(), endPoint.getConfig());
    }
    else{
      authenticationContext = null;
    }
  }


  public MQTT5Protocol(EndPoint endPoint, Packet packet) throws IOException {
    this(endPoint);
    processPacket(packet);
    selectorTask.getReadTask().pushOutstandingData(packet);
  }

  @Override
  public Subject getSubject() {
    return session.getSecurityContext().getSubject();
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
    try {
      if (authenticationContext != null) {
        authenticationContext.close();
      }
    } finally {
      authenticationContext = null;
      if(selectorTask.isOpen()){
        selectorTask.close();
      }
      if(session != null && !session.isClosed()){
        SessionManager.getInstance().close(session, false);
      }
      super.close();
    }
  }

  @Override
  public void connect(@NonNull @NotNull String sessionId, String username, String password) throws IOException {
    connect(sessionId, username, password, null);
  }

  public void connect(@NonNull @NotNull String sessionId, String username, String password, Publish5 willMsg) throws IOException {
    Connect5 connect = new Connect5();
    connect.setCleanSession(false);
    connect.setKeepAlive((int)(keepAlive)/1000);
    connect.add(new SessionExpiryInterval(mqttConfig.getMaximumSessionExpiry()));
    connect.add(new ReceiveMaximum(serverReceiveMaximum));
    if (username != null) {
      connect.setUsername(username);
      connect.setPassword(password.trim().toCharArray());
    }
    if(willMsg != null){
      connect.setWillFlag(true);
      connect.setWillTopic(willMsg.getDestinationName());
      connect.setWillMsg(willMsg.getPayload());
      connect.setWillQOS(willMsg.getQos());
      connect.setWillRetain(willMsg.isRetain());
      connect.setWillProperties(willMsg.getProperties());
    }
    connect.setSessionId(sessionId);
    connectionSessionId = sessionId;
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
        if (existing.ready().get() && isDeliverySessionActive(activeSession)) writeFrame(new io.mapsmessaging.network.protocol.impl.mqtt5.packet.PubRel5(packetId));
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
            writeFrame(new io.mapsmessaging.network.protocol.impl.mqtt5.packet.PubRel5(packetId));
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

  public boolean processPacket(@NonNull @NotNull Packet packet) throws IOException {
    if (!isClosing) {
      int pos = packet.position();
      try {
        while (packet.hasRemaining()) {
          handleMQTTEvent(packet);
          pos = packet.position();
        }
        registerRead();
      } catch (BufferUnderflowException eob) {
        packet.position(pos); // rewind back so we can start again
        packet.compact();
        packet.flip();
      } catch (MalformedException malformed) {
        logger.log(ServerLogMessages.MALFORMED, malformed, malformed.getMessage());
        endPoint.close();
      } catch (EndOfBufferException eobe) {
        packet.position(pos);
        registerRead();
        return false;
      } catch (ClosedChannelException closeException) {
        endPoint.close();
      } catch (IOException | java.io.UncheckedIOException e) {
        logger.log(ServerLogMessages.MQTT5_HANDLE_EVENT_IO_EXCEPTION, e);
        endPoint.close();
      }
    }
    return true;
  }

  protected void handleMQTTEvent(Packet packet) throws MalformedException, EndOfBufferException {
    MQTTPacket5 mqtt = packetFactory.parseFrame(packet);
    if (mqtt != null) {
      logger.log(ServerLogMessages.RECEIVE_PACKET, mqtt);
      EndPoint.totalReceived.increment();
      boolean clientHasAuth = false;
      MessageProperty authMethod = null;
      if (mqtt instanceof Connect5 connect5) {
        // We may have an auth / sasl request so lets c heck first, if so we need to park the connect until after
        // we have authenticated
        authMethod = connect5.getProperties().get(MessagePropertyFactory.AUTHENTICATION_METHOD);
        clientHasAuth = !(authMethod == null || (authMethod.getName() != null && authMethod.getName().isEmpty()));
      }
      MQTTPacket5 response;
      if (clientHasAuth && authenticationContext != null) {
        response = packetListenerFactory.getListener(MQTTPacket5.AUTH).handlePacket(mqtt, null, endPoint, this);
      } else {
        response = packetListenerFactory.getListener(mqtt.getControlPacketId()).handlePacket(mqtt, session, endPoint, this);
      }
      handleResponse(response);
    }
  }

  private void handleResponse(MQTTPacket5 response) {
    if (response != null) {
      EndPoint.totalSent.increment();
      if (logger.isInfoEnabled()) {
        logger.log(ServerLogMessages.RESPONSE_PACKET, response);
      }
      if (sendProblemInformation && response instanceof StatusPacket statusPacket && !(response instanceof ConnAck5)) {
        statusPacket.getProperties().add(new ReasonString(statusPacket.getStatusCode().getDescription()));
      }
      selectorTask.push(response);
    }
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
        logger.log(ServerLogMessages.MQTT5_KEEP_ALIVE_CHECK, keepAlive);
        writeFrame(new PingReq5());
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

  @Override
  public String getName() {
    return "MQTT";
  }

  @Override
  public String getVersion() {
    return "5.0";
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
    SubscriptionContext context = messageEvent.getSubscription().getContext();
    if (context == null) {
      packetIdManager.releaseSendSlot(messageEvent.getSubscription());
      messageEvent.getCompletionTask().run();
      return;
    }
    if (reconcileRecoveredDelivery(messageEvent.getSubscription(), messageEvent.getMessage().getIdentifier(), messageEvent.getCompletionTask())) {
      return;
    }
    boolean retainedReplay = messageEvent.getSubscription().isRetainedReplay(messageEvent.getMessage().getIdentifier());
    ParsedMessage parsedMessage = parseOutboundMessage(messageEvent, context);
    if(parsedMessage == null) {
      packetIdManager.discard(messageEvent.getSubscription(), messageEvent.getMessage().getIdentifier());
      packetIdManager.releaseSendSlot(messageEvent.getSubscription());
      return;
    }
    String topicName = parsedMessage.getDestinationName();
    Message message = parsedMessage.getMessage();
    if (maxBufferSize > 0 && message.getOpaqueData().length >= maxBufferSize) {
      packetIdManager.discard(messageEvent.getSubscription(), messageEvent.getMessage().getIdentifier());
      packetIdManager.releaseSendSlot(messageEvent.getSubscription());
      messageEvent.getCompletionTask().run();
      logger.log(ServerLogMessages.MQTT5_MAX_BUFFER_EXCEEDED, maxBufferSize, message.getOpaqueData().length);
    } else {
      sendPublishFrame(topicName, messageEvent.getSubscription(), context, message, messageEvent.getCompletionTask(), retainedReplay);
    }
  }

  private void sendPublishFrame(@NonNull @NotNull String normalisedName, @NonNull @NotNull SubscribedEventManager subscription, @NonNull @NotNull SubscriptionContext subInfo, @NonNull @NotNull Message message,
                                @NonNull @NotNull Runnable completionTask, boolean retainedReplay) {
    if (reconcileRecoveredDelivery(subscription, message.getIdentifier(), completionTask)) {
      return;
    }
    QualityOfService qos = QualityOfService.getInstance(Math.min(subInfo.getQualityOfService().getLevel(), message.getQualityOfService().getLevel()));
    Integer existingId = packetIdManager.getPacketIdentifier(subscription, message.getIdentifier());
    if (existingId != null && packetIdManager.getPacketQos(existingId) > 0) qos = QualityOfService.getInstance(packetIdManager.getPacketQos(existingId));
    int packetId = getPacketId(qos, subscription, message);
    Runnable r;
    if(subInfo.getQualityOfService().getLevel() != 0 && qos.getLevel() == 0) {
      r = () -> {
        completionTask.run();
        subscription.ackReceived(message.getIdentifier());
      };
    }
    else {
      r = completionTask;
    }

    TopicAlias alias = serverTopicAliasMapping.find(normalisedName);
    String destinationName = normalisedName;
    if (alias != null) {
      destinationName = "";
    } else {
      if (serverTopicAliasMapping.size() < serverTopicAliasMapping.getMaximum()) {
        alias = serverTopicAliasMapping.create(destinationName);
      }
    }
    boolean retain = retainedReplay || (subInfo.isRetainAsPublish() && message.isRetain());
    Publish5 publish = new Publish5(message.getOpaqueData(), qos, packetId, destinationName, retain,
        qos.isSendPacketId() && packetIdManager.isExistingPacketId(packetId));
    if (alias != null) {
      publish.add(alias);
    }
    addProperties(message, publish, subscription);
    publish.setCallback(r);
    writeFrame(publish);
  }

  private boolean reconcileRecoveredDelivery(SubscribedEventManager subscription, long messageId, Runnable completion) {
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

  private int getPacketId(QualityOfService qos, SubscribedEventManager subscription, Message message) {
    if (qos.isSendPacketId()) {
      return packetIdManager.nextPacketIdentifier(subscription, message.getIdentifier(), qos.getLevel());
    }
    return 0;
  }

  private void addProperties(Message message, Publish5 publish, SubscribedEventManager subscription) {
    Map<String, TypedData> hash = message.getDataMap();
    for (Map.Entry<String, TypedData> entry : hash.entrySet()) {
      publish.add(new UserProperty(entry.getKey(), entry.getValue().getData().toString()));
    }
    if (message.getContentType() != null) {
      publish.add(new ContentType(message.getContentType()));
    }
    if (message.getCorrelationData() != null) {
      publish.add(new CorrelationData(message.getCorrelationData()));
    }
    long expiryInSeconds = (message.getExpiry() - System.currentTimeMillis()) / 1000;
    if (expiryInSeconds > 0) {
      publish.add(new MessageExpiryInterval(expiryInSeconds));
    }
    if (message.isUTF8()) {
      publish.add(new PayloadFormatIndicator(message.isUTF8()));
    }
    if (message.getResponseTopic() != null) {
      publish.add(new ResponseTopic(message.getResponseTopic()));
    }
    for (SubscriptionContext context : subscription.getContexts()) {
      if (context.getSubscriptionId() != -1) {
        publish.add(new SubscriptionIdentifier(context.getSubscriptionId()));
      }
    }
  }

  public void writeFrame(ServerPacket frame) {
    sentMessage();
    selectorTask.push(frame);
    logger.log(ServerLogMessages.PUSH_WRITE, frame);
  }

  @Override
  public ProtocolInformationDTO getInformation() {
    MqttV5ProtocolInformation information = new MqttV5ProtocolInformation();
    updateInformation(information);
    Session currentSession = session;
    if (currentSession != null) {
      information.setSessionInfo(currentSession.getSessionInformation());
    }
    return information;
  }

  @Override
  public void subscribeRemote(@NonNull @NotNull String resource, @NonNull @NotNull String mappedResource, @NonNull @NotNull QualityOfService qos, @Nullable ParserExecutor parser, @Nullable InterServerTransformation transformer, StatisticsConfigDTO statistics, Map<String, Object> linkProperties) throws IOException {
    super.subscribeRemote(resource,mappedResource, qos, parser, transformer,statistics, linkProperties);
    Subscribe5 subscribe = new Subscribe5();
    subscribe.setMessageId(remoteSubscriptionTracker.register(List.of(resource)));
    subscribe.getSubscriptionList().add(new SubscriptionInfo(resource, qos));
    writeFrame(subscribe);
  }

  @Override
  public void unsubscribeRemote(@NonNull @NotNull String resource){
    Unsubscribe5 unsubscribe = new Unsubscribe5(List.of(resource));
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

}
