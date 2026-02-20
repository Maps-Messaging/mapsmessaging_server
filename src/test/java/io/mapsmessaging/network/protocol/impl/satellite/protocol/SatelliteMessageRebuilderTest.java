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

package io.mapsmessaging.network.protocol.impl.satellite.protocol;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SatelliteMessageRebuilderTest {

  @Test
  void rebuildIsCorrectRegardlessOfArrivalOrder() {
    SatelliteMessageRebuilder rebuilder = new SatelliteMessageRebuilder();

    byte transformerId = 0;
    boolean compressed = false;

    byte[] original = buildDeterministicPayload(4096);

    int maxBufferSize = 64;
    List<SatelliteMessage> fragments =
        SatelliteMessageFactory.createMessages(original, maxBufferSize, compressed, transformerId);

    assertNotNull(fragments);
    assertTrue(fragments.size() > 1, "Test must create multiple fragments.");

    // Feed in reverse order (worst-case).
    List<SatelliteMessage> reverse = new ArrayList<>(fragments);
    Collections.reverse(reverse);

    SatelliteMessage rebuilt = feedAll(rebuilder, reverse);

    assertNotNull(rebuilt, "Rebuilder did not return a completed message.");
    assertArrayEquals(original, rebuilt.getMessage(), "Rebuilt payload differs from original.");

    // Also test a rotated order (another realistic out-of-order delivery pattern).
    rebuilder.clear();

    List<SatelliteMessage> rotated = new ArrayList<>(fragments);
    Collections.rotate(rotated, fragments.size() / 2);

    SatelliteMessage rebuilt2 = feedAll(rebuilder, rotated);

    assertNotNull(rebuilt2, "Rebuilder did not return a completed message (rotated case).");
    assertArrayEquals(original, rebuilt2.getMessage(), "Rebuilt payload differs from original (rotated case).");
  }

  private static SatelliteMessage feedAll(SatelliteMessageRebuilder rebuilder, List<SatelliteMessage> arrivalOrder) {
    SatelliteMessage rebuilt = null;
    for (SatelliteMessage fragment : arrivalOrder) {
      SatelliteMessage maybe = rebuilder.rebuild(fragment);
      if (maybe != null) {
        rebuilt = maybe;
      }
    }
    return rebuilt;
  }

  private static byte[] buildDeterministicPayload(int size) {
    byte[] data = new byte[size];
    for (int i = 0; i < size; i++) {
      data[i] = (byte) (i * 31 + 7);
    }
    return data;
  }
}
