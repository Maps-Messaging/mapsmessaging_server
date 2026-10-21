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

class PositionTypeTest {

  @Test
  void northAndEastPositionsArePositiveDecimalDegrees() {
    PositionType north = new PositionType("4916.45", "n");
    PositionType east = new PositionType("12311.12", "E");

    assertEquals('N', north.getDirection());
    assertEquals(49.0 + (16.45 / 60.0), north.getPosition(), 0.000001);

    assertEquals('E', east.getDirection());
    assertEquals(123.0 + (11.12 / 60.0), east.getPosition(), 0.000001);
  }

  @Test
  void southAndWestPositionsAreNegativeDecimalDegrees() {
    PositionType south = new PositionType("3450.00", "S");
    PositionType west = new PositionType("05822.50", "w");

    assertEquals(-(34.0 + (50.0 / 60.0)), south.getPosition(), 0.000001);
    assertEquals(-(58.0 + (22.5 / 60.0)), west.getPosition(), 0.000001);
  }

  @Test
  void emptyValuesProduceNeutralPositionAndDirection() {
    PositionType position = new PositionType("", "");

    assertEquals(' ', position.getDirection());
    assertEquals(0.0, position.getPosition(), 0.0);
    assertSame(position, position.jsonPack());
  }

  @Test
  void stringRenderingRetainsCoordinateAndDirection() {
    PositionType position = new PositionType("4916.45", "N");

    assertTrue(position.toString().startsWith("4916."));
    assertTrue(position.toString().endsWith(",N"));
  }
}
