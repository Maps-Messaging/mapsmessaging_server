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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maps.messaging.api.features.Priority;
import org.maps.messaging.api.features.QualityOfService;
import org.maps.messaging.api.message.Message;
import org.maps.messaging.api.message.TypedData;
import org.maps.network.protocol.ProtocolMessageTransformation;

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

  public long getCreation() {
    return creation;
  }

  public void setCreation(long creation) {
    this.creation = creation;
  }

  public @NotNull MessageBuilder setQoS(@NotNull QualityOfService qualityOfService) {
    this.qualityOfService = qualityOfService;
    return this;
  }

  public @NotNull MessageBuilder storeOffline(boolean storeOffline) {
    this.storeOffline = storeOffline;
    return this;
  }

  public @NotNull MessageBuilder setPayloadIndicator(boolean payloadFormatIndicator) {
    payloadUTF8 = payloadFormatIndicator;
    return this;
  }

  public @NotNull MessageBuilder setMessageExpiryInterval(long messageExpiryInterval, TimeUnit timeUnit) {
    expiry = timeUnit.toMillis(messageExpiryInterval);
    return this;
  }

  public @Nullable Map<String, String> getMeta() {
    return meta;
  }

  public @NotNull MessageBuilder setMeta(@Nullable  Map<String, String> meta) {
    this.meta = meta;
    return this;
  }

  public @Nullable Map<String, TypedData> getDataMap() {
    return dataMap;
  }

  public @NotNull MessageBuilder setDataMap(@Nullable Map<String, TypedData> dataMap) {
    this.dataMap = dataMap;
    return this;
  }

  public @Nullable byte[] getOpaqueData() {
    return opaqueData;
  }

  public @NotNull MessageBuilder setOpaqueData(@Nullable byte[] opaqueData) {
    this.opaqueData = opaqueData;
    return this;
  }

  public Object getCorrelationData() {
    return correlationData;
  }

  public @NotNull MessageBuilder setCorrelationData(@Nullable byte[] correlationData) {
    this.correlationData = correlationData;
    return this;
  }

  public @NotNull MessageBuilder setCorrelationData(@Nullable String correlationData) {
    this.correlationData = correlationData;
    return this;
  }

  public String getContentType() {
    return contentType;
  }

  public @NotNull MessageBuilder setContentType(@Nullable String contentType) {
    this.contentType = contentType;
    return this;
  }

  public String getResponseTopic() {
    return responseTopic;
  }

  public @NotNull MessageBuilder setResponseTopic(@Nullable String responseTopicString) {
    responseTopic = responseTopicString;
    return this;
  }

  public long getId() {
    return id;
  }

  public @NotNull MessageBuilder setId(long id) {
    this.id = id;
    return this;
  }

  public long getExpiry() {
    return expiry;
  }

  public Priority getPriority() {
    return priority;
  }

  public @NotNull MessageBuilder setPriority(Priority priority) {
    this.priority = priority;
    return this;
  }

  public QualityOfService getQos() {
    return qualityOfService;
  }

  public boolean isRetain() {
    return retain;
  }

  public @NotNull MessageBuilder setRetain(boolean retain) {
    this.retain = retain;
    return this;
  }

  public boolean isStoreOffline() {
    return storeOffline;
  }

  public boolean isPayloadUTF8() {
    return payloadUTF8;
  }


  public @Nullable ProtocolMessageTransformation getTransformation() {
    return transformation;
  }

  public  @NotNull MessageBuilder setTransformation(ProtocolMessageTransformation transformation) {
    this.transformation = transformation;
    return this;
  }

  public long getDelayed() {
    return delayed;
  }

  public  @NotNull MessageBuilder setDelayed(long delayed) {
    this.delayed = delayed;
    return this;
  }

  public @NotNull Message build() {
    if(transformation != null){
      ProtocolMessageTransformation local = transformation;
      transformation = null;
      return local.incoming(this);
    }
    return new Message(this);
  }

}
