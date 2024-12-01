/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2024 ] [Maps Messaging B.V.]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.network.protocol.impl.mqtt;

import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.api.SubscriptionContextBuilder;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.transformers.Transformer;
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
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.security.auth.Subject;
import lombok.Getter;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
  @Getter
  private final Map<String, String> topicNameMapping;

  private volatile boolean closed;
  @Getter
  private Session session;


  public MQTTProtocol(EndPoint endPoint) throws IOException {
    super(endPoint);
    logger = LoggerFactory.getLogger("MQTT 3.1.1 Protocol on " + endPoint.getName());
    ThreadContext.put("endpoint", endPoint.getName());
    ThreadContext.put("protocol", getName());
    ThreadContext.put("version", getVersion());
    topicNameMapping = new ConcurrentHashMap<>();
    logger.log(ServerLogMessages.MQTT_START);
    mqttConfig = (MqttConfigDTO) endPoint.getConfig().getProtocolConfig("mqtt");
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
    return session.getSecurityContext().getSubject();
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
  public void subscribeRemote(@NonNull @NotNull String resource, @NonNull @NotNull String mappedResource, @Nullable ParserExecutor parser, @Nullable Transformer transformer) {
    topicNameMapping.put(resource, mappedResource);
    if (transformer != null) {
      destinationTransformerMap.put(mappedResource, transformer);
    }
    if(parser != null){
      parserLookup.put(resource, parser);
    }
    Subscribe subscribe = new Subscribe();
    subscribe.setMessageId(packetIdManager.nextPacketIdentifier());
    subscribe.getSubscriptionList().add(new SubscriptionInfo(resource, QualityOfService.AT_MOST_ONCE));
    writeFrame(subscribe);
    completedConnection();
  }

  @Override
  public void subscribeLocal(@NonNull @NotNull String resource, @NonNull @NotNull String mappedResource, @Nullable String selector, @Nullable Transformer transformer)
      throws IOException {
    topicNameMapping.put(resource, mappedResource);
    if (transformer != null) {
      destinationTransformerMap.put(mappedResource, transformer);
    }
    SubscriptionContextBuilder builder = createSubscriptionContextBuilder(resource, selector, QualityOfService.AT_MOST_ONCE, 1024);
    session.addSubscription(builder.build());
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
    SubscriptionContext subInfo = messageEvent.getSubscription().getContext();
    QualityOfService qos = subInfo.getQualityOfService();
    String destinationName = messageEvent.getDestinationName();
    int packetId = 0;
    if (qos.isSendPacketId()) {
      packetId = packetIdManager.nextPacketIdentifier(messageEvent.getSubscription(), messageEvent.getMessage().getIdentifier());
    }
    Message message = processTransformer(destinationName, messageEvent.getMessage());

    byte[] payload;
    if (transformation != null) {
      payload = transformation.outgoing(message, messageEvent.getDestinationName());
    } else {
      payload = message.getOpaqueData();
    }
    if (topicNameMapping != null) {
      String tmp = topicNameMapping.get(destinationName);
      if (tmp != null) {
        destinationName = tmp;
      }
      else{
        for(String key:topicNameMapping.keySet()){
          int index = key.indexOf("#");
          if(index > 0){
            String sub = key.substring(0, index);
            if(destinationName.startsWith(sub)){
              destinationName = topicNameMapping.get(key) + destinationName.substring(sub.length());
            }
          }
        }
      }
    }
    Publish publish = new Publish(message.isRetain(), payload, qos, packetId, destinationName);
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
