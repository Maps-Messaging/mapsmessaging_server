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

class MovingAverageStatisticsTest {

  @Test
  void update_populatesBaseStatsAndAddsMovingAverageFieldsToJson() {
    MovingAverageStatistics stats = new MovingAverageStatistics();

    stats.update(10);
    stats.update(20);

    JsonObject json = stats.toJson();
    assertEquals(2, json.get("count").getAsInt());
    assertEquals(10.0, json.get("first").getAsDouble(), 1e-12);
    assertEquals(20.0, json.get("last").getAsDouble(), 1e-12);

    assertTrue(json.has("1m"));
    assertTrue(json.has("5m"));
    assertTrue(json.has("10m"));
    assertTrue(json.has("15m"));
  }

  @Test
  void reset_doesNotThrow_andClearsBaseStats() {
    MovingAverageStatistics stats = new MovingAverageStatistics();
    stats.update(10);
    stats.update(20);

    stats.reset();
    JsonObject json = stats.toJson();

    assertEquals(0, json.get("count").getAsInt());
    assertTrue(Double.isNaN(json.get("first").getAsDouble()));
    assertTrue(json.has("1m"));
    assertTrue(json.has("15m"));
  }

  @Test
  void create_returnsNewInstance() {
    MovingAverageStatistics stats = new MovingAverageStatistics();
    Statistics created = stats.create();

    assertInstanceOf(MovingAverageStatistics.class, created);
    assertEquals(0, created.toJson().get("count").getAsInt());
  }
}
