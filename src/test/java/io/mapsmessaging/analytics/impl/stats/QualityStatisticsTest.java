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

class QualityStatisticsTest {

  @Test
  void update_nanAndInfinite_incrementCountersAndDoNotAffectCount() {
    QualityStatistics stats = new QualityStatistics();

    stats.update(Double.NaN);
    stats.update(Double.POSITIVE_INFINITY);
    stats.update(Double.NEGATIVE_INFINITY);
    stats.update(1.0);

    JsonObject json = stats.toJson();
    assertEquals(1, json.get("count").getAsInt());
    assertEquals(1L, json.get("nan").getAsLong());
    assertEquals(2L, json.get("infinite").getAsLong());
  }

  @Test
  void updateMissing_incrementsMissingCount_andShowsInJson() {
    QualityStatistics stats = new QualityStatistics();
    stats.updateMissing();
    stats.updateMissing();

    JsonObject json = stats.toJson();
    assertEquals(2L, json.get("missing").getAsLong());
  }

  @Test
  void setOutlierStdDevs_isClampedToNonNegative() {
    QualityStatistics stats = new QualityStatistics();

    stats.setOutlierStdDevs(-5.0);
    JsonObject json = stats.toJson();
    assertEquals(0.0, json.get("outlierStdDevs").getAsDouble(), 0.0);
  }

  @Test
  void outlierDetection_countsObviousOutlier_whenThresholdIsStrict() {
    QualityStatistics stats = new QualityStatistics();
    stats.setOutlierStdDevs(0.5); // make it strict

    // Build some baseline variance then inject a big jump
    for (int i = 0; i < 20; i++) {
      stats.update(100.0 + (i % 2)); // 100,101,100,101...
    }
    long before = stats.getOutlierCount();

    stats.update(1000.0);
    long after = stats.getOutlierCount();

    assertTrue(after >= before + 1, "Expected outlier count to increase");
  }

  @Test
  void create_returnsNewQualityStatisticsWithCleanState() {
    QualityStatistics stats = new QualityStatistics();
    stats.updateMissing();
    stats.update(Double.NaN);
    stats.update(1.0);

    Statistics created = stats.create();
    assertInstanceOf(QualityStatistics.class, created);

    JsonObject json = created.toJson();
    assertEquals(0, json.get("count").getAsInt());
    assertEquals(0L, json.get("missing").getAsLong());
    assertEquals(0L, json.get("nan").getAsLong());
    assertEquals(0L, json.get("infinite").getAsLong());
    assertEquals(0L, json.get("outliers").getAsLong());
  }
}
