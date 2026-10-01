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

package io.mapsmessaging.state.drone.tak;

import io.mapsmessaging.state.drone.core.TwinLifecycleStatus;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Per-asset picture restoration delay: the time from valid upstream data being available at MAPS
 * again until MAPS hands the TAK output a fresh representation of that asset. Measured in two
 * stages per recovery event:
 *
 * <ul>
 *   <li><b>picture</b> - data back at MAPS -> first CoT for the asset handed to TAK;</li>
 *   <li><b>status</b> - data back at MAPS, or the MTI status arriving if that is later -> first
 *       CoT carrying an MTI status. Only tracked where an MTI status is expected.</li>
 * </ul>
 *
 * <p>The asset does not have to be healthy - any MTI status, including "unknown", "hold" or
 * "mitigate", counts as the appropriate status having been restored. Events that never reach a
 * stage within {@link #EVENT_TIMEOUT} are counted as not restored for that stage. Classification
 * failure recovery is not tracked yet: whether falling back to the original CoT type counts as a
 * failure is still an open stakeholder question.
 */
public class PictureRecoveryTracker implements MtiStatusRegistry.StatusListener {

  /** Twins first seen within this window after startup are timed as restart recoveries. */
  static final Duration RESTART_WINDOW = Duration.ofMinutes(5);
  /** An open recovery event that hasn't completed after this long is counted as not restored. */
  static final Duration EVENT_TIMEOUT = Duration.ofMinutes(10);
  /** How long a purged twin is remembered, so its return is timed as a feed loss recovery. */
  static final Duration REMOVED_MEMORY = Duration.ofHours(1);

  public enum FailureType {
    FEED_LOSS,
    RESTART,
    MTI_STATUS_LOSS
  }

  public enum Stage {
    PICTURE,
    STATUS
  }

  private static final class RecoveryEvent {
    private final FailureType failureType;
    private final Instant openedAt;
    private final Instant dataRestoredAt; // null for MTI_STATUS_LOSS: asset data never went away
    private boolean statusExpected;
    private Instant mtiReceivedAt;
    private boolean pictureRestored;
    private boolean statusRestored;

    private RecoveryEvent(FailureType failureType, Instant openedAt, Instant dataRestoredAt, boolean statusExpected) {
      this.failureType = failureType;
      this.openedAt = openedAt;
      this.dataRestoredAt = dataRestoredAt;
      this.statusExpected = statusExpected;
      this.pictureRestored = dataRestoredAt == null;
    }

    private boolean isComplete() {
      return pictureRestored && (!statusExpected || statusRestored);
    }
  }

  private final Clock clock;
  private final Instant startedAt;
  private final Map<FailureType, Map<Stage, RecoveryDurationStats>> stats = new EnumMap<>(FailureType.class);
  private final Map<FailureType, Long> openedCounts = new EnumMap<>(FailureType.class);
  private final Map<String, RecoveryEvent> openEvents = new HashMap<>();
  private final Map<String, Instant> removedTwins = new HashMap<>();
  private final Set<String> twinsWithMtiStatus = new HashSet<>();

  public PictureRecoveryTracker(Clock clock) {
    this.clock = clock;
    this.startedAt = clock.instant();
    for (FailureType failureType : FailureType.values()) {
      Map<Stage, RecoveryDurationStats> byStage = new EnumMap<>(Stage.class);
      for (Stage stage : Stage.values()) {
        byStage.put(stage, new RecoveryDurationStats());
      }
      stats.put(failureType, byStage);
      openedCounts.put(failureType, 0L);
    }
  }

  /** A twin was registered: either a purged twin returning, a twin re-appearing after a restart, or new. */
  public synchronized void onTwinAdded(String twinId, Instant receivedAt) {
    Instant now = resolve(receivedAt);
    expireStale(now);
    Instant removedAt = removedTwins.remove(twinId);
    if (removedAt != null) {
      open(twinId, FailureType.FEED_LOSS, now, twinsWithMtiStatus.contains(twinId));
    } else if (Duration.between(startedAt, now).compareTo(RESTART_WINDOW) < 0) {
      // The MTI cache is in-memory, so after a restart every asset's status has to be rebuilt.
      open(twinId, FailureType.RESTART, now, true);
    }
  }

  /** A stale or disconnected twin reported again. */
  public synchronized void onTwinStatusChanged(
      String twinId, TwinLifecycleStatus previous, TwinLifecycleStatus current, Instant receivedAt) {
    if (current != TwinLifecycleStatus.ACTIVE
        || (previous != TwinLifecycleStatus.STALE && previous != TwinLifecycleStatus.DISCONNECTED)) {
      return;
    }
    Instant now = resolve(receivedAt);
    expireStale(now);
    RecoveryEvent existing = openEvents.get(twinId);
    if (existing != null && !existing.pictureRestored) {
      return; // still waiting on an earlier outage - keep its (earlier) start
    }
    open(twinId, FailureType.FEED_LOSS, now, twinsWithMtiStatus.contains(twinId));
  }

  public synchronized void onTwinRemoved(String twinId) {
    Instant now = clock.instant();
    RecoveryEvent event = openEvents.remove(twinId);
    if (event != null) {
      recordNotRestored(event);
    }
    removedTwins.put(twinId, now);
  }

  @Override
  public synchronized void onStatusAccepted(String twinId, Instant receivedAt) {
    RecoveryEvent event = openEvents.get(twinId);
    if (event != null && event.statusExpected && !event.statusRestored) {
      event.mtiReceivedAt = resolve(receivedAt);
    }
  }

  /** MTI deliberately withdrew the status - not a failure, so stop expecting one. */
  @Override
  public synchronized void onStatusCleared(String twinId) {
    twinsWithMtiStatus.remove(twinId);
    RecoveryEvent event = openEvents.get(twinId);
    if (event == null) {
      return;
    }
    if (event.failureType == FailureType.MTI_STATUS_LOSS) {
      openEvents.remove(twinId);
      return;
    }
    event.statusExpected = false;
    if (event.isComplete()) {
      openEvents.remove(twinId);
    }
  }

  /**
   * A CoT for the twin was handed to the TAK output.
   *
   * @param hasMtiStatus whether the twin had a current MTI status when the CoT was composed
   */
  public synchronized void onCotHandedToTak(String twinId, boolean hasMtiStatus) {
    Instant now = clock.instant();
    expireStale(now);
    RecoveryEvent event = openEvents.get(twinId);
    if (event != null) {
      complete(twinId, event, hasMtiStatus, now);
    }

    if (hasMtiStatus) {
      twinsWithMtiStatus.add(twinId);
    } else if (twinsWithMtiStatus.contains(twinId) && !openEvents.containsKey(twinId)) {
      // Had a status, now composed without one, and MTI never explicitly cleared it: expired.
      openEvents.put(twinId, new RecoveryEvent(FailureType.MTI_STATUS_LOSS, now, null, true));
      openedCounts.merge(FailureType.MTI_STATUS_LOSS, 1L, Long::sum);
    }
  }

  private void complete(String twinId, RecoveryEvent event, boolean hasMtiStatus, Instant now) {
    if (!event.pictureRestored) {
      event.pictureRestored = true;
      stats(event.failureType, Stage.PICTURE).record(Duration.between(event.dataRestoredAt, now).toMillis());
    }
    if (event.statusExpected && !event.statusRestored && hasMtiStatus) {
      Instant statusStart = latest(event.dataRestoredAt, event.mtiReceivedAt);
      if (statusStart != null) {
        event.statusRestored = true;
        stats(event.failureType, Stage.STATUS).record(Duration.between(statusStart, now).toMillis());
      }
    }
    if (event.isComplete()) {
      openEvents.remove(twinId);
    }
  }

  private void open(String twinId, FailureType failureType, Instant dataRestoredAt, boolean statusExpected) {
    RecoveryEvent superseded = openEvents.put(
        twinId, new RecoveryEvent(failureType, dataRestoredAt, dataRestoredAt, statusExpected));
    if (superseded != null) {
      recordNotRestored(superseded);
    }
    openedCounts.merge(failureType, 1L, Long::sum);
  }

  private void expireStale(Instant now) {
    Iterator<RecoveryEvent> events = openEvents.values().iterator();
    while (events.hasNext()) {
      RecoveryEvent event = events.next();
      if (Duration.between(event.openedAt, now).compareTo(EVENT_TIMEOUT) >= 0) {
        recordNotRestored(event);
        events.remove();
      }
    }
    Iterator<Map.Entry<String, Instant>> removed = removedTwins.entrySet().iterator();
    while (removed.hasNext()) {
      Map.Entry<String, Instant> entry = removed.next();
      if (Duration.between(entry.getValue(), now).compareTo(REMOVED_MEMORY) >= 0) {
        twinsWithMtiStatus.remove(entry.getKey());
        removed.remove();
      }
    }
  }

  private void recordNotRestored(RecoveryEvent event) {
    if (!event.pictureRestored) {
      stats(event.failureType, Stage.PICTURE).recordNotRestored();
    }
    if (event.statusExpected && !event.statusRestored) {
      stats(event.failureType, Stage.STATUS).recordNotRestored();
    }
  }

  private Instant resolve(Instant receivedAt) {
    return receivedAt != null ? receivedAt : clock.instant();
  }

  private static Instant latest(Instant first, Instant second) {
    if (first == null) {
      return second;
    }
    if (second == null) {
      return first;
    }
    return first.isAfter(second) ? first : second;
  }

  RecoveryDurationStats stats(FailureType failureType, Stage stage) {
    return stats.get(failureType).get(stage);
  }

  synchronized long getOpenedCount(FailureType failureType) {
    return openedCounts.get(failureType);
  }

  synchronized long getOpenCount(FailureType failureType) {
    long open = 0;
    for (RecoveryEvent event : openEvents.values()) {
      if (event.failureType == failureType) {
        open++;
      }
    }
    return open;
  }
}
