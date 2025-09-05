/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
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

package io.mapsmessaging.network.protocol.impl.satellite.protocol;

import java.nio.ByteBuffer;

public class MessageQueue {

  public static  byte LENGTH_BYTE_SIZE = 3;

  protected void putVarUInt(ByteBuffer buffer, int value, int lengthBytes) {
    if (lengthBytes < 1 || lengthBytes > 4) {
      throw new IllegalArgumentException("lengthBytes must be 1..4: " + lengthBytes);
    }
    long max = (lengthBytes == 4) ? 0xFFFF_FFFFL : ((1L << (lengthBytes * 8)) - 1);
    if (value < 0 || (value & 0xFFFF_FFFFL) > max) {
      throw new IllegalArgumentException("Value out of range for u" + (lengthBytes * 8) + ": " + value);
    }
    for (int i = lengthBytes - 1; i >= 0; i--) {
      buffer.put((byte) (value >>> (i * 8)));
    }
  }

  protected int getVarUInt(ByteBuffer buffer, int lengthBytes) {
    if (lengthBytes < 1 || lengthBytes > 4) {
      throw new IllegalArgumentException("lengthBytes must be 1..4: " + lengthBytes);
    }
    int value = 0;
    for (int i = 0; i < lengthBytes; i++) {
      value = (value << 8) | (buffer.get() & 0xFF);
    }
    return value;
  }


  protected void requireUnsignedInRange(int value, int lengthBytes, String description) {
    if (lengthBytes < 1 || lengthBytes > 4) {
      throw new IllegalArgumentException("lengthBytes must be 1..4: " + lengthBytes);
    }
    long max = (lengthBytes == 4) ? 0xFFFF_FFFFL : ((1L << (lengthBytes * 8)) - 1);
    if (value < 0 || (value & 0xFFFF_FFFFL) > max) {
      throw new IllegalArgumentException(description + " out of range for u" + (lengthBytes * 8) + ": " + value);
    }
  }


}
