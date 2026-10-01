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
import static org.junit.jupiter.api.Assertions.assertTrue;

class TakOutputStatsMaxLatencyTest {

  @Test
  void recentMax_coversCurrentAndPreviousWindow_thenExpires() {
    long start = 1_000_000_000L;
    TakOutputStats.getLatencyRecentMaxSeconds(start + 10 * TakOutputStats.RECENT_WINDOW_MILLIS);
    long base = start + 10 * TakOutputStats.RECENT_WINDOW_MILLIS;

    TakOutputStats.recordRecentMax(800, base + 1);
    TakOutputStats.recordRecentMax(200, base + TakOutputStats.RECENT_WINDOW_MILLIS + 1);

    assertEquals(0.8, TakOutputStats.getLatencyRecentMaxSeconds(base + TakOutputStats.RECENT_WINDOW_MILLIS + 2));
    assertEquals(0.2, TakOutputStats.getLatencyRecentMaxSeconds(base + 2 * TakOutputStats.RECENT_WINDOW_MILLIS + 2));
    assertEquals(-1.0, TakOutputStats.getLatencyRecentMaxSeconds(base + 5 * TakOutputStats.RECENT_WINDOW_MILLIS));
  }

  @Test
  void allTimeMax_neverGoesDown() {
    TakOutputStats.recordLatencyMillis(1_234_567);
    TakOutputStats.recordLatencyMillis(1);

    assertTrue(TakOutputStats.getLatencyMaxSeconds() >= 1234.567);
  }
}
