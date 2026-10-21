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

import io.mapsmessaging.state.drone.model.GeoPosition;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class MavlinkMissionItemIntRelativeAltitudeTest {

  private static final int TARGET_SYSTEM = 10;
  private static final int TARGET_COMPONENT = 1;
  private static final int MISSION_SEQUENCE = 3;
  private static final GeoPosition POSITION = new GeoPosition(59.4673d, 24.828353d, 500.0d, null);

  @Test
  void waypointRelativeAltitudeUsesFrameSixAndExplicitAltitude() {
    MavlinkMissionItemInt item =
        MavlinkMissionItemIntFactory.waypointRelativeAltitude(
            TARGET_SYSTEM,
            TARGET_COMPONENT,
            MISSION_SEQUENCE,
            POSITION,
            10.0d,
            0.0f,
            2.0f,
            0.0f,
            Float.NaN);

    assertPositionItem(item, MavlinkMissionItemIntFactory.MAV_CMD_NAV_WAYPOINT, 10.0f);
  }

  @Test
  void unlimitedLoiterRelativeAltitudeUsesFrameSixAndExplicitAltitude() {
    MavlinkMissionItemInt item =
        MavlinkMissionItemIntFactory.loiterUnlimitedRelativeAltitude(
            TARGET_SYSTEM,
            TARGET_COMPONENT,
            MISSION_SEQUENCE,
            POSITION,
            10.0d,
            25.0d);

    assertPositionItem(item, MavlinkMissionItemIntFactory.MAV_CMD_NAV_LOITER_UNLIM, 10.0f);
    assertEquals(25.0f, item.getParam3());
  }

  @Test
  void timedLoiterRelativeAltitudeUsesFrameSixAndDuration() {
    MavlinkMissionItemInt item =
        MavlinkMissionItemIntFactory.loiterTimeRelativeAltitude(
            TARGET_SYSTEM,
            TARGET_COMPONENT,
            MISSION_SEQUENCE,
            POSITION,
            10.0d,
            Duration.ofSeconds(30),
            25.0d);

    assertPositionItem(item, MavlinkMissionItemIntFactory.MAV_CMD_NAV_LOITER_TIME, 10.0f);
    assertEquals(30.0f, item.getParam1());
  }

  @Test
  void relativeAltitudeMissionItemsRejectNonFiniteAltitude() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            MavlinkMissionItemIntFactory.loiterUnlimitedRelativeAltitude(
                TARGET_SYSTEM,
                TARGET_COMPONENT,
                MISSION_SEQUENCE,
                POSITION,
                Double.NaN,
                25.0d));
  }

  private static void assertPositionItem(MavlinkMissionItemInt item, int expectedCommand, float expectedAltitude) {
    assertEquals(TARGET_SYSTEM, item.getTargetSystem());
    assertEquals(TARGET_COMPONENT, item.getTargetComponent());
    assertEquals(MISSION_SEQUENCE, item.getMissionSequence());
    assertEquals(MavlinkMissionItemIntFactory.MAV_FRAME_GLOBAL_RELATIVE_ALT_INT, item.getFrame());
    assertEquals(expectedCommand, item.getCommand());
    assertEquals(expectedAltitude, item.getAltitude());
    assertEquals((int) Math.round(POSITION.getLatitude() * 10_000_000.0d), item.getLatitude());
    assertEquals((int) Math.round(POSITION.getLongitude() * 10_000_000.0d), item.getLongitude());
  }
}
