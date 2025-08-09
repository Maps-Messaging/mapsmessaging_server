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
import io.mapsmessaging.network.ProtocolClientConnection;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.impl.SelectorTask;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.ProtocolMessageTransformation;
import io.mapsmessaging.network.protocol.impl.orbcomm.modem.device.Modem;
import io.mapsmessaging.network.protocol.impl.orbcomm.modem.device.messages.SendMessageState;
import io.mapsmessaging.network.protocol.impl.orbcomm.modem.device.values.MessageFormat;
import io.mapsmessaging.network.protocol.impl.orbcomm.protocol.OrbCommMessage;
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

public class OrbcommProtocol extends Protocol implements Consumer<Packet> {

  private final Session session;
  private final SelectorTask selectorTask;
  private final Modem modem;
  private final ScheduledFuture<?> scheduledFuture;
  private final Map<String, String> topicNameMapping;
  private final Queue<OrbCommMessage> outboundQueue;

  private int messageId;

  public OrbcommProtocol(EndPoint endPoint, Packet packet) throws LoginException, IOException {
    super(endPoint,  endPoint.getConfig().getProtocolConfig("stogi"));
    topicNameMapping = new ConcurrentHashMap<>();
    outboundQueue = new ConcurrentLinkedQueue<>();
    if (packet != null) {
      packet.clear();
    }
    SessionContextBuilder sessionContextBuilder = new SessionContextBuilder("stogi" + endPoint.getId(), new ProtocolClientConnection(this));
    sessionContextBuilder.setSessionExpiry(0);
    sessionContextBuilder.setKeepAlive(0);
    sessionContextBuilder.setPersistentSession(false);
    session = SessionManager.getInstance().create(sessionContextBuilder.build(), this);
    selectorTask = new SelectorTask(this, endPoint.getConfig().getEndPointConfig());
    endPoint.register(SelectionKey.OP_READ, selectorTask.getReadTask());
    ProtocolMessageTransformation transformation = TransformationManager.getInstance().getTransformation(
        endPoint.getProtocol(),
        endPoint.getName(),
        "stogi",
        session.getSecurityContext().getUsername()
    );
    setTransformation(transformation);
    OrbCommDTO modemConfig = (OrbCommDTO) getProtocolConfig();
    long modemResponseTimeout = modemConfig.getModemResponseTimeout();
    modem = new Modem(this);
    try {
      String init = modem.initializeModem().get(modemResponseTimeout, TimeUnit.MILLISECONDS);
      String query = modem.queryModemInfo().get(modemResponseTimeout, TimeUnit.MILLISECONDS);
      String location = modem.enableLocation().get(modemResponseTimeout, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (ExecutionException | TimeoutException e) {
      throw new IOException(e.getCause());
    }
    messageId = 0;
    scheduledFuture = SimpleTaskScheduler.getInstance().scheduleAtFixedRate(this::pollModemForMessages, modemConfig.getMessagePollInterval(), modemConfig.getMessagePollInterval(), TimeUnit.MILLISECONDS);
    completedConnection();
    endPoint.getServer().handleNewEndPoint(endPoint);
  }

  @Override
  public void close() throws IOException {
    super.close();
    if(scheduledFuture != null) {
      scheduledFuture.cancel(true);
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
      }
      else{
        for (String key : topicNameMapping.keySet()) {
          int index = key.indexOf("#");
          if (index > 0) {
            String sub = key.substring(0, index);
            if (destinationName.startsWith(sub)) {
              destinationName = topicNameMapping.get(key) + destinationName.substring(sub.length());
            }
          }
        }
      }
    }
    OrbCommMessage orbCommMessage = new OrbCommMessage(destinationName, payload);
    outboundQueue.add(orbCommMessage);
    if(outboundQueue.size() == 1) {
      sendMessageViaModem(outboundQueue.peek());
    }
    messageEvent.getCompletionTask().run();
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
      // log this
    }
  }

  @Override
  public ProtocolInformationDTO getInformation() {
    OrbcommProtocolInformation information = new OrbcommProtocolInformation();
    updateInformation(information);
    information.setSessionInfo(session.getSessionInformation());
    return information;
  }

  private void pollModemForMessages(){
    try {
      processOutboundMessages();
      processInboundMessages();
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  private void processInboundMessages() {
    CompletableFuture<List<String>> outgoing = modem.listSentMessages();
    List<String> outgoingList = outgoing.join();
    for(String name:outgoingList){
      if(!name.trim().equalsIgnoreCase("ok") && !name.trim().equalsIgnoreCase("%MGRS:")) {
        SendMessageState state = new SendMessageState(name);
        if(state.getState().equals(SendMessageState.State.TX_FAILED) ||
            state.getState().equals(SendMessageState.State.TX_COMPLETED) ) {
          modem.deleteSentMessages();
          outboundQueue.poll();
          if(!outboundQueue.isEmpty()) {
            sendMessageViaModem(outboundQueue.peek());
          }
        }
      }
    }
  }
  private void processOutboundMessages(){
    CompletableFuture<List<byte[]>>incoming = modem.fetchAllMessages(MessageFormat.BASE64);
    List<byte[]> messages = incoming.join();
    for(byte[] message:messages){
      OrbCommMessage orbCommMessage = new OrbCommMessage( message);
      sendMessageToTopic(orbCommMessage.getNamespace(), orbCommMessage.getMessage());
    }
  }

  private void sendMessageViaModem(OrbCommMessage orbCommMessage) {
    messageId = (messageId+1) % 0xff;
    int sin = (orbCommMessage.getNamespace().hashCode() & 0x7F) | 0x80;
    modem.sendMessage("maps", 2, sin, messageId,  orbCommMessage.packToSend());
  }


  private void sendMessageToTopic(String topic, byte[] data){
    Transformer transformer = destinationTransformationLookup(topic);
    Message message = createMessage(
        data,
        getTransformation(),
        transformer,
        this
    );
    ParserExecutor parserExecutor = getParser(topic);
    if(parserExecutor != null && !parserExecutor.evaluate(message)) {
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
            // Ignore we are in an error state
          }
          future.completeExceptionally(e);
        }
      }
      return destination;
    });
    future.get();
  }

  private Message createMessage(byte[] msg,  ProtocolMessageTransformation transformation, Transformer transformer, Protocol protocol) {
    HashMap<String, String> meta = new LinkedHashMap<>();
    meta.put("protocol", "STOGI");
    meta.put("version", "1");

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

  private String parseForLookup(String initialLookup){
    String lookup = topicNameMapping.get(initialLookup);
    if (lookup == null) {
      lookup = initialLookup;
      for(Map.Entry<String, String> remote:topicNameMapping.entrySet()){
        if(remote.getKey().endsWith("#")){
          String check = remote.getValue();
          String tmp = remote.getKey().substring(0, remote.getKey().length()-1);
          if(lookup.startsWith(tmp)){
            if (lookup.toLowerCase().startsWith(DestinationMode.SCHEMA.getNamespace())) {
              lookup = lookup.substring(DestinationMode.SCHEMA.getNamespace().length());
            }
            lookup = check + lookup;
            lookup = lookup.replace("#", "");
            lookup = lookup.replaceAll("//", "/");
          }
        }
      }
    }
    return lookup;
  }

}
