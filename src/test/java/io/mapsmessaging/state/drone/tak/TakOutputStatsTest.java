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

import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertEquals;

/** TakOutputStats is process-wide, so every assertion here is on a before/after delta. */
class TakOutputStatsTest {

  @Test
  void latency_isBucketedCumulativelyWithCountAndSum() {
    long before0005 = TakOutputStats.getLatencyCumulativeCount(0);
    long before005 = TakOutputStats.getLatencyCumulativeCount(3);
    long before10 = TakOutputStats.getLatencyCumulativeCount(10);
    long beforeCount = TakOutputStats.getLatencyCount();
    double beforeSum = TakOutputStats.getLatencySumSeconds();

    TakOutputStats.recordLatencyMillis(3);
    TakOutputStats.recordLatencyMillis(30);
    TakOutputStats.recordLatencyMillis(20_000);

    assertEquals(1, TakOutputStats.getLatencyCumulativeCount(0) - before0005, "3ms lands in the 5ms bucket");
    assertEquals(2, TakOutputStats.getLatencyCumulativeCount(3) - before005, "50ms bucket is cumulative");
    assertEquals(2, TakOutputStats.getLatencyCumulativeCount(10) - before10, "20s exceeds every bucket (+Inf only)");
    assertEquals(3, TakOutputStats.getLatencyCount() - beforeCount);
    assertEquals(20.033, TakOutputStats.getLatencySumSeconds() - beforeSum, 1e-9);
  }

  @Test
  void latency_negativeValuesFromClockSkewCountAsZero() {
    long before = TakOutputStats.getLatencyCumulativeCount(0);
    double beforeSum = TakOutputStats.getLatencySumSeconds();

    TakOutputStats.recordLatencyMillis(-250);

    assertEquals(1, TakOutputStats.getLatencyCumulativeCount(0) - before);
    assertEquals(0.0, TakOutputStats.getLatencySumSeconds() - beforeSum, 1e-9);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(longs = {5, 10, 25, 50, 100, 250, 500, 1000, 2500, 5000, 10000})
  void latency_bucket_includes_its_exact_bound_and_excludes_next_millisecond(long boundMillis) {
    int bucket = 0;
    while (TakOutputStats.LATENCY_BUCKETS_SECONDS[bucket] * 1000 != boundMillis) {
      bucket++;
    }
    long before = TakOutputStats.getLatencyCumulativeCount(bucket);
    long count = TakOutputStats.getLatencyCount();
    TakOutputStats.recordLatencyMillis(boundMillis);
    TakOutputStats.recordLatencyMillis(boundMillis + 1);
    assertEquals(1, TakOutputStats.getLatencyCumulativeCount(bucket) - before);
    assertEquals(2, TakOutputStats.getLatencyCount() - count);
  }

}
