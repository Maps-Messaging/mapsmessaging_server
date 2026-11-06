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

package io.mapsmessaging.state.metrics;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Process-wide record of when each input/output feed was last active, read by the KPI feed outage
 * monitor. Feed names are {@code <kind>:<instance>}, e.g. {@code mti:status}, {@code mavlink:<source>},
 * {@code cot:<edge>} and {@code tak:<host>_<port>}.
 *
 * <p>Message feeds call {@link #recordActivity}; connection-oriented feeds (TAK servers) also call
 * {@link #setConnected}, because a quiet TAK connection is not an outage - only a dropped one is.
 */
public final class FeedActivityRegistry {

  /** Last activity time and connection state of one feed. */
  public record FeedState(long lastActivityMillis, Boolean connected) {
  }

  private static final Map<String, FeedState> FEEDS = new ConcurrentHashMap<>();

  private FeedActivityRegistry() {
  }

  public static void recordActivity(String feed) {
    if (feed == null) {
      return;
    }
    long now = System.currentTimeMillis();
    FEEDS.compute(feed, (key, current) -> new FeedState(now, current == null ? null : current.connected()));
  }

  public static void setConnected(String feed, boolean connected) {
    if (feed == null) {
      return;
    }
    FEEDS.compute(feed, (key, current) -> new FeedState(current == null ? 0L : current.lastActivityMillis(), connected));
  }

  /** @return a copy of every feed seen so far, keyed by feed name. */
  public static Map<String, FeedState> snapshot() {
    return Map.copyOf(FEEDS);
  }
}
