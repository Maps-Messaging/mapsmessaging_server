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

import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.MessageListener;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.TypedData;
import io.mapsmessaging.dto.rest.messaging.MessageDTO;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

public class RestMessageListener implements MessageListener {

  @Getter
  @Setter
  private static int maxSubscribedMessages = 10;

  private final Map<String, List<Message>> messages;

  private boolean closed = false;

  public RestMessageListener() {
    messages = new ConcurrentHashMap<>();
  }

  @Override
  public synchronized void sendMessage(@NotNull @NonNull MessageEvent messageEvent) {
    messageEvent.getCompletionTask().run(); // ensure the server knows the event has been handled
    if (closed) {
      return;
    }
    String destination = messageEvent.getDestinationName();
    List<Message> destinationMessages = messages.computeIfAbsent(destination, k -> new ArrayList<>());
    destinationMessages.add(messageEvent.getMessage());
    if(destinationMessages.size() > maxSubscribedMessages){
      destinationMessages.remove(0);
    }
  }

  public int subscriptionDepth(String destination) {
    List<Message> destinationMessages = messages.get(destination);
    if(destinationMessages == null){
      return 0;
    }
    return destinationMessages.size();
  }

  public Map<String, Integer> subscriptionDepth() {
    Map<String, Integer> result = new LinkedHashMap<>();
    for(Map.Entry<String, List<Message>> entry : messages.entrySet()){
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
    List<Message> destinationMessages = messages.get(destinationName);
    if(destinationMessages == null){
      destinationMessages = new ArrayList<>();
    }
    List<Message> subMessages;
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
    for(Message message : subMessages) {
      MessageDTO messageDTO = new MessageDTO();
      messageDTO.setPriority(message.getPriority().getValue());
      messageDTO.setPayload(Base64.getEncoder().encodeToString(message.getOpaqueData()));
      messageDTO.setExpiry(message.getExpiry());

      messageDTO.setCorrelationData(message.getCorrelationData());
      messageDTO.setContentType(message.getContentType());
      messageDTO.setQualityOfService(message.getQualityOfService().getLevel());
      Map<String, Object> map =new LinkedHashMap<>();
      for(Map.Entry<String, TypedData> entry: message.getDataMap().entrySet()) {
        map.put(entry.getKey(), entry.getValue().getData());
      }
      messageDTO.setDataMap(map);
      messageList.add(messageDTO);
    }
    return messageList;
  }

  public synchronized void close() {
    closed = false;
    messages.clear();
  }
}
