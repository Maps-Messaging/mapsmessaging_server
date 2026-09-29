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
 * Restoration delays for one (failure type, stage) pair: a cumulative, Prometheus-style duration
 * histogram plus a count of recovery events that timed out before this stage was reached.
 */
final class RecoveryDurationStats {

  /** Upper bounds, in seconds, of the restoration delay buckets. */
  static final double[] BUCKETS_SECONDS = {0.1, 0.25, 0.5, 1, 2.5, 5, 10, 30, 60, 120, 300};

  private final LongAdder[] bucketCounts = new LongAdder[BUCKETS_SECONDS.length];
  private final LongAdder count = new LongAdder();
  private final LongAdder sumMicros = new LongAdder();
  private final LongAdder notRestoredCount = new LongAdder();

  private volatile long lastMillis = -1L;

  RecoveryDurationStats() {
    for (int index = 0; index < bucketCounts.length; index++) {
      bucketCounts[index] = new LongAdder();
    }
  }

  void record(long delayMillis) {
    long clamped = Math.max(0L, delayMillis);
    lastMillis = clamped;
    count.increment();
    sumMicros.add(clamped * 1000L);
    double seconds = clamped / 1000.0;
    for (int index = 0; index < BUCKETS_SECONDS.length; index++) {
      if (seconds <= BUCKETS_SECONDS[index]) {
        bucketCounts[index].increment();
        return;
      }
    }
  }

  void recordNotRestored() {
    notRestoredCount.increment();
  }

  /** Cumulative count of restorations at or below bucket {@code index}. */
  long getCumulativeCount(int index) {
    long total = 0;
    for (int bucket = 0; bucket <= index; bucket++) {
      total += bucketCounts[bucket].sum();
    }
    return total;
  }

  long getCount() {
    return count.sum();
  }

  double getSumSeconds() {
    return sumMicros.sum() / 1_000_000.0;
  }

  /** -1 if nothing has been restored yet. */
  double getLastSeconds() {
    long last = lastMillis;
    return last < 0 ? -1.0 : last / 1000.0;
  }

  long getNotRestoredCount() {
    return notRestoredCount.sum();
  }
}
