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

import io.mapsmessaging.api.*;
import io.mapsmessaging.api.features.DestinationMode;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.Priority;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.TypedData;
import io.mapsmessaging.api.transformers.Transformer;
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
import io.mapsmessaging.network.protocol.ProtocolMessageTransformation;
import io.mapsmessaging.network.protocol.impl.satellite.TaskManager;
import io.mapsmessaging.network.protocol.impl.satellite.gateway.io.SatelliteEndPoint;
import io.mapsmessaging.network.protocol.impl.satellite.modem.device.Modem;
import io.mapsmessaging.network.protocol.impl.satellite.modem.device.impl.BaseModemProtocol;
import io.mapsmessaging.network.protocol.impl.satellite.modem.device.impl.data.NetworkStatus;
import io.mapsmessaging.network.protocol.impl.satellite.modem.device.messages.IncomingMessageDetails;
import io.mapsmessaging.network.protocol.impl.satellite.modem.device.messages.ModemSatelliteMessage;
import io.mapsmessaging.network.protocol.impl.satellite.modem.device.messages.SendMessageState;
import io.mapsmessaging.network.protocol.impl.satellite.protocol.*;
import io.mapsmessaging.network.protocol.transformation.TransformationManager;
import io.mapsmessaging.selector.operators.ParserExecutor;
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
  private final int messageLifeTime;
  private final CipherManager cipherManager;

  private final int maxBufferSize;
  private final int compressionThreshold;
  private final List<SatelliteMessage> currentList;
  private final LocationHandler locationHandler;

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
    if (packet != null) {
      packet.clear();
    }

    ModemStreamHandler streamHandler = new ModemStreamHandler();
    if (endPoint instanceof SerialEndPoint serialEndPoint) {
      serialEndPoint.setStreamHandler(streamHandler);
    }

    session = setupSession();
    session.resumeState();
    transformation = TransformationManager.getInstance().getTransformation(
        endPoint.getProtocol(),
        endPoint.getName(),
        STOGI,
        session.getSecurityContext().getUsername()
    );

    setTransformation(transformation);
    StoGiConfigDTO modemConfig = (StoGiConfigDTO) getProtocolConfig();

    messageLifeTime = modemConfig.getMessageLifeTimeInMinutes();
    modem = new Modem(this, modemConfig.getModemResponseTimeout(), streamHandler);

    if(!modemConfig.getSharedSecret().trim().isEmpty()) {
      cipherManager = new CipherManager(modemConfig.getSharedSecret().getBytes());
    }
    else{
      cipherManager = null;
    }
    long locationPollInterval = modemConfig.getLocationPollInterval() * 1000;
    maxBufferSize = modemConfig.getMaxBufferSize();
    compressionThreshold = modemConfig.getCompressionCutoffSize();

    selectorTask = new SelectorTask(this, endPoint.getConfig().getEndPointConfig());
    endPoint.register(SelectionKey.OP_READ, selectorTask.getReadTask());
    initialiseModem(modemConfig.getModemResponseTimeout());

    messagePoll = modemConfig.getIncomingMessagePollInterval() * 1000;
    outgoingMessagePollInterval = modemConfig.getOutgoingMessagePollInterval() * 1000;
    completedConnection();
    endPoint.getServer().handleNewEndPoint(endPoint);
    String statsDestination = modemConfig.getModemStatsTopic();
    if (statsDestination != null && !statsDestination.isEmpty()) {
      destination = session.findDestination(statsDestination, DestinationType.TOPIC).join();
    }
    lastOutgoingMessagePollInterval = System.currentTimeMillis();
    locationHandler = new LocationHandler(modem, locationPollInterval, destination);
    scheduledFuture = taskManager.schedule(this::pollModemForMessages, messagePoll, TimeUnit.MILLISECONDS);
  }

  // Main function loop, handles modem message flow
  private void pollModemForMessages() {
    boolean hasOutgoing = false;
    long poll = messagePoll;
    try {
      logger.log(STOGI_POLLING_MODEM, modem.getType());
      NetworkStatus networkStatus = modem.getNetworkStatus();
      if(!networkStatus.canSend()) {
        poll = 1000; // check every second while not connected
        satelliteOnlineCount = 0;
        satelliteOnline = false;
        // Log this!!
      }
      else{
        if(!satelliteOnline){
          satelliteOnlineCount++;
          if(satelliteOnlineCount > 10){
            satelliteOnline = true;
            satelliteOnlineCount = 0;
          }
        }
      }
      hasOutgoing = processOutboundMessages();
      if(!hasOutgoing) {
        processInboundMessages();
        locationHandler.processLocationRequest();
      }
    } catch (Throwable th) {
      // Log This, it's important
    } finally {
      poll = hasOutgoing?500:poll;
      scheduledFuture = taskManager.schedule(this::pollModemForMessages, poll, TimeUnit.MILLISECONDS);
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
  public void subscribeRemote(@NonNull @NotNull String resource, @NonNull @NotNull String mappedResource, @Nullable ParserExecutor executor, @Nullable Transformer transformer) {
    // Will send a subscribe event, once we have one
  }

  @Override
  public void subscribeLocal(@NonNull @NotNull String resource, @NonNull @NotNull String mappedResource, @Nullable String selector, @Nullable Transformer transformer) throws IOException {
    topicNameMapping.put(resource, mappedResource);
    if (transformer != null) {
      destinationTransformerMap.put(mappedResource, transformer);
    }
    SubscriptionContextBuilder builder = createSubscriptionContextBuilder(resource, selector, QualityOfService.AT_MOST_ONCE, 1024);
    session.addSubscription(builder.build());
  }

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
    Map<String, List<byte[]>> pending = pendingMessages.get();
    List<byte[]> list = pending.computeIfAbsent(destinationName, key -> new ArrayList<>());
    list.add(payload);
    if(messageEvent.getCompletionTask() != null) {
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
    return "0.1";
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
    if (stateList.isEmpty() &&  currentList.isEmpty() && lastOutgoingMessagePollInterval + outgoingMessagePollInterval > System.currentTimeMillis()) {
      return false;
    }
    if (stateList.isEmpty()) {
      lastOutgoingMessagePollInterval = System.currentTimeMillis();
      if(currentList.isEmpty()){
        Map<String, List<byte[]>> replacement = this.pendingMessages.getAndSet(new LinkedHashMap<>());
        if(!replacement.isEmpty()){
          MessageQueuePacker.Packed packedQueue = MessageQueuePacker.pack(replacement, compressionThreshold, cipherManager);
          currentList.addAll(SatelliteMessageFactory.createMessages(currentStreamId,  packedQueue.data(), maxBufferSize, packedQueue.compressed()));
          currentStreamId++;
        }
      }
      sendMessageViaModem(currentStreamId, currentList.remove(0));
      return currentList.isEmpty();
    } else {
      handleMsgStates(stateList);
    }
    return true;
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
        modemSatelliteMessages.add(satelliteMessage);
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
        SatelliteMessage satelliteMessage = satelliteMessageRebuilder.rebuild(loaded);
        if (satelliteMessage != null) {
          logger.log(STOGI_PROCESSING_INBOUND_EVENT, satelliteMessage.getPacketNumber());
          try {
            Map<String, List<byte[]>> receivedEventMap = MessageQueueUnpacker.unpack(satelliteMessage.getMessage(), satelliteMessage.isCompressed(), cipherManager);
            publishIncomingMap(receivedEventMap);
          } catch (Throwable e) {
            // ToDo Log
          }
        } else {
          logger.log(STOGI_RECEIVED_PARTIAL_MESSAGE, loaded.getPacketNumber());
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
    modem.sendMessage(2, sin, messageId, messageLifeTime,  satelliteMessage.packToSend());
    logger.log(STOGI_SEND_MESSAGE_TO_MODEM, satelliteMessage.getPacketNumber());
  }


  private void sendMessageToTopic(String topic, byte[] data) {
    Transformer transformer = destinationTransformationLookup(topic);
    Message message = createMessage(
        data,
        getTransformation(),
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
      // log this
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

  private Message createMessage(byte[] msg, ProtocolMessageTransformation transformation, Transformer transformer, Protocol protocol) {
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
        .setTransformation(transformation)
        .setDestinationTransformer(transformer);
    return MessageOverrides.createMessageBuilder(protocol.getProtocolConfig().getMessageDefaults(), mb).build();
  }

  private String parseForLookup(String lookup) {
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
    // Normalize: remove any '#', collapse multiple slashes
    result = result.replace("#", "").replaceAll("/{2,}", "/");
    return result;
  }


}
