/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
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

package io.mapsmessaging.analytics.impl.stats;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TrendStatisticsTest {

  @Test
  void timestampRegression_isZeroUntilAtLeastTwoTimestampSamplesExist() {
    TrendStatistics stats = new TrendStatistics();

    stats.update(1_000_000_000L, 3.0); // one sample

    assertEquals(0.0, stats.getTimestampSlopePerSecond(), 0.0);
    assertEquals(0.0, stats.getTimestampIntercept(), 0.0);

    JsonObject json = stats.toJson();
    assertEquals(0.0, json.get("timestampSlopePerSec").getAsDouble(), 0.0);
    assertEquals(0.0, json.get("timestampIntercept").getAsDouble(), 0.0);
  }

  @Test
  void timestampRegression_matchesPerfectLine_withExplicitTimestamps() {
    TrendStatistics stats = new TrendStatistics();

    // y = 2*t + 1, t in seconds; timestamps in nanos
    stats.update(1_000_000_000L, 3.0); // t=1
    stats.update(2_000_000_000L, 5.0); // t=2
    stats.update(3_000_000_000L, 7.0); // t=3

    assertEquals(2.0, stats.getTimestampSlopePerSecond(), 1e-12);
    assertEquals(1.0, stats.getTimestampIntercept(), 1e-12);

    JsonObject json = stats.toJson();
    assertEquals(2.0, json.get("timestampSlopePerSec").getAsDouble(), 1e-12);
    assertEquals(1.0, json.get("timestampIntercept").getAsDouble(), 1e-12);
  }


  @Test
  void timestampRegression_matchesPerfectLine() {
    TrendStatistics stats = new TrendStatistics();

    // y = 2*t + 1, with t in seconds; timestamps are nanos
    stats.update(1_000_000_000L, 3.0);  // t=1 -> y=3
    stats.update(2_000_000_000L, 5.0);  // t=2 -> y=5
    stats.update(3_000_000_000L, 7.0);  // t=3 -> y=7

    assertEquals(2.0, stats.getTimestampSlopePerSecond(), 1e-12);
    assertEquals(1.0, stats.getTimestampIntercept(), 1e-12);

    JsonObject json = stats.toJson();
    assertEquals(2.0, json.get("timestampSlopePerSec").getAsDouble(), 1e-12);
    assertEquals(1.0, json.get("timestampIntercept").getAsDouble(), 1e-12);
  }

  @Test
  void timestampRegression_isZeroWhenDenominatorZero() {
    TrendStatistics stats = new TrendStatistics();

    // same timestamp => denominator becomes 0
    stats.update(1_000_000_000L, 10.0);
    stats.update(1_000_000_000L, 20.0);

    assertEquals(0.0, stats.getTimestampSlopePerSecond(), 0.0);
    assertEquals(0.0, stats.getTimestampIntercept(), 0.0);
  }

  @Test
  void reset_clearsTimestampState() {
    TrendStatistics stats = new TrendStatistics();
    stats.update(1_000_000_000L, 3.0);
    stats.update(2_000_000_000L, 5.0);

    stats.reset();

    assertEquals(0.0, stats.getTimestampSlopePerSecond(), 0.0);
    assertEquals(0.0, stats.getTimestampIntercept(), 0.0);

    JsonObject json = stats.toJson();
    assertEquals(0, json.get("count").getAsInt());
    assertEquals(0.0, json.get("timestampSlopePerSec").getAsDouble(), 0.0);
    assertEquals(0.0, json.get("timestampIntercept").getAsDouble(), 0.0);
  }

  @Test
  void create_returnsNewInstanceWithCleanState() {
    TrendStatistics stats = new TrendStatistics();
    stats.update(1_000_000_000L, 3.0);

    Statistics created = stats.create();
    assertInstanceOf(TrendStatistics.class, created);

    JsonObject json = created.toJson();
    assertEquals(0, json.get("count").getAsInt());
    assertEquals(0.0, json.get("timestampSlopePerSec").getAsDouble(), 0.0);
  }
}
