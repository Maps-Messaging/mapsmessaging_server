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

package io.mapsmessaging.network.protocol.impl.mavlink;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.StreamHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class MavlinkStreamHandler implements StreamHandler {

  private static final int MAVLINK_V1_MAGIC = 0xFE;
  private static final int MAVLINK_V2_MAGIC = 0xFD;

  private static final int MAVLINK_V1_HEADER_REST = 5; // after magic+len
  private static final int MAVLINK_V1_CRC_LEN = 2;

  private static final int MAVLINK_V2_HEADER_REST = 8; // after magic+len (incompat, compat, seq, sys, comp, msgid[3])
  private static final int MAVLINK_V2_CRC_LEN = 2;
  private static final int MAVLINK_V2_SIGNATURE_LEN = 13;
  private static final int MAVLINK_V2_INCOMPAT_FLAG_SIGNED = 0x01;

  private final byte[] smallBuffer;
  private volatile boolean closed;

  public MavlinkStreamHandler() {
    this.smallBuffer = new byte[32];
    this.closed = false;
  }

  @Override
  public void close() {
    closed = true;
  }

  @Override
  public int parseInput(InputStream input, Packet packet) throws IOException {
    if (closed) {
      return -1;
    }
    if (input == null) {
      throw new IOException("InputStream is null");
    }
    if (packet == null) {
      throw new IOException("Packet is null");
    }

    int magic;
    while (true) {
      magic = input.read();
      if (magic < 0) {
        return -1;
      }
      if (magic == MAVLINK_V1_MAGIC || magic == MAVLINK_V2_MAGIC) {
        break;
      }
      if (closed) {
        return -1;
      }
    }

    int payloadLength = input.read();
    if (payloadLength < 0) {
      return -1;
    }

    packet.clear();

    if (magic == MAVLINK_V1_MAGIC) {
      int frameLength = 2 + MAVLINK_V1_HEADER_REST + payloadLength + MAVLINK_V1_CRC_LEN;
      ensureCapacity(packet, frameLength);

      packet.putByte(magic);
      packet.putByte(payloadLength);

      readFully(input, smallBuffer, 0, MAVLINK_V1_HEADER_REST);
      packet.put(smallBuffer, 0, MAVLINK_V1_HEADER_REST);

      readPayload(input, packet, payloadLength);

      readFully(input, smallBuffer, 0, MAVLINK_V1_CRC_LEN);
      packet.put(smallBuffer, 0, MAVLINK_V1_CRC_LEN);
      return frameLength;
    }

    readFully(input, smallBuffer, 0, MAVLINK_V2_HEADER_REST);

    int incompatFlags = smallBuffer[0] & 0xFF;
    boolean signed = (incompatFlags & MAVLINK_V2_INCOMPAT_FLAG_SIGNED) != 0;

    int frameLength = 2 + MAVLINK_V2_HEADER_REST + payloadLength + MAVLINK_V2_CRC_LEN + (signed ? MAVLINK_V2_SIGNATURE_LEN : 0);
    ensureCapacity(packet, frameLength);

    packet.putByte(magic);
    packet.putByte(payloadLength);

    packet.put(smallBuffer, 0, MAVLINK_V2_HEADER_REST);

    readPayload(input, packet, payloadLength);

    readFully(input, smallBuffer, 0, MAVLINK_V2_CRC_LEN);
    packet.put(smallBuffer, 0, MAVLINK_V2_CRC_LEN);

    if (signed) {
      readFully(input, smallBuffer, 0, MAVLINK_V2_SIGNATURE_LEN);
      packet.put(smallBuffer, 0, MAVLINK_V2_SIGNATURE_LEN);
    }
    return frameLength;
  }

  @Override
  public int parseOutput(OutputStream output, Packet packet) throws IOException {
    if (closed) {
      return -1;
    }
    if (output == null) {
      throw new IOException("OutputStream is null");
    }
    if (packet == null) {
      throw new IOException("Packet is null");
    }

    ByteBuffer buffer = packet.getRawBuffer().duplicate();

    int position = buffer.position();
    int limit = buffer.limit();

    if (limit < position) {
      throw new IOException("Invalid packet buffer state (limit < position)");
    }

    int length = limit - position;
    if (length == 0) {
      return 0;
    }

    while (buffer.hasRemaining()) {
      int chunk = Math.min(buffer.remaining(), smallBuffer.length);
      buffer.get(smallBuffer, 0, chunk);
      output.write(smallBuffer, 0, chunk);
    }

    return length;
  }

  private void ensureCapacity(Packet packet, int required) throws IOException {
    if (packet.capacity() < required) {
      throw new IOException("Packet capacity " + packet.capacity() + " is smaller than required frame size " + required);
    }
  }

  private void readPayload(InputStream input, Packet packet, int payloadLength) throws IOException {
    if (payloadLength == 0) {
      return;
    }
    int remaining = payloadLength;
    while (remaining > 0) {
      int chunk = Math.min(remaining, smallBuffer.length);
      readFully(input, smallBuffer, 0, chunk);
      packet.put(smallBuffer, 0, chunk);
      remaining -= chunk;
    }
  }

  private void readFully(InputStream input, byte[] buffer, int offset, int length) throws IOException {
    int readTotal = 0;
    while (readTotal < length) {
      if (closed) {
        throw new IOException("StreamHandler closed");
      }
      int read = input.read(buffer, offset + readTotal, length - readTotal);
      if (read < 0) {
        throw new IOException("Unexpected end of stream");
      }
      readTotal += read;
    }
  }
}
