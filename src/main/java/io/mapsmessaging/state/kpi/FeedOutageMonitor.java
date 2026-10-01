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

import io.mapsmessaging.state.metrics.FeedActivityRegistry;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Outage count and duration per feed (IC26 KPI clarification Q18). A message feed is in outage
 * once it has been silent for {@code feedSilenceMultiplier} x its expected report interval; the
 * outage is dated from the last message, i.e. from when the silence began. A connection feed (TAK)
 * is in outage while its connection is down. Required feeds are in outage from startup until
 * their first message.
 */
final class FeedOutageMonitor {

  /** An outage that started or ended during an update. */
  record OutageEvent(String feed, boolean started, Instant at, Instant detectedAt, Duration duration) {
  }

  static final class FeedStats {
    private final String feed;
    private final Duration expectedInterval;
    private final DurationStats outages = new DurationStats();
    private Instant outageSince;
    private double totalOutageSeconds;

    private FeedStats(String feed, Duration expectedInterval) {
      this.feed = feed;
      this.expectedInterval = expectedInterval;
    }

    String getFeed() {
      return feed;
    }

    /** {@code null} for connection feeds, which have no report interval. */
    Duration getExpectedInterval() {
      return expectedInterval;
    }

    synchronized boolean isInOutage() {
      return outageSince != null;
    }

    synchronized double getCurrentOutageSeconds(Instant now) {
      return outageSince == null ? 0.0 : Duration.between(outageSince, now).toMillis() / 1000.0;
    }

    synchronized double getTotalOutageSeconds() {
      return totalOutageSeconds;
    }

    DurationStats getOutages() {
      return outages;
    }
  }

  private final KpiConfig config;
  private final Instant startedAt;
  private final Map<String, FeedStats> feeds = new TreeMap<>();

  FeedOutageMonitor(KpiConfig config, Instant startedAt) {
    this.config = config;
    this.startedAt = startedAt;
  }

  synchronized List<OutageEvent> update(Map<String, FeedActivityRegistry.FeedState> states, Instant now) {
    Set<String> names = new TreeSet<>(states.keySet());
    names.addAll(config.getRequiredFeeds());
    List<OutageEvent> events = new ArrayList<>();
    for (String name : names) {
      FeedActivityRegistry.FeedState state = states.get(name);
      boolean connectionFeed = state != null && state.connected() != null;
      Duration interval = connectionFeed ? null : config.feedInterval(name);
      if (!connectionFeed && interval == null) {
        continue;
      }
      FeedStats stats = feeds.computeIfAbsent(name, feed -> new FeedStats(feed, interval));
      Instant silenceStart = connectionFeed ? null : silenceStart(state);
      boolean down = connectionFeed
          ? !state.connected()
          : !silenceStart.plusMillis((long) (interval.toMillis() * config.getFeedSilenceMultiplier())).isAfter(now);
      synchronized (stats) {
        if (down && stats.outageSince == null) {
          stats.outageSince = connectionFeed ? now : silenceStart;
          events.add(new OutageEvent(name, true, stats.outageSince, now, null));
        } else if (!down && stats.outageSince != null) {
          Instant end = connectionFeed || state == null ? now : Instant.ofEpochMilli(state.lastActivityMillis());
          Duration duration = Duration.between(stats.outageSince, end);
          stats.outages.record(duration);
          stats.totalOutageSeconds += Math.max(0L, duration.toMillis()) / 1000.0;
          stats.outageSince = null;
          events.add(new OutageEvent(name, false, end, now, duration));
        }
      }
    }
    return events;
  }

  /** A feed never seen (required feeds only) has been silent since the monitor started. */
  private Instant silenceStart(FeedActivityRegistry.FeedState state) {
    if (state == null || state.lastActivityMillis() <= 0L) {
      return startedAt;
    }
    return Instant.ofEpochMilli(state.lastActivityMillis());
  }

  synchronized List<FeedStats> getFeeds() {
    return new ArrayList<>(feeds.values());
  }

  synchronized boolean anyInOutage() {
    for (FeedStats stats : feeds.values()) {
      if (stats.isInOutage()) {
        return true;
      }
    }
    return false;
  }
}
