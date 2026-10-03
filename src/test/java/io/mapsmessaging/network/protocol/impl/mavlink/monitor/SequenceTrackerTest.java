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

package io.mapsmessaging.network.protocol.impl.mavlink.monitor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SequenceTrackerTest {

  @Test
  void firstSequenceIsInitial() {
    SequenceResult result = new SequenceTracker().accept(42);

    assertEquals(-1, result.getPreviousSequenceNumber());
    assertEquals(42, result.getCurrentSequenceNumber());
    assertEquals(42, result.getExpectedSequenceNumber());
    assertEquals(0, result.getDelta());
    assertEquals(0, result.getLostPackets());
    assertEquals(SequenceStatus.INITIAL, result.getStatus());
    assertTrue(result.isStatusChanged());
  }

  @Test
  void consecutiveSequencesBecomeOkAndRepeatedOkDoesNotChangeStatus() {
    SequenceTracker tracker = new SequenceTracker();
    tracker.accept(10);

    SequenceResult firstOk = tracker.accept(11);
    SequenceResult secondOk = tracker.accept(12);

    assertEquals(SequenceStatus.OK, firstOk.getStatus());
    assertEquals(11, firstOk.getExpectedSequenceNumber());
    assertEquals(1, firstOk.getDelta());
    assertEquals(0, firstOk.getLostPackets());
    assertTrue(firstOk.isStatusChanged());

    assertEquals(SequenceStatus.OK, secondOk.getStatus());
    assertFalse(secondOk.isStatusChanged());
  }

  @Test
  void shortForwardGapReportsLoss() {
    SequenceTracker tracker = new SequenceTracker();
    tracker.accept(20);

    SequenceResult result = tracker.accept(25);

    assertEquals(SequenceStatus.LOSS, result.getStatus());
    assertEquals(21, result.getExpectedSequenceNumber());
    assertEquals(5, result.getDelta());
    assertEquals(4, result.getLostPackets());
    assertTrue(result.isStatusChanged());
  }

  @Test
  void largeForwardGapReportsReset() {
    SequenceTracker tracker = new SequenceTracker();
    tracker.accept(10);

    SequenceResult result = tracker.accept(50);

    assertEquals(SequenceStatus.RESET, result.getStatus());
    assertEquals(40, result.getDelta());
    assertEquals(0, result.getLostPackets());
  }

  @Test
  void duplicateSequenceIsOutOfOrder() {
    SequenceTracker tracker = new SequenceTracker();
    tracker.accept(77);

    SequenceResult result = tracker.accept(77);

    assertEquals(SequenceStatus.OUT_OF_ORDER, result.getStatus());
    assertEquals(0, result.getDelta());
    assertEquals(0, result.getLostPackets());
  }

  @Test
  void sequenceWrapFrom255ToZeroIsOk() {
    SequenceTracker tracker = new SequenceTracker();
    tracker.accept(255);

    SequenceResult result = tracker.accept(0);

    assertEquals(0, result.getExpectedSequenceNumber());
    assertEquals(1, result.getDelta());
    assertEquals(SequenceStatus.OK, result.getStatus());
  }

  @Test
  void invalidSequenceValuesAreRejected() {
    SequenceTracker tracker = new SequenceTracker();

    assertThrows(IllegalArgumentException.class, () -> tracker.accept(-1));
    assertThrows(IllegalArgumentException.class, () -> tracker.accept(256));
  }
}
