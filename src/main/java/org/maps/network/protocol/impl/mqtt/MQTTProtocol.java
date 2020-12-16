/*
 * Copyright [2020] [Matthew Buckton]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.maps.network.protocol.impl.mqtt;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import org.apache.logging.log4j.ThreadContext;
import org.jetbrains.annotations.NotNull;
import org.maps.logging.LogMessages;
import org.maps.logging.Logger;
import org.maps.logging.LoggerFactory;
import org.maps.messaging.api.Destination;
import org.maps.messaging.api.Session;
import org.maps.messaging.api.SessionManager;
import org.maps.messaging.api.SubscribedEventManager;
import org.maps.messaging.api.features.QualityOfService;
import org.maps.messaging.api.message.Message;
import org.maps.messaging.engine.destination.subscription.SubscriptionContext;
import org.maps.network.io.EndPoint;
import org.maps.network.io.Packet;
import org.maps.network.io.ServerPacket;
import org.maps.network.io.impl.SelectorTask;
import org.maps.network.protocol.EndOfBufferException;
import org.maps.network.protocol.ProtocolImpl;
import org.maps.network.protocol.impl.mqtt.listeners.PacketListenerFactory;
import org.maps.network.protocol.impl.mqtt.packet.MQTTPacket;
import org.maps.network.protocol.impl.mqtt.packet.MalformedException;
import org.maps.network.protocol.impl.mqtt.packet.PacketFactory;
import org.maps.network.protocol.impl.mqtt.packet.Publish;

@java.lang.SuppressWarnings("DuplicatedBlocks")
public class MQTTProtocol extends ProtocolImpl {

  private final Logger logger;
  private final PacketFactory packetFactory;
  private final PacketListenerFactory packetListenerFactory;
  private final SelectorTask selectorTask;
  private final PacketIdManager packetIdManager;
  private final long maxBufferSize;

  private volatile boolean closed;
  private Session session;

  public MQTTProtocol(EndPoint endPoint, Packet packet) throws IOException {
    super(endPoint);
    logger = LoggerFactory.getLogger("MQTT 3.1.1 Protocol on " + endPoint.getName());

    ThreadContext.put("endpoint", endPoint.getName());
    ThreadContext.put("protocol", getName());
    ThreadContext.put("version", getVersion());
    logger.log(LogMessages.MQTT_START);
    maxBufferSize = endPoint.getConfig().getProperties().getLongProperty("maximumBufferSize", DefaultConstants.MAXIMUM_BUFFER_SIZE);
    selectorTask = new SelectorTask(this, endPoint.getConfig().getProperties());
    packetListenerFactory = new PacketListenerFactory();
    packetFactory = new PacketFactory(this);
    closed = false;
    packetIdManager = new PacketIdManager();
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
  public void sendMessage(@NotNull  Destination destination, @NotNull SubscribedEventManager subscription,
                          @NotNull Message message, @NotNull Runnable completionTask) {
    SubscriptionContext subInfo = subscription.getContext();
    QualityOfService qos = subInfo.getQualityOfService();
    int packetId = 0;
    if (qos.isSendPacketId()) {
      packetId = packetIdManager.nextPacketIdentifier(subscription, message.getIdentifier());
    }
    byte[] payload;
    if(transformation != null){
      payload = transformation.outgoing(message);
    }
    else{
      payload = message.getOpaqueData();
    }
    Publish publish = new Publish(message.isRetain(), payload, qos, packetId, destination.getName());
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
