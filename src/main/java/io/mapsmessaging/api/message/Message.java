/*
 *    Copyright [ 2020 - 2022 ] [Matthew Buckton]
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
import io.mapsmessaging.api.features.Constants;
import io.mapsmessaging.api.features.Priority;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.selector.IdentifierResolver;
import io.mapsmessaging.storage.Storable;
import io.mapsmessaging.storage.impl.streams.BufferObjectReader;
import io.mapsmessaging.storage.impl.streams.ObjectReader;
import io.mapsmessaging.storage.impl.streams.ObjectWriter;
import io.mapsmessaging.storage.impl.streams.StreamObjectWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

public class Message implements IdentifierResolver, Storable {

  private static final int RETAIN_BIT = 0;
  private static final int UTF8_BIT = 1;
  private static final int CORRELATION_BYTE_ARRAY_BIT = 2;
  private static final int SCHEMA_ID_PRESENT = 3;
  private static final int COMPRESSED_PACK = 4;


  @Getter
  private final long expiry;   // time in milliseconds when this message will expire
  @Getter
  private final long creation;
  @Getter
  private final Priority priority;
  @Getter
  private final QualityOfService qualityOfService;
  @Getter
  private final String responseTopic;
  @Getter
  private final String contentType;
  @Getter
  private final byte[] correlationData;
  @Getter
  private final byte[] opaqueData;
  @Getter
  private final Map<String, String> meta;
  @Getter
  private final Map<String, TypedData> dataMap;

  @Getter
  private String schemaId;

  @Getter
  private final boolean storeOffline; // Not stored to disk

  private final BitSet flags;

  @Getter
  private long delayed; // This is set via the engine on the way through

  // <editor-fold desc="Transient data">
  @Getter
  @Setter
  private boolean lastMessage; // This is set via the engine as it is delivered to the client
  // </editor-fold>
  // <editor-fold desc="Persistent data">
  @Getter
  @Setter
  private long identifier;
  // </editor-fold>


  public Message(MessageBuilder builder) {
    flags = new BitSet(8);

    identifier = builder.getId();
    meta = builder.getMeta();
    Map<String, TypedData> map = builder.getDataMap();
    if (map instanceof DataMap) {
      dataMap = map;
    } else if (map != null) {
      dataMap = new DataMap(map);
    } else {
      dataMap = new DataMap();
    }
    ((DataMap) dataMap).setMessage(this);
    opaqueData = builder.getOpaqueData();
    if (builder.getPriority() == null) {
      priority = Priority.NORMAL;
    } else {
      priority = builder.getPriority();
    }
    storeOffline = builder.isStoreOffline();
    qualityOfService = builder.getQualityOfService();
    Object correlation = builder.getCorrelationData();
    if (correlation instanceof String) {
      correlationData = ((String) correlation).getBytes(StandardCharsets.UTF_8);
    } else {
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
    lastMessage = false;
    schemaId = builder.getSchemaId();
    if (schemaId != null) {
      flags.set(SCHEMA_ID_PRESENT);
    }
  }

  Message(ByteBuffer[] packed) throws IOException {
    BufferObjectReader header = new BufferObjectReader(packed[0]);
    BufferObjectReader optional = new BufferObjectReader(packed[1]);

    identifier = header.readLong();
    expiry = header.readLong();
    delayed = header.readLong();
    creation = header.readLong();
    priority = Priority.getInstance(header.readByte());
    qualityOfService = QualityOfService.getInstance(header.readByte());

    flags = BitSet.valueOf(optional.readByteArray());
    responseTopic = optional.readString();
    contentType = optional.readString();
    correlationData = optional.readByteArray();
    if (flags.get(SCHEMA_ID_PRESENT)) {
      schemaId = optional.readString();
    }
    byte containsBuffers = optional.readByte();

    int idx = 2;
    if ((containsBuffers & 0x1) != 0) {
      BufferObjectReader metaReader = new BufferObjectReader(packed[idx]);
      meta = loadMeta(metaReader);
      idx++;
    } else {
      meta = null;
    }

    if ((containsBuffers & 0x2) != 0) {
      BufferObjectReader map = new BufferObjectReader(packed[idx]);
      dataMap = loadDataMap(map);
      idx++;
    } else {
      dataMap = new DataMap();
    }
    if (dataMap != null) {
      ((DataMap) dataMap).setMessage(this);
    }

    if ((containsBuffers & 0x4) != 0) {
      byte[] tmp = packed[idx].array();
      if(flags.get(COMPRESSED_PACK)){
        flags.set(COMPRESSED_PACK, false);
        Inflater inflater = new Inflater();
        int len = packed[idx].getInt();
        opaqueData = new byte[len];
        inflater.setInput(tmp, 4, tmp.length-4 );
        try {
          inflater.inflate(opaqueData);
        } catch (DataFormatException e) {
          // Log this
        }
      }
      else{
        opaqueData = tmp;
      }
    } else {
      opaqueData = null;
    }
    storeOffline = true;
    lastMessage = false;
  }

  ByteBuffer[] pack() throws IOException {
    ByteBuffer header = ByteBuffer.allocate(34);
    header.putLong(identifier);
    header.putLong(expiry);
    header.putLong(delayed);
    header.putLong(creation);
    header.put((byte) priority.getValue());
    header.put((byte) qualityOfService.getLevel());
    header.flip();

    byte containsBuffers = 0;
    boolean hasMeta = meta != null && !meta.isEmpty();
    boolean hasMap = dataMap != null && !dataMap.isEmpty();
    boolean hasOpaque = opaqueData != null;
    int bufferCount = 2;
    if (hasMeta) {
      bufferCount++;
      containsBuffers = 0x1;
    }
    if (hasMap) {
      bufferCount++;
      containsBuffers = (byte) (containsBuffers | 0x2);
    }
    if (hasOpaque) {
      bufferCount++;
      containsBuffers = (byte) (containsBuffers | 0x4);
    }

    boolean compress = Constants.getInstance().isEnableMessageStoreCompression() &&  opaqueData != null && opaqueData.length > Constants.getInstance().getMinimumMessageSize();
    flags.set(COMPRESSED_PACK, compress);
    ByteArrayOutputStream optional = new ByteArrayOutputStream(1024);
    StreamObjectWriter optionalWriter = new StreamObjectWriter(optional);
    optionalWriter.write(flags.toByteArray());
    optionalWriter.write(responseTopic);
    optionalWriter.write(contentType);
    optionalWriter.write(correlationData);
    if (schemaId != null) {
      optionalWriter.write(schemaId);
    }
    optionalWriter.write(containsBuffers);

    ByteBuffer[] packed = new ByteBuffer[bufferCount];
    packed[0] = header;
    packed[1] = ByteBuffer.wrap(optional.toByteArray());
    int idx = 2;
    if (hasMeta) {
      ByteArrayOutputStream metaStream = new ByteArrayOutputStream(1024);
      StreamObjectWriter metaWriter = new StreamObjectWriter(metaStream);
      saveMeta(metaWriter);
      packed[idx] = ByteBuffer.wrap(metaStream.toByteArray());
      idx++;
    }
    if (hasMap) {
      ByteArrayOutputStream mapStream = new ByteArrayOutputStream(1024);
      StreamObjectWriter dataMapWriter = new StreamObjectWriter(mapStream);
      saveDataMap(dataMapWriter);
      packed[idx] = ByteBuffer.wrap(mapStream.toByteArray());
      idx++;
    }
    if (opaqueData != null) {
      if(compress){
        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        deflater.setInput(opaqueData);
        deflater.finish();
        byte[] tmp = new byte[opaqueData.length+10];
        int len = deflater.deflate(tmp);
        ByteBuffer packedBuffer = ByteBuffer.allocate(len+4);
        packedBuffer.putInt(opaqueData.length);
        packedBuffer.put(tmp, 0, len);
        packed[idx] = packedBuffer;
        packedBuffer.flip();
      }
      else {
        packed[idx] = ByteBuffer.wrap(opaqueData);
      }
    }
    return packed;
  }


  private long calculateExpiry(long dly, long exp) {
    long calc = 0;
    if (exp > 0) {
      calc = System.currentTimeMillis() + exp;
      if (dly > 0) {
        calc = calc + dly; // Do not expire the event until AFTER it has been published
      }
    }
    return calc;
  }

  private long calculateDelay(long dly) {
    if (dly > 0) {
      return System.currentTimeMillis() + dly;
    }
    return 0;
  }

  public void setDelayed(long delayed) {
    if (delayed >= 0) {
      this.delayed = delayed;
    }
  }

  public long getKey() {
    return identifier;
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

  public boolean isRetain() {
    return flags.get(RETAIN_BIT);
  }

  public boolean isCorrelationDataByteArray() {
    return flags.get(CORRELATION_BYTE_ARRAY_BIT);
  }

  public boolean isUTF8() {
    return flags.get(UTF8_BIT);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Key:")
        .append(identifier)
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
      for (byte b : correlationData) {
        sb.append(Long.toHexString((0xff & b))).append(",");
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

  public static void main(String[] args){
    try {
      // Encode a String into bytes
      String inputString = "blahblahblah";
      byte[] input = inputString.getBytes("UTF-8");

      // Compress the bytes
      byte[] output = new byte[100];
      Deflater compresser = new Deflater();
      compresser.setInput(input);
      compresser.finish();
      int compressedDataLength = compresser.deflate(output);
      compresser.end();

      // Decompress the bytes
      Inflater decompresser = new Inflater();
      decompresser.setInput(output, 0, compressedDataLength);
      byte[] result = new byte[100];
      int resultLength = decompresser.inflate(result);
      decompresser.end();

      // Decode the bytes into a String
      String outputString = new String(result, 0, resultLength, "UTF-8");
    } catch(java.io.UnsupportedEncodingException ex) {
      // handle
    } catch (java.util.zip.DataFormatException ex) {
      // handle
    }
  }
}
