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

package io.mapsmessaging.state.mavlink.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.mapsmessaging.state.drone.model.GeoPosition;
import io.mapsmessaging.state.mavlink.messages.MavlinkCommandLong;
import io.mapsmessaging.state.mavlink.messages.MavlinkCommandLongFactory;
import io.mapsmessaging.state.mavlink.messages.MavlinkMissionItemInt;
import io.mapsmessaging.state.mavlink.messages.MavlinkMissionItemIntFactory;
import io.mapsmessaging.state.mavlink.model.impl.uav.GenericPx4UavModel;
import io.mapsmessaging.state.mavlink.model.impl.usv.SticklebackArdupilotUsvModel;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ArduPilotMissionSequenceTest {

  private static final UxvCommandContext CONTEXT = new UxvCommandContext(UUID.randomUUID(), 1, 1, 255, 190, 7);
  private static final GeoPosition FIRST = new GeoPosition(59.4877408d, 24.8089485d, 10.0d, null);
  private static final GeoPosition LAST = new GeoPosition(59.4785881d, 24.8086052d, 10.0d, null);

  @Test
  void ardupilotReservesSequenceZeroAndStartsRouteAtSequenceOne() {
    SticklebackArdupilotUsvModel model = new SticklebackArdupilotUsvModel();

    UxvModelCommandSet commandSet = model.buildMission(CONTEXT, new MissionPlan(routeItems()));

    assertEquals(1, model.firstMissionItemSequence());
    assertEquals(3, commandSet.messages().size());
    assertMissionItem(commandSet, 0, 0, FIRST);
    assertMissionItem(commandSet, 1, 1, FIRST);
    assertMissionItem(commandSet, 2, 2, LAST);
  }

  @Test
  void ardupilotRepeatJumpsToFirstRealMissionItem() {
    SticklebackArdupilotUsvModel model = new SticklebackArdupilotUsvModel();

    UxvModelCommandSet commandSet = model.buildMission(CONTEXT, MissionPlan.repeatIndefinitely(routeItems()));

    assertEquals(4, commandSet.messages().size());
    MavlinkMissionItemInt jump = (MavlinkMissionItemInt) commandSet.messages().get(3);
    assertEquals(3, jump.getMissionSequence());
    assertEquals(MavlinkMissionItemIntFactory.MAV_CMD_DO_JUMP, jump.getCommand());
    assertEquals(1.0f, jump.getParam1());
    assertEquals(MavlinkMissionItemIntFactory.MAV_CMD_DO_JUMP_REPEAT_FOREVER, (int) jump.getParam2());
  }

  @Test
  void ardupilotResetsMissionPointerBeforeStartingMission() {
    SticklebackArdupilotUsvModel model = new SticklebackArdupilotUsvModel();

    UxvModelCommandSet commandSet = model.startMission(CONTEXT);

    assertEquals(2, commandSet.messages().size());

    MavlinkCommandLong setCurrent = (MavlinkCommandLong) commandSet.messages().get(0);
    assertEquals(MavlinkCommandLongFactory.MAV_CMD_DO_SET_MISSION_CURRENT, setCurrent.getCommand());
    assertEquals(1.0f, setCurrent.getParam1());
    assertEquals(1.0f, setCurrent.getParam2());

    MavlinkCommandLong start = (MavlinkCommandLong) commandSet.messages().get(1);
    assertEquals(MavlinkCommandLongFactory.MAV_CMD_MISSION_START, start.getCommand());
    assertEquals(0.0f, start.getParam1());
    assertEquals(0.0f, start.getParam2());
  }

  @Test
  void px4KeepsFirstRoutePointAtSequenceZero() {
    GenericPx4UavModel model = new GenericPx4UavModel();

    UxvModelCommandSet commandSet = model.buildMission(CONTEXT, new MissionPlan(routeItems()));

    assertEquals(0, model.firstMissionItemSequence());
    assertEquals(2, commandSet.messages().size());
    assertMissionItem(commandSet, 0, 0, FIRST);
    assertMissionItem(commandSet, 1, 1, LAST);
  }

  @Test
  void px4StartsMissionWithoutArduPilotPointerReset() {
    GenericPx4UavModel model = new GenericPx4UavModel();

    UxvModelCommandSet commandSet = model.startMission(CONTEXT);

    assertEquals(1, commandSet.messages().size());
    MavlinkCommandLong start = (MavlinkCommandLong) commandSet.messages().get(0);
    assertEquals(MavlinkCommandLongFactory.MAV_CMD_MISSION_START, start.getCommand());
  }

  private List<PlanItem> routeItems() {
    return List.of(waypoint(FIRST), waypoint(LAST));
  }

  private PlanItem waypoint(GeoPosition position) {
    return new PlanItem(PlanItemType.WAYPOINT, position, null, null, null, null, null, null);
  }

  private void assertMissionItem(UxvModelCommandSet commandSet, int messageIndex, int missionSequence, GeoPosition position) {
    MavlinkMissionItemInt item = (MavlinkMissionItemInt) commandSet.messages().get(messageIndex);
    assertEquals(missionSequence, item.getMissionSequence());
    assertEquals(MavlinkMissionItemIntFactory.MAV_CMD_NAV_WAYPOINT, item.getCommand());
    assertEquals((int) Math.round(position.getLatitude() * 10_000_000.0d), item.getLatitude());
    assertEquals((int) Math.round(position.getLongitude() * 10_000_000.0d), item.getLongitude());
  }
}
