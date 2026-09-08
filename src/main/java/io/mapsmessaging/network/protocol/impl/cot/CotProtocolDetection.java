/*
 *
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     https://commonsclause.com/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.mapsmessaging.network.protocol.impl.cot;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.EndOfBufferException;
import io.mapsmessaging.network.protocol.detection.Detection;
import java.nio.charset.StandardCharsets;

public class CotProtocolDetection implements Detection {

  private static final byte[] EVENT = "<event".getBytes(StandardCharsets.US_ASCII);
  private static final int HEADER_SIZE = 256;

  @Override
  public int getHeaderSize() {
    return HEADER_SIZE;
  }

  @Override
  public boolean detected(Packet packet) throws EndOfBufferException {
    int available = packet.available();
    for (int offset = 0; offset <= available - EVENT.length; offset++) {
      boolean match = true;
      for (int index = 0; index < EVENT.length; index++) {
        if (packet.get(packet.position() + offset + index) != EVENT[index]) {
          match = false;
          break;
        }
      }
      if (match && hasElementNameBoundary(packet, offset + EVENT.length, available)) {
        return true;
      }
    }
    if (available < HEADER_SIZE) {
      throw new EndOfBufferException();
    }
    return false;
  }

  private boolean hasElementNameBoundary(Packet packet, int offset, int available)
      throws EndOfBufferException {
    if (offset >= available) {
      throw new EndOfBufferException();
    }
    byte value = packet.get(packet.position() + offset);
    return value == ' ' || value == '\t' || value == '\r' || value == '\n'
        || value == '>' || value == '/';
  }
}
