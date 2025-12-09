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

package io.mapsmessaging.network.protocol.impl.satellite.modem.protocol;

import io.mapsmessaging.analytics.Analyser;
import io.mapsmessaging.analytics.AnalyserFactory;
import io.mapsmessaging.api.*;
import io.mapsmessaging.api.features.DestinationMode;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.Priority;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.TypedData;
import io.mapsmessaging.api.transformers.InterServerTransformation;
import io.mapsmessaging.dto.rest.analytics.StatisticsConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.StoGiConfigDTO;
import io.mapsmessaging.dto.rest.protocol.ProtocolInformationDTO;
import io.mapsmessaging.dto.rest.protocol.impl.SatelliteProtocolInformation;
import io.mapsmessaging.engine.destination.MessageOverrides;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.ProtocolClientConnection;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.impl.SelectorTask;
import io.mapsmessaging.network.io.impl.serial.SerialEndPoint;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.impl.satellite.TaskManager;
import io.mapsmessaging.network.protocol.impl.satellite.gateway.io.SatelliteEndPoint;
import io.mapsmessaging.network.protocol.impl.satellite.gateway.model.MessageData;
import io.mapsmessaging.network.protocol.impl.satellite.modem.device.Modem;
import io.mapsmessaging.network.protocol.impl.satellite.modem.device.impl.BaseModemProtocol;
import io.mapsmessaging.network.protocol.impl.satellite.modem.device.impl.data.NetworkStatus;
import io.mapsmessaging.network.protocol.impl.satellite.modem.device.messages.IncomingMessageDetails;
import io.mapsmessaging.network.protocol.impl.satellite.modem.device.messages.ModemSatelliteMessage;
import io.mapsmessaging.network.protocol.impl.satellite.modem.device.messages.SendMessageState;
import io.mapsmessaging.network.protocol.impl.satellite.protocol.*;
import io.mapsmessaging.network.protocol.transformation.ProtocolMessageTransformation;
import io.mapsmessaging.network.protocol.transformation.TransformationManager;
import io.mapsmessaging.selector.operators.ParserExecutor;
import io.mapsmessaging.utilities.filtering.NamespaceFilter;
import io.mapsmessaging.utilities.filtering.NamespaceFilters;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static io.mapsmessaging.logging.ServerLogMessages.*;

public class StoGiProtocol extends Protocol implements Consumer<Packet> {

  private static final String STOGI = "stogi";

  private final TaskManager taskManager;
  private final Logger logger = LoggerFactory.getLogger(StoGiProtocol.class);
  private final Session session;
  private final SelectorTask selectorTask;
  private final Modem modem;
  private final SatelliteMessageRebuilder satelliteMessageRebuilder;
  private final long messagePoll;
  private final long outgoingMessagePollInterval;
  private final AtomicReference<Map<String, List<byte[]>>> pendingMessages;
  private final AtomicReference<Map<String, List<byte[]>>> priorityMessages;
  private final int messageLifeTime;
  private final CipherManager cipherManager;
  private boolean bridgeMode;

  private final String rawMessageTopic;

  private final int maxBufferSize;
  private final int compressionThreshold;
  private final List<SatelliteMessage> currentList;
  private final StatsManager statsManager;
  private final boolean sendHighPriorityEvents;

  private boolean satelliteOnline;
  private int satelliteOnlineCount;

  private ScheduledFuture<?> scheduledFuture;
  private Destination destination;
  private long lastOutgoingMessagePollInterval;

  private int currentStreamId;
  private int messageId;

  public StoGiProtocol(EndPoint endPoint, Packet packet) throws LoginException, IOException {
    super(endPoint, endPoint.getConfig().getProtocolConfig(STOGI));
    satelliteOnline = false;
    satelliteOnlineCount = 0;
    taskManager = new TaskManager();
    satelliteMessageRebuilder = new SatelliteMessageRebuilder();
    messageId = 0;
    currentStreamId = 0;
    currentList = new ArrayList<>();
    pendingMessages = new AtomicReference<>();
    pendingMessages.set(new LinkedHashMap<>());
    priorityMessages = new AtomicReference<>();
    priorityMessages.set(new LinkedHashMap<>());

    if (packet != null) {
      packet.clear();
    }

    ModemStreamHandler streamHandler = new ModemStreamHandler();
    if (endPoint instanceof SerialEndPoint serialEndPoint) {
      serialEndPoint.setStreamHandler(streamHandler);
    }

    session = setupSession();
    session.resumeState();
    protocolMessageTransformation = TransformationManager.getInstance().getTransformation(
        endPoint.getProtocol(),
        endPoint.getName(),
        STOGI,
        session.getSecurityContext().getUsername()
    );

    setProtocolMessageTransformation(protocolMessageTransformation);
    StoGiConfigDTO modemConfig = (StoGiConfigDTO) getProtocolConfig();

    bridgeMode = modemConfig.isBridgeMode();
    messageLifeTime = modemConfig.getMessageLifeTimeInMinutes();
    modem = new Modem(this, modemConfig.getModemResponseTimeout(), streamHandler, taskManager);

    if(!modemConfig.getSharedSecret().trim().isEmpty()) {
      cipherManager = new CipherManager(modemConfig.getSharedSecret().getBytes());
      logger.log(STOGI_ENCRYPTION_STATUS, "enabled");
    }
    else{
      cipherManager = null;
      logger.log(STOGI_ENCRYPTION_STATUS, "disabled");
    }
    long locationPollInterval = modemConfig.getLocationPollInterval() * 1000;
    maxBufferSize = modemConfig.getMaxBufferSize();
    compressionThreshold = modemConfig.getCompressionCutoffSize();
    sendHighPriorityEvents = modemConfig.isSendHighPriorityMessages();
    selectorTask = new SelectorTask(this, endPoint.getConfig().getEndPointConfig());
    endPoint.register(SelectionKey.OP_READ, selectorTask.getReadTask());
    initialiseModem(modemConfig.getModemResponseTimeout());

    messagePoll = modemConfig.getIncomingMessagePollInterval() * 1000;
    outgoingMessagePollInterval = modemConfig.getOutgoingMessagePollInterval() * 1000;
    rawMessageTopic = modemConfig.getModemRawMessages();

    completedConnection();
    String statsDestination = modemConfig.getModemStatsTopic();
    if (statsDestination != null && !statsDestination.isEmpty()) {
      destination = session.findDestination(statsDestination, DestinationType.TOPIC).join();
    }
    lastOutgoingMessagePollInterval = System.currentTimeMillis();
    statsManager = new StatsManager(modem, locationPollInterval, destination);
    scheduledFuture = taskManager.schedule(this::pollModemForMessages, messagePoll, TimeUnit.MILLISECONDS);
    logger.log(STOGI_STARTED_SESSION, modem.getModemProtocol().getType(), messagePoll,outgoingMessagePollInterval );
  }

  // Main function loop, handles modem message flow
  private void pollModemForMessages() {
    long startTime = System.currentTimeMillis();
    boolean hasOutgoing = false;
    long poll = messagePoll;
    try {
      NetworkStatus networkStatus = modem.getNetworkStatus();
      if(!networkStatus.canSend()) {
        poll = 1000; // check every second while not connected
        if(satelliteOnline) {
          satelliteOnlineCount = 0;
          satelliteOnline = false;
          logger.log(STOGI_SATELLITES_STATUS_CHANGE, "Offline - "+networkStatus.noSendReason());
        }
      }
      else{
        if(!satelliteOnline){
          satelliteOnlineCount++;
          if(satelliteOnlineCount > 10){
            logger.log(STOGI_SATELLITES_STATUS_CHANGE, "Online");
            satelliteOnline = true;
            satelliteOnlineCount = 0;
          }
        }
      }
      hasOutgoing = processOutboundMessages();
      if(!hasOutgoing) {
        processInboundMessages();
        statsManager.processLocationRequest(networkStatus);
      }
    } catch (Throwable th) {
      logger.log(STOGI_POLL_RAISED_EXCEPTION, th);
    } finally {
      poll = hasOutgoing?500:poll;
      scheduledFuture = taskManager.schedule(this::pollModemForMessages, poll, TimeUnit.MILLISECONDS);
      startTime = System.currentTimeMillis() - startTime;
      logger.log(STOGI_POLL_FOR_ACTIONS, startTime, poll);
    }
  }

  @Override
  public void close() throws IOException {
    if (scheduledFuture != null) {
      scheduledFuture.cancel(true);
    }
    if (session != null) {
      SessionManager.getInstance().close(session, false);
      modem.close();
    }
    super.close();
    taskManager.close();
  }

  private Session setupSession() throws LoginException, IOException {
    SessionContextBuilder sessionContextBuilder = new SessionContextBuilder(STOGI + endPoint.getId(), new ProtocolClientConnection(this));
    sessionContextBuilder.setSessionExpiry(0);
    sessionContextBuilder.setPersistentSession(false);
    return SessionManager.getInstance().create(sessionContextBuilder.build(), this);
  }


  private void initialiseModem(long modemResponseTimeout) throws IOException {
    try {
      BaseModemProtocol init = modem.initializeModem().get(modemResponseTimeout, TimeUnit.MILLISECONDS);
      if(init == null){
        throw new IOException("Unable to detect modem version");
      }
      modem.queryModemInfo().get(modemResponseTimeout, TimeUnit.MILLISECONDS);
      modem.enableLocation().get(modemResponseTimeout, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (ExecutionException | TimeoutException e) {
      close();
      throw new IOException(e.getCause());
    }
  }

  @Override
  public void connect(String sessionId, String username, String password) throws IOException {
    completedConnection();
  }

  @Override
  public void subscribeRemote(@NonNull @NotNull String resource, @NonNull @NotNull String mappedResource, @NonNull @NotNull QualityOfService qos, @Nullable ParserExecutor executor, @Nullable InterServerTransformation transformer, StatisticsConfigDTO statistics) {
    // Will send a subscribe event, once we have one
  }

  @Override
  public void subscribeLocal(@NonNull @NotNull String resource, @NonNull @NotNull String mappedResource, @NonNull @NotNull QualityOfService qos, @Nullable String selector, @Nullable InterServerTransformation transformer, NamespaceFilters namespaceFilters, StatisticsConfigDTO statistics) throws IOException {
    super.subscribeLocal(resource, mappedResource, qos, selector, transformer, namespaceFilters, statistics);
    SubscriptionContextBuilder builder = createSubscriptionContextBuilder(resource, selector, qos, 1024);
    session.addSubscription(builder.build());
  }

  @Override
  public void sendMessage(@NotNull @NonNull MessageEvent messageEvent) {
    if (bridgeMode) {
      MessageData messageData = new MessageData();
      messageData.setPayload(messageEvent.getMessage().getOpaqueData());
      messageData.setCompletionCallback(messageEvent.getCompletionTask());
      SatelliteMessage satelliteMessage = new BypassSatelliteMessage(0, messageEvent.getMessage().getOpaqueData(), 0, false);
      sendMessageViaModem(currentStreamId, satelliteMessage);
      currentStreamId++;
    } else {
      preparePackedMessage(messageEvent);
    }
  }
  public void preparePackedMessage(@NotNull @NonNull MessageEvent messageEvent) {

    boolean filteredOverride = false;
    int depth = 1;
    try {
      NamespaceFilter namespaceFilter= filterMessage(messageEvent);
      if(namespaceFilter != null) {
        depth = namespaceFilter.getDepth();
        filteredOverride = namespaceFilter.isForcePriority();
      }
    } catch (IOException e) {
      return; // failed filtering
    }

    String destinationName = messageEvent.getDestinationName();
    ParsedMessage parsedMessage = new ParsedMessage(destinationName, messageEvent.getMessage());
    parsedMessage = processInterServerTransformations(messageEvent.getDestinationName(),  parsedMessage);
    Analyser analyser = topicNameAnalyserMap.get(parsedMessage.getDestinationName());
    if(analyser == null && !resourceNameAnalyserMap.isEmpty()){
      StatisticsConfigDTO config = resourceNameAnalyserMap.get(messageEvent.getSubscription().getContext().getAlias());
      if(config != null){
        analyser = AnalyserFactory.getInstance().getAnalyser(config);
        topicNameAnalyserMap.put(messageEvent.getDestinationName(), analyser);
      }
    }

    if(analyser != null) {
      Message message = analyser.ingest(parsedMessage.getMessage());
      if(message == null) {
        if (messageEvent.getCompletionTask() != null) {
          messageEvent.getCompletionTask().run();
        }
        return;
      }
    }


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

    Map<String, List<byte[]>> pending;
    if(sendHighPriorityEvents && (
        payload.getPriority().getValue() > Priority.TWO_BELOW_HIGHEST.getValue())||
        filteredOverride){
      pending = priorityMessages.get();
    }
    else {
      pending = pendingMessages.get();
    }
    List<byte[]> list = pending.computeIfAbsent(destinationName, key -> new ArrayList<>());
    list.add(payload.getOpaqueData());
    while(list.size() > depth){
      list.removeFirst();
    }
    if (messageEvent.getCompletionTask() != null) {
      messageEvent.getCompletionTask().run();
    }
  }


  @Override
  public Subject getSubject() {
    return session.getSecurityContext().getSubject();
  }


  @Override
  public void sendKeepAlive() {
    // no op
  }

  @Override
  public boolean processPacket(Packet packet) throws IOException {
    while (packet.hasRemaining()) {
      modem.process(packet);
    }
    endPoint.register(SelectionKey.OP_READ, selectorTask.getReadTask());
    return true;
  }


  @Override
  public String getName() {
    return STOGI;
  }

  @Override
  public String getSessionId() {
    return STOGI + endPoint.getName();
  }

  @Override
  public String getVersion() {
    return "1.0";
  }

  @Override
  public void accept(Packet packet) {
    try {
      endPoint.sendPacket(packet);
    } catch (IOException e) {
      logger.log(STOGI_EXCEPTION_PROCESSING_PACKET, e);
    }
  }

  @Override
  public ProtocolInformationDTO getInformation() {
    SatelliteProtocolInformation information = new SatelliteProtocolInformation();
    updateInformation(information);
    information.setPacketsSent(modem.getModemProtocol().getSentPackets().get());
    information.setPacketsReceived(modem.getModemProtocol().getReceivedPackets().get());
    information.setBytesReceived(modem.getModemProtocol().getReceivedBytes().get());
    information.setBytesTransmitted(modem.getModemProtocol().getSentBytes().get());
    information.setRemoteDeviceInfo(((SatelliteEndPoint) endPoint).getTerminalInfo());
    information.setSessionInfo(session.getSessionInformation());
    return information;
  }


  private boolean processOutboundMessages() {
    if(!satelliteOnline) {
      return false;
    }

    CompletableFuture<List<SendMessageState>> outgoing = modem.listSentMessages();
    List<SendMessageState> stateList = outgoing.join();
    boolean needToSend = (!priorityMessages.get().isEmpty() ) || (lastOutgoingMessagePollInterval + outgoingMessagePollInterval < System.currentTimeMillis());
    if (stateList.isEmpty() && currentList.isEmpty() && !needToSend) {
      return false;
    }
    if(stateList.isEmpty()) {
      return packAndSendMessages();
    }
    else{
      handleMsgStates(stateList);
    }
    return true;
  }

  private boolean packAndSendMessages() {
    if(currentList.isEmpty()){
      Map<String, List<byte[]>> replacement = priorityMessages.getAndSet(new LinkedHashMap<>());
      if(replacement.isEmpty()){
        lastOutgoingMessagePollInterval = System.currentTimeMillis();
        replacement = this.pendingMessages.getAndSet(new LinkedHashMap<>());
      }
      if(!replacement.isEmpty()) {
        try {
          buildSendList(replacement);
        } catch (IOException e) {
          // Log This
        }
      }
    }
    if(!currentList.isEmpty()){
      SatelliteMessage msg = currentList.removeFirst();
      sendMessageViaModem(currentStreamId,msg);
      logger.log(STOGI_SENT_MESSAGE_TO_MODEM, msg.getMessage().length);
    }
    return currentList.isEmpty();
  }

  private void buildSendList(Map<String, List<byte[]>> replacement) throws IOException {
    if(!replacement.isEmpty()){
      MessageQueuePacker.Packed packedQueue = MessageQueuePacker.pack(replacement, compressionThreshold, cipherManager, protocolMessageTransformation);
      int v = protocolMessageTransformation == null? 0: protocolMessageTransformation.getId();
      currentList.addAll(SatelliteMessageFactory.createMessages(currentStreamId,  packedQueue.data(), maxBufferSize, packedQueue.compressed(), (byte)v));
      currentStreamId++;
    }
  }

  private void handleMsgStates(List<SendMessageState> stateList) {
    for (SendMessageState state : stateList) {
      if (state.getState().equals(SendMessageState.State.TX_FAILED) ||
          state.getState().equals(SendMessageState.State.TX_COMPLETED)) {
        modem.deleteSentMessages(state.getMessageName());
      }
    }
  }

  private void processInboundMessages() {
    List<IncomingMessageDetails> incoming = modem.listIncomingMessages().join();
    if (!incoming.isEmpty()) {
      List<ModemSatelliteMessage> messages = retrieveIncomingList(incoming);
      processMessages(messages);
    }
  }

  private List<ModemSatelliteMessage> retrieveIncomingList(List<IncomingMessageDetails> incoming) {
    List<ModemSatelliteMessage> modemSatelliteMessages = new ArrayList<>();
    for (IncomingMessageDetails details : incoming) {
      ModemSatelliteMessage satelliteMessage;
      satelliteMessage = modem.getMessage(details).join();
      if (satelliteMessage != null) {
        receivedMessage();
        endPoint.getEndPointStatus().updateReadBytes(satelliteMessage.getPayload().length);
        modemSatelliteMessages.add(satelliteMessage);
        logger.log(STOGI_RECEIVED_MESSAGE_TO_MODEM, satelliteMessage.getPayload().length);
      }
      try {
        Thread.sleep(500); // Required to allow modem to return to normal mode
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
    return modemSatelliteMessages;
  }

  private void processMessages(List<ModemSatelliteMessage> messages) {
    for (ModemSatelliteMessage message : messages) {
      if (message != null) {
        SatelliteMessage loaded = new SatelliteMessage(message.getSin(), message.getPayload());
        if(loaded.isRaw()){
          String tmp = rawMessageTopic;
          tmp = tmp.replace("{sin}", Integer.toString(loaded.getStreamNumber()));
          tmp = tmp.replace("{min}", Integer.toString(message.getMin()));
          sendMessageToTopic(tmp, loaded.getMessage());
        }
        else {
          SatelliteMessage satelliteMessage = satelliteMessageRebuilder.rebuild(loaded);
          if (satelliteMessage != null) {
            logger.log(STOGI_PROCESSING_INBOUND_EVENT, satelliteMessage.getPacketNumber());
            try {
              Map<String, List<byte[]>> receivedEventMap = MessageQueueUnpacker.unpack(satelliteMessage.getMessage(), satelliteMessage.isCompressed(), cipherManager);
              publishIncomingMap(receivedEventMap);
            } catch (Throwable e) {
              logger.log(STOGI_EXCEPTION_PROCESSING_PACKET, e);
            }
          } else {
            logger.log(STOGI_RECEIVED_PARTIAL_MESSAGE, loaded.getPacketNumber());
          }
        }
      }
    }
    modem.waitForModemActivity();

    for (ModemSatelliteMessage message : messages) {
      modem.markMessageRetrieved(message.getName()).join();
    }
  }

  private void publishIncomingMap(Map<String, List<byte[]>> incomingMap) {
    for(Map.Entry<String, List<byte[]>> entry : incomingMap.entrySet()) {
      for(byte[] bytes : entry.getValue()) {
        sendMessageToTopic(entry.getKey(), bytes);
      }
    }
  }

  private void sendMessageViaModem(int streamNumber, SatelliteMessage satelliteMessage) {
    messageId = (messageId + 1) % 0xff;
    int sin = (streamNumber & 0x7F) | 0x80;
    byte[] buffer = satelliteMessage.packToSend();
    sentMessage();
    endPoint.getEndPointStatus().updateWriteBytes(buffer.length);
    modem.sendMessage(2, sin, messageId, messageLifeTime,  buffer);
    logger.log(STOGI_SEND_MESSAGE_TO_MODEM, satelliteMessage.getPacketNumber(), buffer.length);
  }


  private void sendMessageToTopic(String topic, byte[] data) {
    InterServerTransformation transformer = destinationTransformationLookup(topic);
    Message message = createMessage(
        data,
        getProtocolMessageTransformation(),
        transformer,
        this
    );
    ParserExecutor parserExecutor = getParser(topic);
    if (parserExecutor != null && !parserExecutor.evaluate(message)) {
      return;
    }
    String destinationName = parseForLookup(topic);
    try {
      processValidDestinations(destinationName, message);
    } catch (ExecutionException e) {
      logger.log(STOGI_EXCEPTION_PROCESSING_PACKET, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }


  private void processValidDestinations(String topicName, Message message)
      throws ExecutionException, InterruptedException {
    if (topicName == null || topicName.isEmpty()) return;
    CompletableFuture<Destination> future = session.findDestination(topicName, DestinationType.TOPIC);
    future.thenApply(topic -> {
      if (topic != null) {
        try {
          topic.storeMessage(message);
        } catch (IOException e) {
          // log this!
          try {
            endPoint.close();
          } catch (IOException ioException) {
            logger.log(STOGI_STORE_EVENT_EXCEPTION, ioException);
          }
          future.completeExceptionally(e);
        }
      }
      return destination;
    });
    future.get();
  }

  private Message createMessage(byte[] msg, ProtocolMessageTransformation transformation, InterServerTransformation transformer, Protocol protocol) {
    HashMap<String, String> meta = new LinkedHashMap<>();
    meta.put("protocol", "STOGI");
    meta.put("version", "1");
    meta.put("sessionId", session.getName());
    meta.put("time_ms", "" + System.currentTimeMillis());

    HashMap<String, TypedData> dataHashMap = new LinkedHashMap<>();
    MessageBuilder mb = new MessageBuilder();
    mb.setDataMap(dataHashMap)
        .setPriority(Priority.NORMAL)
        .setRetain(false)
        .setOpaqueData(msg)
        .setMeta(meta)
        .setQoS(QualityOfService.AT_MOST_ONCE)
        .storeOffline(false)
        .setTransformation(transformation);
    return MessageOverrides.createMessageBuilder(protocol.getProtocolConfig().getMessageDefaults(), mb).build();
  }

  @Override
  public String parseForLookup(String lookup) {
    String exact = topicNameMapping.get(lookup);
    if (exact != null) return exact;

    String ns = DestinationMode.SCHEMA.getNamespace();
    String nsLower = ns.toLowerCase();

    // Longest-prefix wildcard match (key ends with '#')
    String bestKey = null;
    String bestVal = null;
    int bestLen = -1;

    for (Map.Entry<String, String> e : topicNameMapping.entrySet()) {
      String k = e.getKey();
      if (!k.endsWith("#")) continue;
      String prefix = k.substring(0, k.length() - 1);
      if (lookup.startsWith(prefix) && prefix.length() > bestLen) {
        bestLen = prefix.length();
        bestKey = k;
        bestVal = e.getValue();
      }
    }

    if (bestKey == null) return lookup;

    // Strip schema namespace prefix (case-insensitive)
    String normLookup = lookup;
    if (normLookup.toLowerCase().startsWith(nsLower)) {
      normLookup = normLookup.substring(ns.length());
    }

    String result = bestVal + normLookup;
    result = result.replace("#", "").replaceAll("/{2,}", "/");
    return result;
  }


}
