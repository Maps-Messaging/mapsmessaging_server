/*
 *
 *   Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.maps.messaging.api;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maps.messaging.api.features.Priority;
import org.maps.messaging.api.features.QualityOfService;
import org.maps.messaging.api.message.Message;
import org.maps.messaging.api.message.TypedData;
import org.maps.messaging.api.transformers.Transformer;
import org.maps.network.protocol.ProtocolMessageTransformation;

public class MessageBuilder {

  @Getter private java.util.Map<String, String> meta;
  @Getter private Map<String, TypedData> dataMap;
  @Getter private byte[] opaqueData;
  @Getter private Object correlationData;
  @Getter private String contentType;
  @Getter private String responseTopic;
  @Getter private long id;
  @Getter private long expiry;
  @Getter private long delayed;
  @Getter private Priority priority;
  @Getter private long creation;
  @Getter private QualityOfService qualityOfService;
  @Getter private ProtocolMessageTransformation transformation;
  @Getter private boolean retain;
  @Getter private boolean storeOffline;
  @Getter private boolean payloadUTF8;
  @Getter private Transformer transformer;

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

  public @NonNull @NotNull MessageBuilder setMeta(@Nullable  Map<String, String> meta) {
    this.meta = meta;
    return this;
  }

  public @NonNull @NotNull MessageBuilder setDataMap(@Nullable Map<String, TypedData> dataMap) {
    this.dataMap = dataMap;
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

  public  @NonNull @NotNull MessageBuilder setTransformation(ProtocolMessageTransformation transformation) {
    this.transformation = transformation;
    return this;
  }

  public  @NonNull @NotNull MessageBuilder setDelayed(long delayed) {
    this.delayed = delayed;
    return this;
  }

  public  @NonNull @NotNull MessageBuilder setDestinationTransformer(Transformer transformer) {
    this.transformer = transformer;
    return this;
  }

  public @NonNull @NotNull Message build() {
    if(transformation != null){
      ProtocolMessageTransformation local = transformation;
      transformation = null;
      local.incoming(this);
    }
    if(transformer != null){
      transformer.transform(this);
    }
    return new Message(this);
  }

}
