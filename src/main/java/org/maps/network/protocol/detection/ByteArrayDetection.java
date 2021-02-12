/*
 *
 *   Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.maps.network.protocol.detection;

import org.maps.network.io.Packet;
import org.maps.network.protocol.EndOfBufferException;

public class ByteArrayDetection implements Detection {

  private final byte[] checkArray;
  private final int start;

  public ByteArrayDetection(byte[] check, int startPos) {
    checkArray = check;
    start = startPos;
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

    byte[] test = new byte[checkArray.length];
    packet.position(start);
    packet.get(test, 0, checkArray.length);
    for (int x = 0; x < checkArray.length; x++) {
      if (test[x] != checkArray[x]) {
        return false;
      }
    }
    return true;
  }
}
