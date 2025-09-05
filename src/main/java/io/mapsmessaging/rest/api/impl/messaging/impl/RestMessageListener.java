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

package io.mapsmessaging.rest.api.impl.messaging.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.MessageListener;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.TypedData;
import io.mapsmessaging.dto.rest.messaging.AsyncMessageDTO;
import io.mapsmessaging.dto.rest.messaging.MessageDTO;
import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.rest.translation.GsonDateTimeSerialiser;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.formatters.MessageFormatter;
import io.mapsmessaging.schemas.formatters.MessageFormatterFactory;
import io.mapsmessaging.schemas.formatters.impl.RawFormatter;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;
import lombok.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * This class handles message delivery for web clients. This could be either sync or async via SSE.
 * <p>
 * If Sync, then the messages are stored as
 * <p>
 * Map:
 * Key: Subscription namespace : could be a singe destination or a wild card subscription
 * Value : Map : key: String,  Value List of Messages where String is the physical destination name and the list is events from that destination
 * <p>
 * <p>
 * If Async, then the messages are forwarded with the name of the destination and NOT the subscription name
 */
public class RestMessageListener implements MessageListener {

  @Getter
  @Setter
  private static int maxSubscribedMessages = 10;

  private final Map<String, Map<String, List<MessageEvent>>> messages;

  private final Map<String, SessionSubscriptionMap> sessionSubscriptionsMap;
  private final Map<String, SseInfo> eventSinkMap;
  private final Gson gson;

  private boolean closed = false;

  public RestMessageListener() {
    messages = new ConcurrentHashMap<>();
    sessionSubscriptionsMap = new ConcurrentHashMap<>();
    eventSinkMap = new ConcurrentHashMap<>();
    gson = new GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(LocalDateTime.class, new GsonDateTimeSerialiser())
        .create();
  }

  public void registerEventManager(String namespacePath, Session session, SubscribedEventManager subscribedEventManager) {
    sessionSubscriptionsMap.put(namespacePath, new SessionSubscriptionMap(session, subscribedEventManager));
  }

  public void registerEventManager(String namespacePath, Sse sse, SseEventSink eventSink, Session session, SubscribedEventManager subscribedEventManager) {
    SseInfo sseInfo = new SseInfo(sse, eventSink);
    eventSinkMap.put(namespacePath, sseInfo);
    sessionSubscriptionsMap.put(namespacePath, new SessionSubscriptionMap(session, subscribedEventManager));
  }

  public void deregisterEventManager(String topic) {
    messages.remove(topic);
    clearSubscription(topic);
    SseInfo sseInfo = eventSinkMap.remove(topic);
    if (sseInfo != null) {
      sseInfo.close();
    }
  }

  @Override
  public synchronized void sendMessage(@NotNull @NonNull MessageEvent messageEvent) {
    if (closed) {
      messageEvent.getCompletionTask().run(); // ensure the server knows the event has been handled
      messageEvent.getSubscription().ackReceived(messageEvent.getMessage().getIdentifier());
      return;
    }
    String namespacePath = messageEvent.getSubscription().getContext().getDestinationName();
    if (eventSinkMap.containsKey(namespacePath)) {
      handleAsyncDelivery(namespacePath, messageEvent);
    } else {
      handleSyncDelivery(namespacePath, messageEvent);
    }
  }

  private void handleAsyncDelivery(String namespacePath, MessageEvent messageEvent) {
    SseInfo sseInfo = eventSinkMap.get(namespacePath);
    try {
      if (sseInfo != null) {
        if (sseInfo.eventSink.isClosed()) {
          clearSubscription(namespacePath);
        } else {
          String json = gson.toJson(convertToAsyncDTO(messageEvent));
          OutboundSseEvent event = sseInfo.sse.newEventBuilder().name(namespacePath).data(String.class, json).build();
          sseInfo.eventSink.send(event);
        }
      }
    } finally {
      messageEvent.getCompletionTask().run();
    }
  }

  private void handleSyncDelivery(String namespacePath, MessageEvent messageEvent) {
    Map<String, List<MessageEvent>> subscriptionMessages = messages.computeIfAbsent(namespacePath, k -> new LinkedHashMap<>());
    List<MessageEvent> destinationMessages = subscriptionMessages.computeIfAbsent(messageEvent.getDestinationName(), k -> new ArrayList<>());
    destinationMessages.add(messageEvent);
    if (destinationMessages.size() > maxSubscribedMessages) {
      destinationMessages.remove(0);
    }
  }

  private void clearSubscription(String namespacePath) {
    SessionSubscriptionMap subscriptionMap = sessionSubscriptionsMap.remove(namespacePath);
    subscriptionMap.getSession().removeSubscription(namespacePath);
    sessionSubscriptionsMap.remove(namespacePath);
  }

  public void ackReceived(String destination, List<Long> messageId) {
    SessionSubscriptionMap subscribedEventManager = sessionSubscriptionsMap.get(destination);
    if (subscribedEventManager != null) {
      for (long id : messageId) {
        subscribedEventManager.getSubscribedEventManager().ackReceived(id);
      }
    }
  }

  public void nakReceived(String destination, List<Long> messageId) {
    SessionSubscriptionMap subscribedEventManager = sessionSubscriptionsMap.get(destination);
    if (subscribedEventManager != null) {
      for (long id : messageId) {
        subscribedEventManager.getSubscribedEventManager().rollbackReceived(id);
      }
    }
  }

  public int subscriptionDepth(String namespacePath) {
    Map<String, List<MessageEvent>> destinationMessages = messages.get(namespacePath);
    if (destinationMessages == null) {
      return 0;
    }
    return destinationMessages.values().stream().mapToInt(List::size).sum();
  }

  public Map<String, Integer> subscriptionDepth() {
    return messages
        .entrySet()
        .stream()
        .collect(Collectors.toMap(Map.Entry::getKey, entry -> subscriptionDepth(entry.getKey()), (a, b) -> b, LinkedHashMap::new));
  }

  public List<String> getKnownDestinations() {
    return messages.entrySet().stream().flatMap(entry -> entry.getValue().keySet().stream()).collect(Collectors.toList());
  }

  public Map<String, List<MessageDTO>> getMessages(String namespace, int max) {
    int count = max;
    if (count <= 0) count = 10;
    if (count > 1000) count = 1000;
    Map<String, List<MessageEvent>> destinationMessages = messages.get(namespace);
    Map<String, List<MessageDTO>> response = new LinkedHashMap<>();

    boolean hasEvents = true;
    while (hasEvents && count != 0) {
      hasEvents = false;
      for (Map.Entry<String, List<MessageEvent>> entry : destinationMessages.entrySet()) {
        if (!entry.getValue().isEmpty()) {
          List<MessageDTO> returnEvents = response.computeIfAbsent(entry.getKey(), key -> new ArrayList<>());
          MessageEvent msg = entry.getValue().remove(0);
          returnEvents.add(convertToDTO(msg));
          msg.getCompletionTask().run();
          hasEvents = true;
          count--;
        }
      }
    }
    return response;
  }

  private MessageDTO convertToAsyncDTO(MessageEvent message) {
    AsyncMessageDTO messageDTO = new AsyncMessageDTO();
    messageDTO.setDestinationName(message.getDestinationName());
    return convert(messageDTO, message);
  }

  private MessageDTO convertToDTO(MessageEvent message) {
    MessageDTO messageDTO = new MessageDTO();
    return convert(messageDTO, message);
  }

  private MessageDTO convert(MessageDTO messageDTO, MessageEvent message) {
    Message msg = message.getMessage();
    messageDTO.setIdentifier(msg.getIdentifier());
    messageDTO.setPriority(msg.getPriority().getValue());
    byte[] payload = msg.getOpaqueData();
    if(message.getMessage().getSchemaId() != null) {
      SchemaConfig config = SchemaManager.getInstance().getSchema(message.getMessage().getSchemaId());
      try {
        MessageFormatter formatter = MessageFormatterFactory.getInstance().getFormatter(config);
        if (formatter != null && !(formatter instanceof RawFormatter)) {
          JsonObject jsonObject = formatter.parseToJson(payload);
          JsonObject wrapper = new JsonObject();
          wrapper.add("payload", jsonObject);
          wrapper.addProperty("schemaId", message.getMessage().getSchemaId());
          wrapper.addProperty("schemaTitle", config.getTitle());
          payload = gson.toJson(wrapper).getBytes();
        }
      } catch(Throwable e){
      }
    }

    messageDTO.setPayload(Base64.getEncoder().encodeToString(payload));
    messageDTO.setExpiry(msg.getExpiry());
    messageDTO.setCorrelationData(msg.getCorrelationData());
    messageDTO.setContentType(msg.getContentType());
    messageDTO.setQualityOfService(msg.getQualityOfService().getLevel());
    messageDTO.setMetaData(msg.getMeta() == null ? new HashMap<>() : new LinkedHashMap<>(msg.getMeta()));
    String cr = msg.getMeta() != null ? msg.getMeta().get("time_ms") : null;
    if(cr != null) {
      long creation = Long.parseLong(cr);
      messageDTO.setCreation(Instant.ofEpochMilli(creation)
          .atZone(ZoneId.systemDefault())
          .toLocalDateTime());
    }
    Map<String, Object> map = new LinkedHashMap<>();
    for (Map.Entry<String, TypedData> entry : msg.getDataMap().entrySet()) {
      map.put(entry.getKey(), entry.getValue().getData());
    }
    messageDTO.setDataMap(map);
    return messageDTO;
  }

  public synchronized void close() {
    for (String key : sessionSubscriptionsMap.keySet()) {
      deregisterEventManager(key);
    }
    closed = false;
    messages.clear();
    eventSinkMap.clear();
    sessionSubscriptionsMap.clear();
  }

  @Data
  @AllArgsConstructor
  private static final class SseInfo {
    private final Sse sse;
    private final SseEventSink eventSink;

    public void close() {
      try {
        eventSink.close();
      } catch (IOException e) {
        // ignore we are in an exception
      }
    }
  }
}
