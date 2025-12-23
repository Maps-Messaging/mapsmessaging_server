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

class BaseStatisticsTest {

  @Test
  void reset_setsDefaults() {
    BaseStatistics stats = new BaseStatistics();
    stats.update(42);
    stats.incrementMismatch();

    stats.reset();
    JsonObject json = stats.toJson();

    assertTrue(Double.isNaN(json.get("first").getAsDouble()));
    assertTrue(Double.isNaN(json.get("last").getAsDouble()));

    assertEquals(Double.MAX_VALUE, json.get("min").getAsDouble(), 0.0);
    assertEquals(Double.NEGATIVE_INFINITY, json.get("max").getAsDouble(), 0.0);

    assertEquals(0.0, json.get("average").getAsDouble(), 0.0);
    assertEquals(0, json.get("count").getAsInt());
    assertEquals(0, json.get("mismatched").getAsInt());

    assertEquals(0L, json.get("firstUpdateMillis").getAsLong());
    assertEquals(0L, json.get("lastUpdateMillis").getAsLong());
  }

  @Test
  void update_ignoresNonNumbers() {
    BaseStatistics stats = new BaseStatistics();
    stats.update("nope");
    stats.update(null);

    JsonObject json = stats.toJson();
    assertEquals(0, json.get("count").getAsInt());
    assertTrue(Double.isNaN(json.get("first").getAsDouble()));
    assertTrue(Double.isNaN(json.get("last").getAsDouble()));
  }

  @Test
  void update_tracksFirstLastMinMaxAverageCount_andTimestamps() throws Exception {
    BaseStatistics stats = new BaseStatistics();

    stats.update(10);
    Thread.sleep(2);
    stats.update(20);
    Thread.sleep(2);
    stats.update(30);

    JsonObject json = stats.toJson();

    assertEquals(10.0, json.get("first").getAsDouble(), 1e-12);
    assertEquals(30.0, json.get("last").getAsDouble(), 1e-12);
    assertEquals(10.0, json.get("min").getAsDouble(), 1e-12);
    assertEquals(30.0, json.get("max").getAsDouble(), 1e-12);
    assertEquals(20.0, json.get("average").getAsDouble(), 1e-12);
    assertEquals(3, json.get("count").getAsInt());

    long firstTs = json.get("firstUpdateMillis").getAsLong();
    long lastTs = json.get("lastUpdateMillis").getAsLong();
    assertTrue(firstTs > 0L);
    assertTrue(lastTs >= firstTs);
  }

  @Test
  void update_handlesNegativeNumbers_includingMaxInitialization() {
    BaseStatistics stats = new BaseStatistics();

    stats.update(-5);
    stats.update(-10);

    JsonObject json = stats.toJson();
    assertEquals(-5.0, json.get("first").getAsDouble(), 1e-12);
    assertEquals(-10.0, json.get("last").getAsDouble(), 1e-12);
    assertEquals(-10.0, json.get("min").getAsDouble(), 1e-12);
    assertEquals(-5.0, json.get("max").getAsDouble(), 1e-12);
    assertEquals(2, json.get("count").getAsInt());
    assertEquals((-5.0 + -10.0) / 2.0, json.get("average").getAsDouble(), 1e-12);
  }

  @Test
  void incrementMismatch_incrementsAndIsExposedInJson() {
    BaseStatistics stats = new BaseStatistics();

    stats.incrementMismatch();
    stats.incrementMismatch();

    JsonObject json = stats.toJson();
    assertEquals(2, json.get("mismatched").getAsInt());
  }

  @Test
  void create_returnsNewInstanceWithCleanState() {
    BaseStatistics stats = new BaseStatistics();
    stats.update(123);

    Statistics created = stats.create();
    assertNotNull(created);
    assertInstanceOf(BaseStatistics.class, created);

    JsonObject json = created.toJson();
    assertEquals(0, json.get("count").getAsInt());
    assertTrue(Double.isNaN(json.get("first").getAsDouble()));
  }
}
