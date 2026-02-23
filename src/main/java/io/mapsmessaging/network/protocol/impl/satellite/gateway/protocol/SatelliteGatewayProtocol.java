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

package io.mapsmessaging.network.protocol.impl.satellite.gateway.protocol;

import io.mapsmessaging.api.*;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.Priority;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.transformers.ParsedMessage;
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
import io.mapsmessaging.network.protocol.transformation.ProtocolMessageTransformation;
import io.mapsmessaging.network.protocol.transformation.TransformationManager;
import io.mapsmessaging.utilities.filtering.NamespaceFilter;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static io.mapsmessaging.logging.ServerLogMessages.*;


public class SatelliteGatewayProtocol extends Protocol {

  private final Logger logger = LoggerFactory.getLogger(SatelliteGatewayProtocol.class);
  private final SatelliteMessageRebuilder messageRebuilder;
  private final Session session;
  private final Map<String, List<byte[]>> pendingMessages;
  private final Map<String, List<byte[]>> priorityMessages;
  private final CipherManager cipherManager;

  private final String mapsOutboundNamespacePath;

  private final String mapsIncomingNamespacePath;
  private final String commonIncomingNamespacePath;
  private final long outgoingPollInterval;

  private final int maxBufferSize;
  private final int compressionThreshold;
  private final int sinNumber;
  private final boolean sendHighPriorityEvents;

  private final Object outboundLock = new Object();

  private ScheduledFuture<?> scheduledFuture;
  private long nextOutgoingTime;
  private AtomicBoolean closed;

  @SuppressWarnings("java:S2245") // we use random here for polling, this is not a security based issue
  public SatelliteGatewayProtocol(@NonNull @NotNull EndPoint endPoint, @NotNull @NonNull ProtocolConfigDTO protocolConfig) throws LoginException, IOException {
    super(endPoint, protocolConfig);
    pendingMessages = new LinkedHashMap<>();
    priorityMessages = new LinkedHashMap<>();
    String primeId = ((SatelliteEndPoint) endPoint).getTerminalInfo().getUniqueId();
    SatelliteConfigDTO config = (SatelliteConfigDTO) protocolConfig;
    sendHighPriorityEvents = config.isSendHighPriorityMessages();
    if(!config.getSharedSecret().trim().isEmpty()) {
      cipherManager = new CipherManager(config.getSharedSecret().getBytes());
    }
    else{
      cipherManager = null;
    }
    sinNumber = config.getSinNumber();
    outgoingPollInterval = config.getOutgoingMessagePollInterval() * 1000;
    maxBufferSize = config.getMaxBufferSize();
    compressionThreshold = config.getCompressionCutoffSize();
    closed = new AtomicBoolean(false);
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
    String outBoundNamespacePath = config.getCommonOutboundPublishRoot().trim();
    if(!outBoundNamespacePath.isEmpty()){
      String path = outBoundNamespacePath.replace("{deviceId}", primeId);
      path = path.replace("{mailboxId}", config.getMailboxId());
      SubscriptionContextBuilder subBuilder = new SubscriptionContextBuilder(path, ClientAcknowledgement.AUTO);
      subBuilder.setQos(QualityOfService.AT_MOST_ONCE)
          .setReceiveMaximum(config.getMaxInflightEventsPerDevice())
          .setAlias("common_requests")
          .setNoLocalMessages(true);
      session.addSubscription(subBuilder.build());
    }
    String outboundNameSpace = config.getMapsOutboundPublishRoot();
    if(!outboundNameSpace.isEmpty()){
      String path = outboundNameSpace.replace("{deviceId}", primeId);
      path = path.replace("{mailboxId}", config.getMailboxId());
      SubscriptionContextBuilder subBuilder = new SubscriptionContextBuilder(path, ClientAcknowledgement.AUTO);
      subBuilder.setQos(QualityOfService.AT_MOST_ONCE)
          .setReceiveMaximum(config.getMaxInflightEventsPerDevice())
          .setAlias("maps_requests")
          .setNoLocalMessages(true);
      session.addSubscription(subBuilder.build());
      if(path.indexOf('+' ) != -1){
        path = path.substring(0, path.indexOf('+' ));
      }
      if(path.indexOf('#' ) != -1){
        path = path.substring(0, path.indexOf('#' ));
      }
      mapsOutboundNamespacePath = path;

    }
    else{
      mapsOutboundNamespacePath = null;
    }
    mapsIncomingNamespacePath = parsePath(config.getMapsInboundPublishRoot(), "/{deviceId}/maps/in", primeId, config.getMailboxId());
    commonIncomingNamespacePath = parsePath(config.getCommonInboundPublishRoot(), "/{deviceId}/common/in/{sin}/{min}", primeId, config.getMailboxId());


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
    scheduledFuture = TaskManager.getInstance().schedule(this::processOutstandingMessages, 15, TimeUnit.SECONDS);
  }

  private String parsePath(String path, String defaultValue, String primeId, String mailboxId){
    if(path == null || path.isEmpty()){
      path = defaultValue;
    }
    path = path.replace("{deviceId}", primeId);
    return path.replace("{mailboxId}", mailboxId);
  }

  @Override
  public void close() throws IOException {
    if (closed.compareAndSet(false, true)) {
      ((SatelliteEndPoint) endPoint).mute();
      if (scheduledFuture != null) {
        scheduledFuture.cancel(true);
      }
      SessionManager.getInstance().close(session, false);
      super.close();
    }
  }

  private void requestClose() {
    try {
      endPoint.close();
    } catch (IOException ignored) {
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
    if(messageEvent.getSubscription().getContext().getAlias() != null &&
        messageEvent.getSubscription().getContext().getAlias().equals("common_requests")){
      MessageData messageData = new MessageData();
      byte[] tmp = messageEvent.getMessage().getOpaqueData();
      int sin = tmp[0] & 0xFF;
      int min = tmp[1] & 0xFF;
      messageData.setPayload(tmp);
      messageData.setCompletionCallback(messageEvent.getCompletionTask());
      ((SatelliteEndPoint) endPoint).sendMessage(messageData);
      logger.log(SATELLITE_SENT_RAW_MESSAGE, Integer.toString(sin),  Integer.toString(min), tmp.length );
    } else {
      preparePackedMessage(messageEvent);
    }
  }

  private void preparePackedMessage(MessageEvent messageEvent) {
    boolean filteredOverride = false;
    int depth = 1;
    try {
      NamespaceFilter namespaceFilter= filterMessage(messageEvent);
      if(namespaceFilter != null){
        depth = namespaceFilter.getConfig().getDepth();
        filteredOverride = namespaceFilter.getConfig().isForcePriority();
      }
    } catch (IOException e) {
      logger.log(SATELLITE_FILTER_FAILED, e);
      return; // failed filtering
    }

    String destinationName = messageEvent.getDestinationName();
    if(mapsOutboundNamespacePath != null && destinationName.startsWith(mapsOutboundNamespacePath)){
      destinationName = destinationName.substring(mapsOutboundNamespacePath.length());
      if(!(destinationName.startsWith("/"))){
        destinationName = "/" + destinationName;
      }
    }
    ParsedMessage parsedMessage = new ParsedMessage(destinationName, messageEvent.getMessage());
    parsedMessage = processInterServerTransformations (messageEvent.getDestinationName(), parsedMessage);
    Message payload = parsedMessage.getMessage();
    if (protocolMessageTransformation != null) {
      payload = protocolMessageTransformation.outgoing(payload, messageEvent.getDestinationName());
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

    boolean isHighPriority = parsedMessage.getMessage().getPriority().getValue() > Priority.TWO_BELOW_HIGHEST.getValue();
    Map<String, List<byte[]>> pendingRef;
    if (sendHighPriorityEvents && (isHighPriority || filteredOverride)) {
      pendingRef = priorityMessages;
    } else {
      pendingRef = pendingMessages;
    }

    Runnable completionTask = messageEvent.getCompletionTask();

    int listSize =0;
    synchronized (outboundLock) {
      List<byte[]> list = pendingRef.computeIfAbsent(destinationName, key -> new ArrayList<>());
      list.add(payload.getOpaqueData());
      while (list.size() > depth) {
        list.removeFirst();
      }
      listSize = list.size();
    }

    if (completionTask != null) {
      completionTask.run();
    }
    logger.log(SATELLITE_QUEUED_PENDING_MESSAGE, destinationName, listSize);
  }

  private void packAndSend(Map<String, List<byte[]>> replacement) throws IOException {
    if(!replacement.isEmpty()) {
      MessageQueuePacker.Packed packedQueue = MessageQueuePacker.pack(replacement, compressionThreshold, cipherManager, null);
      List<SatelliteMessage> toSend = SatelliteMessageFactory.createMessages(packedQueue.data(), maxBufferSize, packedQueue.compressed(), (byte) packedQueue.transformerNumber());
      int sin = (sinNumber & 0x7f) | 0x80;
      long totalPayloadSize = 0;
      for (SatelliteMessage satelliteMessage : toSend) {
        byte[] tmp = satelliteMessage.packToSend();
        byte[] payload = new byte[tmp.length + 2];
        payload[0] = (byte)sin;
        payload[1] = (byte) 0;
        System.arraycopy(tmp, 0, payload, 2, tmp.length);
        totalPayloadSize += payload.length;
        MessageData submitMessage = new MessageData();
        submitMessage.setPayload(payload);
        ((SatelliteEndPoint) endPoint).sendMessage(submitMessage);
      }
      logger.log(SATELLITE_SENT_PACKED_MESSAGES, sin, toSend.size(), totalPayloadSize);
    }
  }


  private void processOutstandingMessages() {
    try {
      Map<String, List<byte[]>> replacement;
      try {
        if (System.currentTimeMillis() > nextOutgoingTime) {
          synchronized (outboundLock) {
            replacement = new LinkedHashMap<>(pendingMessages);
            pendingMessages.clear();
          }
          packAndSend(replacement);
          nextOutgoingTime = System.currentTimeMillis() + outgoingPollInterval;
        } else {
          synchronized (outboundLock) {
            replacement = new LinkedHashMap<>(priorityMessages);
            priorityMessages.clear();
          }
          if (!replacement.isEmpty()) {
            packAndSend(replacement);
          }
        }
      }
      catch (IOException e) {
        // Log this
      }
    } finally {
      if(!closed.get()) {
        scheduledFuture = TaskManager.getInstance().schedule(this::processOutstandingMessages, 15, TimeUnit.SECONDS);
      }
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
    return "1.0";
  }

  public void handleIncomingMessage(MessageData message) throws ExecutionException, InterruptedException {
    byte[] raw = message.getPayload();
    if(message.isCommon()){
      handleCommonMessage(message, raw);
    }
    else {
      byte[] tmp = new byte[raw.length - 2];
      System.arraycopy(raw, 2, tmp, 0, tmp.length);
      SatelliteMessage satelliteMessage = new SatelliteMessage(tmp);
      Map<String, String> meta = message.getMeta();
      if (satelliteMessage.isRaw()) {
        handleCommonMessage(message, raw);
      } else {
        processMapsMessage(satelliteMessage, meta, raw);
      }
    }
  }

  private void handleCommonMessage(MessageData message, byte[] raw) {
    int sin = message.getSin() & 0xff;
    int min = message.getMin() & 0xff;
    String path = commonIncomingNamespacePath;
    path = path.replace("{sin}", String.valueOf(sin));
    path = path.replace("{min}", String.valueOf(min));
    logger.log(SATELLITE_RECEIVED_RAW_MESSAGE, sin, min, raw.length, path);
    publishMessage(raw, path, null, new LinkedHashMap<>());
  }


  private void processMapsMessage(SatelliteMessage satelliteMessage, Map<String, String> meta,  byte[] raw){
    satelliteMessage = messageRebuilder.rebuild(satelliteMessage);
    if (satelliteMessage != null) {
      int id = satelliteMessage.getTransformationId();
      ProtocolMessageTransformation transformation1 = TransformationManager.getInstance().getTransformation(id);
      try {
        long destinationCount = 0;
        long messageCount = 0;
        Map<String, List<byte[]>> receivedEventMap = MessageQueueUnpacker.unpack(satelliteMessage.getMessage(), satelliteMessage.isCompressed(), cipherManager);
        for (Map.Entry<String, List<byte[]>> entry : receivedEventMap.entrySet()) {
          destinationCount++;
          messageCount += entry.getValue().size();
          String topic = entry.getKey();
          boolean isSchema = false;
          if(topic.toLowerCase().startsWith("$schema")){
            topic = topic.substring("$schema".length());
            isSchema = true;
          }
          if(mapsIncomingNamespacePath != null && !mapsIncomingNamespacePath.isEmpty()){
            topic = mapsIncomingNamespacePath + "/" + topic;
            topic = topic.replace("//", "/"); // to be sure
          }
          if(isSchema){
            topic = "$schema"+topic;
          }
          publishEvents(topic, entry.getValue(), transformation1, meta);
        }
        logger.log(SATELLITE_RECEIVED_PACKED_MESSAGE, destinationCount, messageCount,  satelliteMessage.getMessage().length, raw.length);
      } catch (IOException  e) {
        logger.log(INMARSAT_FAILED_PROCESSING_INCOMING, e);
      }
    }
  }

  private void publishEvents(String namespace, List<byte[]> list, ProtocolMessageTransformation transformation1, Map<String, String> meta) {
    for (byte[] buffer : list) {
      publishMessage(buffer, namespace, transformation1, meta);
    }
  }

  private void publishMessage(byte[] buffer, String namespace, ProtocolMessageTransformation transformation1, Map<String, String> meta)  {
    MessageBuilder messageBuilder = new MessageBuilder();
    messageBuilder.setOpaqueData(buffer);
    messageBuilder.setMeta(meta);
    // Transform
    if(transformation1 != null) {
      transformation1.incoming(messageBuilder);
    }
    Message mapsMessage = messageBuilder.build();

    session.findDestination(namespace, DestinationType.TOPIC)
        .thenAccept(destination -> {
          if(destination == null){
            return;
          }
          try {
            destination.storeMessage(mapsMessage);
          } catch (IOException e) {
            logger.log(OGWS_FAILED_TO_SAVE_MESSAGE, e);
            requestClose();
          }
        })
        .exceptionally(ex -> {
          logger.log(OGWS_FAILED_TO_SAVE_MESSAGE, ex);
          requestClose();
          return null;
        });
  }
}
