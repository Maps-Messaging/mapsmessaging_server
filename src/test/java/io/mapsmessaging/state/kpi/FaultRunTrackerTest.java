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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FaultRunTrackerTest {

  private static final Instant START = Instant.parse("2026-10-01T10:00:00Z");
  private final FaultRunTracker tracker = new FaultRunTracker(Duration.ofSeconds(10));
  private final Map<KpiId, Band> green = Map.of(KpiId.READINESS, Band.GREEN, KpiId.COVERAGE, Band.GREEN);

  @Test
  void evaluation_before_injection_skips_detection_degradation_and_timeout() {
    FaultRunTracker.FaultRun run = tracker.inject("future", "CUSTOM", START, green, Band.GREEN);
    assertTrue(tracker.onEvaluation(Map.of(KpiId.READINESS, Band.RED), Band.RED,
        null, START, START, START.minusMillis(1)).isEmpty());
    assertNull(run.detectedAt);
    assertNull(run.degradedAt);
    assertFalse(run.wentRed);
    assertEquals(1, tracker.getOpenCount());
  }

  @Test
  void timeout_is_strict_and_closes_runs_in_insertion_order_without_recording_no_effect_stages() {
    FaultRunTracker.FaultRun first = tracker.inject("first", "Z_CUSTOM", START, green, Band.GREEN);
    FaultRunTracker.FaultRun second = tracker.inject("second", "A_CUSTOM", START, green, Band.GREEN);
    assertTrue(tracker.onEvaluation(green, Band.NO_DATA, null, null, null, START.plusSeconds(10)).isEmpty());
    assertEquals(List.of(first, second), tracker.onEvaluation(green, Band.NO_DATA,
        null, null, null, START.plusSeconds(10).plusMillis(1)));
    assertEquals("NO_EFFECT", first.outcome);
    assertEquals(2, tracker.statsFor(FaultRunTracker.ALL).getNoEffect());
    assertEquals(0, tracker.statsFor(FaultRunTracker.ALL).stage(FaultRunTracker.Stage.DETECTION).getCount());
    assertEquals(List.of("ALL", "A_CUSTOM", "Z_CUSTOM"), List.copyOf(tracker.getStats().keySet()));
  }

  @Test
  void restoration_takes_precedence_over_timeout_and_clamps_dates_to_degradation() {
    FaultRunTracker.FaultRun run = tracker.inject("restore", "CUSTOM", START, green, Band.GREEN);
    assertTrue(tracker.onEvaluation(Map.of(KpiId.READINESS, Band.RED), Band.RED,
        START.minusSeconds(5), null, null, START.plusSeconds(2)).isEmpty());
    assertEquals(START.toEpochMilli(), run.degradedAt);
    assertEquals(List.of(run), tracker.onEvaluation(green, Band.GREEN, null,
        START.minusSeconds(1), START.minusSeconds(1), START.plusSeconds(11)));
    assertEquals("RESTORED", run.outcome);
    assertEquals(START.toEpochMilli(), run.usableRestoredAt);
    assertEquals(START.toEpochMilli(), run.fullyRestoredAt);
    assertEquals(0, tracker.statsFor("CUSTOM").getNotRestored());
    assertEquals(2.0, tracker.statsFor("CUSTOM").stage(FaultRunTracker.Stage.DETECTION).getLastSeconds());
    assertEquals(0.0, tracker.statsFor("CUSTOM").stage(FaultRunTracker.Stage.FULLY_RESTORED).getLastSeconds());
  }

  @Test
  void usable_restoration_resets_when_lost_and_amber_without_red_does_not_record_it() {
    FaultRunTracker.FaultRun red = tracker.inject("red", "CUSTOM", START, green, Band.GREEN);
    tracker.onEvaluation(green, Band.RED, null, START, null, START.plusSeconds(1));
    assertEquals(START.plusSeconds(1).toEpochMilli(), red.usableRestoredAt);
    tracker.onEvaluation(green, Band.AMBER, null, null, null, START.plusSeconds(2));
    assertNull(red.usableRestoredAt);
    tracker.onEvaluation(green, Band.AMBER, null, START.plusSeconds(3), null, START.plusSeconds(3));
    assertEquals(START.plusSeconds(3).toEpochMilli(), red.usableRestoredAt);
    FaultRunTracker.FaultRun amber = tracker.inject("amber", "CUSTOM", START.plusSeconds(3), green, Band.GREEN);
    tracker.onEvaluation(green, Band.AMBER, null, START.plusSeconds(3), null, START.plusSeconds(4));
    assertNull(amber.usableRestoredAt);
    assertFalse(amber.wentRed);
  }

  @Test
  void first_worsened_critical_kpi_in_map_order_detects_once_and_machine_detection_is_preserved() {
    FaultRunTracker.FaultRun run = tracker.inject("kpi", "CUSTOM", START, green, Band.GREEN);
    Map<KpiId, Band> raw = new LinkedHashMap<>();
    raw.put(KpiId.UNKNOWN, Band.RED);
    raw.put(KpiId.COVERAGE, Band.RED);
    raw.put(KpiId.READINESS, Band.RED);
    tracker.onEvaluation(raw, Band.GREEN, null, null, null, START);
    assertEquals("kpi:COVERAGE", run.detectedBy);
    tracker.onMachineFlag("later", START.plusSeconds(1));
    assertEquals("kpi:COVERAGE", run.detectedBy);
    FaultRunTracker.FaultRun machine = tracker.inject("machine", "CUSTOM", START, green, Band.GREEN);
    tracker.onMachineFlag("feed", START.plusSeconds(2));
    tracker.onEvaluation(raw, Band.GREEN, null, null, null, START.plusSeconds(3));
    assertEquals("feed", machine.detectedBy);
    assertEquals(START.plusSeconds(2).toEpochMilli(), machine.detectedAt);
  }
}
