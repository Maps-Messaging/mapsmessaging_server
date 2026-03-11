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
import java.util.Arrays;

public class JsonBracePacketAssembler extends Assembler {


  private final int maximumPacketSize;
  private final boolean directBuffer;

  private byte[] workingBuffer;
  private int workingLength;

  @Getter
  private int braceDepth;

  public JsonBracePacketAssembler(int maximumPacketSize, boolean directBuffer, CompleteHandler completeHandler) {
    super(completeHandler);
    if (maximumPacketSize < 2) {
      throw new IllegalArgumentException("Maximum packet size must be at least 2");
    }
    if (completeHandler == null) {
      throw new IllegalArgumentException("CompleteHandler must not be null");
    }

    this.maximumPacketSize = maximumPacketSize;
    this.directBuffer = directBuffer;
    this.workingBuffer = new byte[Math.min(maximumPacketSize, 1024)];
    this.workingLength = 0;
    this.braceDepth = 0;
  }

  public void processPacket(@NonNull @NotNull Packet packet) throws IOException {
    ByteBuffer sourceBuffer = packet.getRawBuffer();

    while (sourceBuffer.hasRemaining()) {
      byte value = sourceBuffer.get();

      if (workingLength == 0) {
        if (value == '{') {
          append(value);
          braceDepth = 1;
        }
        continue;
      }

      append(value);

      if (value == '{') {
        braceDepth++;
      } else if (value == '}') {
        braceDepth--;
        if (braceDepth == 0) {
          completeCurrentPacket();
        } else if (braceDepth < 0) {
          reset();
          throw new IllegalStateException("JSON brace depth became negative");
        }
      }
    }
  }

  public void reset() {
    workingLength = 0;
    braceDepth = 0;
  }

  private void append(byte value) {
    if (workingLength == maximumPacketSize) {
      reset();
      throw new IllegalStateException("JSON packet exceeds configured maximum of " + maximumPacketSize + " bytes");
    }

    ensureCapacity(workingLength + 1);
    workingBuffer[workingLength] = value;
    workingLength++;
  }

  private void ensureCapacity(int requiredCapacity) {
    if (requiredCapacity <= workingBuffer.length) {
      return;
    }

    int newCapacity = Math.max(workingBuffer.length << 1, requiredCapacity);
    newCapacity = Math.min(newCapacity, maximumPacketSize);
    workingBuffer = Arrays.copyOf(workingBuffer, newCapacity);
  }

  private void completeCurrentPacket() {
    ByteBuffer completedBuffer;
    if (directBuffer) {
      completedBuffer = ByteBuffer.allocateDirect(workingLength);
      completedBuffer.put(workingBuffer, 0, workingLength);
      completedBuffer.flip();
    } else {
      completedBuffer = ByteBuffer.wrap(Arrays.copyOf(workingBuffer, workingLength));
    }

    workingLength = 0;
    braceDepth = 0;
    completeHandler.completePacket(completedBuffer.asReadOnlyBuffer());
  }
}