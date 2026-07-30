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

class MavlinkCommandIntFactoryTest {

  private static final int TARGET_SYSTEM = 2;
  private static final int TARGET_COMPONENT = 1;
  private static final int PACKET_SEQUENCE = 17;
  private static final GeoPosition MSL_POSITION = new GeoPosition(-33.8688d, 151.2093d, 120.0d, null);
  private static final GeoPosition AGL_POSITION = new GeoPosition(-33.8688d, 151.2093d, null, 35.0d);

  @Test
  void repositionUsesGlobalIntForMslAltitude() {
    MavlinkCommandInt command = MavlinkCommandIntFactory.reposition(TARGET_SYSTEM, TARGET_COMPONENT, MSL_POSITION, PACKET_SEQUENCE);

    assertEnvelope(command, MavlinkCommandIntFactory.MAV_FRAME_GLOBAL_INT, MavlinkCommandIntFactory.MAV_CMD_DO_REPOSITION);
    assertEquals(-1.0f, command.getParam1());
    assertEquals(1.0f, command.getParam2());
    assertTrue(Float.isNaN(command.getParam3()));
    assertTrue(Float.isNaN(command.getParam4()));
    assertPosition(command, MSL_POSITION, 120.0f);
  }

  @Test
  void repositionUsesTerrainFrameForAglAltitude() {
    MavlinkCommandInt command = MavlinkCommandIntFactory.reposition(TARGET_SYSTEM, TARGET_COMPONENT, AGL_POSITION, PACKET_SEQUENCE);

    assertEnvelope(command, MavlinkCommandIntFactory.MAV_FRAME_GLOBAL_TERRAIN_ALT_INT, MavlinkCommandIntFactory.MAV_CMD_DO_REPOSITION);
    assertPosition(command, AGL_POSITION, 35.0f);
  }

  @Test
  void repositionRelativeAltitudeUsesExplicitHomeRelativeAltitude() {
    MavlinkCommandInt command =
        MavlinkCommandIntFactory.repositionRelativeAltitude(TARGET_SYSTEM, TARGET_COMPONENT, MSL_POSITION, 10.0d, PACKET_SEQUENCE);

    assertEnvelope(command, MavlinkCommandIntFactory.MAV_FRAME_GLOBAL_RELATIVE_ALT_INT, MavlinkCommandIntFactory.MAV_CMD_DO_REPOSITION);
    assertPosition(command, MSL_POSITION, 10.0f);
  }

  @Test
  void unlimitedLoiterUsesMatchingMslFrameAndNormalisesYaw() {
    MavlinkCommandInt command =
        MavlinkCommandIntFactory.loiterUnlimited(TARGET_SYSTEM, TARGET_COMPONENT, MSL_POSITION, 75.0d, -90.0f, PACKET_SEQUENCE);

    assertEnvelope(command, MavlinkCommandIntFactory.MAV_FRAME_GLOBAL_INT, MavlinkCommandIntFactory.MAV_CMD_NAV_LOITER_UNLIM);
    assertEquals(0.0f, command.getParam1());
    assertEquals(0.0f, command.getParam2());
    assertEquals(75.0f, command.getParam3());
    assertEquals(270.0f, command.getParam4());
    assertPosition(command, MSL_POSITION, 120.0f);
  }

  @Test
  void unlimitedLoiterRelativeAltitudeIgnoresPositionAltitude() {
    MavlinkCommandInt command =
        MavlinkCommandIntFactory.loiterUnlimitedRelativeAltitude(
            TARGET_SYSTEM,
            TARGET_COMPONENT,
            MSL_POSITION,
            10.0d,
            50.0d,
            Float.NaN,
            PACKET_SEQUENCE);

    assertEnvelope(command, MavlinkCommandIntFactory.MAV_FRAME_GLOBAL_RELATIVE_ALT_INT, MavlinkCommandIntFactory.MAV_CMD_NAV_LOITER_UNLIM);
    assertTrue(Float.isNaN(command.getParam4()));
    assertPosition(command, MSL_POSITION, 10.0f);
  }

  @Test
  void timedLoiterUsesFractionalDuration() {
    MavlinkCommandInt command =
        MavlinkCommandIntFactory.loiterTime(TARGET_SYSTEM, TARGET_COMPONENT, AGL_POSITION, 60.0d, Duration.ofMillis(1500), PACKET_SEQUENCE);

    assertEnvelope(command, MavlinkCommandIntFactory.MAV_FRAME_GLOBAL_TERRAIN_ALT_INT, MavlinkCommandIntFactory.MAV_CMD_NAV_LOITER_TIME);
    assertEquals(1.5f, command.getParam1());
    assertEquals(0.0f, command.getParam2());
    assertEquals(60.0f, command.getParam3());
    assertTrue(Float.isNaN(command.getParam4()));
    assertPosition(command, AGL_POSITION, 35.0f);
  }

  @Test
  void timedLoiterRejectsNegativeDuration() {
    assertThrows(
        IllegalArgumentException.class,
        () -> MavlinkCommandIntFactory.loiterTime(TARGET_SYSTEM, TARGET_COMPONENT, MSL_POSITION, 60.0d, Duration.ofMillis(-1), PACKET_SEQUENCE));
  }

  @Test
  void relativeAltitudeCommandsRejectNonFiniteAltitude() {
    assertThrows(
        IllegalArgumentException.class,
        () -> MavlinkCommandIntFactory.repositionRelativeAltitude(TARGET_SYSTEM, TARGET_COMPONENT, MSL_POSITION, Double.NaN, PACKET_SEQUENCE));
  }

  @Test
  void orbitPopulatesRadiusAndYawBehaviour() {
    MavlinkCommandInt command = MavlinkCommandIntFactory.orbit(TARGET_SYSTEM, TARGET_COMPONENT, MSL_POSITION, -125.0d, PACKET_SEQUENCE);

    assertEnvelope(command, MavlinkCommandIntFactory.MAV_FRAME_GLOBAL, MavlinkCommandIntFactory.MAV_CMD_DO_ORBIT);
    assertEquals(-125.0f, command.getParam1());
    assertTrue(Float.isNaN(command.getParam2()));
    assertEquals(MavlinkCommandIntFactory.ORBIT_YAW_BEHAVIOUR_HOLD_FRONT_TO_CIRCLE_CENTER, command.getParam3());
    assertTrue(Float.isNaN(command.getParam4()));
    assertPosition(command, MSL_POSITION, 120.0f);
  }

  @Test
  void positionalCommandsRejectNullPosition() {
    assertThrows(IllegalArgumentException.class, () -> MavlinkCommandIntFactory.reposition(TARGET_SYSTEM, TARGET_COMPONENT, null, PACKET_SEQUENCE));
    assertThrows(
        IllegalArgumentException.class,
        () -> MavlinkCommandIntFactory.loiterUnlimited(TARGET_SYSTEM, TARGET_COMPONENT, null, 50.0d, Float.NaN, PACKET_SEQUENCE));
  }

  private static void assertEnvelope(MavlinkCommandInt command, int expectedFrame, int expectedCommand) {
    assertEquals(TARGET_SYSTEM, command.getTargetSystem());
    assertEquals(TARGET_COMPONENT, command.getTargetComponent());
    assertEquals(PACKET_SEQUENCE, command.getSequence());
    assertEquals(expectedFrame, command.getFrame());
    assertEquals(expectedCommand, command.getCommand());
    assertEquals(0, command.getCurrent());
    assertEquals(0, command.getAutocontinue());
  }

  private static void assertPosition(MavlinkCommandInt command, GeoPosition position, float expectedAltitude) {
    assertEquals((int) Math.round(position.getLatitude() * 10_000_000.0d), command.getLatitude());
    assertEquals((int) Math.round(position.getLongitude() * 10_000_000.0d), command.getLongitude());
    assertEquals(expectedAltitude, command.getAltitude());
  }
}
