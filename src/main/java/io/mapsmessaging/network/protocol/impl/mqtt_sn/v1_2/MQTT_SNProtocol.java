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

package io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2;

import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.config.protocol.impl.MqttSnConfig;
import io.mapsmessaging.dto.rest.protocol.ProtocolInformationDTO;
import io.mapsmessaging.dto.rest.protocol.impl.MqttSnProtocolInformation;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.impl.SelectorTask;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.impl.mqtt.PacketIdManager;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.MQTTSNInterfaceManager;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.RegisteredTopicConfiguration;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.*;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.state.InitialConnectionState;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.state.StateEngine;
import io.mapsmessaging.utilities.threads.SimpleTaskScheduler;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import javax.security.auth.Subject;
import java.io.IOException;
import java.net.SocketAddress;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

// The protocol is MQTT_SN, so it makes sense, ignoring the Camel Case rule in class names
@java.lang.SuppressWarnings("squid:S00101")
public class MQTT_SNProtocol extends Protocol {

  protected final Logger logger;
  protected final PacketFactory packetFactory;
  protected final SelectorTask selectorTask;
  protected final MQTTSNInterfaceManager factory;
  protected final StateEngine stateEngine;
  @Getter
  protected final PacketIdManager packetIdManager;
  private final ScheduledFuture<?> monitor;

  @Getter
  @Setter
  protected SocketAddress addressKey;

  protected volatile boolean closed;
  @Getter
  protected Session session;

  public MQTT_SNProtocol(@NonNull @NotNull MQTTSNInterfaceManager factory,
      @NonNull @NotNull EndPoint endPoint,
      @NonNull @NotNull SocketAddress remoteClient,
      @NonNull @NotNull SelectorTask selectorTask,
      @NonNull @NotNull String loggerName,
      @NonNull @NotNull PacketFactory packetFactory,
      @NonNull @NotNull RegisteredTopicConfiguration registeredTopicConfiguration,
      @NonNull @NotNull MqttSnConfig mqttSnConfig) {
    super(endPoint, remoteClient, mqttSnConfig);
    this.logger = LoggerFactory.getLogger(loggerName);
    this.selectorTask = selectorTask;
    this.factory = factory;
    addressKey = remoteClient;
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
      @NonNull @NotNull Connect connect,
      @NonNull @NotNull MqttSnConfig mqttSnConfig) {
    this(factory, endPoint, remoteClient, selectorTask, "MQTT-SN 1.2 Protocol on " + endPoint.getName(), new PacketFactory(), registeredTopicConfiguration, mqttSnConfig);
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

  @Override
  public Subject getSubject() {
    return session.getSecurityContext().getSubject();
  }

  protected void finish() throws IOException {
    if (!session.isClosed()) {
      SessionManager.getInstance().close(session, false);
    }
    factory.close(addressKey);
    packetIdManager.close();
    endPoint.close();
    monitor.cancel(false);
    if (mbean != null) {
      mbean.close();
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

  public void setSession(Session session) {
    this.session = session;
  }

  @Override
  public boolean processPacket(@NonNull @NotNull Packet packet) throws IOException {
    endPoint.updateReadBytes(packet.available());
    MQTT_SNPacket mqtt = packetFactory.parseFrame(packet);
    if (mqtt != null) {
      handleMQTTEvent(mqtt);
    }
    return true;
  }

  protected void handleMQTTEvent(@NonNull @NotNull MQTT_SNPacket mqtt) {
    if (logger.isInfoEnabled()) {
      logger.log(ServerLogMessages.RECEIVE_PACKET, mqtt);
    }
    EndPoint.totalReceived.increment();
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
    //writeFrame(getPingRequest());
    endPoint.updateWriteBytes(2);
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
    return "MQTT_SN";
  }

  @Override
  public void sendMessage(@NotNull @NonNull MessageEvent messageEvent) {
    if (stateEngine.getMaxBufferSize() > 0 &&
        stateEngine.getMaxBufferSize() < messageEvent.getMessage().getOpaqueData().length + 9) {
      messageEvent.getCompletionTask().run();
    } else {
      stateEngine.queueMessage(messageEvent);
    }
  }

  public void writeFrame(@NonNull @NotNull MQTT_SNPacket frame) {
    frame.setFromAddress(addressKey);
    EndPoint.totalSent.increment();
    selectorTask.push(frame);
    logger.log(ServerLogMessages.PUSH_WRITE, frame);
    sentMessage();
  }

  public MQTT_SNPacket buildPublish(short alias, int packetId, MessageEvent messageEvent, QualityOfService qos, short topicTypeId) {
    byte[] data = messageEvent.getMessage().getOpaqueData();
    if (transformation != null) {
      data = transformation.outgoing(messageEvent.getMessage(), messageEvent.getDestinationName() );
    }
    Publish publish = new Publish(alias, packetId, data);
    publish.setQoS(qos);
    publish.setCallback(messageEvent.getCompletionTask());
    publish.setTopicIdType(stateEngine.getTopicAliasManager().getTopicAliasType(messageEvent.getDestinationName()));
    return publish;
  }

  @Override
  public ProtocolInformationDTO getInformation() {
    MqttSnProtocolInformation information = new MqttSnProtocolInformation();
    updateInformation(information);
    information.setSessionInfo(session.getSessionInformation());
    return information;
  }

  public MQTT_SNPacket getPingRequest() {
    return new PingRequest();
  }

  private class TimeOutMonitor implements Runnable {

    @Override
    public void run() {
      if (packetIdManager.scanForTimeOut()) {
        try {
          close();
        } catch (IOException e) {
          // Catch and ignore
        }
      }
    }
  }
}
