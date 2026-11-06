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

package io.mapsmessaging.network.protocol.impl.nmea.types;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HeightTypeTest {

  @Test
  void parsesHeightAndNormalisesUnitCase() {
    HeightType height = new HeightType("123.45", "m");

    assertEquals(123.45, height.getHeight(), 0.0);
    assertEquals('M', height.getUnit());
    assertEquals("123.45,M", height.toString());
    assertEquals("123.45 M", height.jsonPack());
  }

  @Test
  void emptyUnitDefaultsToMetres() {
    HeightType height = new HeightType("10", "");

    assertEquals(10.0, height.getHeight(), 0.0);
    assertEquals('M', height.getUnit());
  }

  @Test
  void blankOrMalformedHeightFallsBackToZero() {
    assertEquals(0.0, new HeightType("", "M").getHeight(), 0.0);
    assertEquals(0.0, new HeightType("not-a-number", "F").getHeight(), 0.0);
  }
}
