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

package io.mapsmessaging.state.n2k.msg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.model.GeoPosition;
import java.time.Instant;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class AisMappingSupportTest {

  private static final double DELTA = 0.0000001d;

  @ParameterizedTest
  @MethodSource("corePositionValues")
  void hasCorePosition_requiresMmsiAndFiniteCoordinates(
      Long mmsi,
      Double latitude,
      Double longitude,
      boolean expected) {
    DroneTwin droneTwin = new DroneTwin();
    droneTwin.setMmsi(mmsi);
    if (latitude != null || longitude != null) {
      droneTwin.setGeoPosition(new GeoPosition(latitude, longitude, null, null));
    }

    assertEquals(expected, AisMappingSupport.hasCorePosition(droneTwin));
  }

  static Stream<Arguments> corePositionValues() {
    return Stream.of(
        Arguments.of(null, -33.8d, 151.2d, false),
        Arguments.of(123_456_789L, null, null, false),
        Arguments.of(123_456_789L, null, 151.2d, false),
        Arguments.of(123_456_789L, -33.8d, null, false),
        Arguments.of(123_456_789L, Double.NaN, 151.2d, false),
        Arguments.of(123_456_789L, -33.8d, Double.POSITIVE_INFINITY, false),
        Arguments.of(123_456_789L, -90.0001d, 151.2d, false),
        Arguments.of(123_456_789L, 90.0001d, 151.2d, false),
        Arguments.of(123_456_789L, -33.8d, -180.0001d, false),
        Arguments.of(123_456_789L, -33.8d, 180.0001d, false),
        Arguments.of(123_456_789L, -90.0d, 180.0d, true),
        Arguments.of(123_456_789L, 90.0d, -180.0d, true));
  }

  @Test
  void hasCorePosition_nullTwin_returnsFalse() {
    assertFalse(AisMappingSupport.hasCorePosition(null));
  }

  @Test
  void toSecondOfMinute_handlesPositiveNegativeAndNullInstants() {
    assertNull(AisMappingSupport.toSecondOfMinute(null));
    assertEquals(5L, AisMappingSupport.toSecondOfMinute(Instant.ofEpochSecond(65L)));
    assertEquals(59L, AisMappingSupport.toSecondOfMinute(Instant.ofEpochSecond(-1L)));
  }

  @ParameterizedTest
  @MethodSource("angleValues")
  void toRadians_normalizesFiniteDegreesAndRejectsNonFinite(Double degrees, Double expectedRadians) {
    Double actual = AisMappingSupport.toRadians(degrees);
    if (expectedRadians == null) {
      assertNull(actual);
    } else {
      assertEquals(expectedRadians, actual, DELTA);
    }
  }

  static Stream<Arguments> angleValues() {
    return Stream.of(
        Arguments.of(null, null),
        Arguments.of(Double.NaN, null),
        Arguments.of(Double.POSITIVE_INFINITY, null),
        Arguments.of(-90.0d, Math.toRadians(270.0d)),
        Arguments.of(360.0d, 0.0d),
        Arguments.of(450.0d, Math.toRadians(90.0d)));
  }

  @Test
  void resolveName_appliesConfiguredThenTwinFallbacksAndSanitises() {
    DroneTwin droneTwin = new DroneTwin();
    droneTwin.setTwinId("twin-id");
    droneTwin.setRegistrationId("VH-DRN-01");
    droneTwin.setDisplayName("Survey@Drone   Alpha");

    assertEquals("Configured Name", AisMappingSupport.resolveName(droneTwin, "Configured_Name"));
    assertEquals("Survey Drone Alpha", AisMappingSupport.resolveName(droneTwin, null));

    droneTwin.setDisplayName(" ");
    assertEquals("VH DRN 01", AisMappingSupport.resolveName(droneTwin, null));

    droneTwin.setRegistrationId(null);
    assertEquals("twin id", AisMappingSupport.resolveName(droneTwin, null));
  }

  @Test
  void resolveName_truncatesToTwentyCharacters() {
    DroneTwin droneTwin = new DroneTwin();
    droneTwin.setDisplayName("1234567890123456789012345");

    assertEquals("12345678901234567890", AisMappingSupport.resolveName(droneTwin, null));
  }

  @Test
  void resolveCallsign_prefersTwinThenConfiguredFallbackAndUppercases() {
    DroneTwin droneTwin = new DroneTwin();
    droneTwin.setCallSign("uxv-42");

    assertEquals("UXV 42", AisMappingSupport.resolveCallsign(droneTwin, "config"));

    droneTwin.setCallSign(" ");
    assertEquals("CONFIG", AisMappingSupport.resolveCallsign(droneTwin, "config"));
    assertNull(AisMappingSupport.resolveCallsign(droneTwin, null));
    assertEquals("CONFIG", AisMappingSupport.resolveCallsign(null, "config"));
  }

  @Test
  void resolveVendorId_uppercasesSanitisesAndTruncates() {
    assertNull(AisMappingSupport.resolveVendorId(null));
    assertNull(AisMappingSupport.resolveVendorId("  "));
    assertEquals("MAPS BV", AisMappingSupport.resolveVendorId("maps@bv"));
    assertEquals("ABCDEFG", AisMappingSupport.resolveVendorId("abcdefghij"));
  }

  @Test
  void deriveSequenceId_usesConfiguredValueOrStableTwinHash() {
    DroneTwin droneTwin = new DroneTwin();
    droneTwin.setTwinId("drone-alpha");

    assertEquals(12L, AisMappingSupport.deriveSequenceId(droneTwin, 12L));
    Long derived = AisMappingSupport.deriveSequenceId(droneTwin, null);
    assertTrue(derived >= 0L && derived <= 252L);
    assertEquals(derived, AisMappingSupport.deriveSequenceId(droneTwin, null));
    assertEquals(0L, AisMappingSupport.deriveSequenceId(null, null));
  }

  @Test
  void truncate_emptyAfterSanitising_returnsEmptyForFieldSourceToOmit() {
    assertEquals("", AisMappingSupport.truncate("@@@", 7));
  }
}
