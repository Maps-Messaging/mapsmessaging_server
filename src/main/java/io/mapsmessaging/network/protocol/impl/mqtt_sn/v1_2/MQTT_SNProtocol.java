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

package io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2;

import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.impl.SelectorTask;
import io.mapsmessaging.network.protocol.ProtocolImpl;
import io.mapsmessaging.network.protocol.impl.mqtt.PacketIdManager;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.MQTTSNInterfaceManager;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.RegisteredTopicConfiguration;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.Connect;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.MQTT_SNPacket;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.PacketFactory;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.PingRequest;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.Publish;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.state.InitialConnectionState;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.state.StateEngine;
import io.mapsmessaging.utilities.scheduler.SimpleTaskScheduler;
import java.io.IOException;
import java.net.SocketAddress;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

// The protocol is MQTT_SN, so it makes sense, ignoring the Camel Case rule in class names
@java.lang.SuppressWarnings("squid:S00101")
public class MQTT_SNProtocol extends ProtocolImpl {

  protected final Logger logger;
  protected final PacketFactory packetFactory;
  protected final SocketAddress remoteClient;
  protected final SelectorTask selectorTask;
  protected final MQTTSNInterfaceManager factory;
  protected final StateEngine stateEngine;
  protected final PacketIdManager packetIdManager;
  private final ScheduledFuture<?> monitor;

  protected @Getter SocketAddress addressKey;

  protected volatile boolean closed;
  protected Session session;

  public MQTT_SNProtocol(@NonNull @NotNull MQTTSNInterfaceManager factory,
      @NonNull @NotNull EndPoint endPoint,
      @NonNull @NotNull SocketAddress remoteClient,
      @NonNull @NotNull SelectorTask selectorTask,
      @NonNull @NotNull String loggerName,
      @NonNull @NotNull PacketFactory packetFactory,
      @NonNull @NotNull RegisteredTopicConfiguration registeredTopicConfiguration){
    super(endPoint);
    this.logger = LoggerFactory.getLogger(loggerName);
    this.remoteClient = remoteClient;
    this.selectorTask = selectorTask;
    this.factory = factory;
    packetIdManager = new PacketIdManager();
    logger.log(ServerLogMessages.MQTT_SN_INSTANCE);
    this.packetFactory = packetFactory;
    closed = false;
    stateEngine = new StateEngine(this, registeredTopicConfiguration);
    monitor = SimpleTaskScheduler.getInstance().scheduleAtFixedRate(new TimeOutMonitor(), 60, 60, TimeUnit.SECONDS);

  }


  public MQTT_SNProtocol(
      @NonNull @NotNull MQTTSNInterfaceManager factory,
      @NonNull @NotNull EndPoint endPoint,
      @NonNull @NotNull SocketAddress remoteClient,
      @NonNull @NotNull SelectorTask selectorTask,
      @NonNull @NotNull RegisteredTopicConfiguration registeredTopicConfiguration,
      @NonNull @NotNull Connect connect) {
    this(factory, endPoint, remoteClient, selectorTask, "MQTT-SN 1.2 Protocol on " + endPoint.getName(), new PacketFactory(), registeredTopicConfiguration);
    logger.log(ServerLogMessages.MQTT_SN_START);
    stateEngine.setState(new InitialConnectionState());
    addressKey = connect.getFromAddress();
    handleMQTTEvent(connect);
  }

  @Override
  public void close() throws IOException {
    if (!closed) {
      logger.log(ServerLogMessages.MQTT_SN_CLOSED);
      closed = true;
      finish();
    }
  }

  protected void finish() throws IOException {
    SessionManager.getInstance().close(session, false);
    factory.close(remoteClient);
    packetIdManager.close();
    monitor.cancel(false);
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

  protected void handleMQTTEvent(@NonNull @NotNull MQTT_SNPacket mqtt) {
    if (logger.isInfoEnabled()) {
      logger.log(ServerLogMessages.RECEIVE_PACKET, mqtt);
    }
    receivedMessageAverages.increment();
    try {
      MQTT_SNPacket response = stateEngine.handleMQTTEvent(mqtt, session, endPoint, this);
      if (response != null) {
        writeFrame(response);
      }
    } catch (Exception e) {
      logger.log(ServerLogMessages.MQTT_SN_PACKET_EXCEPTION, e, mqtt);
      try {
        close();
      } catch (IOException ioException) {
        // Ignore this since we are in an error state already
      }
    }
  }

  @Override
  public void sendKeepAlive() {
    logger.log(ServerLogMessages.MQTT_SN_KEEP_ALIVE_SEND, endPoint.getName());
    writeFrame(getPingRequest());
    long timeout = System.currentTimeMillis() - (keepAlive + 1000);
    if (endPoint.getLastRead() < timeout && endPoint.getLastWrite() < timeout) {
      try {
        logger.log(ServerLogMessages.MQTT_SN_KEEP_ALIVE_TIMED_OUT, endPoint.getName());
        close();
      } catch (IOException e) {
        logger.log(ServerLogMessages.END_POINT_CLOSE_EXCEPTION, e);
      }
    }
  }

  public String getName() {
    return "MQTT_SN 1.2";
  }

  @Override
  public void sendMessage(@NotNull @NonNull MessageEvent messageEvent) {
    if(stateEngine.getMaxBufferSize() > 0 &&
        stateEngine.getMaxBufferSize() < messageEvent.getMessage().getOpaqueData().length + 9 ) {
      messageEvent.getCompletionTask().run();
    }
    else {
      stateEngine.queueMessage(messageEvent);
    }
  }

  public void writeFrame(@NonNull @NotNull MQTT_SNPacket frame) {
    frame.setFromAddress(remoteClient);
    sentMessageAverages.increment();
    selectorTask.push(frame);
    logger.log(ServerLogMessages.PUSH_WRITE, frame);
    sentMessage();
  }

  public MQTT_SNPacket buildPublish(short alias, int packetId, MessageEvent messageEvent, QualityOfService qos, short topicTypeId){
    byte[] data = messageEvent.getMessage().getOpaqueData();
    if(transformation != null){
      data = transformation.outgoing(messageEvent.getMessage());
    }
    Publish publish = new Publish(alias, packetId,  data);
    publish.setQoS(qos);
    publish.setCallback(messageEvent.getCompletionTask());
    publish.setTopicIdType(stateEngine.getTopicAliasManager().getTopicAliasType(messageEvent.getDestinationName()));
    return publish;
  }

  public PacketIdManager getPacketIdManager() {
    return packetIdManager;
  }

  public MQTT_SNPacket getPingRequest(){
    return new PingRequest();
  }
  private class TimeOutMonitor implements Runnable{

    @Override
    public void run() {
      if(packetIdManager.scanForTimeOut()){
        try {
          close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }
}
