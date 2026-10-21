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

package io.mapsmessaging.state.mavlink.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.mapsmessaging.state.drone.model.GeoPosition;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class MavlinkMissionItemIntFactoryTest {

  private static final int TARGET_SYSTEM = 2;
  private static final int TARGET_COMPONENT = 1;
  private static final int MISSION_SEQUENCE = 3;

  @Test
  void waypointWithMslAltitudeUsesGlobalIntFrame() {
    MavlinkMissionItemInt item =
        waypoint(new GeoPosition(-33.8688d, 151.2093d, 100.0d, null));

    assertEquals(MavlinkMissionItemIntFactory.MAV_FRAME_GLOBAL_INT, item.getFrame());
    assertEquals(100.0f, item.getAltitude());
  }

  @Test
  void timedLoiterWithMslAltitudeUsesGlobalIntFrame() {
    GeoPosition position = new GeoPosition(-33.8688d, 151.2093d, 100.0d, null);

    MavlinkMissionItemInt item =
        MavlinkMissionItemIntFactory.loiterTime(
            TARGET_SYSTEM,
            TARGET_COMPONENT,
            MISSION_SEQUENCE,
            position,
            Duration.ofSeconds(30),
            50.0d);

    assertEquals(MavlinkMissionItemIntFactory.MAV_FRAME_GLOBAL_INT, item.getFrame());
    assertEquals(100.0f, item.getAltitude());
  }

  @Test
  void waypointWithAglAltitudeUsesTerrainAltitudeFrame() {
    MavlinkMissionItemInt item =
        waypoint(new GeoPosition(-33.8688d, 151.2093d, null, 35.0d));

    assertEquals(
        MavlinkMissionItemIntFactory.MAV_FRAME_GLOBAL_TERRAIN_ALT_INT,
        item.getFrame());
    assertEquals(35.0f, item.getAltitude());
  }

  @Test
  void timedLoiterWithAglAltitudeUsesTerrainAltitudeFrame() {
    GeoPosition position = new GeoPosition(-33.8688d, 151.2093d, null, 35.0d);

    MavlinkMissionItemInt item =
        MavlinkMissionItemIntFactory.loiterTime(
            TARGET_SYSTEM,
            TARGET_COMPONENT,
            MISSION_SEQUENCE,
            position,
            Duration.ofSeconds(30),
            50.0d);

    assertEquals(
        MavlinkMissionItemIntFactory.MAV_FRAME_GLOBAL_TERRAIN_ALT_INT,
        item.getFrame());
    assertEquals(35.0f, item.getAltitude());
  }

  @Test
  void mslAltitudeTakesPrecedenceWhenBothAltitudeTypesArePresent() {
    MavlinkMissionItemInt item =
        waypoint(new GeoPosition(-33.8688d, 151.2093d, 120.0d, 35.0d));

    assertEquals(MavlinkMissionItemIntFactory.MAV_FRAME_GLOBAL_INT, item.getFrame());
    assertEquals(120.0f, item.getAltitude());
  }

  @Test
  void waypointWithoutAltitudeUsesGlobalRelativeAltitudeFrame() {
    MavlinkMissionItemInt item =
        waypoint(new GeoPosition(-33.8688d, 151.2093d, null, null));

    assertEquals(
        MavlinkMissionItemIntFactory.MAV_FRAME_GLOBAL_RELATIVE_ALT_INT,
        item.getFrame());
    assertEquals(0.0f, item.getAltitude());
  }

  @Test
  void waypointPopulatesMissionEnvelopeCoordinatesAndParameters() {
    GeoPosition position = new GeoPosition(-33.8688123d, 151.2093456d, 123.5d, null);

    MavlinkMissionItemInt item =
        MavlinkMissionItemIntFactory.waypoint(
            TARGET_SYSTEM,
            TARGET_COMPONENT,
            MISSION_SEQUENCE,
            position,
            1.5f,
            4.0f,
            2.0f,
            270.0f);

    assertMissionEnvelope(
        item,
        MavlinkMissionItemIntFactory.MAV_FRAME_GLOBAL_INT,
        MavlinkMissionItemIntFactory.MAV_CMD_NAV_WAYPOINT,
        MISSION_SEQUENCE);
    assertEquals(-338_688_123, item.getLatitude());
    assertEquals(1_512_093_456, item.getLongitude());
    assertEquals(123.5f, item.getAltitude());
    assertEquals(1.5f, item.getParam1());
    assertEquals(4.0f, item.getParam2());
    assertEquals(2.0f, item.getParam3());
    assertEquals(270.0f, item.getParam4());
  }

  @Test
  void defaultWaypointUsesExpectedDefaults() {
    MavlinkMissionItemInt item =
        waypoint(new GeoPosition(-33.8688d, 151.2093d, 100.0d, null));

    assertEquals(0.0f, item.getParam1());
    assertEquals(2.0f, item.getParam2());
    assertEquals(0.0f, item.getParam3());
    assertTrue(Float.isNaN(item.getParam4()));
  }

  @Test
  void unlimitedLoiterUsesExpectedParameters() {
    MavlinkMissionItemInt item =
        MavlinkMissionItemIntFactory.loiterUnlimited(
            TARGET_SYSTEM,
            TARGET_COMPONENT,
            MISSION_SEQUENCE,
            new GeoPosition(-33.8688d, 151.2093d, 100.0d, null),
            75.0d);

    assertMissionEnvelope(
        item,
        MavlinkMissionItemIntFactory.MAV_FRAME_GLOBAL_INT,
        MavlinkMissionItemIntFactory.MAV_CMD_NAV_LOITER_UNLIM,
        MISSION_SEQUENCE);
    assertEquals(0.0f, item.getParam1());
    assertEquals(0.0f, item.getParam2());
    assertEquals(75.0f, item.getParam3());
    assertTrue(Float.isNaN(item.getParam4()));
  }

  @Test
  void timedLoiterPreservesFractionalSeconds() {
    MavlinkMissionItemInt item =
        MavlinkMissionItemIntFactory.loiterTime(
            TARGET_SYSTEM,
            TARGET_COMPONENT,
            MISSION_SEQUENCE,
            new GeoPosition(-33.8688d, 151.2093d, 100.0d, null),
            Duration.ofMillis(1500),
            50.0d);

    assertEquals(1.5f, item.getParam1());
    assertEquals(0.0f, item.getParam2());
    assertEquals(50.0f, item.getParam3());
    assertTrue(Float.isNaN(item.getParam4()));
  }

  @Test
  void timedLoiterRejectsNegativeDuration() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            MavlinkMissionItemIntFactory.loiterTime(
                TARGET_SYSTEM,
                TARGET_COMPONENT,
                MISSION_SEQUENCE,
                new GeoPosition(-33.8688d, 151.2093d, 100.0d, null),
                Duration.ofMillis(-1),
                50.0d));
  }

  @Test
  void jumpUsesMissionFrameAndExpectedParameters() {
    MavlinkMissionItemInt item =
        MavlinkMissionItemIntFactory.jump(
            TARGET_SYSTEM,
            TARGET_COMPONENT,
            4,
            0,
            2);

    assertMissionEnvelope(
        item,
        MavlinkMissionItemIntFactory.MAV_FRAME_MISSION,
        MavlinkMissionItemIntFactory.MAV_CMD_DO_JUMP,
        4);
    assertEquals(0.0f, item.getParam1());
    assertEquals(2.0f, item.getParam2());
    assertEquals(0.0f, item.getParam3());
    assertEquals(0.0f, item.getParam4());
    assertEquals(0, item.getLatitude());
    assertEquals(0, item.getLongitude());
    assertEquals(0.0f, item.getAltitude());
  }

  @Test
  void jumpRejectsNegativeTargetSequence() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            MavlinkMissionItemIntFactory.jump(
                TARGET_SYSTEM,
                TARGET_COMPONENT,
                MISSION_SEQUENCE,
                -1,
                2));
  }

  @Test
  void jumpAcceptsRepeatForever() {
    MavlinkMissionItemInt item =
        MavlinkMissionItemIntFactory.jump(
            TARGET_SYSTEM,
            TARGET_COMPONENT,
            MISSION_SEQUENCE,
            0,
            MavlinkMissionItemIntFactory.MAV_CMD_DO_JUMP_REPEAT_FOREVER);

    assertEquals(MavlinkMissionItemIntFactory.MAV_CMD_DO_JUMP_REPEAT_FOREVER, (int) item.getParam2());
  }

  @Test
  void jumpRejectsRepeatCountBelowRepeatForever() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            MavlinkMissionItemIntFactory.jump(
                TARGET_SYSTEM,
                TARGET_COMPONENT,
                MISSION_SEQUENCE,
                0,
                MavlinkMissionItemIntFactory.MAV_CMD_DO_JUMP_REPEAT_FOREVER - 1));
  }

  @Test
  void returnToLaunchUsesMissionFrame() {
    MavlinkMissionItemInt item =
        MavlinkMissionItemIntFactory.returnToLaunch(
            TARGET_SYSTEM,
            TARGET_COMPONENT,
            MISSION_SEQUENCE);

    assertMissionEnvelope(
        item,
        MavlinkMissionItemIntFactory.MAV_FRAME_MISSION,
        MavlinkMissionItemIntFactory.MAV_CMD_NAV_RETURN_TO_LAUNCH,
        MISSION_SEQUENCE);
    assertEquals(0, item.getLatitude());
    assertEquals(0, item.getLongitude());
    assertEquals(0.0f, item.getAltitude());
  }

  @Test
  void boundaryCoordinatesAreAcceptedAndScaledExactly() {
    MavlinkMissionItemInt southWest =
        waypoint(new GeoPosition(-90.0d, -180.0d, 10.0d, null));
    MavlinkMissionItemInt northEast =
        waypoint(new GeoPosition(90.0d, 180.0d, 10.0d, null));

    assertEquals(-900_000_000, southWest.getLatitude());
    assertEquals(-1_800_000_000, southWest.getLongitude());
    assertEquals(900_000_000, northEast.getLatitude());
    assertEquals(1_800_000_000, northEast.getLongitude());
  }

  @Test
  void rejectsLatitudeOutsideValidRange() {
    assertThrows(
        IllegalArgumentException.class,
        () -> waypoint(new GeoPosition(91.0d, 151.2093d, 100.0d, null)));
  }

  @Test
  void rejectsLongitudeOutsideValidRange() {
    assertThrows(
        IllegalArgumentException.class,
        () -> waypoint(new GeoPosition(-33.8688d, 181.0d, 100.0d, null)));
  }

  @Test
  void rejectsMissingCoordinatesAndNullPosition() {
    assertThrows(
        IllegalArgumentException.class,
        () -> waypoint(new GeoPosition(null, 151.2093d, 100.0d, null)));
    assertThrows(
        IllegalArgumentException.class,
        () -> waypoint(new GeoPosition(-33.8688d, null, 100.0d, null)));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            MavlinkMissionItemIntFactory.waypoint(
                TARGET_SYSTEM,
                TARGET_COMPONENT,
                MISSION_SEQUENCE,
                null));
  }

  @Test
  void rejectsNonFiniteCoordinates() {
    assertThrows(
        IllegalArgumentException.class,
        () -> waypoint(new GeoPosition(Double.NaN, 151.2093d, 100.0d, null)));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            waypoint(
                new GeoPosition(
                    -33.8688d,
                    Double.POSITIVE_INFINITY,
                    100.0d,
                    null)));
  }

  @Test
  void rejectsNonFiniteMslAltitude() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            waypoint(
                new GeoPosition(
                    -33.8688d,
                    151.2093d,
                    Double.NaN,
                    null)));
  }

  @Test
  void rejectsNonFiniteAglAltitude() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            waypoint(
                new GeoPosition(
                    -33.8688d,
                    151.2093d,
                    null,
                    Double.POSITIVE_INFINITY)));
  }

  private static MavlinkMissionItemInt waypoint(GeoPosition position) {
    return MavlinkMissionItemIntFactory.waypoint(
        TARGET_SYSTEM,
        TARGET_COMPONENT,
        MISSION_SEQUENCE,
        position);
  }

  private static void assertMissionEnvelope(
      MavlinkMissionItemInt item,
      int expectedFrame,
      int expectedCommand,
      int expectedSequence) {
    assertEquals(TARGET_SYSTEM, item.getTargetSystem());
    assertEquals(TARGET_COMPONENT, item.getTargetComponent());
    assertEquals(expectedSequence, item.getMissionSequence());
    assertEquals(expectedFrame, item.getFrame());
    assertEquals(expectedCommand, item.getCommand());
    assertEquals(0, item.getCurrent());
    assertEquals(1, item.getAutocontinue());
    assertEquals(
        MavlinkMissionItemIntFactory.MAV_MISSION_TYPE_MISSION,
        item.getMissionType());
  }
}