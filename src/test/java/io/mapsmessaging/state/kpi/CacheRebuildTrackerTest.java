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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.mapsmessaging.state.kpi.CacheRebuildTracker.Target.ALL;
import static io.mapsmessaging.state.kpi.CacheRebuildTracker.Target.TARGET_FRACTION;
import static io.mapsmessaging.state.kpi.CacheRebuildTracker.Trigger.CACHE_LOSS;
import static io.mapsmessaging.state.kpi.CacheRebuildTracker.Trigger.RESTART;
import static org.junit.jupiter.api.Assertions.*;

class CacheRebuildTrackerTest {

  private static final Instant T0 = Instant.parse("2026-10-01T10:00:00Z");

  private static Set<String> assets(int count) {
    Set<String> assets = new HashSet<>();
    for (int index = 0; index < count; index++) {
      assets.add("asset-" + index);
    }
    return assets;
  }

  @Test
  void targetFraction_isMeasuredAgainstPreFailureFreshAssets() {
    CacheRebuildTracker tracker = new CacheRebuildTracker(0.95, Duration.ofMinutes(30));
    tracker.start(RESTART, T0, assets(20));

    assertEquals(List.of(), tracker.update(assets(18), T0.plusSeconds(5)));
    List<CacheRebuildTracker.RebuildEvent> reached = tracker.update(assets(19), T0.plusSeconds(8));
    assertEquals(1, reached.size());
    assertEquals(TARGET_FRACTION, reached.get(0).target());
    assertEquals(Duration.ofSeconds(8), reached.get(0).duration());
    assertTrue(tracker.isInProgress());

    List<CacheRebuildTracker.RebuildEvent> complete = tracker.update(assets(20), T0.plusSeconds(12));
    assertEquals(ALL, complete.get(0).target());
    assertFalse(tracker.isInProgress());
    assertEquals(12.0, tracker.stats(RESTART, ALL).getLastSeconds());
  }

  @Test
  void assetsNewSinceTheFailure_doNotCount() {
    CacheRebuildTracker tracker = new CacheRebuildTracker(0.95, Duration.ofMinutes(30));
    tracker.start(CACHE_LOSS, T0, Set.of("a", "b"));

    assertEquals(List.of(), tracker.update(Set.of("a", "new-1", "new-2"), T0.plusSeconds(1)));
    assertEquals(1, tracker.getRestoredCount());
  }

  @Test
  void allRestoredAtOnce_recordsBothTargets() {
    CacheRebuildTracker tracker = new CacheRebuildTracker(0.95, Duration.ofMinutes(30));
    tracker.start(CACHE_LOSS, T0, Set.of("a"));

    assertEquals(2, tracker.update(Set.of("a"), T0.plusSeconds(3)).size());
    assertEquals(1, tracker.stats(CACHE_LOSS, TARGET_FRACTION).getCount());
    assertEquals(1, tracker.stats(CACHE_LOSS, ALL).getCount());
  }

  @Test
  void timeout_countsAsNotReached() {
    CacheRebuildTracker tracker = new CacheRebuildTracker(0.95, Duration.ofMinutes(1));
    tracker.start(CACHE_LOSS, T0, assets(10));

    List<CacheRebuildTracker.RebuildEvent> events = tracker.update(assets(5), T0.plusSeconds(61));

    assertEquals(2, events.size());
    assertFalse(events.get(0).reached());
    assertEquals(1, tracker.stats(CACHE_LOSS, ALL).getNotReachedCount());
    assertFalse(tracker.isInProgress());
  }

  @Test
  void nothingFreshBeforeTheFailure_startsNoRebuild() {
    CacheRebuildTracker tracker = new CacheRebuildTracker(0.95, Duration.ofMinutes(1));

    assertFalse(tracker.start(RESTART, T0, Set.of()));
    assertFalse(tracker.start(RESTART, T0, null));
  }
}
