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

package io.mapsmessaging.state.kpi;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class BandTrackerTest {

  private static final Instant T0 = Instant.parse("2026-10-01T10:00:00Z");

  private static Instant at(long seconds) {
    return T0.plusSeconds(seconds);
  }

  private BandTracker greenTracker() {
    BandTracker tracker = new BandTracker(Duration.ofSeconds(15));
    tracker.update(Band.GREEN, T0);
    return tracker;
  }

  @Test
  void degradation_isShownImmediately() {
    BandTracker tracker = greenTracker();

    BandTracker.Transition transition = tracker.update(Band.RED, at(1));

    assertNotNull(transition);
    assertEquals(Band.RED, tracker.getDisplayed());
    assertEquals(at(1), transition.effectiveAt());
  }

  @Test
  void recovery_waitsForHold_andIsDatedToStartOfStablePeriod() {
    BandTracker tracker = greenTracker();
    tracker.update(Band.RED, at(1));

    for (long second = 10; second < 25; second++) {
      assertNull(tracker.update(Band.GREEN, at(second)), "still holding at " + second);
    }
    BandTracker.Transition transition = tracker.update(Band.GREEN, at(25));

    assertEquals(Band.GREEN, tracker.getDisplayed());
    assertEquals(at(10), transition.effectiveAt());
    assertEquals(at(10), tracker.getGreenSince());
    assertEquals(at(10), tracker.getNotRedSince());
  }

  @Test
  void flickerBackIntoRed_restartsTheHold() {
    BandTracker tracker = greenTracker();
    tracker.update(Band.RED, at(1));
    tracker.update(Band.GREEN, at(2));
    tracker.update(Band.RED, at(10));

    assertNull(tracker.update(Band.GREEN, at(17)));
    assertEquals(Band.RED, tracker.getDisplayed());
    assertNull(tracker.getNotRedSince());
  }

  @Test
  void stepwiseRecovery_showsAmberBeforeGreen() {
    BandTracker tracker = greenTracker();
    tracker.update(Band.RED, at(1));
    tracker.update(Band.AMBER, at(10));
    tracker.update(Band.GREEN, at(15));

    BandTracker.Transition toAmber = tracker.update(Band.GREEN, at(25));
    assertEquals(Band.AMBER, toAmber.to());
    assertEquals(at(10), toAmber.effectiveAt());

    BandTracker.Transition toGreen = tracker.update(Band.GREEN, at(30));
    assertEquals(Band.GREEN, toGreen.to());
    assertEquals(at(15), toGreen.effectiveAt());
    assertEquals(at(10), tracker.getNotRedSince());
  }

  @Test
  void noData_isShownAtOnce_andLeavingItNeedsNoHold() {
    BandTracker tracker = greenTracker();
    tracker.update(Band.NO_DATA, at(1));
    assertEquals(Band.NO_DATA, tracker.getDisplayed());

    tracker.update(Band.AMBER, at(2));
    assertEquals(Band.AMBER, tracker.getDisplayed());
  }
}
