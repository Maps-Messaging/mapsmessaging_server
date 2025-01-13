/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2025 ] [Maps Messaging B.V.]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.rest.api.impl.messaging.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.MessageListener;
import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.TypedData;
import io.mapsmessaging.dto.rest.messaging.MessageDTO;
import io.mapsmessaging.rest.translation.GsonDateTimeSerialiser;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import lombok.*;
import org.jetbrains.annotations.NotNull;

public class RestMessageListener implements MessageListener, Serializable {

  @Getter
  @Setter
  private static int maxSubscribedMessages = 10;

  private final Map<String, List<MessageEvent>> messages;
  private final Map<String, SubscribedEventManager> subscribedEventManagerMap;
  private final Map<String, SseInfo> eventSinkMap;


  private boolean closed = false;

  public RestMessageListener() {
    messages = new ConcurrentHashMap<>();
    subscribedEventManagerMap = new ConcurrentHashMap<>();
    eventSinkMap = new ConcurrentHashMap<>();
  }

  public void registerEventManager(String topic, SubscribedEventManager subscribedEventManager) {
    subscribedEventManagerMap.put(topic, subscribedEventManager);
  }

  public void registerEventManager(String topic, Sse sse, SseEventSink eventSink, SubscribedEventManager subscribedEventManager) {
    SseInfo sseInfo = new SseInfo(sse, eventSink);
    eventSinkMap.put(topic, sseInfo);
    subscribedEventManagerMap.put(topic, subscribedEventManager);
  }


  @Override
  public synchronized void sendMessage(@NotNull @NonNull MessageEvent messageEvent) {
    if (closed) {
      messageEvent.getCompletionTask().run(); // ensure the server knows the event has been handled
      messageEvent.getSubscription().ackReceived(messageEvent.getMessage().getIdentifier());
      return;
    }
    String destination = messageEvent.getDestinationName();
    if(eventSinkMap.containsKey(destination)) {
      handleAsyncDelivery(destination, messageEvent);
    }
    else{
      handleSyncDelivery(destination, messageEvent);
    }
  }

  private void handleAsyncDelivery(String destination,MessageEvent messageEvent) {
    SseInfo sseInfo = eventSinkMap.get(destination);
    if(sseInfo != null) {
      Gson gson = new GsonBuilder()
          .setPrettyPrinting()
          .registerTypeAdapter(LocalDateTime.class, new GsonDateTimeSerialiser())
          .create();
      String json = gson.toJson(convertToDTO(messageEvent));
      OutboundSseEvent event = sseInfo.sse.newEventBuilder()
          .name(messageEvent.getDestinationName())
          .data(String.class, json)
          .build();
      sseInfo.eventSink.send(event);
      messageEvent.getCompletionTask().run();
    }
  }

  private void handleSyncDelivery(String destination, MessageEvent messageEvent) {
    List<MessageEvent> destinationMessages =
        messages.computeIfAbsent(destination, k -> new ArrayList<>());
    destinationMessages.add(messageEvent);
    if (destinationMessages.size() > maxSubscribedMessages) {
      destinationMessages.remove(0);
    }
  }

  public void ackReceived(String destination, List<Long> messageId) {
    SubscribedEventManager subscribedEventManager = subscribedEventManagerMap.get(destination);
    if (subscribedEventManager != null) {
      for (long id : messageId) {
        subscribedEventManager.ackReceived(id);
      }
    }
  }

  public void nakReceived(String destination, List<Long> messageId) {
    SubscribedEventManager subscribedEventManager = subscribedEventManagerMap.get(destination);
    if (subscribedEventManager != null) {
      for (long id : messageId) {
        subscribedEventManager.rollbackReceived(id);
      }
    }
  }

  public int subscriptionDepth(String destination) {
    List<MessageEvent> destinationMessages = messages.get(destination);
    if(destinationMessages == null){
      return 0;
    }
    return destinationMessages.size();
  }

  public Map<String, Integer> subscriptionDepth() {
    Map<String, Integer> result = new LinkedHashMap<>();
    for (Map.Entry<String, List<MessageEvent>> entry : messages.entrySet()) {
      result.put(entry.getKey(), subscriptionDepth(entry.getKey()));
    }
    return result;
  }

  public List<String> getKnownDestinations() {
    return new ArrayList<>(messages.keySet());
  }

  public List<MessageDTO> getMessages(String destinationName, int max) {
    if (max <= 0) max = 10;
    if(max > 1000) max = 1000;
    List<MessageEvent> destinationMessages = messages.get(destinationName);
    if(destinationMessages == null){
      destinationMessages = new ArrayList<>();
    }
    List<MessageEvent> subMessages;
    if(destinationMessages.size() > max){
      subMessages = destinationMessages.subList(0, max);
      destinationMessages = destinationMessages.subList(max, destinationMessages.size());
      messages.remove(destinationName);
      messages.put(destinationName, destinationMessages);
    }
    else{
      subMessages = new ArrayList<>(destinationMessages);
      destinationMessages.clear();
    }

    List<MessageDTO> messageList = new ArrayList<>();
    for (MessageEvent message : subMessages) {
      MessageDTO messageDTO = convertToDTO(message);
      messageList.add(messageDTO);
      message.getCompletionTask().run();
    }
    return messageList;
  }

  private MessageDTO convertToDTO(MessageEvent message) {
    MessageDTO messageDTO = new MessageDTO();
    Message msg = message.getMessage();
    messageDTO.setIdentifier(msg.getIdentifier());
    messageDTO.setPriority(msg.getPriority().getValue());
    messageDTO.setPayload(Base64.getEncoder().encodeToString(msg.getOpaqueData()));
    messageDTO.setExpiry(msg.getExpiry());
    messageDTO.setCorrelationData(msg.getCorrelationData());
    messageDTO.setContentType(msg.getContentType());
    messageDTO.setQualityOfService(msg.getQualityOfService().getLevel());

    long creation = Long.parseLong( msg.getMeta().get("time_ms"));
    messageDTO.setCreation(Instant.ofEpochMilli(creation)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime());

    Map<String, Object> map =new LinkedHashMap<>();
    for (Map.Entry<String, TypedData> entry : msg.getDataMap().entrySet()) {
      map.put(entry.getKey(), entry.getValue().getData());
    }
    messageDTO.setDataMap(map);
    return messageDTO;
  }

  public synchronized void close() {
    closed = false;
    messages.clear();
    eventSinkMap.clear();
    subscribedEventManagerMap.clear();
  }

  @Data
  @AllArgsConstructor
  private static final class SseInfo{
    private final Sse sse;
    private final SseEventSink eventSink;
  }
}
