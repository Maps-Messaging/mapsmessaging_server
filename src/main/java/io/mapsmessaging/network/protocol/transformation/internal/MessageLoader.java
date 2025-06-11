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

package io.mapsmessaging.network.protocol.transformation.internal;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.features.Priority;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.TypedData;
import io.mapsmessaging.network.protocol.ProtocolMessageTransformation;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Map;

@Data
@NoArgsConstructor
@ToString
public class MessageLoader {

  private boolean retain;
  private boolean storeOffline;
  private boolean payloadUTF8;
  private boolean lastMessage;
  private boolean correlationDataByteArray;
  private boolean utf8;

  private long id;
  private long expiry;
  private long delayed;
  private long creation;
  private long identifier;
  private long time;
  private long key;

  private String contentType;
  private String responseTopic;
  private String schemaId;

  private Object correlationData;
  private byte[] opaqueData;
  private Priority priority;
  private QualityOfService qualityOfService;
  private ProtocolMessageTransformation transformation;
  private Map<String, String> meta;
  private Map<String, TypedData> dataMap;



  public void load(MessageBuilder messageBuilder){
    Map<String, String> current = messageBuilder.getMeta();
    if(current != null && getMeta() != null){
      current.putAll(getMeta());
    }

    messageBuilder.setMeta(current)
        .setDataMap(getDataMap())
        .setOpaqueData(getOpaqueData())
        .setContentType(getContentType())
        .setResponseTopic(getResponseTopic())
        .setPriority(getPriority())
        .setRetain(isRetain())
        .setTransformation(null)
        .setDelayed(getDelayed())
        .setSchemaId(getSchemaId());

    if(getCorrelationData() != null ){
      Object corr = getCorrelationData();
      if(corr instanceof byte[]){
        messageBuilder.setCorrelationData((byte[])correlationData);
      }
      else if(corr instanceof String){
        messageBuilder.setCorrelationData((String)correlationData);
      }
    }
  }
}
