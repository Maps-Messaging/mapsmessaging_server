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

package io.mapsmessaging.state.drone.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SyntheticMmsiGeneratorTest {

  @Test
  void generateSyntheticMmsi_sameTrimmedTwinId_returnsStableNineDigitValue() {
    long firstMmsi = SyntheticMmsiGenerator.generateSyntheticMmsi(" drone-42 ");
    long secondMmsi = SyntheticMmsiGenerator.generateSyntheticMmsi("drone-42");

    assertEquals(firstMmsi, secondMmsi);
    assertTrue(firstMmsi >= 980_000_001L);
    assertTrue(firstMmsi <= 989_999_999L);
    assertEquals(9, SyntheticMmsiGenerator.formatMmsi(firstMmsi).length());
  }

  @Test
  void generateSyntheticMmsi_differentTwinIds_returnDifferentValues() {
    long firstMmsi = SyntheticMmsiGenerator.generateSyntheticMmsi("drone-1");
    long secondMmsi = SyntheticMmsiGenerator.generateSyntheticMmsi("drone-2");

    assertTrue(firstMmsi != secondMmsi);
  }

  @Test
  void generateSyntheticMmsi_nullOrBlankTwinId_throws() {
    assertThrows(IllegalArgumentException.class, () -> SyntheticMmsiGenerator.generateSyntheticMmsi(null));
    assertThrows(IllegalArgumentException.class, () -> SyntheticMmsiGenerator.generateSyntheticMmsi(""));
    assertThrows(IllegalArgumentException.class, () -> SyntheticMmsiGenerator.generateSyntheticMmsi(" \t "));
  }

  @Test
  void formatMmsi_acceptsNineDigitBoundaries() {
    assertEquals("100000000", SyntheticMmsiGenerator.formatMmsi(100_000_000L));
    assertEquals("999999999", SyntheticMmsiGenerator.formatMmsi(999_999_999L));
  }

  @Test
  void formatMmsi_rejectsValuesOutsideNineDigitRange() {
    assertThrows(IllegalArgumentException.class, () -> SyntheticMmsiGenerator.formatMmsi(99_999_999L));
    assertThrows(IllegalArgumentException.class, () -> SyntheticMmsiGenerator.formatMmsi(1_000_000_000L));
  }
}
