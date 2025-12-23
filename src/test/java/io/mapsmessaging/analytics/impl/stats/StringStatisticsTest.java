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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StringStatisticsTest {

  @Test
  void update_countsTrimmedStringsAndNumbers() {
    StringStatistics stats = new StringStatistics();
    stats.reset();

    stats.update("  apple  ");
    stats.update("apple");
    stats.update(123);
    stats.update(123);
    stats.update(null);
    stats.update(new Object()); // ignored

    JsonObject json = stats.toJson();
    assertEquals(4L, json.get("totalCount").getAsLong());
  }

  @Test
  void toJson_distributionIsSortedByCountDescending() {
    StringStatistics stats = new StringStatistics();
    stats.reset();

    stats.update("a");
    stats.update("b");
    stats.update("b");
    stats.update("c");
    stats.update("c");
    stats.update("c");

    JsonObject json = stats.toJson();
    JsonArray dist = json.getAsJsonArray("distribution");

    assertEquals("c", dist.get(0).getAsJsonObject().get("value").getAsString());
    assertEquals(3L, dist.get(0).getAsJsonObject().get("count").getAsLong());

    assertEquals("b", dist.get(1).getAsJsonObject().get("value").getAsString());
    assertEquals(2L, dist.get(1).getAsJsonObject().get("count").getAsLong());

    assertEquals("a", dist.get(2).getAsJsonObject().get("value").getAsString());
    assertEquals(1L, dist.get(2).getAsJsonObject().get("count").getAsLong());
  }

  @Test
  void incrementMismatch_incrementsAndAppearsInJson() {
    StringStatistics stats = new StringStatistics();
    stats.reset();

    stats.incrementMismatch();
    stats.incrementMismatch();

    JsonObject json = stats.toJson();
    assertEquals(2, json.get("mismatched").getAsInt());
  }

  @Test
  void reset_clearsCountsAndMismatch() {
    StringStatistics stats = new StringStatistics();
    stats.update("x");
    stats.incrementMismatch();

    stats.reset();
    JsonObject json = stats.toJson();

    assertEquals(0L, json.get("totalCount").getAsLong());
    assertEquals(0, json.get("mismatched").getAsInt());
    assertEquals(0, json.getAsJsonArray("distribution").size());
  }

  @Test
  void create_returnsNewInstanceWithCleanState() {
    StringStatistics stats = new StringStatistics();
    stats.update("x");

    Statistics created = stats.create();
    assertInstanceOf(StringStatistics.class, created);

    JsonObject json = created.toJson();
    assertEquals(0L, json.get("totalCount").getAsLong());
    assertEquals(0, json.getAsJsonArray("distribution").size());
  }
}
