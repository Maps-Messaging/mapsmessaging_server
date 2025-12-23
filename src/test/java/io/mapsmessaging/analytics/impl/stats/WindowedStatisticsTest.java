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

class WindowedStatisticsTest {

  @Test
  void window_tracksOnlyLastNValues() {
    WindowedStatistics stats = new WindowedStatistics(3);

    stats.update(1.0);
    stats.update(2.0);
    stats.update(3.0);
    assertEquals(3, stats.getWindowCount());
    assertEquals(2.0, stats.getWindowMean(), 1e-12);

    stats.update(4.0); // window now [2,3,4]
    assertEquals(3, stats.getWindowCount());
    assertEquals(3.0, stats.getWindowMean(), 1e-12);
  }

  @Test
  void windowStdDev_matchesSampleStdDevOfWindow() {
    WindowedStatistics stats = new WindowedStatistics(3);

    // window [2,3,4] => mean=3, sample variance = ((4+9+16) - 9*3)/2 = (29-27)/2 = 1
    stats.update(1.0);
    stats.update(2.0);
    stats.update(3.0);
    stats.update(4.0);

    assertEquals(1.0, stats.getWindowStdDeviation(), 1e-12);
  }

  @Test
  void windowStdDev_isZeroWhenFewerThanTwoSamples() {
    WindowedStatistics stats = new WindowedStatistics(5);
    stats.update(10.0);

    assertEquals(1, stats.getWindowCount());
    assertEquals(0.0, stats.getWindowStdDeviation(), 0.0);
  }

  @Test
  void toJson_containsWindowFields() {
    WindowedStatistics stats = new WindowedStatistics(3);
    stats.update(1.0);
    stats.update(2.0);
    stats.update(3.0);

    JsonObject json = stats.toJson();
    assertEquals(3, json.get("windowSize").getAsInt());
    assertEquals(3, json.get("windowCount").getAsInt());
    assertEquals(2.0, json.get("windowMean").getAsDouble(), 1e-12);
    assertTrue(json.has("windowStdDev"));
  }

  @Test
  void reset_clearsWindowAndBaseStats() {
    WindowedStatistics stats = new WindowedStatistics(3);
    stats.update(1.0);
    stats.update(2.0);

    stats.reset();

    assertEquals(0, stats.getWindowCount());
    assertEquals(0.0, stats.getWindowMean(), 0.0);
    assertEquals(0.0, stats.getWindowStdDeviation(), 0.0);

    JsonObject json = stats.toJson();
    assertEquals(0, json.get("count").getAsInt());
  }

  @Test
  void create_returnsNewInstanceSameWindowSize_cleanState() {
    WindowedStatistics stats = new WindowedStatistics(7);
    stats.update(1.0);

    Statistics created = stats.create();
    assertInstanceOf(WindowedStatistics.class, created);

    WindowedStatistics ws = (WindowedStatistics) created;
    assertEquals(0, ws.getWindowCount());
    assertEquals(0.0, ws.getWindowMean(), 0.0);

    JsonObject json = ws.toJson();
    assertEquals(7, json.get("windowSize").getAsInt());
    assertEquals(0, json.get("windowCount").getAsInt());
  }
}
