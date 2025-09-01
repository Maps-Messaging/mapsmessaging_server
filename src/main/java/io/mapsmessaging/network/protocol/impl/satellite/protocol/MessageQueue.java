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

  private static final int MAX_U24 = 0xFFFFFF;

  protected void putTriByte(ByteBuffer buffer, int value) {
    if (value < 0 || value > 0xFFFFFF) {
      throw new IllegalArgumentException("Value out of 24-bit range: " + value);
    }
    buffer.put((byte) (value >>> 16));
    buffer.put((byte) (value >>> 8));
    buffer.put((byte) value);
  }

  protected int getTriByte(ByteBuffer buffer) {
    int b1 = buffer.get() & 0xFF;
    int b2 = buffer.get() & 0xFF;
    int b3 = buffer.get() & 0xFF;
    return (b1 << 16) | (b2 << 8) | b3;
  }

  protected void putTriByte(byte[] array, int offset, int value) {
    if (value < 0 || value > 0xFFFFFF) {
      throw new IllegalArgumentException("Value out of 24-bit range: " + value);
    }
    array[offset]     = (byte) (value >>> 16);
    array[offset + 1] = (byte) (value >>> 8);
    array[offset + 2] = (byte) value;
  }
  public static int getTriByte(byte[] array, int offset) {
    return ((array[offset]     & 0xFF) << 16) |
        ((array[offset + 1] & 0xFF) << 8)  |
        (array[offset + 2] & 0xFF);
  }


  protected void requireU24(int value, String description) {
    if (value < 0 || value > MAX_U24) {
      throw new IllegalArgumentException(description + " out of range for u24: " + value);
    }
  }
}
