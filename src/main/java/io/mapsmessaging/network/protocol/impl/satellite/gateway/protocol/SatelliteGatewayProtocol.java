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

package io.mapsmessaging.network.protocol.impl.satellite.gateway.protocol;

import io.mapsmessaging.api.*;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.Priority;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.dto.rest.config.protocol.ProtocolConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.SatelliteConfigDTO;
import io.mapsmessaging.dto.rest.protocol.ProtocolInformationDTO;
import io.mapsmessaging.dto.rest.protocol.impl.SatelliteProtocolInformation;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.ProtocolClientConnection;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.impl.satellite.TaskManager;
import io.mapsmessaging.network.protocol.impl.satellite.gateway.io.SatelliteEndPoint;
import io.mapsmessaging.network.protocol.impl.satellite.gateway.model.MessageData;
import io.mapsmessaging.network.protocol.impl.satellite.protocol.*;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static io.mapsmessaging.logging.ServerLogMessages.*;


public class SatelliteGatewayProtocol extends Protocol {

  private final TaskManager taskManager;
  private final Logger logger = LoggerFactory.getLogger(SatelliteGatewayProtocol.class);
  private final SatelliteMessageRebuilder messageRebuilder;
  private final Session session;
  private final String namespacePath;
  private final int maxBufferSize;
  private final int compressionThreshold;
  private final AtomicReference<Map<String, List<byte[]>>> pendingMessages;
  private final AtomicReference<Map<String, List<byte[]>>> priorityMessages;
  private final long outgoingPollInterval;
  private final CipherManager cipherManager;
  private final boolean sendHighPriorityEvents;


  private long nextOutgoingTime;
  private ScheduledFuture<?> scheduledFuture;
  private boolean closed;
  private int currentStreamId =0;

  public SatelliteGatewayProtocol(@NonNull @NotNull EndPoint endPoint, @NotNull @NonNull ProtocolConfigDTO protocolConfig) throws LoginException, IOException {
    super(endPoint, protocolConfig);
    pendingMessages = new AtomicReference<>();
    pendingMessages.set(new LinkedHashMap<>());
    priorityMessages = new AtomicReference<>();
    priorityMessages.set(new LinkedHashMap<>());
    taskManager = new TaskManager();
    String primeId = ((SatelliteEndPoint) endPoint).getTerminalInfo().getUniqueId();
    SatelliteConfigDTO config = (SatelliteConfigDTO) protocolConfig;
    sendHighPriorityEvents = config.isSendHighPriorityMessages();
    if(!config.getSharedSecret().trim().isEmpty()) {
      cipherManager = new CipherManager(config.getSharedSecret().getBytes());
    }
    else{
      cipherManager = null;
    }
    outgoingPollInterval = config.getOutgoingMessagePollInterval() * 1000;
    maxBufferSize = config.getMaxBufferSize();
    compressionThreshold = config.getCompressionCutoffSize();
    closed = false;
    messageRebuilder = new SatelliteMessageRebuilder();
    SessionContextBuilder scb = new SessionContextBuilder(primeId, new ProtocolClientConnection(this));
    scb.setPersistentSession(false)
        .setResetState(true)
        .setSessionExpiry(100)
        .setReceiveMaximum(config.getMaxInflightEventsPerDevice());


    long millis = TimeUnit.SECONDS.toMillis(config.getMaxInflightEventsPerDevice());
    Random random = new Random();
    setKeepAlive(millis + random.nextLong(millis));
    session = SessionManager.getInstance().create(scb.build(), this);
    session.resumeState();
    namespacePath = config.getOutboundNamespaceRoot().trim();
    if(!namespacePath.isEmpty()){
      String path = namespacePath.replace("{deviceId}", primeId);
      SubscriptionContextBuilder subBuilder = new SubscriptionContextBuilder(path, ClientAcknowledgement.AUTO);
      subBuilder.setQos(QualityOfService.AT_MOST_ONCE)
          .setReceiveMaximum(config.getMaxInflightEventsPerDevice())
          .setNoLocalMessages(true);
      session.addSubscription(subBuilder.build());
    }

    String bcast = config.getOutboundBroadcast();
    if(bcast != null && !bcast.isEmpty()){
      SubscriptionContextBuilder subBuilder = new SubscriptionContextBuilder(bcast, ClientAcknowledgement.AUTO);
      subBuilder.setQos(QualityOfService.AT_MOST_ONCE)
          .setReceiveMaximum(config.getMaxInflightEventsPerDevice())
          .setNoLocalMessages(true);
      session.addSubscription(subBuilder.build());
    }
    ((SatelliteEndPoint) endPoint).unmute();
    nextOutgoingTime = System.currentTimeMillis() + outgoingPollInterval;
    scheduledFuture = taskManager.schedule(this::processOutstandingMessages, 15, TimeUnit.SECONDS);
  }

  @Override
  public void close() throws IOException {
    if (!closed) {
      ((SatelliteEndPoint) endPoint).mute();
      if(scheduledFuture != null){
        scheduledFuture.cancel(true);
      }
      closed = true;
      SessionManager.getInstance().close(session, false);
      super.close();
      taskManager.close();
    }
  }

  public Subject getSubject() {
    if (session != null) {
      return session.getSecurityContext().getSubject();
    }
    return new Subject();
  }

  @Override
  public void sendKeepAlive() {
    try {
      ((SatelliteEndPoint) endPoint).updateTerminalInfo();
    } catch (IOException e) {
      logger.log(INMARSAT_WEB_REQUEST_FAILED, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  @Override
  public String getSessionId() {
    if (session == null) {
      return "waiting";
    }
    return session.getName();
  }

  @Override
  public ProtocolInformationDTO getInformation() {
    SatelliteProtocolInformation information = new SatelliteProtocolInformation();
    updateInformation(information);
    information.setRemoteDeviceInfo(((SatelliteEndPoint) endPoint).getTerminalInfo());
    information.setSessionInfo(session.getSessionInformation());
    return information;
  }


  @Override
  public void sendMessage(@NotNull @NonNull MessageEvent messageEvent) {
    String destinationName = messageEvent.getDestinationName();
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
      } else {
        scanForName(destinationName);
      }
    }
    destinationName = scanForName(destinationName);
    Map<String, List<byte[]>> pending;
    if(sendHighPriorityEvents && message.getPriority().getValue() > Priority.TWO_BELOW_HIGHEST.getValue()){
      pending = priorityMessages.get();
    }
    else {
      pending = pendingMessages.get();
    }

    List<byte[]> list =pending.computeIfAbsent(destinationName, key -> new ArrayList<>());
    list.add(payload);
    if(messageEvent.getCompletionTask() != null) {
      messageEvent.getCompletionTask().run();
    }
  }

  private void packAndSend(Map<String, List<byte[]>> replacement){
    if(!replacement.isEmpty()) {
      MessageQueuePacker.Packed packedQueue = MessageQueuePacker.pack(replacement, compressionThreshold, cipherManager);
      List<SatelliteMessage> toSend = SatelliteMessageFactory.createMessages(currentStreamId, packedQueue.data(), maxBufferSize, packedQueue.compressed());
      int sin = (currentStreamId &0x7f) | 0x80;
      int idx =0;
      for (SatelliteMessage satelliteMessage : toSend) {
        byte[] tmp = satelliteMessage.packToSend();
        byte[] payload = new byte[tmp.length + 2];
        payload[0] = (byte)sin;
        payload[1] = (byte) idx++;
        System.arraycopy(tmp, 0, payload, 2, tmp.length);
        MessageData submitMessage = new MessageData();
        submitMessage.setPayload(payload);
        ((SatelliteEndPoint) endPoint).sendMessage(submitMessage);
      }
      currentStreamId++;
    }
  }


  private void processOutstandingMessages() {
    try {
      Map<String, List<byte[]>> replacement;
      if(System.currentTimeMillis() > nextOutgoingTime) {
        replacement = pendingMessages.getAndSet(new LinkedHashMap<>());
        packAndSend(replacement);
        nextOutgoingTime = System.currentTimeMillis() + outgoingPollInterval;
      }
      else {
        replacement = priorityMessages.getAndSet(new LinkedHashMap<>());
        if (!replacement.isEmpty()) {
          packAndSend(replacement);
        }
      }
    } finally {
      scheduledFuture = taskManager.schedule(this::processOutstandingMessages, 15, TimeUnit.SECONDS);
    }
  }

  @Override
  public boolean processPacket(@NonNull @NotNull Packet packet) throws IOException {
    return false;
  }

  @Override
  public String getName() {
    return ((SatelliteEndPoint) endPoint).getTerminalInfo().getUniqueId();
  }

  @Override
  public String getVersion() {
    return "0.1-beta";
  }

  public void handleIncomingMessage(MessageData message) throws ExecutionException, InterruptedException {
    byte[] raw = message.getPayload();
    byte sin = raw[0];
    byte[] tmp = new byte[raw.length - 2];
    System.arraycopy(raw, 2, tmp, 0, tmp.length);
    SatelliteMessage satelliteMessage = new SatelliteMessage(sin, tmp);
    satelliteMessage = messageRebuilder.rebuild(satelliteMessage);
    if (satelliteMessage != null) {
      try {
        Map<String, List<byte[]>> receivedEventMap = MessageQueueUnpacker.unpack(satelliteMessage.getMessage(), satelliteMessage.isCompressed(), cipherManager);
        for(Map.Entry<String, List<byte[]>> entry : receivedEventMap.entrySet()){
          publishEvents(entry.getKey(), entry.getValue());
        }
      } catch (IOException e) {
        logger.log(INMARSAT_FAILED_PROCESSING_INCOMING, e);
      }
    }
  }

  private void publishEvents(String namespace, List<byte[]> list) throws ExecutionException, InterruptedException {
    for (byte[] buffer : list) {
      MessageBuilder messageBuilder = new MessageBuilder();
      messageBuilder.setOpaqueData(buffer);
      Message mapsMessage = messageBuilder.build();
      // Transform

      CompletableFuture<Destination> future = session.findDestination(namespace, DestinationType.TOPIC);
      future.thenApply(destination -> {
        if (destination != null) {
          try {
            destination.storeMessage(mapsMessage);
          } catch (IOException e) {
            logger.log(OGWS_FAILED_TO_SAVE_MESSAGE, e);
            try {
              endPoint.close();
            } catch (IOException ioException) {
              // ignore
            }
            future.completeExceptionally(e);
          }
        }
        return destination;
      });
      future.get();
    }
  }

}
