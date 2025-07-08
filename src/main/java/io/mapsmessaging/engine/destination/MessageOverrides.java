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

package io.mapsmessaging.engine.destination;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.features.Priority;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.TypedData;
import io.mapsmessaging.dto.rest.config.destination.MessageOverrideDTO;

import java.util.HashMap;
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

  private static MessageBuilder applyOverrides(MessageBuilder messageBuilder, MessageOverrideDTO messageOverride) {
    if(messageOverride.getQualityOfService() != null){
      messageBuilder.setQualityOfService(messageOverride.getQualityOfService());
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
    if(messageOverride.getExpiry() >= 0){
      messageBuilder.setExpiry(messageOverride.getExpiry());
    }
    if(messageOverride.getRetain() != null){
      messageBuilder.setRetain(messageOverride.getRetain());
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
        dataMap.put(entry.getKey(), new TypedData(entry.getValue()));
      }
    }
    if(messageOverride.getMeta() != null){
      Map<String, String> meta = messageBuilder.getMeta();
      if(meta == null){
        meta = new HashMap<>();
        messageBuilder.setMeta(meta);
      }
      meta.putAll(messageOverride.getMeta());
    }
    return messageBuilder;
  }

}
