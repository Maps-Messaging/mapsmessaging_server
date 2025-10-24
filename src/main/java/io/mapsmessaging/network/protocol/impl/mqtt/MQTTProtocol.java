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

package io.mapsmessaging.network.protocol.impl.mqtt;

import io.mapsmessaging.api.*;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.transformers.Transformer;
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
import io.mapsmessaging.selector.operators.ParserExecutor;
import io.mapsmessaging.utilities.filtering.NamespaceFilters;
import lombok.Getter;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.security.auth.Subject;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.util.List;


@java.lang.SuppressWarnings("DuplicatedBlocks")
public class MQTTProtocol extends Protocol {

  private final Logger logger;
  private final PacketFactory packetFactory;
  private final PacketListenerFactory packetListenerFactory;
  private final SelectorTask selectorTask;
  @Getter
  private final PacketIdManager packetIdManager;
  private final long maxBufferSize;

  @Getter
  private final MqttConfigDTO mqttConfig;

  private volatile boolean closed;
  @Getter
  private Session session;


  public MQTTProtocol(EndPoint endPoint) throws IOException {
    super(endPoint, endPoint.getConfig().getProtocolConfig("mqtt"));
    logger = LoggerFactory.getLogger("MQTT 3.1.1 Protocol on " + endPoint.getName());
    ThreadContext.put("endpoint", endPoint.getName());
    ThreadContext.put("protocol", getName());
    ThreadContext.put("version", getVersion());
    logger.log(ServerLogMessages.MQTT_START);
    mqttConfig = (MqttConfigDTO) protocolConfig;
    maxBufferSize =  mqttConfig.getMaximumBufferSize();
    selectorTask = new SelectorTask(this, endPoint.getConfig().getEndPointConfig());
    packetListenerFactory = new PacketListenerFactory();
    packetFactory = new PacketFactory(this);
    closed = false;
    packetIdManager = new PacketIdManager();
  }

  public MQTTProtocol(EndPoint endPoint, Packet packet) throws IOException {
    this(endPoint);
    processPacket(packet);
    selectorTask.getReadTask().pushOutstandingData(packet);
  }

  @Override
  public void close() throws IOException {
    if (!closed) {
      closed = true;
      selectorTask.close();
      SessionManager.getInstance().close(session, false);
      super.close();
    }
  }

  @Override
  public Subject getSubject() {
    if(session != null) {
      return session.getSecurityContext().getSubject();
    }
    return new Subject();
  }

  @Override
  public void connect(@NonNull @NotNull String sessionId, String username, String password) throws IOException {
    Connect connect = new Connect();
    if (username != null) {
      connect.setUsername(username);
      connect.setPassword(password.trim().toCharArray());
    }
    connect.setSessionId(sessionId);
    writeFrame(connect);
    registerRead();
    completedConnection();
  }

  @Override
  public void subscribeRemote(@NonNull @NotNull String resource, @NonNull @NotNull String mappedResource, @NonNull @NotNull QualityOfService qos, @Nullable ParserExecutor parser, @Nullable Transformer transformer, StatisticsConfigDTO statistics) throws IOException {
    super.subscribeRemote(resource,mappedResource, qos, parser, transformer,statistics);
    Subscribe subscribe = new Subscribe();
    subscribe.setMessageId(packetIdManager.nextPacketIdentifier());
    subscribe.getSubscriptionList().add(new SubscriptionInfo(resource, qos));
    writeFrame(subscribe);
    completedConnection();
  }

  @Override
  public void unsubscribeRemote(@NonNull @NotNull String resource){
    Unsubscribe unsubscribe = new Unsubscribe(List.of(resource));
    unsubscribe.setMessageId(packetIdManager.nextPacketIdentifier());
    writeFrame(unsubscribe);
  }

  @Override
  public void subscribeLocal(@NonNull @NotNull String resource, @NonNull @NotNull String mappedResource,@NonNull @NotNull QualityOfService qos, @Nullable String selector, @Nullable Transformer transformer, @Nullable NamespaceFilters namespaceFilters, StatisticsConfigDTO statistics)
      throws IOException {
    super.subscribeLocal(resource, mappedResource, qos, selector, transformer, namespaceFilters, statistics);
    SubscriptionContextBuilder builder = createSubscriptionContextBuilder(resource, selector, qos, 1024);
    session.addSubscription(builder.build());
  }

  @Override
  public void unsubscribeLocal(@NonNull @NotNull String resource){
    session.removeSubscription(resource);
  }

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

  public void setSession(Session session) {
    this.session = session;
    completedConnection();
  }

  public boolean processPacket(Packet packet) throws IOException {
    int pos = packet.position();
    try {
      boolean resume = false;
      while (packet.hasRemaining()) {
        resume = handleMQTTEvent(packet);
        pos = packet.position();
      }
      if (resume) {
        registerRead();
      }
    } catch (EndOfBufferException eobe) {
      packet.position(pos);
      registerRead();
      return false;
    } catch (MalformedException | IOException e) {
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
    ThreadContext.put("session", session.getName());
    ThreadContext.put("protocol", getName());
    ThreadContext.put("endpoint", endPoint.getName());
    ThreadContext.put("version", getVersion());
    logger.log(ServerLogMessages.MQTT_KEEPALIVE_TIMOUT, keepAlive);
    long timeout = System.currentTimeMillis() - (keepAlive + 1000);
    if (endPoint.isClient()) {
      writeFrame(new PingReq());
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

  public String getName() {
    return "MQTT";
  }

  @Override
  public void sendMessage(@NotNull @NonNull MessageEvent messageEvent) {
    Message msg = messageEvent.getMessage();
    SubscriptionContext subInfo = messageEvent.getSubscription().getContext();
    QualityOfService qos = subInfo.getQualityOfService();
    int packetId = 0;
    if (qos.isSendPacketId()) {
      packetId = packetIdManager.nextPacketIdentifier(messageEvent.getSubscription(),msg.getIdentifier());
    }
    ParsedMessage parsedMessage = parseOutboundMessage(messageEvent);
    if(parsedMessage == null) {
      return;
    }
    String topicName = parsedMessage.getDestinationName();
    MessageBuilder messageBuilder = parsedMessage.getMessageBuilder();
    Publish publish = new Publish(msg.isRetain(), messageBuilder.getOpaqueData(), qos, packetId, topicName);
    publish.setCallback(messageEvent.getCompletionTask());
    writeFrame(publish);
  }


  public void writeFrame(ServerPacket frame) {
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
    information.setSessionInfo(session.getSessionInformation());
    return information;
  }
}
