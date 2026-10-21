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
import io.mapsmessaging.state.metrics.FeedActivityRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FeedOutageMonitorTest {

  private static final Instant T0 = Instant.parse("2026-10-01T10:00:00Z");

  private static FeedActivityRegistry.FeedState lastSeen(Instant at) {
    return new FeedActivityRegistry.FeedState(at.toEpochMilli(), null);
  }

  private static FeedOutageMonitor monitor(Map<String, Object> config) {
    return new FeedOutageMonitor(KpiConfig.from(new ConfigurationProperties(config)), T0);
  }

  @Test
  void messageFeed_inOutageAfterThreeMissedIntervals_datedFromLastMessage() {
    FeedOutageMonitor monitor = monitor(Map.of());
    Map<String, FeedActivityRegistry.FeedState> states = Map.of("mti:status", lastSeen(T0));

    assertEquals(List.of(), monitor.update(states, T0.plusSeconds(89)));
    List<FeedOutageMonitor.OutageEvent> started = monitor.update(states, T0.plusSeconds(90));

    assertEquals(1, started.size());
    assertTrue(started.get(0).started());
    assertEquals(T0, started.get(0).at());
    assertEquals(T0.plusSeconds(90), started.get(0).detectedAt());
  }

  @Test
  void outageEnd_recordsDurationUntilTheNextMessage() {
    FeedOutageMonitor monitor = monitor(Map.of());
    monitor.update(Map.of("mavlink:drone", lastSeen(T0)), T0.plusSeconds(5));

    List<FeedOutageMonitor.OutageEvent> ended = monitor.update(Map.of("mavlink:drone", lastSeen(T0.plusSeconds(40))), T0.plusSeconds(41));

    assertFalse(ended.get(0).started());
    assertEquals(Duration.ofSeconds(40), ended.get(0).duration());
    FeedOutageMonitor.FeedStats stats = monitor.getFeeds().get(0);
    assertEquals(1, stats.getOutages().getCount());
    assertEquals(40.0, stats.getTotalOutageSeconds());
    assertFalse(stats.isInOutage());
  }

  @Test
  void prefixInterval_appliesToEveryFeedOfThatKind_exactKeyWins() {
    KpiConfig config = KpiConfig.from(new ConfigurationProperties(Map.of("feeds", Map.of("cot:", 5, "cot:ihmc", 60))));

    assertEquals(Duration.ofSeconds(5), config.feedInterval("cot:edge-1"));
    assertEquals(Duration.ofSeconds(60), config.feedInterval("cot:ihmc"));
    assertNull(config.feedInterval("unknown:feed"));
  }

  @Test
  void unconfiguredFeed_isNotMonitored() {
    FeedOutageMonitor monitor = monitor(Map.of());

    assertEquals(List.of(), monitor.update(Map.of("unknown:feed", lastSeen(T0)), T0.plusSeconds(3600)));
    assertEquals(List.of(), monitor.getFeeds());
  }

  @Test
  void requiredFeedNeverSeen_isInOutageFromStartup() {
    FeedOutageMonitor monitor = monitor(Map.of("requiredFeeds", "mti:status"));

    List<FeedOutageMonitor.OutageEvent> events = monitor.update(Map.of(), T0.plusSeconds(90));

    assertEquals(1, events.size());
    assertEquals(T0, events.get(0).at());
  }

  @Test
  void connectionFeed_inOutageWhileDisconnected_notWhileQuiet() {
    FeedOutageMonitor monitor = monitor(Map.of());
    FeedActivityRegistry.FeedState connected = new FeedActivityRegistry.FeedState(T0.toEpochMilli(), true);
    FeedActivityRegistry.FeedState disconnected = new FeedActivityRegistry.FeedState(T0.toEpochMilli(), false);

    assertEquals(List.of(), monitor.update(Map.of("tak:central_8089", connected), T0.plusSeconds(3600)));
    assertTrue(monitor.update(Map.of("tak:central_8089", disconnected), T0.plusSeconds(3601)).get(0).started());
    assertTrue(monitor.anyInOutage());
    FeedOutageMonitor.OutageEvent ended = monitor.update(Map.of("tak:central_8089", connected), T0.plusSeconds(3611)).get(0);
    assertEquals(Duration.ofSeconds(10), ended.duration());
  }
}
