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

package io.mapsmessaging.network.protocol.impl.tak.codec;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.WireFormat;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.schemas.config.impl.ProtoBufSchemaConfig;
import io.mapsmessaging.schemas.formatters.MessageFormatter;
import io.mapsmessaging.schemas.formatters.MessageFormatterFactory;
import io.mapsmessaging.selector.IdentifierResolver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

public class TakProtobufCodec implements TakPayloadCodec {

  private static final int DEFAULT_MAX_PAYLOAD_BYTES = 1024 * 1024;
  private final CotXmlCodec cotXmlCodec;
  private final int maxPayloadBytes;
  private final MessageFormatter protobufFormatter;
  private final String protobufSchemaId;

  public TakProtobufCodec() {
    this(new CotXmlCodec(), DEFAULT_MAX_PAYLOAD_BYTES, null, null);
  }

  public TakProtobufCodec(CotXmlCodec cotXmlCodec, int maxPayloadBytes) {
    this(cotXmlCodec, maxPayloadBytes, null, null);
  }

  public TakProtobufCodec(CotXmlCodec cotXmlCodec, int maxPayloadBytes, MessageFormatter protobufFormatter) {
    this(cotXmlCodec, maxPayloadBytes, protobufFormatter, null);
  }

  public TakProtobufCodec(CotXmlCodec cotXmlCodec, int maxPayloadBytes, MessageFormatter protobufFormatter, String protobufSchemaId) {
    this.cotXmlCodec = cotXmlCodec;
    this.maxPayloadBytes = Math.max(1024, maxPayloadBytes);
    this.protobufFormatter = protobufFormatter;
    this.protobufSchemaId = protobufSchemaId;
  }

  public static TakProtobufCodec withSchemaFormatter(CotXmlCodec cotXmlCodec, int maxPayloadBytes,
                                                     String descriptorBase64, String messageName) {
    return withSchemaFormatter(cotXmlCodec, maxPayloadBytes, descriptorBase64, messageName, null);
  }

  public static TakProtobufCodec withSchemaFormatter(CotXmlCodec cotXmlCodec, int maxPayloadBytes,
                                                     String descriptorBase64, String messageName, String protobufSchemaId) {
    return new TakProtobufCodec(cotXmlCodec, maxPayloadBytes, createFormatter(descriptorBase64, messageName), protobufSchemaId);
  }

  @Override
  public Message decode(byte[] payload) throws IOException {
    if (payload == null || payload.length == 0) {
      throw new IOException("Invalid TAK protobuf payload: empty");
    }
    if (payload.length > maxPayloadBytes) {
      throw new IOException("Invalid TAK protobuf payload: exceeds max size");
    }

    Map<String, String> meta = new LinkedHashMap<>();
    meta.put("tak.format", "protobuf");
    meta.put("tak.transport", "stream");

    byte[] embeddedCot = extractEmbeddedCotXml(payload);
    String schemaId = protobufSchemaId;
    if (embeddedCot != null) {
      try {
        Message cotMessage = cotXmlCodec.decode(embeddedCot);
        if (cotMessage.getMeta() != null) {
          meta.putAll(cotMessage.getMeta());
        }
        if (cotMessage.getSchemaId() != null && !cotMessage.getSchemaId().isBlank()) {
          schemaId = cotMessage.getSchemaId();
        }
        meta.put("tak.format", "protobuf");
        meta.put("tak.transport", "stream");
        meta.put("tak.protobuf_embedded_xml", "true");
      } catch (IOException ignored) {
        // Keep protobuf metadata only when embedded CoT is malformed.
      }
    } else if (protobufFormatter != null) {
      mergeSchemaParsedMeta(meta, payload);
    }
    addSelectorAliases(meta);

    MessageBuilder builder = new MessageBuilder()
        .setOpaqueData(payload)
        .setContentType("application/x-protobuf")
        .setMeta(meta);
    if (schemaId != null && !schemaId.isBlank()) {
      builder.setSchemaId(schemaId);
    }
    return builder.build();
  }

  @Override
  public byte[] encode(Message message) throws IOException {
    byte[] opaque = message.getOpaqueData();
    byte[] cloudEventPayload = TakCloudEventPayloadExtractor.tryExtractPayload(opaque);
    if (cloudEventPayload != null) {
      opaque = cloudEventPayload;
    }
    if (opaque != null && opaque.length > 0 && !looksLikeCotXml(opaque)) {
      if (opaque.length > maxPayloadBytes) {
        throw new IOException("TAK protobuf payload exceeds max size");
      }
      return opaque;
    }

    // Fallback mode: wrap CoT XML as protobuf field #1 (length-delimited).
    // This keeps the transport protobuf-framed for phase-2 interoperability work.
    byte[] cotXml = (opaque != null && opaque.length > 0) ? opaque : cotXmlCodec.encode(message);
    if (cotXml.length > maxPayloadBytes) {
      throw new IOException("TAK protobuf payload exceeds max size");
    }
    return wrapCotInLengthDelimitedField(cotXml);
  }

  private static byte[] wrapCotInLengthDelimitedField(byte[] cotXml) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream(cotXml.length + 8);
    CodedOutputStream codedOutputStream = CodedOutputStream.newInstance(out);
    codedOutputStream.writeByteArray(1, cotXml);
    codedOutputStream.flush();
    return out.toByteArray();
  }

  private static boolean looksLikeCotXml(byte[] payload) {
    String candidate = new String(payload, StandardCharsets.UTF_8).trim();
    return candidate.startsWith("<event");
  }

  private static byte[] extractEmbeddedCotXml(byte[] payload) throws IOException {
    CodedInputStream input = CodedInputStream.newInstance(payload);
    input.setSizeLimit(Math.max(DEFAULT_MAX_PAYLOAD_BYTES, payload.length + 1));
    while (!input.isAtEnd()) {
      int tag = input.readTag();
      if (tag == 0) {
        break;
      }
      int wireType = WireFormat.getTagWireType(tag);
      if (wireType == WireFormat.WIRETYPE_LENGTH_DELIMITED) {
        byte[] candidate = input.readByteArray();
        if (candidate.length > 0 && looksLikeCotXml(candidate)) {
          return candidate;
        }
      } else if (!input.skipField(tag)) {
        break;
      }
    }
    return null;
  }

  private void mergeSchemaParsedMeta(Map<String, String> meta, byte[] payload) {
    try {
      IdentifierResolver parsed = protobufFormatter.parse(payload);
      if (parsed == null) {
        return;
      }
      promoteAny(parsed, meta, "tak.uid", "uid", "event.uid");
      promoteAny(parsed, meta, "tak.type", "type", "event.type");
      promoteAny(parsed, meta, "tak.time", "time", "event.time");
      promoteAny(parsed, meta, "tak.start", "start", "event.start");
      promoteAny(parsed, meta, "tak.stale", "stale", "event.stale");
      promoteAny(parsed, meta, "tak.how", "how", "event.how");
      meta.put("tak.protobuf_parsed", "true");
    } catch (Exception ignored) {
      // keep base protobuf metadata when schema parsing fails
    }
  }

  private static void promoteAny(IdentifierResolver parsed, Map<String, String> meta, String toKey, String... fromKeys) {
    for (String fromKey : fromKeys) {
      Object val = parsed.get(fromKey);
      if (val != null) {
        meta.put(toKey, val.toString());
        return;
      }
    }
  }

  private static void addSelectorAliases(Map<String, String> meta) {
    alias(meta, "tak.uid", "tak_uid");
    alias(meta, "tak.type", "tak_type");
    alias(meta, "tak.time", "tak_time");
    alias(meta, "tak.start", "tak_start");
    alias(meta, "tak.stale", "tak_stale");
    alias(meta, "tak.how", "tak_how");
    alias(meta, "tak.lat", "tak_lat");
    alias(meta, "tak.lon", "tak_lon");
    alias(meta, "tak.hae", "tak_hae");
    alias(meta, "tak.ce", "tak_ce");
    alias(meta, "tak.le", "tak_le");
    alias(meta, "tak.format", "tak_format");
    alias(meta, "tak.transport", "tak_transport");
  }

  private static void alias(Map<String, String> meta, String sourceKey, String aliasKey) {
    String value = meta.get(sourceKey);
    if (value != null && !value.isBlank()) {
      meta.put(aliasKey, value);
    }
  }

  private static MessageFormatter createFormatter(String descriptorBase64, String messageName) {
    if (descriptorBase64 == null || descriptorBase64.isBlank() || messageName == null || messageName.isBlank()) {
      return null;
    }
    try {
      ProtoBufSchemaConfig schemaConfig = new ProtoBufSchemaConfig();
      ProtoBufSchemaConfig.ProtobufConfig protobufConfig = new ProtoBufSchemaConfig.ProtobufConfig();
      protobufConfig.setDescriptorValue(Base64.getDecoder().decode(descriptorBase64));
      protobufConfig.setMessageName(messageName);
      schemaConfig.setProtobufConfig(protobufConfig);
      return MessageFormatterFactory.getInstance().getFormatter(schemaConfig);
    } catch (Exception ignored) {
      return null;
    }
  }
}
