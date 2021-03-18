/*
 *    Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */

package io.mapsmessaging.api.message;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.features.Priority;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.engine.serializer.SerializedObject;
import io.mapsmessaging.selector.IdentifierResolver;
import io.mapsmessaging.utilities.streams.ObjectReader;
import io.mapsmessaging.utilities.streams.ObjectWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Message implements SerializedObject, IdentifierResolver {

  private static final int RETAIN_BIT = 0;
  private static final int UTF8_BIT = 1;
  private static final int CORRELATION_BYTE_ARRAY_BIT = 2;

  // time in milliseconds when this message will expire
  private final long expiry;
  private final long creation;
  private final Priority priority;
  private final QualityOfService qualityOfService;
  private final BitSet flags;
  private final String responseTopic;
  private final String contentType;
  private final byte[] correlationData;
  private final byte[] opaqueData;
  private final Map<String, String> meta;
  private final Map<String, TypedData> dataMap;
  private long delayed;

  // <editor-fold desc="Transient data">
  private final boolean storeOffline;
  private boolean isLastMessage;
  // </editor-fold>
  // <editor-fold desc="Persistent data">
  private long id;
  // </editor-fold>

  public Message(MessageBuilder builder) {
    flags = new BitSet(8);

    id = builder.getId();
    meta = builder.getMeta();
    Map<String, TypedData> map = builder.getDataMap();
    if(map instanceof DataMap){
      dataMap = map;
    }
    else if(map != null) {
      dataMap = new DataMap(map);
    }
    else {
      dataMap = new DataMap();
    }
    ((DataMap)dataMap).setMessage(this);
    opaqueData = builder.getOpaqueData();
    if(builder.getPriority() == null){
      priority = Priority.NORMAL;
    }
    else {
      priority = builder.getPriority();
    }
    storeOffline = builder.isStoreOffline();
    qualityOfService = builder.getQualityOfService();
    Object correlation = builder.getCorrelationData();
    if(correlation instanceof String){
      correlationData = ((String)correlation).getBytes(StandardCharsets.UTF_8);
    }
    else {
      correlationData = (byte[]) builder.getCorrelationData();
      flags.set(CORRELATION_BYTE_ARRAY_BIT); // Mark as byte[]
    }
    contentType = builder.getContentType();
    long dly = builder.getDelayed();
    long exp = builder.getExpiry();
    expiry = calculateExpiry(dly, exp);
    delayed = calculateDelay(dly);
    creation = builder.getCreation();
    responseTopic = builder.getResponseTopic();

    if (builder.isRetain()) {
      flags.set(RETAIN_BIT);
    }

    if (builder.isPayloadUTF8()) {
      flags.set(UTF8_BIT);
    }
    isLastMessage = false;
  }

  private long calculateExpiry(long dly, long exp){
    if (exp > 0) {
      exp = System.currentTimeMillis() + exp;
      if(dly > 0){
        return exp + dly; // Do not expire the event until AFTER it has been published
      }
      else{
        return exp;
      }
    }
    return 0;
  }

  private long calculateDelay(long dly){
    if(dly > 0){
      return System.currentTimeMillis() + dly;
    }
    return 0;
  }

  public Message(ObjectReader reader) throws IOException {
    // Fixed header - 19 bytes - Native data types
    id = reader.readLong();
    expiry = reader.readLong();
    delayed = reader.readLong();
    creation = reader.readLong();
    priority = Priority.getInstance(reader.readByte());
    qualityOfService = QualityOfService.getInstance(reader.readByte());
    flags = BitSet.valueOf(reader.readByteArray());

    // Optional
    responseTopic = reader.readString();
    contentType = reader.readString();
    correlationData = reader.readByteArray();

    // Complex data types
    meta = loadMeta(reader);
    dataMap = loadDataMap(reader);
    if(dataMap != null) {
      ((DataMap) dataMap).setMessage(this);
    }
    opaqueData = reader.readByteArray();
    storeOffline = false;
    isLastMessage = false;
  }

  // <editor-fold desc="Collection read/write functions">
  public void write(ObjectWriter writer) throws IOException {
    // Read Fixed header - Native data types
    writer.write(id);
    writer.write(expiry);
    writer.write(delayed);
    writer.write(creation);
    writer.write((byte)priority.getValue());
    writer.write((byte) qualityOfService.getLevel());
    writer.write(flags.toByteArray());

    // Optional
    writer.write(responseTopic);
    writer.write(contentType);
    writer.write(correlationData);

    // Complex data
    saveMeta(writer);
    saveDataMap(writer);
    writer.write(opaqueData);
  }

  public long getDelayed() {
    return delayed;
  }

  public void setDelayed(long delayed) {
    if(delayed >= 0) {
      this.delayed = delayed;
    }
  }

  public long getCreation() {
    return creation;
  }

  public long getIdentifier() {
    return id;
  }

  public void setIdentifier(long id) {
    this.id = id;
  }

  public @Nullable Map<String, String> getMeta() {
    return meta;
  }

  public @NotNull Map<String, TypedData> getDataMap() {
    return dataMap;
  }

  @Override
  public Object get(String key) {
    TypedData data = dataMap.get(key);
    if (data != null) {
      Object response = data.getData();
      if (response != null) {
        if (response instanceof Number) {
          if (response instanceof Double || response instanceof Float) {
            return ((Number) response).doubleValue();
          }
          return ((Number) response).longValue();
        }
        return data.getData();
      }
    }
    if (meta != null) {
      return meta.get(key);
    }
    return null;
  }

  @Override
  public byte[] getOpaqueData() {
    return opaqueData;
  }

  public @NonNull @NotNull Priority getPriority() {
    return priority;
  }

  public boolean isRetain() {
    return flags.get(RETAIN_BIT);
  }

  public boolean isStoreOffline() {
    return storeOffline;
  }

  public byte[] getCorrelationData() {
    return correlationData;
  }

  public boolean isCorrelationDataByteArray(){
    return flags.get(CORRELATION_BYTE_ARRAY_BIT);
  }

  public @Nullable String getContentType() {
    return contentType;
  }

  public long getExpiry() {
    return expiry;
  }

  public boolean isLastMessage() {
    return isLastMessage;
  }

  public void setLastMessage(boolean lastMessage) {
    isLastMessage = lastMessage;
  }

  public boolean isUTF8() {
    return flags.get(UTF8_BIT);
  }

  public @NonNull @NotNull QualityOfService getQualityOfService() {
    return qualityOfService;
  }

  public @Nullable String getResponseTopic() {
    return responseTopic;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Key:")
        .append(id)
        .append(" meta:")
        .append(meta)
        .append(" map:")
        .append(dataMap)
        .append(" Opaque:");
    if (opaqueData != null) {
      sb.append(opaqueData.length);
    } else {
      sb.append("NULL");
    }
    sb.append(" StoreOffline::")
        .append(isStoreOffline())
        .append(" Retain::")
        .append(isRetain())
        .append(" isUTF8:")
        .append(isUTF8())
        .append(" ContentType:")
        .append(contentType)
        .append(" Creation:")
        .append(creation)
        .append(" Expiry:")
        .append(expiry)
        .append(" Delay:")
        .append(delayed)
        .append(" CorrelationData:");
    if (correlationData != null) {
      for(byte b:correlationData){
        sb.append(Long.toHexString((0xff&b))).append(",");
      }
      sb.append("[").append(new String(correlationData)).append("]");
      sb.append(correlationData.length);
    } else {
      sb.append("NULL");
    }
    return sb.toString();
  }

  private static @Nullable Map<String, TypedData> loadDataMap(ObjectReader reader) throws IOException {
    Map<String, TypedData> map = null;
    int len = reader.readInt();
    if (len > -1) {
      map = new DataMap();
      for (int x = 0; x < len; x++) {
        map.put(reader.readString(), new TypedData(reader));
      }
    }
    return map;
  }

  private void saveDataMap(ObjectWriter writer) throws IOException {
    if (dataMap != null) {
      writer.write(dataMap.size());
      for (Map.Entry<String, TypedData> entry : dataMap.entrySet()) {
        writer.write(entry.getKey());
        entry.getValue().write(writer);
      }
    } else {
      writer.write(-1);
    }
  }

  private Map<String, String> loadMeta(ObjectReader reader) throws IOException {
    Map<String, String> result = null;
    int len = reader.readInt();
    if (len > -1) {
      result = new LinkedHashMap<>();
      for (int x = 0; x < len; x++) {
        result.put(reader.readString(), reader.readString());
      }
    }
    return result;
  }

  private void saveMeta(ObjectWriter writer) throws IOException {
    if (meta != null) {
      writer.write(meta.size());
      for (Map.Entry<String, String> entry : meta.entrySet()) {
        writer.write(entry.getKey());
        writer.write(entry.getValue());
      }
    } else {
      writer.write(-1);
    }
  }

  // </editor-fold>

}
