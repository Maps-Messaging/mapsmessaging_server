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

package io.mapsmessaging.state.mavlink.model.impl.usv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.mapsmessaging.state.drone.model.GeoPosition;
import io.mapsmessaging.state.mavlink.messages.MavlinkCommandInt;
import io.mapsmessaging.state.mavlink.messages.MavlinkCommandIntFactory;
import io.mapsmessaging.state.mavlink.messages.MavlinkCommandLong;
import io.mapsmessaging.state.mavlink.messages.MavlinkCommandLongFactory;
import io.mapsmessaging.state.mavlink.messages.MavlinkMissionItem;
import io.mapsmessaging.state.mavlink.messages.MavlinkMissionItemFactory;
import io.mapsmessaging.state.mavlink.messages.MavlinkMissionItemInt;
import io.mapsmessaging.state.mavlink.messages.MavlinkMissionItemIntFactory;
import io.mapsmessaging.state.mavlink.model.LoiterRequest;
import io.mapsmessaging.state.mavlink.model.MissionPlan;
import io.mapsmessaging.state.mavlink.model.PlanItem;
import io.mapsmessaging.state.mavlink.model.PlanItemType;
import io.mapsmessaging.state.mavlink.model.RepositionRequest;
import io.mapsmessaging.state.mavlink.model.UxvCommandContext;
import io.mapsmessaging.state.mavlink.model.UxvModelCommandSet;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SticklebackArdupilotUsvModelTest {

  private static final UxvCommandContext CONTEXT = new UxvCommandContext(UUID.randomUUID(), 10, 1, 255, 190, 42);

  @Test
  void repositionUsesFixedRelativeAltitudeWithoutMutatingSurfacePosition() {
    SticklebackArdupilotUsvModel model = new SticklebackArdupilotUsvModel();
    GeoPosition position = new GeoPosition(59.4673d, 24.828353d, 123.0d, null);

    UxvModelCommandSet commandSet = model.reposition(CONTEXT, new RepositionRequest(position, null, null));

    assertEquals(3, commandSet.messages().size());

    MavlinkCommandInt reposition = assertInstanceOf(MavlinkCommandInt.class, commandSet.messages().get(0));
    assertEquals(MavlinkCommandIntFactory.MAV_CMD_DO_REPOSITION, reposition.getCommand());
    assertEquals(MavlinkCommandIntFactory.MAV_FRAME_GLOBAL_RELATIVE_ALT_INT, reposition.getFrame());
    assertEquals((float) SticklebackArdupilotUsvModel.MAX_ALTITUDE_METERS, reposition.getAltitude());

    MavlinkCommandLong guidedMode = assertInstanceOf(MavlinkCommandLong.class, commandSet.messages().get(1));
    assertEquals(MavlinkCommandLongFactory.MAV_CMD_DO_SET_MODE, guidedMode.getCommand());
    assertEquals(MavlinkCommandLongFactory.ARDUPLANE_MODE_GUIDED, guidedMode.getParam2());

    MavlinkMissionItem guidedWaypoint = assertInstanceOf(MavlinkMissionItem.class, commandSet.messages().get(2));
    assertEquals(MavlinkMissionItemFactory.MAV_FRAME_GLOBAL_RELATIVE_ALT, guidedWaypoint.getFrame());
    assertEquals((float) SticklebackArdupilotUsvModel.MAX_ALTITUDE_METERS, guidedWaypoint.getAltitude());
    assertEquals(2, guidedWaypoint.getCurrent());

    assertEquals(123.0d, position.getAltitudeMslMeters());
    assertNull(position.getAltitudeAglMeters());
  }

  @Test
  void repositionUsesResolvedAltitudeForEveryCommand() {
    SticklebackArdupilotUsvModel model = new SticklebackArdupilotUsvModel();
    GeoPosition position = new GeoPosition(59.4673d, 24.828353d, 123.0d, null);

    UxvModelCommandSet commandSet =
        model.reposition(
            CONTEXT, new RepositionRequest(position, null, null, 7.5d));

    MavlinkCommandInt reposition =
        assertInstanceOf(MavlinkCommandInt.class, commandSet.messages().get(0));
    MavlinkMissionItem guidedWaypoint =
        assertInstanceOf(MavlinkMissionItem.class, commandSet.messages().get(2));
    assertEquals(7.5f, reposition.getAltitude());
    assertEquals(7.5f, guidedWaypoint.getAltitude());
    assertEquals(123.0d, position.getAltitudeMslMeters());
  }

  @Test
  void repositionRejectsNonFiniteResolvedAltitude() {
    SticklebackArdupilotUsvModel model = new SticklebackArdupilotUsvModel();
    GeoPosition position = new GeoPosition(59.4673d, 24.828353d, null, null);

    assertThrows(
        IllegalArgumentException.class,
        () ->
            model.reposition(
                CONTEXT,
                new RepositionRequest(position, null, null, Double.NaN)));
  }

  @Test
  void repositionAcceptsNegativeResolvedAltitude() {
    SticklebackArdupilotUsvModel model = new SticklebackArdupilotUsvModel();
    GeoPosition position = new GeoPosition(59.4673d, 24.828353d, null, null);

    UxvModelCommandSet commandSet =
        model.reposition(
            CONTEXT, new RepositionRequest(position, null, null, -10.0d));

    MavlinkCommandInt reposition =
        assertInstanceOf(MavlinkCommandInt.class, commandSet.messages().get(0));
    assertEquals(-10.0f, reposition.getAltitude());
  }

  @Test
  void resumeVehicleReentersAutoMissionAfterGuidedIntervention() {
    SticklebackArdupilotUsvModel model = new SticklebackArdupilotUsvModel();

    UxvModelCommandSet commandSet = model.resumeVehicle(CONTEXT);

    MavlinkCommandLong command =
        assertInstanceOf(MavlinkCommandLong.class, commandSet.messages().get(0));
    assertEquals(1, commandSet.messages().size());
    assertEquals(MavlinkCommandLongFactory.MAV_CMD_MISSION_START, command.getCommand());
    assertEquals(0.0f, command.getParam1());
    assertEquals(0.0f, command.getParam2());
  }

  @Test
  void unlimitedLoiterUsesFixedRelativeAltitudeWithoutMutatingPosition() {
    SticklebackArdupilotUsvModel model = new SticklebackArdupilotUsvModel();
    GeoPosition position = new GeoPosition(59.4673d, 24.828353d, 87.0d, null);

    UxvModelCommandSet commandSet = model.loiter(CONTEXT, new LoiterRequest(position, 25.0d, Duration.ZERO, null, null, null));

    MavlinkCommandInt loiter = assertInstanceOf(MavlinkCommandInt.class, commandSet.messages().get(0));
    assertEquals(MavlinkCommandIntFactory.MAV_CMD_NAV_LOITER_UNLIM, loiter.getCommand());
    assertEquals(MavlinkCommandIntFactory.MAV_FRAME_GLOBAL_RELATIVE_ALT_INT, loiter.getFrame());
    assertEquals((float) SticklebackArdupilotUsvModel.MAX_ALTITUDE_METERS, loiter.getAltitude());
    assertEquals(87.0d, position.getAltitudeMslMeters());
  }

  @Test
  void timedLoiterUsesFixedRelativeAltitude() {
    SticklebackArdupilotUsvModel model = new SticklebackArdupilotUsvModel();
    GeoPosition position = new GeoPosition(59.4673d, 24.828353d, null, null);

    UxvModelCommandSet commandSet = model.loiter(CONTEXT, new LoiterRequest(position, 25.0d, Duration.ofSeconds(30), null, null, null));

    MavlinkCommandInt loiter = assertInstanceOf(MavlinkCommandInt.class, commandSet.messages().get(0));
    assertEquals(MavlinkCommandIntFactory.MAV_CMD_NAV_LOITER_TIME, loiter.getCommand());
    assertEquals(MavlinkCommandIntFactory.MAV_FRAME_GLOBAL_RELATIVE_ALT_INT, loiter.getFrame());
    assertEquals((float) SticklebackArdupilotUsvModel.MAX_ALTITUDE_METERS, loiter.getAltitude());
    assertEquals(30.0f, loiter.getParam1());
  }

  @Test
  void missionItemsUseFixedRelativeAltitudeWithoutMutatingPositions() {
    SticklebackArdupilotUsvModel model = new SticklebackArdupilotUsvModel();
    GeoPosition waypointPosition = new GeoPosition(59.4673d, 24.828353d, 123.0d, null);
    GeoPosition loiterPosition = new GeoPosition(59.4680d, 24.8290d, null, 8.0d);
    MissionPlan missionPlan =
        new MissionPlan(
            List.of(
                new PlanItem(PlanItemType.WAYPOINT, waypointPosition, Duration.ZERO, null, null, null, null, null),
                new PlanItem(PlanItemType.LOITER, loiterPosition, Duration.ZERO, 25.0d, null, null, null, null)));

    UxvModelCommandSet commandSet = model.buildMission(CONTEXT, missionPlan);

    assertEquals(3, commandSet.messages().size());

    MavlinkMissionItemInt home = assertInstanceOf(MavlinkMissionItemInt.class, commandSet.messages().get(0));
    assertEquals(0, home.getMissionSequence());
    assertEquals(MavlinkMissionItemIntFactory.MAV_CMD_NAV_WAYPOINT, home.getCommand());
    assertEquals(MavlinkMissionItemIntFactory.MAV_FRAME_GLOBAL_INT, home.getFrame());
    assertEquals(123.0f, home.getAltitude());

    MavlinkMissionItemInt waypoint = assertInstanceOf(MavlinkMissionItemInt.class, commandSet.messages().get(1));
    assertEquals(1, waypoint.getMissionSequence());
    assertEquals(MavlinkMissionItemIntFactory.MAV_CMD_NAV_WAYPOINT, waypoint.getCommand());
    assertEquals(MavlinkMissionItemIntFactory.MAV_FRAME_GLOBAL_RELATIVE_ALT_INT, waypoint.getFrame());
    assertEquals((float) SticklebackArdupilotUsvModel.MAX_ALTITUDE_METERS, waypoint.getAltitude());

    MavlinkMissionItemInt loiter = assertInstanceOf(MavlinkMissionItemInt.class, commandSet.messages().get(2));
    assertEquals(2, loiter.getMissionSequence());
    assertEquals(MavlinkMissionItemIntFactory.MAV_CMD_NAV_LOITER_UNLIM, loiter.getCommand());
    assertEquals(MavlinkMissionItemIntFactory.MAV_FRAME_GLOBAL_RELATIVE_ALT_INT, loiter.getFrame());
    assertEquals((float) SticklebackArdupilotUsvModel.MAX_ALTITUDE_METERS, loiter.getAltitude());

    assertEquals(123.0d, waypointPosition.getAltitudeMslMeters());
    assertEquals(8.0d, loiterPosition.getAltitudeAglMeters());
  }
}
