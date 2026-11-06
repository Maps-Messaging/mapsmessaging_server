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

package io.mapsmessaging.state.util;

import io.mapsmessaging.state.drone.model.GeoPosition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GeoUtilsTest {

  @Test
  void distance_same_position_is_zero() {
    GeoPosition position = position(38.444, -9.101);

    assertEquals(0.0, GeoUtils.distanceMeters(position, position), 0.001);
  }

  @Test
  void distance_one_degree_at_equator_is_about_111195_metres() {
    double distance = GeoUtils.distanceMeters(position(0.0, 0.0), position(0.0, 1.0));

    assertEquals(111_194.9, distance, 1.0);
  }

  @Test
  void distance_crossing_antimeridian_uses_short_path() {
    double distance = GeoUtils.distanceMeters(position(0.0, 179.9), position(0.0, -179.9));

    assertEquals(22_239.0, distance, 2.0);
  }

  @Test
  void distance_rejects_missing_positions_or_coordinates() {
    assertThrows(IllegalArgumentException.class, () -> GeoUtils.distanceMeters(null, position(0.0, 0.0)));
    assertThrows(IllegalArgumentException.class, () -> GeoUtils.distanceMeters(position(0.0, 0.0), null));
    assertThrows(IllegalArgumentException.class,
        () -> GeoUtils.distanceMeters(new GeoPosition(null, 0.0, null, null, null), position(0.0, 0.0)));
    assertThrows(IllegalArgumentException.class,
        () -> GeoUtils.distanceMeters(position(0.0, 0.0), new GeoPosition(0.0, null, null, null, null)));
  }

  @Test
  void altitude_delta_uses_preferred_altitude() {
    GeoPosition first = new GeoPosition(0.0, 0.0, 120.0, 35.0, 20.0);
    GeoPosition second = new GeoPosition(0.0, 0.0, 105.5, 10.0, 5.0);

    assertEquals(14.5, GeoUtils.altitudeDeltaMeters(first, second), 0.001);
  }

  @Test
  void altitude_delta_without_altitude_returns_max_value() {
    assertEquals(Double.MAX_VALUE,
        GeoUtils.altitudeDeltaMeters(position(0.0, 0.0), new GeoPosition(0.0, 0.0, 10.0, null, null)));
  }

  @Test
  void bearing_returns_cardinal_directions() {
    GeoPosition origin = position(0.0, 0.0);

    assertEquals(0.0f, GeoUtils.bearingDegrees(origin, position(1.0, 0.0)), 0.01f);
    assertEquals(90.0f, GeoUtils.bearingDegrees(origin, position(0.0, 1.0)), 0.01f);
    assertEquals(180.0f, GeoUtils.bearingDegrees(origin, position(-1.0, 0.0)), 0.01f);
    assertEquals(270.0f, GeoUtils.bearingDegrees(origin, position(0.0, -1.0)), 0.01f);
  }

  @Test
  void bearing_same_position_is_zero() {
    GeoPosition position = position(38.444, -9.101);

    assertEquals(0.0f, GeoUtils.bearingDegrees(position, position));
  }

  @Test
  void bearing_invalid_coordinates_returns_zero() {
    assertEquals(0.0f, GeoUtils.bearingDegrees(null, position(0.0, 0.0)));
    assertEquals(0.0f, GeoUtils.bearingDegrees(position(0.0, 0.0), null));
    assertEquals(0.0f, GeoUtils.bearingDegrees(new GeoPosition(Double.NaN, 0.0, null, null, null), position(0.0, 0.0)));
    assertEquals(0.0f, GeoUtils.bearingDegrees(new GeoPosition(91.0, 0.0, null, null, null), position(0.0, 0.0)));
    assertEquals(0.0f, GeoUtils.bearingDegrees(new GeoPosition(0.0, 181.0, null, null, null), position(0.0, 0.0)));
  }

  private GeoPosition position(double latitude, double longitude) {
    return new GeoPosition(latitude, longitude, null, null, null);
  }
}
