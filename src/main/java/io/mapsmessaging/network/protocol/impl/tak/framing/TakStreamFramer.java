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

package io.mapsmessaging.network.protocol.impl.tak.framing;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TakStreamFramer {

  public enum Mode {
    XML_STREAM,
    PROTO_STREAM
  }

  private static final byte TAK_PROTO_MAGIC = (byte) 0xBF;
  private static final byte[] XML_END_TAG = "</event>".getBytes(StandardCharsets.UTF_8);

  private final Mode mode;
  private final int maxPayloadBytes;
  private byte[] buffer;

  public TakStreamFramer(Mode mode, int maxPayloadBytes) {
    this.mode = mode;
    this.maxPayloadBytes = maxPayloadBytes;
    this.buffer = new byte[0];
  }

  public synchronized List<byte[]> onBytes(byte[] incoming, int length) throws IOException {
    if (incoming == null || length <= 0) {
      return List.of();
    }
    append(incoming, length);
    if (buffer.length > maxPayloadBytes * 2) {
      throw new IOException("Buffered TAK stream exceeds safe bound");
    }
    return switch (mode) {
      case XML_STREAM -> parseXml();
      case PROTO_STREAM -> parseProto();
    };
  }

  public synchronized byte[] frame(byte[] payload) throws IOException {
    if (payload == null) {
      return new byte[0];
    }
    if (payload.length > maxPayloadBytes) {
      throw new IOException("Payload exceeds max frame size");
    }
    if (mode == Mode.XML_STREAM) {
      return payload;
    }
    byte[] varint = encodeVarint(payload.length);
    byte[] out = new byte[1 + varint.length + payload.length];
    out[0] = TAK_PROTO_MAGIC;
    System.arraycopy(varint, 0, out, 1, varint.length);
    System.arraycopy(payload, 0, out, 1 + varint.length, payload.length);
    return out;
  }

  private List<byte[]> parseXml() throws IOException {
    List<byte[]> frames = new ArrayList<>();
    while (true) {
      int endTag = indexOf(buffer, XML_END_TAG, 0);
      if (endTag < 0) {
        break;
      }
      int frameEnd = endTag + XML_END_TAG.length;
      while (frameEnd < buffer.length && Character.isWhitespace((char) buffer[frameEnd])) {
        frameEnd++;
      }
      if (frameEnd > maxPayloadBytes) {
        throw new IOException("TAK XML frame exceeds max frame size");
      }
      frames.add(Arrays.copyOfRange(buffer, 0, frameEnd));
      buffer = Arrays.copyOfRange(buffer, frameEnd, buffer.length);
    }
    return frames;
  }

  private List<byte[]> parseProto() throws IOException {
    List<byte[]> frames = new ArrayList<>();
    while (buffer.length > 0) {
      if (buffer[0] != TAK_PROTO_MAGIC) {
        throw new IOException("Invalid TAK protobuf stream header");
      }
      if (buffer.length < 2) {
        break;
      }
      VarintResult varintResult = decodeVarint(buffer, 1);
      if (varintResult == null) {
        break;
      }
      int length = varintResult.value;
      if (length < 0 || length > maxPayloadBytes) {
        throw new IOException("Invalid TAK protobuf payload length: " + length);
      }
      int frameLength = 1 + varintResult.bytesRead + length;
      if (buffer.length < frameLength) {
        break;
      }
      int payloadOffset = 1 + varintResult.bytesRead;
      frames.add(Arrays.copyOfRange(buffer, payloadOffset, payloadOffset + length));
      buffer = Arrays.copyOfRange(buffer, frameLength, buffer.length);
    }
    return frames;
  }

  private void append(byte[] data, int length) {
    int copyLength = Math.min(length, data.length);
    byte[] next = Arrays.copyOf(buffer, buffer.length + copyLength);
    System.arraycopy(data, 0, next, buffer.length, copyLength);
    buffer = next;
  }

  private static byte[] encodeVarint(int value) {
    byte[] tmp = new byte[5];
    int count = 0;
    int v = value;
    while ((v & ~0x7F) != 0) {
      tmp[count++] = (byte) ((v & 0x7F) | 0x80);
      v >>>= 7;
    }
    tmp[count++] = (byte) (v & 0x7F);
    return Arrays.copyOf(tmp, count);
  }

  private static VarintResult decodeVarint(byte[] data, int offset) throws IOException {
    int value = 0;
    int shift = 0;
    int bytes = 0;
    for (int i = offset; i < data.length && bytes < 5; i++) {
      int b = data[i] & 0xFF;
      value |= (b & 0x7F) << shift;
      bytes++;
      if ((b & 0x80) == 0) {
        return new VarintResult(value, bytes);
      }
      shift += 7;
    }
    if (bytes >= 5 && (data[offset + 4] & 0x80) != 0) {
      throw new IOException("Malformed varint length");
    }
    return null;
  }

  private static int indexOf(byte[] haystack, byte[] needle, int from) {
    if (needle.length == 0 || haystack.length < needle.length) {
      return -1;
    }
    for (int i = from; i <= haystack.length - needle.length; i++) {
      boolean match = true;
      for (int j = 0; j < needle.length; j++) {
        if (haystack[i + j] != needle[j]) {
          match = false;
          break;
        }
      }
      if (match) {
        return i;
      }
    }
    return -1;
  }

  private record VarintResult(int value, int bytesRead) {}
}
