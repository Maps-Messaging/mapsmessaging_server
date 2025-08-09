/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  https://commonsclause.com/
 *
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
import io.mapsmessaging.network.protocol.impl.orbcomm.modem.device.BaseModem;
import io.mapsmessaging.network.protocol.impl.orbcomm.modem.device.ModemFactory;
import io.mapsmessaging.network.protocol.impl.orbcomm.modem.device.messages.PayloadMessage;
import io.mapsmessaging.network.protocol.impl.orbcomm.modem.device.messages.SendMessageState;
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
  private final BaseModem modem;
  private final ScheduledFuture<?> scheduledFuture;
  private final Map<String, String> topicNameMapping;
  private final Queue<OrbCommMessage> outboundQueue;

  public OrbcommProtocol(EndPoint endPoint, Packet packet) throws LoginException, IOException {
    super(endPoint,  endPoint.getConfig().getProtocolConfig("stogi"));
    topicNameMapping = new ConcurrentHashMap<>();
    outboundQueue = new ConcurrentLinkedQueue<>();
    if (packet != null) packet.clear();

    SessionContextBuilder sessionContextBuilder =
        new SessionContextBuilder("stogi" + endPoint.getId(), new ProtocolClientConnection(this));
    sessionContextBuilder.setSessionExpiry(0);
    sessionContextBuilder.setKeepAlive(0);
    sessionContextBuilder.setPersistentSession(false);
    session = SessionManager.getInstance().create(sessionContextBuilder.build(), this);

    selectorTask = new SelectorTask(this, endPoint.getConfig().getEndPointConfig());
    endPoint.register(SelectionKey.OP_READ, selectorTask.getReadTask());

    ProtocolMessageTransformation transformation =
        TransformationManager.getInstance().getTransformation(
            endPoint.getProtocol(),
            endPoint.getName(),
            "stogi",
            session.getSecurityContext().getUsername());
    setTransformation(transformation);

    OrbCommDTO modemConfig = (OrbCommDTO) getProtocolConfig();
    long modemResponseTimeout = modemConfig.getModemResponseTimeout();

    // Create modem via factory and perform minimal bring-up
    modem = ModemFactory.create(this);
    try {
      modem.getFirmwareId().get(modemResponseTimeout, TimeUnit.MILLISECONDS);
      try {
        modem.setGnssPower(true).get(modemResponseTimeout, TimeUnit.MILLISECONDS);
      } catch (Exception ignore) {
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (ExecutionException | TimeoutException e) {
      throw new IOException(e.getCause());
    }

    // Typed event sink for URCs/messages
    modem.setMessageSink(msg -> {
      if (msg instanceof SendMessageState state) {
        switch (state.getState()) {
          case TX_COMPLETED, TX_FAILED -> {
            outboundQueue.poll();
            if (!outboundQueue.isEmpty()) sendMessageViaModem(outboundQueue.peek());
          }
          default -> {
          }
        }
        return;
      }
      if (msg instanceof PayloadMessage pm) {
        byte[] payload = pm.getPayload();
        if (payload != null && payload.length > 0) {
          OrbCommMessage m = new OrbCommMessage(payload);
          sendMessageToTopic(m.getNamespace(), m.getMessage());
        }
      }
      // Position and other message types can be handled later.
    });

    scheduledFuture = SimpleTaskScheduler.getInstance().scheduleAtFixedRate(
        this::pollModemForMessages,
        modemConfig.getMessagePollInterval(),
        modemConfig.getMessagePollInterval(),
        TimeUnit.MILLISECONDS
    );

    completedConnection();
    endPoint.getServer().handleNewEndPoint(endPoint);
  }

  @Override
  public void close() throws IOException {
    super.close();
    if (scheduledFuture != null) scheduledFuture.cancel(true);
    if (modem != null) modem.close();
  }

  @Override
  public void connect(String sessionId, String username, String password) throws IOException {
    completedConnection();
  }

  @Override
  public void subscribeRemote(@NonNull @NotNull String resource, @NonNull @NotNull String mappedResource,
                              @Nullable ParserExecutor executor, @Nullable Transformer transformer) {
    // no-op for now
  }

  @Override
  public void subscribeLocal(@NonNull @NotNull String resource, @NonNull @NotNull String mappedResource,
                             @Nullable String selector, @Nullable Transformer transformer) throws IOException {
    topicNameMapping.put(resource, mappedResource);
    if (transformer != null) destinationTransformerMap.put(mappedResource, transformer);
    SubscriptionContextBuilder builder =
        createSubscriptionContextBuilder(resource, selector, QualityOfService.AT_MOST_ONCE, 1024);
    session.addSubscription(builder.build());
  }

  public void sendMessage(@NotNull @NonNull MessageEvent messageEvent) {
    String destinationName = messageEvent.getDestinationName();
    Message message = processTransformer(destinationName, messageEvent.getMessage());

    byte[] payload = (transformation != null)
        ? transformation.outgoing(message, messageEvent.getDestinationName())
        : message.getOpaqueData();

    if (topicNameMapping != null) {
      String tmp = topicNameMapping.get(destinationName);
      if (tmp != null) {
        destinationName = tmp;
      } else {
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
    if (outboundQueue.size() == 1) {
      sendMessageViaModem(orbCommMessage);
    }
    messageEvent.getCompletionTask().run();
  }

  @Override
  public Subject getSubject() {
    return session.getSecurityContext().getSubject();
  }

  @Override
  public void sendKeepAlive() { /* no op */ }

  @Override
  public boolean processPacket(Packet packet) throws IOException {
    while (packet.hasRemaining()) {
      modem.onPacket(packet);
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
      processInboundMessages();
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  private void processInboundMessages() {
    try {
      List<Long> ids = modem.listMtIds().get(1500, TimeUnit.MILLISECONDS);
      if (ids == null || ids.isEmpty()) return;

      for (Long id : ids) {
        if (id == null) continue;
        byte[] payload = modem.fetchMtPayload(id).get(3000, TimeUnit.MILLISECONDS);
        if (payload != null && payload.length > 0) {
          OrbCommMessage m = new OrbCommMessage(payload);
          sendMessageToTopic(m.getNamespace(), m.getMessage());
        }
        try {
          modem.ackMt(id).get(1500, TimeUnit.MILLISECONDS);
        } catch (Exception ignore) {
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (Exception ignore) {
      // swallow and retry next tick
    }
  }

  private void sendMessageViaModem(OrbCommMessage orbCommMessage) {
    int sin = (orbCommMessage.getNamespace().hashCode() & 0x7F) | 0x80;
    modem.sendMoMessage("maps", 2, sin, orbCommMessage.packToSend())
        .whenComplete((resp, ex) -> {
          outboundQueue.poll();
          if (!outboundQueue.isEmpty()) {
            sendMessageViaModem(outboundQueue.peek());
          }
        });
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
