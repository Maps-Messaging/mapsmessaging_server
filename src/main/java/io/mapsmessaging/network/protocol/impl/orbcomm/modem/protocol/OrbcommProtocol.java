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

package io.mapsmessaging.network.protocol.impl.orbcomm.modem.protocol;

import io.mapsmessaging.api.*;
import io.mapsmessaging.api.features.DestinationMode;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.Priority;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.TypedData;
import io.mapsmessaging.api.transformers.Transformer;
import io.mapsmessaging.dto.rest.config.protocol.impl.OrbCommDTO;
import io.mapsmessaging.dto.rest.protocol.ProtocolInformationDTO;
import io.mapsmessaging.dto.rest.protocol.impl.OrbcommProtocolInformation;
import io.mapsmessaging.engine.destination.MessageOverrides;
import io.mapsmessaging.location.LocationManager;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.ProtocolClientConnection;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.impl.SelectorTask;
import io.mapsmessaging.network.io.impl.serial.SerialEndPoint;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.ProtocolMessageTransformation;
import io.mapsmessaging.network.protocol.impl.nmea.sentences.Sentence;
import io.mapsmessaging.network.protocol.impl.nmea.types.PositionType;
import io.mapsmessaging.network.protocol.impl.orbcomm.modem.device.Modem;
import io.mapsmessaging.network.protocol.impl.orbcomm.modem.device.messages.SendMessageState;
import io.mapsmessaging.network.protocol.impl.orbcomm.modem.device.values.MessageFormat;
import io.mapsmessaging.network.protocol.impl.orbcomm.protocol.OrbCommMessage;
import io.mapsmessaging.network.protocol.impl.orbcomm.protocol.OrbCommMessageFactory;
import io.mapsmessaging.network.protocol.impl.orbcomm.protocol.OrbCommMessageRebuilder;
import io.mapsmessaging.network.protocol.transformation.TransformationManager;
import io.mapsmessaging.selector.operators.ParserExecutor;
import io.mapsmessaging.utilities.threads.SimpleTaskScheduler;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.util.*;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.function.Consumer;

import static io.mapsmessaging.logging.ServerLogMessages.*;

public class OrbcommProtocol extends Protocol implements Consumer<Packet> {


  private final Logger logger = LoggerFactory.getLogger(OrbcommProtocol.class);

  private final Session session;
  private final SelectorTask selectorTask;
  private final Modem modem;
  private final ScheduledFuture<?> scheduledFuture;
  private final Map<String, String> topicNameMapping;
  private final Queue<OrbCommMessage> outboundQueue;
  private final OrbCommMessageRebuilder orbCommMessageRebuilder;
  private final LocationParser locationParser;
  private final boolean setServerLocation;

  private long lastLocationPoll;
  private int messageId;

  public OrbcommProtocol(EndPoint endPoint, Packet packet) throws LoginException, IOException {
    super(endPoint, endPoint.getConfig().getProtocolConfig("stogi"));
    topicNameMapping = new ConcurrentHashMap<>();
    outboundQueue = new ConcurrentLinkedQueue<>();
    orbCommMessageRebuilder = new OrbCommMessageRebuilder();
    locationParser = new LocationParser();
    lastLocationPoll = System.currentTimeMillis();
    messageId = 0;
    modem = new Modem(this);


    if (packet != null) {
      packet.clear();
    }
    if (endPoint instanceof SerialEndPoint serialEndPoint) {
      serialEndPoint.setStreamHandler(new ModemStreamHandler());
    }

    session = setupSession();
    transformation = TransformationManager.getInstance().getTransformation(
        endPoint.getProtocol(),
        endPoint.getName(),
        "stogi",
        session.getSecurityContext().getUsername()
    );

    setTransformation(transformation);
    OrbCommDTO modemConfig = (OrbCommDTO) getProtocolConfig();
    setServerLocation = modemConfig.isSetServerLocation();
    selectorTask = new SelectorTask(this, endPoint.getConfig().getEndPointConfig());
    endPoint.register(SelectionKey.OP_READ, selectorTask.getReadTask());
    initialiseModem(modemConfig.getModemResponseTimeout());
    scheduledFuture = SimpleTaskScheduler.getInstance().scheduleAtFixedRate(this::pollModemForMessages, modemConfig.getMessagePollInterval(), modemConfig.getMessagePollInterval(), TimeUnit.MILLISECONDS);
    completedConnection();
    endPoint.getServer().handleNewEndPoint(endPoint);
  }

  private Session setupSession() throws LoginException, IOException {
    SessionContextBuilder sessionContextBuilder = new SessionContextBuilder("stogi" + endPoint.getId(), new ProtocolClientConnection(this));
    sessionContextBuilder.setSessionExpiry(0);
    sessionContextBuilder.setKeepAlive(0);
    sessionContextBuilder.setPersistentSession(false);
    return SessionManager.getInstance().create(sessionContextBuilder.build(), this);
  }


  private void initialiseModem(long modemResponseTimeout) throws IOException {
    try {
      String init = modem.initializeModem().get(modemResponseTimeout, TimeUnit.MILLISECONDS);
      String query = modem.queryModemInfo().get(modemResponseTimeout, TimeUnit.MILLISECONDS);
      String enable = modem.enableLocation().get(modemResponseTimeout, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (ExecutionException | TimeoutException e) {
      close();
      throw new IOException(e.getCause());
    }
  }

  @Override
  public void close() throws IOException {
    if (scheduledFuture != null) {
      scheduledFuture.cancel(true);
    }
    super.close();
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

    List<OrbCommMessage> messages = OrbCommMessageFactory.createMessages(destinationName, payload);
    messages.getLast().setCompletionCallback(messageEvent.getCompletionTask());
    outboundQueue.addAll(messages);
  }

  private String scanForName(String destinationName) {
    for (Map.Entry<String, String> entry : topicNameMapping.entrySet()) {
      int index = entry.getKey().indexOf("#");
      if (index > 0) {
        String sub = entry.getKey().substring(0, index);
        if (destinationName.startsWith(sub)) {
          destinationName = entry.getValue() + destinationName.substring(sub.length());
        }
      }
    }
    return destinationName;
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
    return "stogi";
  }

  @Override
  public String getSessionId() {
    return "stogi" + endPoint.getName();
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
    OrbcommProtocolInformation information = new OrbcommProtocolInformation();
    updateInformation(information);
    information.setSessionInfo(session.getSessionInformation());
    return information;
  }

  private void pollModemForMessages() {
    logger.log(STOGI_POLLING_MODEM, modem.getType());
    processOutboundMessages();
    processInboundMessages();
    if (setServerLocation) {
      processLocationRequest();
    }
  }

  private void processLocationRequest() {
    if (lastLocationPoll + 60_000 < System.currentTimeMillis()) {
      List<String> location = modem.getLocation().join();
      for (String loc : location) {
        Sentence sentence = locationParser.parseLocation(loc);
        if (sentence != null && sentence.getName().equalsIgnoreCase("GPGGA")) {
          PositionType latitude = (PositionType) sentence.get("latitude");
          PositionType longitude = (PositionType) sentence.get("longitude");
          LocationManager.getInstance().setPosition(latitude.getPosition(), longitude.getPosition());
        }
      }
      lastLocationPoll = System.currentTimeMillis();
    }
  }

  private void processOutboundMessages() {
    if (outboundQueue.isEmpty()) {
      return;
    }
    CompletableFuture<List<SendMessageState>> outgoing = modem.listSentMessages();
    List<SendMessageState> stateList = outgoing.join();
    if (stateList.isEmpty()) {
      OrbCommMessage msg = outboundQueue.peek();
      if (msg != null) {
        sendMessageViaModem(msg);
      }
    } else {
      handleMsgStates(stateList);
    }
  }

  private void handleMsgStates(List<SendMessageState> stateList) {
    for (SendMessageState state : stateList) {
      if (state.getState().equals(SendMessageState.State.TX_FAILED) ||
          state.getState().equals(SendMessageState.State.TX_COMPLETED)) {
        modem.deleteSentMessages(state.getMessageName());
        OrbCommMessage msg = outboundQueue.poll();
        if (msg != null && msg.getCompletionCallback() != null) {
          msg.getCompletionCallback().run();
        }
      }
    }
  }

  private void processInboundMessages() {
    CompletableFuture<List<byte[]>> incoming = modem.fetchAllMessages(MessageFormat.BASE64);
    List<byte[]> messages = incoming.join();
    for (byte[] message : messages) {
      if (message != null) {
        OrbCommMessage orbCommMessage = new OrbCommMessage(message);
        orbCommMessage = orbCommMessageRebuilder.rebuild(orbCommMessage);
        if (orbCommMessage != null) {
          logger.log(STOGI_PROCESSING_INBOUND_EVENT, orbCommMessage.getNamespace());
          sendMessageToTopic(orbCommMessage.getNamespace(), orbCommMessage.getMessage());
        } else {
          logger.log(STOGI_RECEIVED_PARTIAL_MESSAGE, orbCommMessage.getNamespace(), orbCommMessage.getPacketNumber());
        }
      }
    }
  }

  private void sendMessageViaModem(OrbCommMessage orbCommMessage) {
    messageId = (messageId + 1) % 0xff;
    int sin = (orbCommMessage.getNamespace().hashCode() & 0x7F) | 0x80;
    modem.sendMessage(2, sin, messageId, orbCommMessage.packToSend());
    logger.log(STOGI_SEND_MESSAGE_TO_MODEM, orbCommMessage.getNamespace(), orbCommMessage.getPacketNumber());
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
    CompletableFuture<Destination> future = session.findDestination(topicName, DestinationType.TOPIC);
    future.thenApply(destination -> {
      if (destination != null) {
        try {
          destination.storeMessage(message);
        } catch (IOException e) {
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
