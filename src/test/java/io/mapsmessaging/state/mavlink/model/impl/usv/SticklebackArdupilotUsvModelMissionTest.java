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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.mapsmessaging.state.drone.model.GeoPosition;
import io.mapsmessaging.state.mavlink.messages.MavlinkMissionItemInt;
import io.mapsmessaging.state.mavlink.messages.MavlinkMissionItemIntFactory;
import io.mapsmessaging.state.mavlink.model.MissionPlan;
import io.mapsmessaging.state.mavlink.model.PlanItem;
import io.mapsmessaging.state.mavlink.model.PlanItemType;
import io.mapsmessaging.state.mavlink.model.UxvCommandContext;
import io.mapsmessaging.state.mavlink.model.UxvModelCommandSet;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SticklebackArdupilotUsvModelMissionTest {

  private static final UxvCommandContext CONTEXT = new UxvCommandContext(UUID.randomUUID(), 10, 1, 255, 190, 42);

  private final SticklebackArdupilotUsvModel model = new SticklebackArdupilotUsvModel();

  @Test
  void compilesWaypointWithHoldRadiusAndYaw() {
    PlanItem waypoint =
        new PlanItem(
            PlanItemType.WAYPOINT,
            new GeoPosition(59.434079d, 24.747487d, null, null),
            Duration.ofMillis(1500),
            6.0d,
            -90.0f,
            null,
            null,
            null);

    MavlinkMissionItemInt item = item(model.buildMission(CONTEXT, new MissionPlan(List.of(waypoint))), 1);

    assertEquals(1, item.getMissionSequence());
    assertEquals(MavlinkMissionItemIntFactory.MAV_CMD_NAV_WAYPOINT, item.getCommand());
    assertEquals(MavlinkMissionItemIntFactory.MAV_FRAME_GLOBAL_RELATIVE_ALT_INT, item.getFrame());
    assertEquals(594_340_790, item.getLatitude());
    assertEquals(247_474_870, item.getLongitude());
    assertEquals((float) SticklebackArdupilotUsvModel.MAX_ALTITUDE_METERS, item.getAltitude());
    assertEquals(1.5f, item.getParam1());
    assertEquals(6.0f, item.getParam2());
    assertEquals(270.0f, item.getParam4());
  }

  @Test
  void zeroDurationLoiterCompilesAsUnlimitedLoiter() {
    MavlinkMissionItemInt item = item(model.buildMission(CONTEXT, new MissionPlan(List.of(loiter(Duration.ZERO, 40.0d, null)))), 1);

    assertEquals(1, item.getMissionSequence());
    assertEquals(MavlinkMissionItemIntFactory.MAV_CMD_NAV_LOITER_UNLIM, item.getCommand());
    assertEquals((float) SticklebackArdupilotUsvModel.MAX_ALTITUDE_METERS, item.getAltitude());
    assertEquals(40.0f, item.getParam3());
    assertTrue(Float.isNaN(item.getParam4()));
  }

  @Test
  void timedLoiterPreservesFractionalDuration() {
    MavlinkMissionItemInt item = item(model.buildMission(CONTEXT, new MissionPlan(List.of(loiter(Duration.ofMillis(2500), 45.0d, null)))), 1);

    assertEquals(1, item.getMissionSequence());
    assertEquals(MavlinkMissionItemIntFactory.MAV_CMD_NAV_LOITER_TIME, item.getCommand());
    assertEquals((float) SticklebackArdupilotUsvModel.MAX_ALTITUDE_METERS, item.getAltitude());
    assertEquals(2.5f, item.getParam1());
    assertEquals(45.0f, item.getParam3());
    assertTrue(Float.isNaN(item.getParam4()));
  }

  @Test
  void repeatingUsvRouteAppendsCorrectJump() {
    MissionPlan missionPlan = new MissionPlan(List.of(waypoint(59.434079d, 24.747487d), loiter(Duration.ofSeconds(5), 40.0d, null)), 3);

    UxvModelCommandSet commandSet = model.buildMission(CONTEXT, missionPlan);

    assertEquals(4, commandSet.messages().size());
    assertEquals(MavlinkMissionItemIntFactory.MAV_CMD_NAV_WAYPOINT, item(commandSet, 1).getCommand());
    assertEquals(MavlinkMissionItemIntFactory.MAV_CMD_NAV_LOITER_TIME, item(commandSet, 2).getCommand());

    MavlinkMissionItemInt jump = item(commandSet, 3);
    assertEquals(3, jump.getMissionSequence());
    assertEquals(MavlinkMissionItemIntFactory.MAV_CMD_DO_JUMP, jump.getCommand());
    assertEquals(1.0f, jump.getParam1());
    assertEquals(2.0f, jump.getParam2());
  }

  @Test
  void rejectsUnsupportedMissionItemsAndFields() {
    assertFalse(model.validateMission(new MissionPlan(List.of(positionItem(PlanItemType.ORBIT)))).valid());
    assertFalse(model.validateMission(new MissionPlan(List.of(positionItem(PlanItemType.HOLD_POSITION)))).valid());
    assertFalse(model.validateMission(new MissionPlan(List.of(loiter(Duration.ZERO, 40.0d, 90.0f)))).valid());

    assertFalse(
        model
            .validateMission(
                new MissionPlan(
                    List.of(
                        new PlanItem(
                            PlanItemType.WAYPOINT,
                            new GeoPosition(59.434079d, 24.747487d, null, null),
                            null,
                            null,
                            null,
                            5.0d,
                            null,
                            null))))
            .valid());

    assertFalse(
        model
            .validateMission(
                new MissionPlan(
                    List.of(
                        new PlanItem(
                            PlanItemType.WAYPOINT,
                            new GeoPosition(59.434079d, 24.747487d, null, null),
                            null,
                            null,
                            null,
                            null,
                            10.0d,
                            null))))
            .valid());

    assertFalse(
        model
            .validateMission(
                new MissionPlan(
                    List.of(
                        new PlanItem(
                            PlanItemType.WAYPOINT,
                            new GeoPosition(59.434079d, 24.747487d, null, null),
                            null,
                            null,
                            null,
                            null,
                            null,
                            2.0d))))
            .valid());
  }

  private static PlanItem waypoint(double latitude, double longitude) {
    return new PlanItem(PlanItemType.WAYPOINT, new GeoPosition(latitude, longitude, null, null), null, null, null, null, null, null);
  }

  private static PlanItem loiter(Duration duration, Double radiusMeters, Float yawDegrees) {
    return new PlanItem(PlanItemType.LOITER, new GeoPosition(59.434079d, 24.747487d, null, null), duration, radiusMeters, yawDegrees, null, null, null);
  }

  private static PlanItem positionItem(PlanItemType type) {
    return new PlanItem(type, new GeoPosition(59.434079d, 24.747487d, null, null), null, 40.0d, null, null, null, null);
  }

  private static MavlinkMissionItemInt item(UxvModelCommandSet commandSet, int index) {
    return (MavlinkMissionItemInt) commandSet.messages().get(index);
  }
}