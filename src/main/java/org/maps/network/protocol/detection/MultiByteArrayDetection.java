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

public class MultiByteArrayDetection implements Detection {

  private final ByteArrayDetection[] individualDetection;

  public MultiByteArrayDetection(byte[][] detection, int offset) {
    individualDetection = new ByteArrayDetection[detection.length];
    for (int x = 0; x < individualDetection.length; x++) {
      individualDetection[x] = new ByteArrayDetection(detection[x], offset);
    }
  }

  @Override
  public int getHeaderSize() {
    int length = 0;
    for (ByteArrayDetection detection : individualDetection) {
      length = Math.max(detection.getHeaderSize(), length);
    }
    return length;
  }

  @Override
  public boolean detected(Packet packet) throws EndOfBufferException {
    EndOfBufferException exception = null;
    for (ByteArrayDetection detection : individualDetection) {
      try {
        if (detection.detected(packet)) {
          return true;
        }
      } catch (EndOfBufferException e) {
        exception = e;
      }
    }
    if (exception != null) {
      throw exception;
    }
    return false;
  }
}
