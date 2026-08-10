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

package io.mapsmessaging.network.protocol.impl.mavlink;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class MavlinkFrameExtractorTest {

  @Test
  void extractsBackToBackV1FramesUsingPayloadPlusEightBytes() {
    byte[] first = {(byte) 0xFE, 2, 7, 11, 3, 42, 90, 91, 12, 13};
    byte[] second = {(byte) 0xFE, 1, 8, 12, 4, 43, 92, 14, 15};
    byte[] input = new byte[first.length + second.length];
    System.arraycopy(first, 0, input, 0, first.length);
    System.arraycopy(second, 0, input, first.length, second.length);

    List<byte[]> frames = MavlinkFrameExtractor.extractMavlinkFrames(input);

    assertEquals(2, frames.size());
    assertArrayEquals(first, frames.get(0));
    assertArrayEquals(second, frames.get(1));
    assertEquals(11, MavlinkFrameExtractor.getSystemId(first));
  }

  @Test
  void ignoresNoiseAndLeavesTruncatedTrailingFrameUnconsumed() {
    byte[] complete = {(byte) 0xFE, 0, 1, 21, 2, 0, 10, 11};
    byte[] input = {99, complete[0], complete[1], complete[2], complete[3], complete[4], complete[5], complete[6], complete[7], (byte) 0xFE, 4, 1};

    List<byte[]> frames = MavlinkFrameExtractor.extractMavlinkFrames(input);

    assertEquals(1, frames.size());
    assertArrayEquals(complete, frames.getFirst());
  }
}
