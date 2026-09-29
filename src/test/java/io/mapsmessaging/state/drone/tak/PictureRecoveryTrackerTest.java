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
import io.mapsmessaging.state.drone.tak.PictureRecoveryTracker.FailureType;
import io.mapsmessaging.state.drone.tak.PictureRecoveryTracker.Stage;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PictureRecoveryTrackerTest {

  private static final String TWIN = "Aircraft 01";

  private final MutableClock clock = new MutableClock(Instant.parse("2026-09-28T12:00:00Z"));
  private final PictureRecoveryTracker tracker = new PictureRecoveryTracker(clock);

  @Test
  void feedLoss_withMtiStillCached_timesBothStagesFromTheDataReturning() {
    pastRestartWindow();
    tracker.onCotHandedToTak(TWIN, true);

    Instant dataBack = clock.instant();
    tracker.onTwinStatusChanged(TWIN, TwinLifecycleStatus.STALE, TwinLifecycleStatus.ACTIVE, dataBack);
    clock.advance(Duration.ofMillis(200));
    tracker.onCotHandedToTak(TWIN, true);

    assertRestored(FailureType.FEED_LOSS, Stage.PICTURE, 1, 0.2);
    assertRestored(FailureType.FEED_LOSS, Stage.STATUS, 1, 0.2);
    assertEquals(1, tracker.getOpenedCount(FailureType.FEED_LOSS));
    assertEquals(0, tracker.getOpenCount(FailureType.FEED_LOSS));
  }

  @Test
  void feedLoss_forAnAssetMtiNeverCovered_completesOnThePictureAlone() {
    pastRestartWindow();
    tracker.onCotHandedToTak(TWIN, false);

    tracker.onTwinStatusChanged(TWIN, TwinLifecycleStatus.DISCONNECTED, TwinLifecycleStatus.ACTIVE, clock.instant());
    clock.advance(Duration.ofMillis(50));
    tracker.onCotHandedToTak(TWIN, false);

    assertRestored(FailureType.FEED_LOSS, Stage.PICTURE, 1, 0.05);
    assertEquals(0, tracker.stats(FailureType.FEED_LOSS, Stage.STATUS).getCount());
    assertEquals(0, tracker.stats(FailureType.FEED_LOSS, Stage.STATUS).getNotRestoredCount());
    assertEquals(0, tracker.getOpenCount(FailureType.FEED_LOSS));
  }

  @Test
  void feedLoss_usesTheUpdateReceiptTime_notTheTimeTheObserverRan() {
    pastRestartWindow();
    Instant receivedAt = clock.instant();
    clock.advance(Duration.ofSeconds(2));

    tracker.onTwinStatusChanged(TWIN, TwinLifecycleStatus.STALE, TwinLifecycleStatus.ACTIVE, receivedAt);
    tracker.onCotHandedToTak(TWIN, false);

    assertRestored(FailureType.FEED_LOSS, Stage.PICTURE, 1, 2.0);
  }

  @Test
  void onlyStaleOrDisconnectedToActive_opensAFeedLossRecovery() {
    pastRestartWindow();

    tracker.onTwinStatusChanged(TWIN, TwinLifecycleStatus.ACTIVE, TwinLifecycleStatus.STALE, clock.instant());
    tracker.onTwinStatusChanged(TWIN, TwinLifecycleStatus.DISCONNECTED, TwinLifecycleStatus.STALE, clock.instant());

    assertEquals(0, tracker.getOpenedCount(FailureType.FEED_LOSS));
  }

  @Test
  void restart_statusStageIsTimedFromTheMtiStatusArriving() {
    clock.advance(Duration.ofSeconds(1));
    tracker.onTwinAdded(TWIN, clock.instant());
    clock.advance(Duration.ofMillis(100));
    tracker.onCotHandedToTak(TWIN, false);

    clock.advance(Duration.ofSeconds(30));
    tracker.onStatusAccepted(TWIN, clock.instant());
    clock.advance(Duration.ofMillis(500));
    tracker.onCotHandedToTak(TWIN, true);

    assertRestored(FailureType.RESTART, Stage.PICTURE, 1, 0.1);
    assertRestored(FailureType.RESTART, Stage.STATUS, 1, 0.5);
    assertEquals(0, tracker.getOpenCount(FailureType.RESTART));
  }

  @Test
  void restart_withoutAnMtiStatusBeforeTheTimeout_countsTheStatusStageAsNotRestored() {
    tracker.onTwinAdded(TWIN, clock.instant());
    tracker.onCotHandedToTak(TWIN, false);

    clock.advance(PictureRecoveryTracker.EVENT_TIMEOUT);
    tracker.onCotHandedToTak("another twin", false);

    assertEquals(1, tracker.stats(FailureType.RESTART, Stage.PICTURE).getCount());
    assertEquals(0, tracker.stats(FailureType.RESTART, Stage.PICTURE).getNotRestoredCount());
    assertEquals(1, tracker.stats(FailureType.RESTART, Stage.STATUS).getNotRestoredCount());
    assertEquals(0, tracker.getOpenCount(FailureType.RESTART));
  }

  @Test
  void newTwinAfterTheRestartWindow_isNotARecovery() {
    pastRestartWindow();

    tracker.onTwinAdded(TWIN, clock.instant());
    tracker.onCotHandedToTak(TWIN, false);

    for (FailureType failureType : FailureType.values()) {
      assertEquals(0, tracker.getOpenedCount(failureType), failureType.name());
    }
  }

  @Test
  void purgedTwinReturning_isTimedAsAFeedLoss() {
    pastRestartWindow();
    tracker.onCotHandedToTak(TWIN, true);
    tracker.onTwinRemoved(TWIN);

    clock.advance(Duration.ofMinutes(3));
    tracker.onTwinAdded(TWIN, clock.instant());
    clock.advance(Duration.ofMillis(300));
    tracker.onCotHandedToTak(TWIN, true);

    assertRestored(FailureType.FEED_LOSS, Stage.PICTURE, 1, 0.3);
    assertRestored(FailureType.FEED_LOSS, Stage.STATUS, 1, 0.3);
  }

  @Test
  void mtiStatusExpiring_isTimedFromTheStatusArrivingAgain() {
    pastRestartWindow();
    tracker.onCotHandedToTak(TWIN, true);
    clock.advance(Duration.ofSeconds(5));
    tracker.onCotHandedToTak(TWIN, false);
    assertEquals(1, tracker.getOpenCount(FailureType.MTI_STATUS_LOSS));

    clock.advance(Duration.ofSeconds(40));
    tracker.onStatusAccepted(TWIN, clock.instant());
    clock.advance(Duration.ofMillis(300));
    tracker.onCotHandedToTak(TWIN, true);

    assertRestored(FailureType.MTI_STATUS_LOSS, Stage.STATUS, 1, 0.3);
    assertEquals(0, tracker.stats(FailureType.MTI_STATUS_LOSS, Stage.PICTURE).getCount(),
        "the asset's own data never went away, so there is no picture stage");
    assertEquals(0, tracker.getOpenCount(FailureType.MTI_STATUS_LOSS));
  }

  @Test
  void explicitMtiDelete_isNotTreatedAsAStatusLoss() {
    pastRestartWindow();
    tracker.onCotHandedToTak(TWIN, true);

    tracker.onStatusCleared(TWIN);
    tracker.onCotHandedToTak(TWIN, false);

    assertEquals(0, tracker.getOpenedCount(FailureType.MTI_STATUS_LOSS));
  }

  @Test
  void explicitMtiDeleteDuringARestartRecovery_stopsWaitingForAStatus() {
    tracker.onTwinAdded(TWIN, clock.instant());
    tracker.onCotHandedToTak(TWIN, false);

    tracker.onStatusCleared(TWIN);
    clock.advance(PictureRecoveryTracker.EVENT_TIMEOUT);
    tracker.onCotHandedToTak("another twin", false);

    assertEquals(0, tracker.getOpenCount(FailureType.RESTART));
    assertEquals(0, tracker.stats(FailureType.RESTART, Stage.STATUS).getNotRestoredCount());
  }

  @Test
  void secondOutageBeforeThePictureWasRestored_keepsTheEarlierStart() {
    pastRestartWindow();
    Instant firstReturn = clock.instant();
    tracker.onTwinStatusChanged(TWIN, TwinLifecycleStatus.STALE, TwinLifecycleStatus.ACTIVE, firstReturn);
    clock.advance(Duration.ofSeconds(1));
    tracker.onTwinStatusChanged(TWIN, TwinLifecycleStatus.DISCONNECTED, TwinLifecycleStatus.ACTIVE, clock.instant());
    tracker.onCotHandedToTak(TWIN, false);

    assertRestored(FailureType.FEED_LOSS, Stage.PICTURE, 1, 1.0);
    assertEquals(1, tracker.getOpenedCount(FailureType.FEED_LOSS));
  }

  @Test
  void twinRemovedBeforeRecovering_countsAsNotRestored() {
    tracker.onTwinAdded(TWIN, clock.instant());

    tracker.onTwinRemoved(TWIN);

    assertEquals(1, tracker.stats(FailureType.RESTART, Stage.PICTURE).getNotRestoredCount());
    assertEquals(1, tracker.stats(FailureType.RESTART, Stage.STATUS).getNotRestoredCount());
  }

  @Test
  void histogramBuckets_areCumulative() {
    RecoveryDurationStats stats = new RecoveryDurationStats();
    stats.record(80);
    stats.record(900);
    stats.record(45_000);
    stats.record(400_000);

    assertEquals(1, stats.getCumulativeCount(0), "<= 0.1s");
    assertEquals(2, stats.getCumulativeCount(3), "<= 1s");
    assertEquals(3, stats.getCumulativeCount(8), "<= 60s");
    assertEquals(3, stats.getCumulativeCount(RecoveryDurationStats.BUCKETS_SECONDS.length - 1),
        "400s is beyond the last bound and only appears in the count");
    assertEquals(4, stats.getCount());
    assertEquals(445.98, stats.getSumSeconds(), 0.0001);
    assertEquals(400.0, stats.getLastSeconds(), 0.0001);
  }

  private void pastRestartWindow() {
    clock.advance(PictureRecoveryTracker.RESTART_WINDOW.plusSeconds(1));
  }

  private void assertRestored(FailureType failureType, Stage stage, long count, double lastSeconds) {
    RecoveryDurationStats stats = tracker.stats(failureType, stage);
    assertEquals(count, stats.getCount(), failureType + "/" + stage + " count");
    assertEquals(lastSeconds, stats.getLastSeconds(), 0.0001, failureType + "/" + stage + " last delay");
  }

  private static final class MutableClock extends Clock {
    private Instant now;

    private MutableClock(Instant start) {
      this.now = start;
    }

    private void advance(Duration duration) {
      now = now.plus(duration);
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return now;
    }
  }
}
