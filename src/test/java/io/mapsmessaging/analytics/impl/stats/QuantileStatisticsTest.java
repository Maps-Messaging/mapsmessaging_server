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

class QuantileStatisticsTest {

  @Test
  void quantiles_areNaNBeforeAnySamples() {
    QuantileStatistics stats = new QuantileStatistics();
    assertTrue(Double.isNaN(stats.getMedian()));
    assertTrue(Double.isNaN(stats.getP90()));
  }

  @Test
  void quantiles_existInJson_afterSomeSamples() {
    QuantileStatistics stats = new QuantileStatistics();
    for (int i = 1; i <= 5; i++) {
      stats.update(i);
    }

    JsonObject json = stats.toJson();
    assertTrue(json.has("median"));
    assertTrue(json.has("p90"));
    assertTrue(json.has("p95"));
    assertTrue(json.has("p99"));
  }

  @Test
  void median_isReasonableForOrderedSequence() {
    QuantileStatistics stats = new QuantileStatistics();
    for (int i = 1; i <= 101; i++) {
      stats.update(i);
    }

    // True median is 51 for 1..101
    double median = stats.getMedian();
    assertFalse(Double.isNaN(median));
    assertEquals(51.0, median, 2.0); // P² is approximate; give it room to breathe
  }

  @Test
  void create_returnsNewQuantileStatisticsWithCleanState() {
    QuantileStatistics stats = new QuantileStatistics();
    for (int i = 1; i <= 10; i++) {
      stats.update(i);
    }

    Statistics created = stats.create();
    assertInstanceOf(QuantileStatistics.class, created);

    JsonObject json = created.toJson();
    assertEquals(0, json.get("count").getAsInt());
    assertTrue(Double.isNaN(((QuantileStatistics) created).getMedian()));
  }
}

