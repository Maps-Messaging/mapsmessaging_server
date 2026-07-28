/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at:
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

package io.mapsmessaging.state.mavlink.bootstrap;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MavlinkBootstrapRequestTrackerTest {

  private static final Instant START = Instant.parse("2026-07-28T00:00:00Z");
  private static final Duration RETRY_INTERVAL = Duration.ofSeconds(2);
  private static final Duration TIMEOUT = Duration.ofSeconds(15);

  @Test
  void new_tracker_can_request_immediately() {
    MavlinkBootstrapRequestTracker tracker = tracker();

    assertTrue(tracker.canRetry(START, RETRY_INTERVAL, 3));
    assertFalse(tracker.hasTimedOut(START, TIMEOUT));
  }

  @Test
  void mark_requested_preserves_first_time_and_advances_last_time_and_count() {
    MavlinkBootstrapRequestTracker tracker = tracker();

    tracker.markRequested(START);
    tracker.markRequested(START.plusSeconds(2));

    assertEquals(2, tracker.getRequestCount());
    assertEquals(START, tracker.getFirstRequestedAt());
    assertEquals(START.plusSeconds(2), tracker.getLastRequestedAt());
  }

  @Test
  void retry_requires_full_interval_and_rejects_out_of_order_time() {
    MavlinkBootstrapRequestTracker tracker = tracker();
    tracker.markRequested(START.plusSeconds(10));

    assertFalse(tracker.canRetry(START.plusSeconds(9), RETRY_INTERVAL, 3));
    assertFalse(tracker.canRetry(START.plusSeconds(11), RETRY_INTERVAL, 3));
    assertTrue(tracker.canRetry(START.plusSeconds(12), RETRY_INTERVAL, 3));
  }

  @Test
  void maximum_retry_count_is_inclusive_and_zero_disables_requests() {
    MavlinkBootstrapRequestTracker tracker = tracker();

    assertFalse(tracker.canRetry(START, RETRY_INTERVAL, 0));

    tracker.markRequested(START);
    tracker.markRequested(START.plusSeconds(2));
    tracker.markRequested(START.plusSeconds(4));

    assertFalse(tracker.canRetry(START.plusSeconds(6), RETRY_INTERVAL, 3));
  }

  @Test
  void timeout_is_measured_from_first_request_and_is_inclusive() {
    MavlinkBootstrapRequestTracker tracker = tracker();
    tracker.markRequested(START);
    tracker.markRequested(START.plusSeconds(4));

    assertFalse(tracker.hasTimedOut(START.minusSeconds(1), TIMEOUT));
    assertFalse(tracker.hasTimedOut(START.plusSeconds(14), TIMEOUT));
    assertTrue(tracker.hasTimedOut(START.plusSeconds(15), TIMEOUT));
  }

  @Test
  void timed_out_tracker_never_retries_and_remains_timed_out() {
    MavlinkBootstrapRequestTracker tracker = tracker();
    tracker.markRequested(START);
    tracker.setTimedOut(true);

    assertTrue(tracker.hasTimedOut(START.plusSeconds(1), TIMEOUT));
    assertFalse(tracker.canRetry(START.plusSeconds(100), RETRY_INTERVAL, 100));
  }

  @Test
  void unrequested_tracker_has_no_request_timestamps() {
    MavlinkBootstrapRequestTracker tracker = tracker();

    assertNull(tracker.getFirstRequestedAt());
    assertNull(tracker.getLastRequestedAt());
    assertEquals(0, tracker.getRequestCount());
  }

  private MavlinkBootstrapRequestTracker tracker() {
    return new MavlinkBootstrapRequestTracker(DroneTwinMissingState.MISSING_BATTERY_STATE);
  }
}
