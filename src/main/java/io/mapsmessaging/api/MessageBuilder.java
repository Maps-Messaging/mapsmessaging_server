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

package io.mapsmessaging.api;

import io.mapsmessaging.api.features.Priority;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.TypedData;
import io.mapsmessaging.api.transformers.Transformer;
import io.mapsmessaging.dto.rest.messaging.MessageDTO;
import io.mapsmessaging.network.protocol.ProtocolMessageTransformation;
import lombok.Data;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Data
public class MessageBuilder {

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
  private Transformer transformer;
  private String schemaId;

  public MessageBuilder() {
    id = 0;
    meta = null;
    dataMap = null;
    opaqueData = null;
    priority = Priority.NORMAL;
    retain = false;
    storeOffline = false;
    payloadUTF8 = false;
    expiry = 0;
    delayed = 0;
    creation = System.currentTimeMillis();
    contentType = null;
    correlationData = null;
    qualityOfService = QualityOfService.AT_MOST_ONCE;
    schemaId = null;
  }

  public MessageBuilder(Message previousMessage) {
    id = 0;
    meta = previousMessage.getMeta();
    dataMap = previousMessage.getDataMap();
    opaqueData = previousMessage.getOpaqueData();
    priority = previousMessage.getPriority();
    retain = previousMessage.isRetain();
    storeOffline = previousMessage.isStoreOffline();
    payloadUTF8 = previousMessage.isUTF8();
    expiry = previousMessage.getExpiry();
    contentType = previousMessage.getContentType();
    correlationData = previousMessage.getCorrelationData();
    qualityOfService = previousMessage.getQualityOfService();
    delayed = previousMessage.getDelayed();
  }


  public MessageBuilder(MessageDTO messageDTO) {
    id = 0;
    meta = null;
    retain = false;
    storeOffline = false;
    payloadUTF8 = false;
    delayed = 0;
    creation = System.currentTimeMillis();
    schemaId = null;

    dataMap = new LinkedHashMap<>();
    for(Map.Entry<String, Object> entry : messageDTO.getDataMap().entrySet()) {
      dataMap.put(entry.getKey(), new TypedData(entry.getValue()));
    }
    opaqueData = Base64.getDecoder().decode(messageDTO.getPayload());
    priority = Priority.getInstance(messageDTO.getPriority());
    expiry = messageDTO.getExpiry();
    contentType = messageDTO.getContentType();
    correlationData = messageDTO.getCorrelationData();
    qualityOfService = QualityOfService.getInstance(messageDTO.getQualityOfService());
  }

  public @NonNull @NotNull MessageBuilder setCreation(long creation) {
    this.creation = creation;
    return this;
  }

  public @NonNull @NotNull MessageBuilder setQoS(@NonNull @NotNull QualityOfService qualityOfService) {
    this.qualityOfService = qualityOfService;
    return this;
  }

  public @NonNull @NotNull MessageBuilder storeOffline(boolean storeOffline) {
    this.storeOffline = storeOffline;
    return this;
  }

  public @NonNull @NotNull MessageBuilder setPayloadIndicator(boolean payloadFormatIndicator) {
    payloadUTF8 = payloadFormatIndicator;
    return this;
  }

  public @NonNull @NotNull MessageBuilder setMessageExpiryInterval(long messageExpiryInterval, TimeUnit timeUnit) {
    expiry = timeUnit.toMillis(messageExpiryInterval);
    return this;
  }

  public @NonNull @NotNull MessageBuilder setMeta(@Nullable Map<String, String> meta) {
    if(this.meta != null && meta != null) {
      this.meta.putAll(meta);
    }
    else {
      this.meta = meta;
    }
    return this;
  }

  public @NonNull @NotNull MessageBuilder setDataMap(@Nullable Map<String, TypedData> dataMap) {
    if(this.dataMap != null && dataMap != null) {
      this.dataMap.putAll(dataMap);
    }
    else {
      this.dataMap = dataMap;
    }
    return this;
  }

  public @NonNull @NotNull MessageBuilder setOpaqueData(byte[] opaqueData) {
    this.opaqueData = opaqueData;
    return this;
  }

  public @NonNull @NotNull MessageBuilder setCorrelationData(byte[] correlationData) {
    this.correlationData = correlationData;
    return this;
  }

  public @NonNull @NotNull MessageBuilder setCorrelationData(@Nullable String correlationData) {
    this.correlationData = correlationData;
    return this;
  }

  public @NonNull @NotNull MessageBuilder setContentType(@Nullable String contentType) {
    this.contentType = contentType;
    return this;
  }

  public @NonNull @NotNull MessageBuilder setResponseTopic(@Nullable String responseTopicString) {
    responseTopic = responseTopicString;
    return this;
  }

  public @NonNull @NotNull MessageBuilder setId(long id) {
    this.id = id;
    return this;
  }

  public @NonNull @NotNull MessageBuilder setPriority(Priority priority) {
    this.priority = priority;
    return this;
  }

  public @NonNull @NotNull MessageBuilder setRetain(boolean retain) {
    this.retain = retain;
    return this;
  }

  public @NonNull @NotNull MessageBuilder setTransformation(ProtocolMessageTransformation transformation) {
    this.transformation = transformation;
    return this;
  }

  public @NonNull @NotNull MessageBuilder setDelayed(long delayed) {
    this.delayed = delayed;
    return this;
  }

  public @NonNull @NotNull MessageBuilder setDestinationTransformer(Transformer transformer) {
    this.transformer = transformer;
    return this;
  }

  public @NonNull @NotNull MessageBuilder setSchemaId(String schemaId) {
    this.schemaId = schemaId;
    return this;
  }

  public @NonNull @NotNull Message build() {
    if (transformation != null) {
      ProtocolMessageTransformation local = transformation;
      transformation = null;
      local.incoming(this);
    }
    if (transformer != null) {
      transformer.transform(this);
    }
    return new Message(this);
  }

}
