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

package io.mapsmessaging.network.protocol.detection;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.EndOfBufferException;

public class ByteArrayDetection implements Detection {

  private final byte[] checkArray;
  private final int start;
  private final int range;

  public ByteArrayDetection(byte[] check, int startPos) {
    this(check, startPos, 0);
  }

  public ByteArrayDetection(byte[] check, int startPos, int range) {
    checkArray = check;
    start = startPos;
    this.range = range;
  }

  @Override
  public int getHeaderSize() {
    return start + checkArray.length;
  }

  @Override
  public boolean detected(Packet packet) throws EndOfBufferException {
    if (packet.limit() - start < checkArray.length) {
      throw new EndOfBufferException();
    }

    int currentPos = packet.position();
    byte[] test = new byte[checkArray.length];
    int originPoint = currentPos+start;
    int shifted = 0;
    boolean found = false;
    while (!found) {
      packet.position(originPoint);
      packet.get(test, 0, checkArray.length);
      if (test[0] != checkArray[0]) {
        if (shifted == range) {
          return false;
        }
        originPoint++;
        shifted++;
        if (packet.limit() - originPoint < checkArray.length) {
          throw new EndOfBufferException();
        }
      } else {
        found = true;
      }
    }

    for (int x = 0; x < checkArray.length; x++) {
      if (test[x] != checkArray[x]) {
        return false;
      }
    }
    return true;
  }
}
