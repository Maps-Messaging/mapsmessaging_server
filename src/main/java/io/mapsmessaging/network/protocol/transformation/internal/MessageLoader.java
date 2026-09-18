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

package io.mapsmessaging.network.protocol.transformation.internal;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.features.Priority;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.TypedData;
import io.mapsmessaging.network.protocol.transformation.ProtocolMessageTransformation;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Base64;
import java.util.LinkedHashMap;
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
    if (getMeta() != null) {
      if (current == null) {
        current = new LinkedHashMap<>(getMeta());
      } else {
        current.putAll(getMeta());
      }
    }

    long now = System.currentTimeMillis();
    long remainingDelay = getDelayed() > now ? getDelayed() - now : 0;
    long expiryBase = getDelayed() > now ? getDelayed() : now;
    long remainingExpiry = getExpiry() > expiryBase ? getExpiry() - expiryBase : 0;

    messageBuilder.setMeta(current)
        .setDataMap(getDataMap())
        .setOpaqueData(getOpaqueData())
        .setContentType(getContentType())
        .setResponseTopic(getResponseTopic())
        .setPriority(getPriority())
        .setRetain(isRetain())
        .setTransformation(null)
        .setDelayed(remainingDelay)
        .setExpiry(remainingExpiry)
        .setSchemaId(getSchemaId())
        .storeOffline(isStoreOffline())
        .setPayloadUTF8(isUtf8() || isPayloadUTF8());

    if (getQualityOfService() != null) {
      messageBuilder.setQoS(getQualityOfService());
    }
    if (getCreation() > 0) {
      messageBuilder.setCreation(getCreation());
    }

    if (getCorrelationData() != null) {
      Object corr = getCorrelationData();
      if (corr instanceof byte[] bytes) {
        messageBuilder.setCorrelationData(bytes);
      } else if (corr instanceof String value) {
        if (isCorrelationDataByteArray()) {
          messageBuilder.setCorrelationData(Base64.getDecoder().decode(value));
        } else {
          messageBuilder.setCorrelationData(value);
        }
      }
    }
  }
}
