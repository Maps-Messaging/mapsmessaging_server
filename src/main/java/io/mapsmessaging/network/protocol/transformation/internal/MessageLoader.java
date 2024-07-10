/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
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

package io.mapsmessaging.network.protocol.transformation.internal;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.features.Priority;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.TypedData;
import io.mapsmessaging.network.protocol.ProtocolMessageTransformation;
import java.util.Map;
import lombok.*;

@Data
@NoArgsConstructor
@ToString
public class MessageLoader {
  private java.util.Map<String, String> meta;
  private Map<String, TypedData> dataMap;
  private byte[] opaqueData;
  private Object correlationData;
  private String contentType;
  private String responseTopic;
  private long id;
  private long expiry;
  private long delayed;
  private Priority priority;
  private long creation;
  private QualityOfService qualityOfService;
  private ProtocolMessageTransformation transformation;
  private boolean retain;
  private boolean storeOffline;
  private boolean payloadUTF8;
  private String schemaId;
  private boolean lastMessage;
  private long identifier;
  private boolean correlationDataByteArray;
  private boolean utf8;
  private long time;


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
