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
import org.junit.jupiter.api.Test;

class MavlinkMissionItemFactoryTest {

  private static final int TARGET_SYSTEM = 10;
  private static final int TARGET_COMPONENT = 1;

  @Test
  void guidedWaypointUsesGlobalFrameForMslAltitude() {
    GeoPosition position = new GeoPosition(59.4673d, 24.828353d, 120.0d, null);

    MavlinkMissionItem item = MavlinkMissionItemFactory.guidedWaypoint(TARGET_SYSTEM, TARGET_COMPONENT, position);

    assertWaypoint(item, MavlinkMissionItemFactory.MAV_FRAME_GLOBAL, 120.0f);
  }

  @Test
  void guidedWaypointUsesTerrainFrameForAglAltitude() {
    GeoPosition position = new GeoPosition(59.4673d, 24.828353d, null, 8.5d);

    MavlinkMissionItem item = MavlinkMissionItemFactory.guidedWaypoint(TARGET_SYSTEM, TARGET_COMPONENT, position);

    assertWaypoint(item, MavlinkMissionItemFactory.MAV_FRAME_GLOBAL_TERRAIN_ALT, 8.5f);
  }

  @Test
  void guidedWaypointWithoutAltitudeUsesZeroRelativeAltitude() {
    GeoPosition position = new GeoPosition(59.4673d, 24.828353d, null, null);

    MavlinkMissionItem item = MavlinkMissionItemFactory.guidedWaypoint(TARGET_SYSTEM, TARGET_COMPONENT, position);

    assertWaypoint(item, MavlinkMissionItemFactory.MAV_FRAME_GLOBAL_RELATIVE_ALT, 0.0f);
  }

  @Test
  void guidedWaypointRelativeAltitudeUsesExplicitAltitude() {
    GeoPosition position = new GeoPosition(59.4673d, 24.828353d, 500.0d, null);

    MavlinkMissionItem item = MavlinkMissionItemFactory.guidedWaypointRelativeAltitude(TARGET_SYSTEM, TARGET_COMPONENT, position, 10.0d);

    assertWaypoint(item, MavlinkMissionItemFactory.MAV_FRAME_GLOBAL_RELATIVE_ALT, 10.0f);
  }

  @Test
  void guidedWaypointRelativeAltitudeRejectsNonFiniteAltitude() {
    GeoPosition position = new GeoPosition(59.4673d, 24.828353d, null, null);

    assertThrows(
        IllegalArgumentException.class,
        () -> MavlinkMissionItemFactory.guidedWaypointRelativeAltitude(TARGET_SYSTEM, TARGET_COMPONENT, position, Double.NaN));
  }

  private static void assertWaypoint(MavlinkMissionItem item, int expectedFrame, float expectedAltitude) {
    assertEquals(TARGET_SYSTEM, item.getTargetSystem());
    assertEquals(TARGET_COMPONENT, item.getTargetComponent());
    assertEquals(0, item.getMissionSequence());
    assertEquals(expectedFrame, item.getFrame());
    assertEquals(MavlinkMissionItemFactory.MAV_CMD_NAV_WAYPOINT, item.getCommand());
    assertEquals(2, item.getCurrent());
    assertEquals(1, item.getAutocontinue());
    assertEquals(MavlinkMissionItemFactory.MAV_MISSION_TYPE_MISSION, item.getMissionType());
    assertEquals(59.4673f, item.getLatitude());
    assertEquals(24.828353f, item.getLongitude());
    assertEquals(expectedAltitude, item.getAltitude());
  }
}
