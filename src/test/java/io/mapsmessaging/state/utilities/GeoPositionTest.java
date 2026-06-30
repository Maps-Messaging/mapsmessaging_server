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

package io.mapsmessaging.state.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.mapsmessaging.state.drone.model.GeoPosition;
import io.mapsmessaging.state.util.GeoUtils;
import org.junit.jupiter.api.Test;

class GeoPositionUtilsTest {

  private static final double TOLERANCE_DEGREES = 0.000001d;

  @Test
  void shouldReturnNorthForNullCurrentPosition() {
    GeoPosition destinationPosition = new GeoPosition(1.0d, 1.0d, null, null);

    double bearingDegrees = GeoUtils.bearingDegrees(null, destinationPosition);

    assertEquals(0.0d, bearingDegrees, TOLERANCE_DEGREES);
  }

  @Test
  void shouldReturnNorthForNullDestinationPosition() {
    GeoPosition currentPosition = new GeoPosition(1.0d, 1.0d, null, null);

    double bearingDegrees = GeoUtils.bearingDegrees(currentPosition, null);

    assertEquals(0.0d, bearingDegrees, TOLERANCE_DEGREES);
  }

  @Test
  void shouldReturnNorthForMissingLatitude() {
    GeoPosition currentPosition = new GeoPosition(null, 151.2093d, null, null);
    GeoPosition destinationPosition = new GeoPosition(-33.8688d, 151.2093d, null, null);

    double bearingDegrees = GeoUtils.bearingDegrees(currentPosition, destinationPosition);

    assertEquals(0.0d, bearingDegrees, TOLERANCE_DEGREES);
  }

  @Test
  void shouldReturnNorthForMissingLongitude() {
    GeoPosition currentPosition = new GeoPosition(-33.8688d, null, null, null);
    GeoPosition destinationPosition = new GeoPosition(-33.8688d, 151.2093d, null, null);

    double bearingDegrees = GeoUtils.bearingDegrees(currentPosition, destinationPosition);

    assertEquals(0.0d, bearingDegrees, TOLERANCE_DEGREES);
  }

  @Test
  void shouldReturnNorthForInvalidLatitude() {
    GeoPosition currentPosition = new GeoPosition(-91.0d, 151.2093d, null, null);
    GeoPosition destinationPosition = new GeoPosition(-33.8688d, 151.2093d, null, null);

    double bearingDegrees = GeoUtils.bearingDegrees(currentPosition, destinationPosition);

    assertEquals(0.0d, bearingDegrees, TOLERANCE_DEGREES);
  }

  @Test
  void shouldReturnNorthForInvalidLongitude() {
    GeoPosition currentPosition = new GeoPosition(-33.8688d, 181.0d, null, null);
    GeoPosition destinationPosition = new GeoPosition(-33.8688d, 151.2093d, null, null);

    double bearingDegrees = GeoUtils.bearingDegrees(currentPosition, destinationPosition);

    assertEquals(0.0d, bearingDegrees, TOLERANCE_DEGREES);
  }

  @Test
  void shouldReturnNorthForNaNLatitude() {
    GeoPosition currentPosition = new GeoPosition(Double.NaN, 151.2093d, null, null);
    GeoPosition destinationPosition = new GeoPosition(-33.8688d, 151.2093d, null, null);

    double bearingDegrees = GeoUtils.bearingDegrees(currentPosition, destinationPosition);

    assertEquals(0.0d, bearingDegrees, TOLERANCE_DEGREES);
  }

  @Test
  void shouldReturnNorthForInfiniteLongitude() {
    GeoPosition currentPosition = new GeoPosition(-33.8688d, Double.POSITIVE_INFINITY, null, null);
    GeoPosition destinationPosition = new GeoPosition(-33.8688d, 151.2093d, null, null);

    double bearingDegrees = GeoUtils.bearingDegrees(currentPosition, destinationPosition);

    assertEquals(0.0d, bearingDegrees, TOLERANCE_DEGREES);
  }

  @Test
  void shouldReturnNorthWhenPositionsAreEqual() {
    GeoPosition currentPosition = new GeoPosition(-33.8688d, 151.2093d, null, null);
    GeoPosition destinationPosition = new GeoPosition(-33.8688d, 151.2093d, null, null);

    double bearingDegrees = GeoUtils.bearingDegrees(currentPosition, destinationPosition);

    assertEquals(0.0d, bearingDegrees, TOLERANCE_DEGREES);
  }

  @Test
  void shouldReturnNorthForDestinationDueNorth() {
    GeoPosition currentPosition = new GeoPosition(-34.0d, 151.0d, null, null);
    GeoPosition destinationPosition = new GeoPosition(-33.0d, 151.0d, null, null);

    double bearingDegrees = GeoUtils.bearingDegrees(currentPosition, destinationPosition);

    assertEquals(0.0d, bearingDegrees, TOLERANCE_DEGREES);
  }

  @Test
  void shouldReturnEastForDestinationDueEastOnEquator() {
    GeoPosition currentPosition = new GeoPosition(0.0d, 0.0d, null, null);
    GeoPosition destinationPosition = new GeoPosition(0.0d, 1.0d, null, null);

    double bearingDegrees = GeoUtils.bearingDegrees(currentPosition, destinationPosition);

    assertEquals(90.0d, bearingDegrees, TOLERANCE_DEGREES);
  }

  @Test
  void shouldReturnSouthForDestinationDueSouth() {
    GeoPosition currentPosition = new GeoPosition(-33.0d, 151.0d, null, null);
    GeoPosition destinationPosition = new GeoPosition(-34.0d, 151.0d, null, null);

    double bearingDegrees = GeoUtils.bearingDegrees(currentPosition, destinationPosition);

    assertEquals(180.0d, bearingDegrees, TOLERANCE_DEGREES);
  }

  @Test
  void shouldReturnWestForDestinationDueWestOnEquator() {
    GeoPosition currentPosition = new GeoPosition(0.0d, 1.0d, null, null);
    GeoPosition destinationPosition = new GeoPosition(0.0d, 0.0d, null, null);

    double bearingDegrees = GeoUtils.bearingDegrees(currentPosition, destinationPosition);

    assertEquals(270.0d, bearingDegrees, TOLERANCE_DEGREES);
  }

  @Test
  void shouldReturnExpectedBearingFromSydneyToMelbourne() {
    GeoPosition currentPosition = new GeoPosition(-33.8688d, 151.2093d, null, null);
    GeoPosition destinationPosition = new GeoPosition(-37.8136d, 144.9631d, null, null);

    double bearingDegrees = GeoUtils.bearingDegrees(currentPosition, destinationPosition);

    assertEquals(230.280, bearingDegrees, 0.001d);
  }
}