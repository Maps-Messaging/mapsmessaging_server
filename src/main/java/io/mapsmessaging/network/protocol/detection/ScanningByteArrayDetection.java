/*
 *
 *   Copyright [ 2020 - 2022 ] [Matthew Buckton]
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

package io.mapsmessaging.network.protocol.detection;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.EndOfBufferException;

public class ScanningByteArrayDetection implements Detection {

  private final byte[] checkArray;
  private final int start;
  private final int end;

  public ScanningByteArrayDetection(byte[] check, int startPos, int endPos) {
    checkArray = check;
    start = startPos;
    end = endPos;
  }

  @Override
  public int getHeaderSize() {
    return end;
  }


  @Override
  public boolean detected(Packet packet) throws EndOfBufferException {
    if (packet.limit() < checkArray.length) {
      throw new EndOfBufferException();
    }

    byte[] test = new byte[end - start];
    packet.position(0);
    packet.get(test, 0, test.length);
    for (int y = 0; y < test.length - (checkArray.length); y++) {
      boolean found = true;
      for (int x = 0; x < checkArray.length; x++) {
        if (test[y + x] != checkArray[x]) {
          found = false;
          break;
        }
      }
      if (found) {
        return true;
      }
    }
    return false;
  }
}
