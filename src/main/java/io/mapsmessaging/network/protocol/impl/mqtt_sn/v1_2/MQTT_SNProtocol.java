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

package io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2;

import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.transformers.ParsedMessage;
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
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MalformedException;
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
import java.io.UncheckedIOException;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.HashMap;
import java.util.Map;
import io.mapsmessaging.storage.delivery.PacketIdStore;
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
  private final Queue<Integer> recoveredPubRels = new ArrayDeque<>();
  private Runnable resumeAfterRecovery;
  private Integer recoveringPacketId;
  private Session recoverySession;
  private boolean recoveredPubRelSent;
  private final Map<Integer, PacketIdStore.Delivery> pendingEngineAcknowledgements = new HashMap<>();
  private final Map<Integer, PacketIdStore.Delivery> reconciledDeliveries = new HashMap<>();

  @Getter
  @Setter
  protected SocketAddress addressKey;

  protected volatile boolean closed;
  @Setter
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
    try {
      packetIdManager.close();
      if (session != null && !session.isClosed()) {
        SessionManager.getInstance().close(session, false);
      }
    } finally {
      try {
        factory.close(addressKey);
      } finally {
        try {
          endPoint.close();
        } finally {
          monitor.cancel(false);
          if (mbean != null) {
            mbean.close();
          }
        }
      }
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
    try {
      if (completeRecoveredEvent(messageEvent)) {
        return;
      }
      sendOutboundMessage(messageEvent);
    } catch (UncheckedIOException failure) {
      closeAfterStorageFailure(failure, messageEvent);
    }
  }

  private void closeAfterStorageFailure(UncheckedIOException failure, Object context) {
    logger.log(ServerLogMessages.MQTT_SN_PACKET_EXCEPTION, failure, context);
    try {
      close();
    } catch (IOException | RuntimeException closeFailure) {
      failure.addSuppressed(closeFailure);
    }
  }

  private void sendOutboundMessage(MessageEvent messageEvent) {
    ParsedMessage parsedMessage = parseOutboundMessage(messageEvent);
    if(parsedMessage == null) {
      return;
    }
    Message message = parsedMessage.getMessage();

    if (stateEngine.getMaxBufferSize() > 0 &&
        stateEngine.getMaxBufferSize() < message.getOpaqueData().length + 9) {
      packetIdManager.discard(messageEvent.getSubscription(), messageEvent.getMessage().getIdentifier());
      messageEvent.getCompletionTask().run();
    } else {
      MessageEvent evt = new MessageEvent(parsedMessage.getDestinationName(), messageEvent.getSubscription(), message, messageEvent.getCompletionTask());
      stateEngine.queueMessage(evt);
    }
  }

  /** Recover receiver-owned QoS 2 handshakes before resuming broker message interest. */
  public synchronized void resumeSession(Session currentSession) {
    try {
      recoveredPubRels.addAll(packetIdManager.getAwaitingPubCompPacketIds());
      recoverySession = currentSession;
      resumeAfterRecovery = currentSession::resumeState;
      sendNextRecoveredPubRel();
    } catch (UncheckedIOException failure) {
      closeAfterStorageFailure(failure, currentSession);
    }
  }

  private synchronized void sendNextRecoveredPubRel() {
    recoveredPubRelSent = false;
    if (recoveredPubRels.isEmpty()) {
      recoveringPacketId = null;
      Session expectedSession = recoverySession;
      recoverySession = null;
      Runnable resume = resumeAfterRecovery;
      resumeAfterRecovery = null;
      if (resume != null && isActiveSession(expectedSession)) {
        resume.run();
      }
      return;
    }
    int identifier = recoveredPubRels.peek();
    recoveringPacketId = identifier;
    try {
      PacketIdStore.Delivery delivery = recoverySession.getPacketIdStore().getDelivery(identifier);
      if (delivery == null) {
        recoveredPubRels.poll();
        sendNextRecoveredPubRel();
        return;
      }
      // Keep stage 1 durable until engine interest is acknowledged, then resume the receiver-owned exchange.
      Session expectedSession = recoverySession;
      expectedSession.ackRecoveredDelivery(delivery).whenComplete((ignored, failure) -> {
        synchronized (MQTT_SNProtocol.this) {
          if (closed || endPoint.isClosed() || session != expectedSession || recoveringPacketId == null || recoveringPacketId != identifier) {
            return;
          }
          if (!delivery.equals(packetIdManager.getDelivery(identifier)) || !packetIdManager.isAwaitingPubComp(identifier)) {
            recoveredPubRels.poll();
            sendNextRecoveredPubRel();
            return;
          }
          if (failure != null) {
            closeRecoveryFailure(failure);
          } else if (isActiveSession(expectedSession)) {
            recoveredPubRelSent = true;
            writeFrame(new PubRel(identifier));
          }
        }
      });
    } catch (IOException | RuntimeException failure) {
      closeRecoveryFailure(failure);
    }
  }

  public synchronized void sendPubRelAfterEngineAck(int identifier, Session currentSession) {
    try {
      PacketIdStore.Delivery delivery = currentSession.getPacketIdStore().getDelivery(identifier);
      if (delivery == null) {
        throw new IOException("No durable delivery for PUBREC " + identifier);
      }
      if (delivery.equals(pendingEngineAcknowledgements.get(identifier))) {
        return;
      }
      boolean reconciled = delivery.equals(reconciledDeliveries.get(identifier));
      if (reconciled || recoveringPacketId != null && recoveringPacketId == identifier) {
        if (isActiveSession(currentSession) && (reconciled || recoveredPubRelSent)) {
          writeFrame(new PubRel(identifier));
        }
        return;
      }
      reconciledDeliveries.remove(identifier);
      pendingEngineAcknowledgements.put(identifier, delivery);
      currentSession.ackRecoveredDelivery(delivery).whenComplete((ignored, failure) -> {
        synchronized (MQTT_SNProtocol.this) {
          if (closed || endPoint.isClosed() || session != currentSession) {
            return;
          }
          if (pendingEngineAcknowledgements.get(identifier) != delivery) {
            return;
          }
          if (!delivery.equals(packetIdManager.getDelivery(identifier)) || !packetIdManager.isAwaitingPubComp(identifier)) {
            pendingEngineAcknowledgements.remove(identifier);
            return;
          }
          if (failure != null) {
            closeAfterStorageFailure(new UncheckedIOException(new IOException("Failed to acknowledge QoS 2 engine interest", failure)), currentSession);
          } else if (isActiveSession(currentSession)) {
            pendingEngineAcknowledgements.remove(identifier);
            reconciledDeliveries.put(identifier, delivery);
            writeFrame(new PubRel(identifier));
          }
        }
      });
    } catch (IOException | RuntimeException failure) {
      closeAfterStorageFailure(new UncheckedIOException(new IOException("Failed to acknowledge QoS 2 engine interest", failure)), currentSession);
    }
  }

  private boolean isActiveSession(Session expectedSession) {
    return !closed && !endPoint.isClosed() && expectedSession != null && session == expectedSession && !expectedSession.isClosed();
  }

  public synchronized boolean isEngineAckPending(int identifier) {
    return pendingEngineAcknowledgements.containsKey(identifier);
  }

  public synchronized boolean consumeReconciledDelivery(int identifier) {
    return reconciledDeliveries.remove(identifier) != null;
  }

  private void closeRecoveryFailure(Throwable failure) {
    closeAfterStorageFailure(new UncheckedIOException(new IOException("Failed to reconcile recovered QoS 2 delivery", failure)), recoverySession);
  }

  public synchronized boolean completeRecoveredPubRel(int identifier) throws MalformedException {
    if (recoveringPacketId == null) {
      return false;
    }
    if (recoveringPacketId != identifier || !recoveredPubRelSent) {
      throw new MalformedException("PUBCOMP does not match the active restored PUBREL " + identifier);
    }
    packetIdManager.completePacketId(identifier);
    recoveredPubRels.poll();
    sendNextRecoveredPubRel();
    return true;
  }

  private synchronized boolean completeRecoveredEvent(MessageEvent event) {
    if (!packetIdManager.consumeCompletedRecovery(event.getSubscription(), event.getMessage().getIdentifier())) {
      return false;
    }
    event.getSubscription().ackReceived(event.getMessage().getIdentifier());
    if (event.getCompletionTask() != null) {
      event.getCompletionTask().run();
    }
    return true;
  }

  public void writeFrame(@NonNull @NotNull MQTT_SNPacket frame) {
    frame.setFromAddress(addressKey);
    EndPoint.totalSent.increment();
    selectorTask.push(frame);
    logger.log(ServerLogMessages.PUSH_WRITE, frame);
    sentMessage();
  }

  public MQTT_SNPacket buildPublish(short alias, int packetId, MessageEvent messageEvent, QualityOfService qos, short topicTypeId) {
    Message data = messageEvent.getMessage();
    Publish publish = new Publish(alias, packetId, data.getOpaqueData());
    publish.setQoS(qos);
    publish.setCallback(messageEvent.getCompletionTask());
    publish.setTopicIdType(stateEngine.getTopicAliasManager().getTopicAliasType(messageEvent.getDestinationName()));
    return publish;
  }

  @Override
  public ProtocolInformationDTO getInformation() {
    MqttSnProtocolInformation information = new MqttSnProtocolInformation();
    updateInformation(information);
    Session currentSession = session;
    if (currentSession != null) {
      information.setSessionInfo(currentSession.getSessionInformation());
    }
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
