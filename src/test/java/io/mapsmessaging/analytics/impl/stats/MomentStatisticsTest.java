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

class MomentStatisticsTest {

  @Test
  void reset_clearsSkewnessAndKurtosisInJson() {
    MomentStatistics stats = new MomentStatistics();
    stats.update(1);
    stats.update(2);
    stats.update(3);

    stats.reset();
    JsonObject json = stats.toJson();

    assertEquals(0.0, json.get("skewness").getAsDouble(), 0.0);
    assertEquals(0.0, json.get("kurtosisExcess").getAsDouble(), 0.0);
  }

  @Test
  void skewness_isZeroForFewerThanThreeSamples_orZeroVariance() {
    MomentStatistics stats = new MomentStatistics();

    stats.update(5);
    stats.update(6);
    assertEquals(0.0, stats.getSampleSkewness(), 0.0);

    stats.reset();
    stats.update(7);
    stats.update(7);
    stats.update(7);
    assertEquals(0.0, stats.getSampleSkewness(), 0.0);
  }

  @Test
  void kurtosisExcess_isZeroForFewerThanFourSamples() {
    MomentStatistics stats = new MomentStatistics();
    stats.update(1);
    stats.update(2);
    stats.update(3);

    assertEquals(0.0, stats.getSampleKurtosisExcess(), 0.0);
  }

  @Test
  void symmetricData_hasNearZeroSkewness() {
    MomentStatistics stats = new MomentStatistics();
    // symmetric around 0
    double[] values = {-2, -1, 0, 1, 2};
    for (double v : values) {
      stats.update(v);
    }

    assertEquals(0.0, stats.getSampleSkewness(), 1e-12);

    JsonObject json = stats.toJson();
    assertEquals(0.0, json.get("skewness").getAsDouble(), 1e-12);
  }

  @Test
  void create_returnsNewMomentStatisticsWithCleanState() {
    MomentStatistics stats = new MomentStatistics();
    stats.update(1);
    stats.update(2);
    stats.update(3);
    Statistics created = stats.create();

    assertInstanceOf(MomentStatistics.class, created);
    JsonObject json = created.toJson();

    assertEquals(0, json.get("count").getAsInt());
    assertEquals(0.0, json.get("skewness").getAsDouble(), 0.0);
    assertEquals(0.0, json.get("kurtosisExcess").getAsDouble(), 0.0);
  }
}
