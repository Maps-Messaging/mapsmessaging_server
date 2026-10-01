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

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * MTI cache rebuild time after a cache loss (IC26 KPI clarification Q15), relative to the assets
 * that had fresh MTI opinions immediately before the failure: time until the target fraction
 * (default 95%) of them have a fresh opinion again, and time until all of them do.
 */
final class CacheRebuildTracker {

  enum Trigger {
    /** MAPS restarted and lost its in-memory MTI cache; baseline persisted before shutdown. */
    RESTART,
    /** Controlled cache-loss injection cleared the MTI cache. */
    CACHE_LOSS
  }

  enum Target {
    TARGET_FRACTION,
    ALL
  }

  /** A rebuild that reached a target, or timed out. */
  record RebuildEvent(Trigger trigger, Target target, Duration duration, boolean reached, int baselineSize) {
  }

  private static final class Rebuild {
    private final Trigger trigger;
    private final Instant startedAt;
    private final Set<String> baseline;
    private boolean targetReached;
    private int restored;

    private Rebuild(Trigger trigger, Instant startedAt, Set<String> baseline) {
      this.trigger = trigger;
      this.startedAt = startedAt;
      this.baseline = baseline;
    }
  }

  private final double targetFraction;
  private final Duration timeout;
  private final Map<Trigger, Map<Target, DurationStats>> stats = new EnumMap<>(Trigger.class);
  private Rebuild current;

  CacheRebuildTracker(double targetFraction, Duration timeout) {
    this.targetFraction = targetFraction;
    this.timeout = timeout;
    for (Trigger trigger : Trigger.values()) {
      Map<Target, DurationStats> targets = new EnumMap<>(Target.class);
      for (Target target : Target.values()) {
        targets.put(target, new DurationStats());
      }
      stats.put(trigger, targets);
    }
  }

  /** @return false if there was nothing to rebuild (no fresh opinions before the failure). */
  synchronized boolean start(Trigger trigger, Instant at, Set<String> baseline) {
    if (baseline == null || baseline.isEmpty()) {
      return false;
    }
    current = new Rebuild(trigger, at, new TreeSet<>(baseline));
    return true;
  }

  synchronized List<RebuildEvent> update(Set<String> freshNow, Instant now) {
    if (current == null) {
      return List.of();
    }
    int restored = 0;
    for (String uid : current.baseline) {
      if (freshNow.contains(uid)) {
        restored++;
      }
    }
    current.restored = restored;
    List<RebuildEvent> events = new ArrayList<>();
    Duration elapsed = Duration.between(current.startedAt, now);
    int size = current.baseline.size();
    if (!current.targetReached && restored >= Math.ceil(targetFraction * size)) {
      current.targetReached = true;
      stats.get(current.trigger).get(Target.TARGET_FRACTION).record(elapsed);
      events.add(new RebuildEvent(current.trigger, Target.TARGET_FRACTION, elapsed, true, size));
    }
    if (restored >= size) {
      stats.get(current.trigger).get(Target.ALL).record(elapsed);
      events.add(new RebuildEvent(current.trigger, Target.ALL, elapsed, true, size));
      current = null;
    } else if (elapsed.compareTo(timeout) > 0) {
      if (!current.targetReached) {
        stats.get(current.trigger).get(Target.TARGET_FRACTION).recordNotReached();
        events.add(new RebuildEvent(current.trigger, Target.TARGET_FRACTION, elapsed, false, size));
      }
      stats.get(current.trigger).get(Target.ALL).recordNotReached();
      events.add(new RebuildEvent(current.trigger, Target.ALL, elapsed, false, size));
      current = null;
    }
    return events;
  }

  DurationStats stats(Trigger trigger, Target target) {
    return stats.get(trigger).get(target);
  }

  synchronized boolean isInProgress() {
    return current != null;
  }

  synchronized int getBaselineSize() {
    return current == null ? 0 : current.baseline.size();
  }

  synchronized int getRestoredCount() {
    return current == null ? 0 : current.restored;
  }

  double getTargetFraction() {
    return targetFraction;
  }
}
