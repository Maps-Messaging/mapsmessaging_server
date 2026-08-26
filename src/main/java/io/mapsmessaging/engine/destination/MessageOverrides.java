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

package io.mapsmessaging.engine.destination;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.TypedData;
import io.mapsmessaging.dto.rest.config.destination.DestinationConfigDTO;
import io.mapsmessaging.dto.rest.config.destination.MessageOverrideDTO;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class MessageOverrides {

  private MessageOverrides() {
  }

  public static MessageBuilder createMessageBuilder(MessageOverrideDTO messageOverride, MessageBuilder messageBuilder) {
    if(messageOverride != null) {
      applyOverrides(messageBuilder, messageOverride);
    }
    return messageBuilder;
  }

  public static Message setOverrides(MessageOverrideDTO messageOverride, Message message) {
    if(messageOverride == null) return message;
    return applyOverrides(new MessageBuilder(message), messageOverride).build();
  }

  public static MessageOverrideDTO resolve(String destinationName, Map<String, DestinationConfigDTO> destinationConfigs) {
    MessageOverrideDTO merged = null;
    for (Map.Entry<String, DestinationConfigDTO> entry : destinationConfigs.entrySet().stream()
        .filter(item -> DestinationManagerPipeline.matchesNamespace(destinationName, item.getKey()))
        .sorted(Comparator.comparingInt(item -> item.getKey().length()))
        .toList()) {
      MessageOverrideDTO configured = entry.getValue().getMessageOverride();
      if (configured != null) {
        if (merged == null) {
          merged = new MessageOverrideDTO();
        }
        merge(merged, configured);
      }
    }
    return merged;
  }

  private static void merge(MessageOverrideDTO target, MessageOverrideDTO configured) {
    if (configured.getQualityOfService() != null) {
      target.setQualityOfService(configured.getQualityOfService());
    }
    if (configured.getPriority() != null) {
      target.setPriority(configured.getPriority());
    }
    if (configured.getContentType() != null) {
      target.setContentType(configured.getContentType());
    }
    if (configured.getResponseTopic() != null) {
      target.setResponseTopic(configured.getResponseTopic());
    }
    if (configured.getExpiry() != null && configured.getExpiry() >= 0) {
      target.setExpiry(configured.getExpiry());
    }
    if (configured.getRetain() != null) {
      target.setRetain(configured.getRetain());
    }
    if (configured.getStoreOffline() != null) {
      target.setStoreOffline(configured.getStoreOffline());
    }
    if (configured.getSchemaId() != null) {
      target.setSchemaId(configured.getSchemaId());
    }
    if (configured.getDataMap() != null) {
      Map<String, Object> dataMap = target.getDataMap();
      if (dataMap == null) {
        dataMap = new LinkedHashMap<>();
        target.setDataMap(dataMap);
      }
      dataMap.putAll(configured.getDataMap());
    }
    if (configured.getMeta() != null) {
      Map<String, String> meta = target.getMeta();
      if (meta == null) {
        meta = new LinkedHashMap<>();
        target.setMeta(meta);
      }
      meta.putAll(configured.getMeta());
    }
  }

  private static MessageBuilder applyOverrides(MessageBuilder messageBuilder, MessageOverrideDTO messageOverride) {
    if(messageOverride.getQualityOfService() != null){
      messageBuilder.setQoS(messageOverride.getQualityOfService());
    }
    if(messageOverride.getPriority() != null){
      messageBuilder.setPriority(messageOverride.getPriority());
    }
    if(messageOverride.getContentType() != null){
      messageBuilder.setContentType(messageOverride.getContentType());
    }
    if(messageOverride.getResponseTopic() != null){
      messageBuilder.setResponseTopic(messageOverride.getResponseTopic());
    }
    if (messageOverride.getExpiry() != null && messageOverride.getExpiry() >= 0) {
      messageBuilder.setExpiry(messageOverride.getExpiry());
    }
    if(messageOverride.getRetain() != null){
      messageBuilder.setRetain(messageOverride.getRetain());
    }
    if (messageOverride.getStoreOffline() != null) {
      messageBuilder.storeOffline(messageOverride.getStoreOffline());
    }
    if(messageOverride.getSchemaId() != null){
      messageBuilder.setSchemaId(messageOverride.getSchemaId());
    }
    if(messageOverride.getDataMap() != null){
      Map<String, TypedData> dataMap = messageBuilder.getDataMap();
      if(dataMap == null){
        dataMap = new HashMap<>();
        messageBuilder.setDataMap(dataMap);
      }
      for(Map.Entry<String, Object> entry : messageOverride.getDataMap().entrySet()){
        if (entry.getValue() != null) {
          dataMap.putIfAbsent(entry.getKey(), new TypedData(entry.getValue()));
        }
      }
    }
    if(messageOverride.getMeta() != null){
      Map<String, String> meta = messageBuilder.getMeta();
      if(meta == null){
        meta = new HashMap<>();
        messageBuilder.setMeta(meta);
      }
      for(Map.Entry<String, String> entry : messageOverride.getMeta().entrySet()){
        if (entry.getValue() != null) {
          meta.putIfAbsent(entry.getKey(), entry.getValue());
        }
      }
    }
    return messageBuilder;
  }

}
