/*
 *
 *   Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package io.mapsmessaging.network.protocol.impl.mqtt;

import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.api.SubscriptionContextBuilder;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.transformers.Transformer;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.logging.LogMessages;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.ServerPacket;
import io.mapsmessaging.network.io.impl.SelectorTask;
import io.mapsmessaging.network.protocol.EndOfBufferException;
import io.mapsmessaging.network.protocol.ProtocolImpl;
import io.mapsmessaging.network.protocol.impl.mqtt.listeners.PacketListenerFactory;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.Connect;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MQTTPacket;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MalformedException;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.PacketFactory;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.PingReq;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.Publish;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.Subscribe;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.SubscriptionInfo;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.NonNull;
import org.apache.logging.log4j.ThreadContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@java.lang.SuppressWarnings("DuplicatedBlocks")
public class MQTTProtocol extends ProtocolImpl {

  private final Logger logger;
  private final PacketFactory packetFactory;
  private final PacketListenerFactory packetListenerFactory;
  private final SelectorTask selectorTask;
  private final PacketIdManager packetIdManager;
  private final long maxBufferSize;

  private final Map<String, String> topicNameMapping;

  private volatile boolean closed;
  private Session session;


  public MQTTProtocol(EndPoint endPoint) throws IOException {
    super(endPoint);
    logger = LoggerFactory.getLogger("MQTT 3.1.1 Protocol on " + endPoint.getName());
    ThreadContext.put("endpoint", endPoint.getName());
    ThreadContext.put("protocol", getName());
    ThreadContext.put("version", getVersion());
    topicNameMapping = new ConcurrentHashMap<>();
    logger.log(LogMessages.MQTT_START);
    maxBufferSize = endPoint.getConfig().getProperties().getLongProperty("maximumBufferSize", DefaultConstants.MAXIMUM_BUFFER_SIZE);
    selectorTask = new SelectorTask(this, endPoint.getConfig().getProperties());
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
      SessionManager.getInstance().close(session);
      super.close();
    }
  }

  @Override
  public void connect(@NonNull @NotNull String sessionId,String username,String password) throws IOException {
    Connect connect = new Connect();
    if(username != null) {
      connect.setUsername(username);
      connect.setPassword(password.trim().toCharArray());
    }
    connect.setSessionId(sessionId);
    writeFrame(connect);
    registerRead();
    completedConnection();
  }

  @Override
  public void subscribeRemote(@NonNull @NotNull String resource,@NonNull @NotNull  String mappedResource, @Nullable Transformer transformer){
    topicNameMapping.put(resource, mappedResource);
    if(transformer != null) {
      destinationTransformerMap.put(resource, transformer);
    }
    Subscribe subscribe = new Subscribe();
    subscribe.setMessageId(packetIdManager.nextPacketIdentifier());
    subscribe.getSubscriptionList().add(new SubscriptionInfo(resource, QualityOfService.AT_MOST_ONCE));
    writeFrame(subscribe);
    completedConnection();
  }

  @Override
  public void subscribeLocal(@NonNull @NotNull String resource, @NonNull @NotNull String mappedResource,@Nullable String selector,  @Nullable Transformer transformer) throws IOException {
    topicNameMapping.put(resource, mappedResource);
    if(transformer != null) {
      destinationTransformerMap.put(resource, transformer);
    }
    SubscriptionContextBuilder builder = createSubscriptionContextBuilder(resource, selector, QualityOfService.AT_MOST_ONCE, 1024);
    session.addSubscription(builder.build());
  }

  public Map<String, String> getTopicNameMapping() {
    return topicNameMapping;
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

  public Session getSession() {
    return session;
  }

  public void setSession(Session session) {
    this.session = session;
    completedConnection();
  }

  public boolean processPacket(Packet packet) throws IOException {
    int pos = packet.position();
    try {
      while (packet.hasRemaining()) {
        handleMQTTEvent(packet);
        pos = packet.position();
      }
      registerRead();
    } catch (EndOfBufferException eobe) {
      packet.position(pos);
      registerRead();
      return false;
    } catch (MalformedException | IOException e) {
      logger.log(LogMessages.MALFORMED, e, e.getMessage());
      endPoint.close();
    }
    return true;
  }

  protected void handleMQTTEvent(Packet packet) throws MalformedException, EndOfBufferException {
    MQTTPacket mqtt = packetFactory.parseFrame(packet);
    if (mqtt != null) {
      if (logger.isInfoEnabled()) {
        logger.log(LogMessages.RECEIVE_PACKET, mqtt.toString());
      }
      receivedMessageAverages.increment();
      MQTTPacket response =
          packetListenerFactory
              .getListener(mqtt.getControlPacketId())
              .handlePacket(mqtt, session, endPoint, this);
      if (response != null) {
        sentMessageAverages.increment();
        if (logger.isInfoEnabled()) {
          logger.log(LogMessages.RESPONSE_PACKET, response.toString());
        }
        selectorTask.push(response);
      }
    }
  }

  @Override
  public void sendKeepAlive() {
    ThreadContext.put("session", session.getName());
    ThreadContext.put("protocol", getName());
    ThreadContext.put("endpoint", endPoint.getName());
    ThreadContext.put("version", getVersion());
    logger.log(LogMessages.MQTT_KEEPALIVE_TIMOUT, keepAlive);
    long timeout = System.currentTimeMillis() - (keepAlive + 1000);
    if(endPoint.isClient()) {
      writeFrame( new PingReq());
      timeout = System.currentTimeMillis() - (keepAlive *2);

    }
    boolean readTimeOut = endPoint.getLastRead() < timeout;
    boolean writeTimeOut = endPoint.getLastWrite() < timeout;
    if (readTimeOut && writeTimeOut) {
      logger.log(LogMessages.MQTT_DISCONNECT_TIMEOUT);
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
  public void sendMessage(@NonNull @NotNull Destination destination, @NonNull @NotNull String  normalisedName, @NonNull @NotNull SubscribedEventManager subscription,
                          @NonNull @NotNull Message message, @NonNull @NotNull Runnable completionTask) {
    SubscriptionContext subInfo = subscription.getContext();
    QualityOfService qos = subInfo.getQualityOfService();
    int packetId = 0;
    if (qos.isSendPacketId()) {
      packetId = packetIdManager.nextPacketIdentifier(subscription, message.getIdentifier());
    }
    message = processTransformer(normalisedName, message);

    byte[] payload;
    if(transformation != null){
      payload = transformation.outgoing(message);
    }
    else{
      payload = message.getOpaqueData();
    }
    if(topicNameMapping != null){
      String tmp = topicNameMapping.get(normalisedName);
      if(tmp != null){
        normalisedName = tmp;
      }
    }
    Publish publish = new Publish(message.isRetain(), payload, qos, packetId, normalisedName);
    publish.setCallback(completionTask);
    writeFrame(publish);
  }


  public void writeFrame(ServerPacket frame) {
    sentMessage();
    selectorTask.push(frame);
    logger.log(LogMessages.PUSH_WRITE, frame);
  }

  public PacketIdManager getPacketIdManager() {
    return packetIdManager;
  }

  public long getMaximumBufferSize() {
    return maxBufferSize;
  }
}
