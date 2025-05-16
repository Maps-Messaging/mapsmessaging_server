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

public class MultiByteArrayDetection implements Detection {

  private final ByteArrayDetection[] individualDetection;

  public MultiByteArrayDetection(byte[][] detection, int offset) {
    this(detection, offset, 0);
  }

  public MultiByteArrayDetection(byte[][] detection, int offset, int range) {
    individualDetection = new ByteArrayDetection[detection.length];
    for (int x = 0; x < individualDetection.length; x++) {
      individualDetection[x] = new ByteArrayDetection(detection[x], offset, range);
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
    int start = packet.position();
    for (ByteArrayDetection detection : individualDetection) {
      try {
        boolean found = detection.detected(packet);
        packet.position(start);
        if (found) {
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
