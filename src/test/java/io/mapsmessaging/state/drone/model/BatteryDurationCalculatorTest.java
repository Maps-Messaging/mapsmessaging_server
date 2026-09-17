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

package io.mapsmessaging.state.drone.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BatteryDurationCalculatorTest {

  @Test
  void calculatesDurationFromMilliampHours() {
    assertEquals("PT4H", BatteryDurationCalculator.calculateDuration(80_000.0, 20.0));
  }

  @Test
  void calculatesDurationFromAmpHours() {
    assertEquals("PT8H", BatteryDurationCalculator.calculateDurationFromAmpHours(160.0, 20.0));
  }

  @Test
  void formatsMultiDayDuration() {
    assertEquals("P2DT12H", BatteryDurationCalculator.calculateDurationFromAmpHours(120.0, 2.0));
  }

  @Test
  void roundsFractionalSeconds() {
    assertEquals("PT4S", BatteryDurationCalculator.calculateDuration(1.0, 1.0));
  }

  @Test
  void zeroRemainingCapacityReturnsZeroDuration() {
    assertEquals("PT0S", BatteryDurationCalculator.calculateDuration(0.0, 10.0));
  }

  @Test
  void invalidCurrentReturnsNull() {
    assertNull(BatteryDurationCalculator.calculateDuration(10_000.0, null));
    assertNull(BatteryDurationCalculator.calculateDuration(10_000.0, 0.0));
    assertNull(BatteryDurationCalculator.calculateDuration(10_000.0, -1.0));
  }

  @Test
  void invalidCapacityReturnsNull() {
    assertNull(BatteryDurationCalculator.calculateDuration(null, 10.0));
    assertNull(BatteryDurationCalculator.calculateDuration(-1.0, 10.0));
  }

  @Test
  void nonFiniteInputsReturnNull() {
    assertNull(BatteryDurationCalculator.calculateDuration(Double.NaN, 10.0));
    assertNull(BatteryDurationCalculator.calculateDuration(Double.POSITIVE_INFINITY, 10.0));
    assertNull(BatteryDurationCalculator.calculateDuration(10_000.0, Double.NaN));
    assertNull(BatteryDurationCalculator.calculateDuration(10_000.0, Double.POSITIVE_INFINITY));
  }
}
