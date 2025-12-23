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

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class TimeWindowMovingAverageTest {

  @Test
  void name_isTimeAndUnit() {
    TimeWindowMovingAverage avg = new TimeWindowMovingAverage(5, TimeUnit.MINUTES);
    assertEquals("5_MINUTES", avg.getName());
  }

  @Test
  void average_isZeroWhenEmpty() {
    TimeWindowMovingAverage avg = new TimeWindowMovingAverage(1, TimeUnit.SECONDS);
    assertEquals(0.0, avg.getAverage(), 0.0);
  }

  @Test
  void add_includesValuesInAverageUntilExpired() throws Exception {
    TimeWindowMovingAverage avg = new TimeWindowMovingAverage(5, TimeUnit.MILLISECONDS);

    avg.add(10);
    avg.add(20);
    assertEquals(15.0, avg.getAverage(), 1e-12);

    // Wait for expiry
    Thread.sleep(10);
    avg.update();
    assertEquals(0.0, avg.getAverage(), 0.0);
  }

  @Test
  void reset_clearsState() {
    TimeWindowMovingAverage avg = new TimeWindowMovingAverage(1, TimeUnit.SECONDS);

    avg.add(10);
    avg.add(20);
    assertEquals(15.0, avg.getAverage(), 1e-12);

    avg.reset();
    assertEquals(0.0, avg.getAverage(), 0.0);
  }

  @Test
  void update_doesNotThrow_whenEmpty() {
    TimeWindowMovingAverage avg = new TimeWindowMovingAverage(1, TimeUnit.SECONDS);
    assertDoesNotThrow(avg::update);
  }
}
