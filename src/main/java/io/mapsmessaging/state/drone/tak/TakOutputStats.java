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

import java.util.concurrent.atomic.LongAdder;

/**
 * Process-wide counters for the TAK output leg. Aggregated across all {@link TakSocketConnection}
 * instances (one shared, or one per twin depending on config), so counts survive individual
 * connections being closed and recreated.
 */
final class TakOutputStats {

  /** Upper bounds, in seconds, of the update-to-CoT latency histogram buckets. */
  static final double[] LATENCY_BUCKETS_SECONDS = {0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1, 2.5, 5, 10};

  static final LongAdder CONNECT_COUNT = new LongAdder();
  static final LongAdder CONNECT_FAILURE_COUNT = new LongAdder();
  static final LongAdder DISCONNECT_COUNT = new LongAdder();
  static final LongAdder SOCKET_DROPPED_COUNT = new LongAdder();

  private static final LongAdder[] LATENCY_BUCKET_COUNTS = new LongAdder[LATENCY_BUCKETS_SECONDS.length];
  private static final LongAdder LATENCY_COUNT = new LongAdder();
  private static final LongAdder LATENCY_SUM_MICROS = new LongAdder();

  private static volatile long lastWriteMillis = 0L;

  static {
    for (int i = 0; i < LATENCY_BUCKET_COUNTS.length; i++) {
      LATENCY_BUCKET_COUNTS[i] = new LongAdder();
    }
  }

  private TakOutputStats() {
  }

  static void recordWrite() {
    lastWriteMillis = System.currentTimeMillis();
  }

  /** -1 if nothing has been written to a TAK socket yet. */
  static long getLastWriteAgeMillis() {
    long at = lastWriteMillis;
    return at == 0L ? -1L : System.currentTimeMillis() - at;
  }

  static void recordLatencyMillis(long latencyMillis) {
    long clamped = Math.max(0L, latencyMillis);
    LATENCY_COUNT.increment();
    LATENCY_SUM_MICROS.add(clamped * 1000L);
    double seconds = clamped / 1000.0;
    for (int i = 0; i < LATENCY_BUCKETS_SECONDS.length; i++) {
      if (seconds <= LATENCY_BUCKETS_SECONDS[i]) {
        LATENCY_BUCKET_COUNTS[i].increment();
        return;
      }
    }
  }

  /** Cumulative count of observations at or below bucket {@code index}, Prometheus-style. */
  static long getLatencyCumulativeCount(int index) {
    long total = 0;
    for (int i = 0; i <= index; i++) {
      total += LATENCY_BUCKET_COUNTS[i].sum();
    }
    return total;
  }

  static long getLatencyCount() {
    return LATENCY_COUNT.sum();
  }

  static double getLatencySumSeconds() {
    return LATENCY_SUM_MICROS.sum() / 1_000_000.0;
  }
}
