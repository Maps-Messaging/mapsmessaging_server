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

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.state.drone.core.TwinType;
import io.mapsmessaging.state.drone.tak.ClassificationRegistry;
import io.mapsmessaging.state.drone.tak.MtiStatusSnapshot;
import io.mapsmessaging.state.metrics.FeedActivityRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class KpiEvaluatorTest {

  private static final Instant T0 = Instant.parse("2026-10-01T10:00:00Z");
  private static final String FEED = "mavlink:fleet";

  @TempDir
  Path dataDirectory;

  /** A fleet of assets whose reports, MTI status and feed activity the test controls. */
  private final class Fleet {
    final MutableClock clock = new MutableClock(T0);
    final Map<String, Instant> lastSeen = new TreeMap<>();
    final Map<String, MtiStatusSnapshot> mti = new HashMap<>();
    final Map<String, FeedActivityRegistry.FeedState> feeds = new HashMap<>();
    /** Base CoT type per asset; assets not listed are friendly drones. */
    final Map<String, String> types = new HashMap<>();
    boolean cacheCleared;
    KpiEvaluator evaluator;

    Fleet(int size, Map<String, Object> extraConfig) {
      for (int index = 0; index < size; index++) {
        lastSeen.put("uas-" + index, T0);
      }
      Map<String, Object> config = new HashMap<>(Map.of("dataDirectory", dataDirectory.toString()));
      config.putAll(extraConfig);
      evaluator = newEvaluator(KpiConfig.from(new ConfigurationProperties(config)));
    }

    KpiEvaluator newEvaluator(KpiConfig config) {
      KpiEvaluator.Sources sources = new KpiEvaluator.Sources(
          () -> {
            List<KpiEvaluator.TwinInfo> twins = new ArrayList<>();
            lastSeen.forEach((uid, at) -> twins.add(new KpiEvaluator.TwinInfo(uid, TwinType.DRONE, at)));
            return twins;
          },
          mti::get,
          uid -> new ClassificationRegistry.Classification(
              ClassificationRegistry.Outcome.RESOLVED, types.getOrDefault(uid, "a-f-A-M-F-Q")),
          () -> Map.copyOf(feeds),
          () -> {
            cacheCleared = true;
            mti.clear();
            return true;
          });
      return new KpiEvaluator(config, sources, clock, new KpiEventLog(dataDirectory), new KpiStateStore(dataDirectory));
    }

    /** Every asset reports and MTI says go, as of now. */
    void healthy() {
      Instant now = clock.instant();
      lastSeen.replaceAll((uid, at) -> now);
      lastSeen.keySet().forEach(uid -> mti.put(uid, new MtiStatusSnapshot("go", now, now.plusSeconds(600))));
      feeds.put(FEED, new FeedActivityRegistry.FeedState(now.toEpochMilli(), null));
    }

    /** Advances second by second, re-evaluating each time. */
    void run(long seconds, Runnable eachSecond) {
      for (long second = 0; second < seconds; second++) {
        clock.advanceSeconds(1);
        eachSecond.run();
        evaluator.evaluate();
      }
    }
  }

  private List<String> logLines() throws IOException {
    try (Stream<Path> files = Files.list(dataDirectory)) {
      List<String> lines = new ArrayList<>();
      for (Path file : files.filter(path -> path.getFileName().toString().endsWith(".jsonl")).toList()) {
        lines.addAll(Files.readAllLines(file, StandardCharsets.UTF_8));
      }
      return lines;
    }
  }

  @Test
  void healthyFleet_isGreenAndFullyRestored() {
    Fleet fleet = new Fleet(20, Map.of());
    fleet.evaluator.start();
    fleet.run(2, fleet::healthy);

    assertEquals(Band.GREEN, fleet.evaluator.getOverall());
    assertTrue(fleet.evaluator.isUsable());
    assertTrue(fleet.evaluator.isFullyRestored());
    assertEquals(1.0, fleet.evaluator.getLatest().value(KpiId.READINESS).value());
  }

  @Test
  void feedLossRun_timesDetectionDegradationAndRestoration() throws IOException {
    Fleet fleet = new Fleet(20, Map.of());
    fleet.evaluator.start();
    fleet.run(5, fleet::healthy);

    Instant injectedAt = fleet.clock.instant();
    fleet.evaluator.handleControl("{\"event\":\"inject\",\"run_id\":\"feed-loss-1\",\"failure_type\":\"feed_loss\"}");
    // MAVLink goes silent: no reports, MTI opinions keep coming.
    fleet.run(60, () -> fleet.mti.replaceAll((uid, status) ->
        new MtiStatusSnapshot("go", fleet.clock.instant(), fleet.clock.instant().plusSeconds(600))));
    assertEquals(Band.RED, fleet.evaluator.getOverall());
    assertEquals(Band.RED, fleet.evaluator.getDisplayedBand(KpiId.POSITION_STALE));

    fleet.evaluator.handleControl("{\"event\":\"operator_aware\",\"run_id\":\"feed-loss-1\"}");
    Instant restoredAt = fleet.clock.instant().plusSeconds(1);
    fleet.run(20, fleet::healthy);

    FaultRunTracker.RunTypeStats stats = fleet.evaluator.getRuns().statsFor("FEED_LOSS");
    assertEquals(1, stats.getRuns());
    assertEquals(0, fleet.evaluator.getRuns().getOpenCount());
    // Detected by the feed outage monitor at 3 x the 1 s MAVLink interval, before any KPI moved.
    assertEquals(3.0, stats.stage(FaultRunTracker.Stage.DETECTION).getLastSeconds());
    // Position goes stale after 30 s: degraded as soon as the first asset passes the threshold.
    assertEquals(31.0, stats.stage(FaultRunTracker.Stage.DEGRADED).getLastSeconds());
    double expectedRestore = (restoredAt.toEpochMilli() - injectedAt.toEpochMilli()) / 1000.0;
    // Dated to when the data came back, not when the 15 s hold expired.
    assertEquals(expectedRestore, stats.stage(FaultRunTracker.Stage.USABLE_RESTORED).getLastSeconds());
    assertEquals(expectedRestore, stats.stage(FaultRunTracker.Stage.FULLY_RESTORED).getLastSeconds());
    assertEquals(60.0, stats.stage(FaultRunTracker.Stage.OPERATOR_AWARE).getLastSeconds());
    assertEquals(1, fleet.evaluator.getRuns().statsFor(FaultRunTracker.ALL).getRuns());

    List<String> log = logLines();
    assertTrue(log.stream().anyMatch(line -> line.contains("\"type\":\"run_summary\"") && line.contains("\"outcome\":\"RESTORED\"")));
    assertTrue(log.stream().anyMatch(line -> line.contains("\"type\":\"feed_outage_start\"")));
    assertTrue(log.stream().anyMatch(line -> line.contains("\"type\":\"band_change\"") && line.contains("POSITION_STALE")));
  }

  @Test
  void criticalKpiWithoutData_keepsOverallAmber_notGreen() {
    // Announced eligibility and MTI silent: readiness and coverage have no population.
    Fleet fleet = new Fleet(5, Map.of("eligibilityMode", "announced"));
    fleet.evaluator.start();
    fleet.run(2, () -> fleet.lastSeen.replaceAll((uid, at) -> fleet.clock.instant()));

    assertEquals(Band.NO_DATA, fleet.evaluator.getDisplayedBand(KpiId.READINESS));
    assertEquals(Band.AMBER, fleet.evaluator.getOverall());
    assertFalse(fleet.evaluator.isFullyRestored());
    assertTrue(fleet.evaluator.isUsable());
  }

  @Test
  void non_friendly_assets_are_left_out_of_every_kpi() {
    Fleet fleet = new Fleet(4, Map.of());
    fleet.types.put("uas-1", "a-h-A-M-F-Q");
    fleet.types.put("uas-2", "a-u-A");
    fleet.evaluator.start();
    fleet.run(2, () -> {
      fleet.healthy();
      fleet.mti.put("uas-1", new MtiStatusSnapshot("hold", fleet.clock.instant(), fleet.clock.instant().plusSeconds(600)));
      fleet.mti.put("uas-2", new MtiStatusSnapshot("hold", fleet.clock.instant(), fleet.clock.instant().plusSeconds(600)));
    });

    KpiSnapshot latest = fleet.evaluator.getLatest();
    assertEquals(2, latest.rosterSize());
    assertEquals(2, latest.eligibleSize());
    assertEquals(1.0, latest.value(KpiId.READINESS).value());
    assertEquals(0.0, latest.value(KpiId.DEGRADED).value());
    assertEquals(2, latest.value(KpiId.POSITION_STALE).denominator());
  }

  @Test
  void an_asset_reclassified_as_hostile_drops_out_and_mti_unknown_keeps_a_friend_in() {
    Fleet fleet = new Fleet(3, Map.of());
    fleet.evaluator.start();
    fleet.run(2, fleet::healthy);
    assertEquals(3, fleet.evaluator.getLatest().rosterSize());

    fleet.types.put("uas-0", "a-h-A-M-F-Q");
    fleet.run(1, () -> {
      fleet.healthy();
      fleet.mti.put("uas-1", new MtiStatusSnapshot("unknown", fleet.clock.instant(), fleet.clock.instant().plusSeconds(600)));
    });

    KpiSnapshot latest = fleet.evaluator.getLatest();
    assertEquals(2, latest.rosterSize());
    assertEquals(2, latest.eligibleSize());
    assertEquals(0.5, latest.value(KpiId.READINESS).value());
  }

  @Test
  void mtiOutage_shrinksCoverage_notTheEligiblePopulation() {
    Fleet fleet = new Fleet(10, Map.of("eligibilityMode", "announced"));
    fleet.evaluator.start();
    fleet.lastSeen.keySet().forEach(uid -> fleet.evaluator.onStatusAccepted(uid, T0));
    fleet.run(2, fleet::healthy);
    assertEquals(1.0, fleet.evaluator.getLatest().value(KpiId.COVERAGE).value());

    fleet.mti.clear();
    fleet.run(1, () -> fleet.lastSeen.replaceAll((uid, at) -> fleet.clock.instant()));

    assertEquals(10, fleet.evaluator.getLatest().eligibleSize());
    assertEquals(0.0, fleet.evaluator.getLatest().value(KpiId.COVERAGE).value());
    assertEquals(10, fleet.evaluator.getLatest().unknownReasons().get(AssetAssessment.UnknownReason.MTI_STALE));
  }

  @Test
  void restart_restoresRosterAndOpenRun_andTimesTheCacheRebuild() {
    Fleet fleet = new Fleet(20, Map.of());
    fleet.evaluator.start();
    fleet.run(3, fleet::healthy);
    fleet.evaluator.handleControl("{\"event\":\"inject\",\"run_id\":\"restart-1\",\"failure_type\":\"RESTART\"}");
    fleet.evaluator.stop();

    // MAPS is down for 20 s; nothing reports in the meantime and the MTI cache is gone.
    fleet.clock.advanceSeconds(20);
    fleet.mti.clear();
    fleet.lastSeen.clear();
    fleet.evaluator = fleet.newEvaluator(KpiConfig.from(new ConfigurationProperties(Map.of("dataDirectory", dataDirectory.toString()))));
    fleet.evaluator.start();
    fleet.run(1, () -> { });

    assertEquals(20, fleet.evaluator.getLatest().rosterSize(), "roster survives the restart");
    assertTrue(fleet.evaluator.getRebuilds().isInProgress());
    assertEquals(1, fleet.evaluator.getRuns().getOpenCount());

    for (int index = 0; index < 20; index++) {
      fleet.lastSeen.put("uas-" + index, fleet.clock.instant());
    }
    fleet.run(30, fleet::healthy);

    assertFalse(fleet.evaluator.getRebuilds().isInProgress());
    assertEquals(1, fleet.evaluator.getRebuilds().stats(CacheRebuildTracker.Trigger.RESTART, CacheRebuildTracker.Target.ALL).getCount());
    FaultRunTracker.RunTypeStats restart = fleet.evaluator.getRuns().statsFor("RESTART");
    assertEquals(1, restart.getRuns());
    assertEquals(20.0, restart.stage(FaultRunTracker.Stage.DETECTION).getLastSeconds(), "flagged when MAPS is back");
  }

  @Test
  void clearMtiCache_needsFaultInjectionEnabled() throws IOException {
    Fleet disabled = new Fleet(5, Map.of());
    disabled.evaluator.start();
    disabled.run(2, disabled::healthy);
    disabled.evaluator.handleControl("{\"event\":\"inject\",\"failure_type\":\"CACHE_LOSS\",\"clear_mti_cache\":true}");

    assertFalse(disabled.cacheCleared);
    assertTrue(logLines().stream().anyMatch(line -> line.contains("faultInjection")));
  }

  @Test
  void clearMtiCache_startsARebuildFromThePreFailureBaseline() {
    Fleet fleet = new Fleet(5, Map.of("faultInjection", true));
    fleet.evaluator.start();
    fleet.run(2, fleet::healthy);
    fleet.evaluator.handleControl("{\"event\":\"inject\",\"run_id\":\"cache-1\",\"failure_type\":\"CACHE_LOSS\",\"clear_mti_cache\":true}");

    assertTrue(fleet.cacheCleared);
    assertTrue(fleet.evaluator.getRebuilds().isInProgress());
    assertEquals(5, fleet.evaluator.getRebuilds().getBaselineSize());

    fleet.run(4, fleet::healthy);
    assertEquals(1, fleet.evaluator.getRebuilds().stats(CacheRebuildTracker.Trigger.CACHE_LOSS, CacheRebuildTracker.Target.ALL).getCount());
  }

  @Test
  void endMission_emptiesTheRoster_deactivateRemovesOne() {
    Fleet fleet = new Fleet(3, Map.of());
    fleet.evaluator.start();
    fleet.run(1, fleet::healthy);

    fleet.lastSeen.remove("uas-0");
    fleet.evaluator.handleControl("{\"event\":\"deactivate\",\"uid\":\"uas-0\"}");
    fleet.run(1, () -> { });
    assertEquals(2, fleet.evaluator.getLatest().rosterSize());

    fleet.lastSeen.clear();
    fleet.evaluator.handleControl("{\"event\":\"end_mission\"}");
    fleet.run(1, () -> { });
    assertEquals(0, fleet.evaluator.getLatest().rosterSize());
    assertEquals(Band.NO_DATA, fleet.evaluator.getOverall());
  }

  @Test
  void malformedControlMessages_areLoggedAndIgnored() throws IOException {
    Fleet fleet = new Fleet(1, Map.of());
    fleet.evaluator.start();

    fleet.evaluator.handleControl("not json");
    fleet.evaluator.handleControl("[1,2]");
    fleet.evaluator.handleControl("{\"event\":\"inject\"}");
    fleet.evaluator.handleControl("{\"event\":\"mitigation\"}");
    fleet.evaluator.handleControl("{\"event\":\"teleport\"}");

    assertEquals(0, fleet.evaluator.getRuns().getOpenCount());
    assertEquals(5, logLines().stream().filter(line -> line.contains("control_rejected")).count());
  }
}
