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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class TakProtobufCodec implements TakPayloadCodec {

  private static final int DEFAULT_MAX_PAYLOAD_BYTES = 1024 * 1024;
  private final CotXmlCodec cotXmlCodec;
  private final int maxPayloadBytes;

  public TakProtobufCodec() {
    this(new CotXmlCodec(), DEFAULT_MAX_PAYLOAD_BYTES);
  }

  public TakProtobufCodec(CotXmlCodec cotXmlCodec, int maxPayloadBytes) {
    this.cotXmlCodec = cotXmlCodec;
    this.maxPayloadBytes = Math.max(1024, maxPayloadBytes);
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
    if (embeddedCot != null) {
      try {
        Message cotMessage = cotXmlCodec.decode(embeddedCot);
        if (cotMessage.getMeta() != null) {
          meta.putAll(cotMessage.getMeta());
        }
        meta.put("tak.format", "protobuf");
        meta.put("tak.transport", "stream");
        meta.put("tak.protobuf_embedded_xml", "true");
      } catch (IOException ignored) {
        // Keep protobuf metadata only when embedded CoT is malformed.
      }
    }

    return new MessageBuilder()
        .setOpaqueData(payload)
        .setContentType("application/x-protobuf")
        .setMeta(meta)
        .build();
  }

  @Override
  public byte[] encode(Message message) throws IOException {
    byte[] opaque = message.getOpaqueData();
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
}
