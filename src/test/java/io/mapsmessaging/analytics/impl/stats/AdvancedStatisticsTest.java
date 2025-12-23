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

class AdvancedStatisticsTest {

  @Test
  void reset_clearsSubclassDerivedValuesInJson() {
    AdvancedStatistics stats = new AdvancedStatistics();
    stats.reset();

    JsonObject json = stats.toJson();
    assertEquals(0.0, json.get("stdDev").getAsDouble(), 0.0);
    assertEquals(0.0, json.get("slope").getAsDouble(), 0.0);
    assertEquals(0.0, json.get("intercept").getAsDouble(), 0.0);
  }

  @Test
  void stdDev_isZeroForFewerThanTwoSamples() {
    AdvancedStatistics stats = new AdvancedStatistics();
    stats.update(5);

    assertEquals(0.0, stats.getStdDeviation(), 0.0);
    JsonObject json = stats.toJson();
    assertEquals(0.0, json.get("stdDev").getAsDouble(), 0.0);
  }

  @Test
  void stdDev_matchesSampleStdDev() {
    AdvancedStatistics stats = new AdvancedStatistics();
    // values: 2, 4, 4, 4, 5, 5, 7, 9
    // sample std dev = sqrt( (sum (xi-xbar)^2) / (n-1) ) = sqrt(32/7)
    double[] values = {2, 4, 4, 4, 5, 5, 7, 9};
    for (double value : values) {
      stats.update(value);
    }

    double expected = Math.sqrt(32.0 / 7.0);
    assertEquals(expected, stats.getStdDeviation(), 1e-12);

    JsonObject json = stats.toJson();
    assertEquals(expected, json.get("stdDev").getAsDouble(), 1e-12);
  }

  @Test
  void regression_slopeAndIntercept_areCorrectForPerfectLine_overSampleIndex() {
    AdvancedStatistics stats = new AdvancedStatistics();
    // time = count (1..n). Use y = 2*t + 1
    for (int index = 1; index <= 5; index++) {
      double y = 2.0 * index + 1.0;
      stats.update(y);
    }

    assertEquals(2.0, stats.getSlope(), 1e-12);
    assertEquals(1.0, stats.getIntercept(), 1e-12);

    JsonObject json = stats.toJson();
    assertEquals(2.0, json.get("slope").getAsDouble(), 1e-12);
    assertEquals(1.0, json.get("intercept").getAsDouble(), 1e-12);
  }

  @Test
  void regression_returnsZeroForFewerThanTwoSamples() {
    AdvancedStatistics stats = new AdvancedStatistics();
    stats.update(123);

    assertEquals(0.0, stats.getSlope(), 0.0);
    assertEquals(0.0, stats.getIntercept(), 0.0);
  }

  @Test
  void create_returnsNewInstanceWithCleanState() {
    AdvancedStatistics stats = new AdvancedStatistics();
    stats.update(123);

    Statistics created = stats.create();
    assertNotNull(created);
    assertInstanceOf(AdvancedStatistics.class, created);

    JsonObject json = created.toJson();
    assertEquals(0, json.get("count").getAsInt());
    assertEquals(0.0, json.get("stdDev").getAsDouble(), 0.0);
    assertEquals(0.0, json.get("slope").getAsDouble(), 0.0);
    assertEquals(0.0, json.get("intercept").getAsDouble(), 0.0);
  }
}
