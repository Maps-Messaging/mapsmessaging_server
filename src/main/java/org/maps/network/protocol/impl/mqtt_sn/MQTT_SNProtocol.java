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

package org.maps.network.protocol.impl.mqtt_sn;

import java.io.IOException;
import java.net.SocketAddress;
import lombok.NonNull;
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
import org.maps.network.io.impl.SelectorTask;
import org.maps.network.protocol.ProtocolImpl;
import org.maps.network.protocol.impl.mqtt.PacketIdManager;
import org.maps.network.protocol.impl.mqtt_sn.packet.Connect;
import org.maps.network.protocol.impl.mqtt_sn.packet.MQTT_SNPacket;
import org.maps.network.protocol.impl.mqtt_sn.packet.PacketFactory;
import org.maps.network.protocol.impl.mqtt_sn.packet.PingRequest;
import org.maps.network.protocol.impl.mqtt_sn.packet.Publish;
import org.maps.network.protocol.impl.mqtt_sn.packet.Register;
import org.maps.network.protocol.impl.mqtt_sn.state.InitialConnectionState;
import org.maps.network.protocol.impl.mqtt_sn.state.StateEngine;

// The protocol is MQTT_SN so it makes sense, ignoring the Camel Case rule in class names
@java.lang.SuppressWarnings("squid:S00101")
public class MQTT_SNProtocol extends ProtocolImpl {

  private final Logger logger;
  private final PacketFactory packetFactory;
  private final SocketAddress remoteClient;
  private final SelectorTask selectorTask;
  private final MQTTSNInterfaceManager factory;
  private final StateEngine stateEngine;
  private final PacketIdManager packetIdManager;
  private final SleepManager sleepManager;

  private volatile boolean closed;
  private Session session;

  public MQTT_SNProtocol(@NonNull @NotNull MQTTSNInterfaceManager factory, @NonNull @NotNull EndPoint endPoint,
      @NonNull @NotNull SocketAddress remoteClient, @NonNull @NotNull SelectorTask selectorTask, @NonNull @NotNull Connect connect) {
    super(endPoint);
    logger = LoggerFactory.getLogger("MQTT-SN 1.2 Protocol on " + endPoint.getName());

    this.remoteClient = remoteClient;
    this.selectorTask = selectorTask;
    this.factory = factory;
    packetIdManager = new PacketIdManager();
    sleepManager = new SleepManager(endPoint.getConfig().getProperties().getIntProperty("eventsPerTopicDuringSleep", DefaultConstants.MAX_SLEEP_EVENTS));
    logger.log(LogMessages.MQTT_SN_INSTANCE);
    packetFactory = new PacketFactory();
    closed = false;
    stateEngine = new StateEngine(new InitialConnectionState());
    handleMQTTEvent(connect);
  }

  @Override
  public void close() throws IOException {
    if (!closed) {
      closed = true;
      SessionManager.getInstance().close(session);
      factory.close(remoteClient);
      super.close();
    }
  }

  @Override
  public String getSessionId() {
    if (session != null) {
      return session.getName();
    }
    return "waiting";
  }

  public String getVersion() {
    return "1.2";
  }

  public Session getSession() {
    return session;
  }

  public void setSession(Session session) {
    this.session = session;
  }

  @Override
  public boolean processPacket(@NonNull @NotNull Packet packet) throws IOException {
    MQTT_SNPacket mqtt = packetFactory.parseFrame(packet);
    if(mqtt != null) {
      handleMQTTEvent(mqtt);
    }
    return true;
  }

  private void handleMQTTEvent(@NonNull @NotNull MQTT_SNPacket mqtt) {
    if (logger.isInfoEnabled()) {
      logger.log(LogMessages.RECEIVE_PACKET, mqtt.toString());
    }
    receivedMessageAverages.increment();
    try {
      MQTT_SNPacket response = stateEngine.handleMQTTEvent(mqtt, session, endPoint, this);
      if (response != null) {
        writeFrame(response);
      }
    } catch (Exception e) {
      e.printStackTrace();
      logger.log(LogMessages.MQTT_SN_PACKET_EXCEPTION, e, mqtt);
      try {
        close();
      } catch (IOException ioException) {
        // Ignore this since we are in an error state already
      }
    }
  }

  @Override
  public void sendKeepAlive() {
    writeFrame(new PingRequest());
    long timeout = System.currentTimeMillis() - (keepAlive + 1000);
    if (endPoint.getLastRead() < timeout && endPoint.getLastWrite() < timeout) {
      try {
        close();
      } catch (IOException e) {
        logger.log(LogMessages.END_POINT_CLOSE_EXCEPTION, e);
      }
    }
  }

  public String getName() {
    return "MQTT_SN";
  }

  @Override
  public void sendMessage(@NonNull @NotNull Destination destination, @NonNull @NotNull String normalisedName, @NonNull @NotNull SubscribedEventManager subscription, @NonNull @NotNull Message message, @NonNull @NotNull Runnable completionTask) {
    SubscriptionContext subInfo = subscription.getContext();
    QualityOfService qos = subInfo.getQualityOfService();
    int packetId = 0;
    if (qos.isSendPacketId()) {
      packetId = packetIdManager.nextPacketIdentifier(subscription, message.getIdentifier());
    }
    short alias = stateEngine.findTopicAlias(normalisedName);
    //
    // If this event is from a wild card then the client would not have registered it, so lets do that now
    //
    if (alias == -1) {
      //
      // Updating the client with the new topic id for the destination
      //
      alias = stateEngine.getTopicAlias(normalisedName);
      Register register = new Register(alias, (short) 0, normalisedName);
      writeFrame(register);
    }
    Publish publish = new Publish(alias, packetId, message.getOpaqueData());
    publish.setQoS(qos);
    publish.setCallback(completionTask);
    stateEngine.sendPublish(this, normalisedName, publish);
  }

  public void writeFrame(@NonNull @NotNull MQTT_SNPacket frame) {
    frame.setFromAddress(remoteClient);
    sentMessageAverages.increment();
    selectorTask.push(frame);
    logger.log(LogMessages.PUSH_WRITE, frame);
    if (frame.getCallback() != null) {
      frame.getCallback().run();
    }
    sentMessage();
  }

  public PacketIdManager getPacketIdManager() {
    return packetIdManager;
  }

  public SleepManager getSleepManager() {
    return sleepManager;
  }
}
