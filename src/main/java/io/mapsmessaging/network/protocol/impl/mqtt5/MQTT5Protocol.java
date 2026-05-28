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
import io.mapsmessaging.network.protocol.impl.mqtt.PacketIdManager;
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
public class MQTT5Protocol extends Protocol {

  private final Logger logger;
  private final PacketFactory5 packetFactory;
  private final SelectorTask selectorTask;

  @Getter
  private final PacketListenerFactory5 packetListenerFactory;
  @Getter
  private final PacketIdManager packetIdManager;
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
    minimumKeepAlive = mqttConfig.getMinServerKeepAlive() * 1000; // Convert to milliseconds
    selectorTask = new SelectorTask(this, endPoint.getConfig().getEndPointConfig());
    packetListenerFactory = new PacketListenerFactory5();
    packetFactory = new PacketFactory5(this);
    closed = false;
    packetIdManager = new PacketIdManager();
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
    if (!closed) {
      closed = true;
      selectorTask.close();
      if (session != null) {
        SessionManager.getInstance().close(session, false);
      }
    }
    super.close();
  }

  @Override
  public void connect(@NonNull @NotNull String sessionId, String username, String password) throws IOException {
    connect(sessionId, username, password, null);
  }

  public void connect(@NonNull @NotNull String sessionId, String username, String password, Publish5 willMsg) throws IOException {
    Connect5 connect = new Connect5();
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
    connectionTimeOut = SimpleTaskScheduler.getInstance().schedule(new ConnectCompletionTimeout(this), 1, TimeUnit.MINUTES);
    writeFrame(connect);
    registerRead();
    completedConnection();
  }

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

  public void setSession(Session session) {
    this.session = session;
    completedConnection();
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
      } catch (IOException e) {
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
      if (mqtt instanceof Connect5) {
        // We may have an auth / sasl request so lets c heck first, if so we need to park the connect until after
        // we have authenticated
        Connect5 connect5 = (Connect5) mqtt;
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
      if (sendProblemInformation && response instanceof StatusPacket && !(response instanceof ConnAck5)) {
        StatusPacket statusPacket = (StatusPacket) response;
        statusPacket.getProperties().add(new ReasonString(statusPacket.getStatusCode().getDescription()));
      }
      selectorTask.push(response);
    }
  }

  @Override
  public void sendKeepAlive() {
    ThreadContext.put("session", session.getName());
    ThreadContext.put("protocol", getName());
    ThreadContext.put("endpoint", endPoint.getName());
    ThreadContext.put("version", getVersion());
    logger.log(ServerLogMessages.MQTT5_KEEP_ALIVE_CHECK, keepAlive);
    long timeout = System.currentTimeMillis() - (keepAlive + 1000);
    if (endPoint.isClient()) {
      writeFrame(new PingReq5());
      timeout = System.currentTimeMillis() - (keepAlive * 2);

    }
    boolean readTimeOut = endPoint.getLastRead() < timeout;
    boolean writeTimeOut = endPoint.getLastWrite() < timeout;
    if (readTimeOut && writeTimeOut) {
      logger.log(ServerLogMessages.MQTT_DISCONNECT_TIMEOUT);
      try {
        close();
      } catch (IOException e) {
        // Ignore this, we are closing
      }
    }
    ThreadContext.clearMap();
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
    ParsedMessage parsedMessage = parseOutboundMessage(messageEvent);
    if(parsedMessage == null) {
      return;
    }
    String topicName = parsedMessage.getDestinationName();
    Message message = parsedMessage.getMessage();
    if (maxBufferSize > 0 && message.getOpaqueData().length >= maxBufferSize) {
      messageEvent.getCompletionTask().run();
      logger.log(ServerLogMessages.MQTT5_MAX_BUFFER_EXCEEDED, maxBufferSize, message.getOpaqueData().length);
    } else {
      sendPublishFrame(topicName, messageEvent.getSubscription(), message, messageEvent.getCompletionTask());
    }
  }

  private void sendPublishFrame(@NonNull @NotNull String normalisedName, @NonNull @NotNull SubscribedEventManager subscription, @NonNull @NotNull Message message,
      @NonNull @NotNull Runnable completionTask) {
    SubscriptionContext subInfo = subscription.getContext();
    QualityOfService qos = QualityOfService.getInstance(Math.min(subInfo.getQualityOfService().getLevel(), message.getQualityOfService().getLevel()));
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
    //
    // Weird MQTT5 flag
    //
    boolean retain = message.isRetain();
    if (!subInfo.isRetainAsPublish()) {
      retain = false;
    }
    Publish5 publish = new Publish5(message.getOpaqueData(), qos, packetId, destinationName, retain);
    if (alias != null) {
      publish.add(alias);
    }
    addProperties(message, publish, subscription);
    publish.setCallback(r);
    writeFrame(publish);
  }

  private int getPacketId(QualityOfService qos, SubscribedEventManager subscription, Message message) {
    if (qos.isSendPacketId()) {
      return packetIdManager.nextPacketIdentifier(subscription, message.getIdentifier());
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
    information.setSessionInfo(session.getSessionInformation());
    return information;
  }

  @Override
  public void subscribeRemote(@NonNull @NotNull String resource, @NonNull @NotNull String mappedResource, @NonNull @NotNull QualityOfService qos, @Nullable ParserExecutor parser, @Nullable InterServerTransformation transformer, StatisticsConfigDTO statistics, Map<String, Object> linkProperties) throws IOException {
    super.subscribeRemote(resource,mappedResource, qos, parser, transformer,statistics, linkProperties);
    Subscribe5 subscribe = new Subscribe5();
    subscribe.setMessageId(packetIdManager.nextPacketIdentifier());
    subscribe.getSubscriptionList().add(new SubscriptionInfo(resource, qos));
    writeFrame(subscribe);
    completedConnection();
  }

  @Override
  public void unsubscribeRemote(@NonNull @NotNull String resource){
    Unsubscribe5 unsubscribe = new Unsubscribe5(List.of(resource));
    unsubscribe.setMessageId(packetIdManager.nextPacketIdentifier());
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
