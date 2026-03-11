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

package io.mapsmessaging.network.protocol.impl.stream.assemblers;


import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.stream.CompleteHandler;
import lombok.Getter;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;

public class LengthPrefixedPacketAssembler extends Assembler {
  private final LengthEndian endian;
  private final ByteBuffer lengthBuffer;
  private final int maximumPacketSize;
  private final boolean directBuffer;

  @Getter
  private ByteBuffer payloadBuffer;

  public LengthPrefixedPacketAssembler(
      int lengthFieldSize,
      int maximumPacketSize,
      boolean directBuffer,
      LengthEndian endian,
      CompleteHandler completeHandler) {
    super(completeHandler);

    if (lengthFieldSize < 1 || lengthFieldSize > 4) {
      throw new IllegalArgumentException("Length field size must be between 1 and 4 bytes");
    }

    this.maximumPacketSize = maximumPacketSize;
    this.directBuffer = directBuffer;
    this.endian = endian;
    this.lengthBuffer = ByteBuffer.allocate(lengthFieldSize);
  }

  public void processPacket(@NonNull @NotNull Packet packet) throws IOException {
    ByteBuffer sourceBuffer = packet.getRawBuffer();

    while (sourceBuffer.hasRemaining()) {
      if (payloadBuffer == null) {
        readLength(sourceBuffer);
      }

      if (payloadBuffer != null) {
        readPayload(sourceBuffer);
      }
    }
  }

  public void reset() {
    lengthBuffer.clear();
    payloadBuffer = null;
  }


  private void readLength(ByteBuffer sourceBuffer) {
    while (sourceBuffer.hasRemaining() && lengthBuffer.hasRemaining()) {
      lengthBuffer.put(sourceBuffer.get());
    }

    if (!lengthBuffer.hasRemaining()) {
      lengthBuffer.flip();
      int packetLength = decodeLength(lengthBuffer);
      lengthBuffer.clear();

      if (packetLength < 0) {
        throw new IllegalStateException("Negative packet length is invalid: " + packetLength);
      }

      if (packetLength > maximumPacketSize) {
        throw new IllegalStateException("Packet length " + packetLength + " exceeds configured maximum of " + maximumPacketSize);
      }

      payloadBuffer = directBuffer ? ByteBuffer.allocateDirect(packetLength) : ByteBuffer.allocate(packetLength);

      if (packetLength == 0) {
        payloadBuffer.flip();
        ByteBuffer completedBuffer = payloadBuffer.asReadOnlyBuffer();
        payloadBuffer = null;
        completeHandler.completePacket(completedBuffer);
      }
    }
  }

  private int decodeLength(ByteBuffer buffer) {
    int value = 0;

    if (endian == LengthEndian.BIG) {
      while (buffer.hasRemaining()) {
        value = (value << 8) | (buffer.get() & 0xff);
      }
    } else {
      int shift = 0;
      while (buffer.hasRemaining()) {
        value |= (buffer.get() & 0xff) << shift;
        shift += 8;
      }
    }

    return value;
  }

  private void readPayload(ByteBuffer sourceBuffer) {
    int bytesToCopy = Math.min(sourceBuffer.remaining(), payloadBuffer.remaining());
    int originalLimit = sourceBuffer.limit();

    sourceBuffer.limit(sourceBuffer.position() + bytesToCopy);
    payloadBuffer.put(sourceBuffer);
    sourceBuffer.limit(originalLimit);

    if (!payloadBuffer.hasRemaining()) {
      payloadBuffer.flip();
      ByteBuffer completedBuffer = payloadBuffer.asReadOnlyBuffer();
      payloadBuffer = null;
      completeHandler.completePacket(completedBuffer);
    }
  }
}