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

package io.mapsmessaging.state.mavlink.model.impl.uav;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.mapsmessaging.state.drone.model.GeoPosition;
import io.mapsmessaging.state.mavlink.messages.MavlinkMissionItemInt;
import io.mapsmessaging.state.mavlink.messages.MavlinkMissionItemIntFactory;
import io.mapsmessaging.state.mavlink.model.MissionPlan;
import io.mapsmessaging.state.mavlink.model.PlanItem;
import io.mapsmessaging.state.mavlink.model.PlanItemType;
import io.mapsmessaging.state.mavlink.model.UxvCommandContext;
import io.mapsmessaging.state.mavlink.model.UxvModelCommandSet;
import io.mapsmessaging.state.mavlink.model.UxvOperation;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class GenericPx4UavModelMissionCompilationTest {

  private static final int TARGET_SYSTEM = 2;
  private static final int TARGET_COMPONENT = 1;

  private final GenericPx4UavModel model = new GenericPx4UavModel();

  @Test
  void compilesMixedMissionWithExactOrderingFramesCoordinatesAndParameters() {
    MissionPlan missionPlan =
        new MissionPlan(
            List.of(
                waypoint(
                    new GeoPosition(-33.8688123d, 151.2093456d, 120.0d, null),
                    Duration.ofMillis(1500),
                    4.0d,
                    -90.0f,
                    null),
                waypoint(
                    new GeoPosition(-33.8695d, 151.2102d, null, 45.0d),
                    null,
                    null,
                    null,
                    null),
                loiter(
                    new GeoPosition(-33.8700d, 151.2110d, 130.0d, null),
                    Duration.ofMillis(10_250),
                    30.0d),
                returnToHome()));

    UxvModelCommandSet commandSet = model.buildMission(context(), missionPlan);

    assertEquals(UxvOperation.BUILD_MISSION, commandSet.operation());
    assertEquals(GenericPx4UavModel.MODEL_NAME, commandSet.modelName());
    assertEquals(4, commandSet.messages().size());

    MavlinkMissionItemInt first = item(commandSet, 0);
    assertEnvelope(
        first,
        0,
        MavlinkMissionItemIntFactory.MAV_FRAME_GLOBAL_INT,
        MavlinkMissionItemIntFactory.MAV_CMD_NAV_WAYPOINT);
    assertEquals(-338_688_123, first.getLatitude());
    assertEquals(1_512_093_456, first.getLongitude());
    assertEquals(120.0f, first.getAltitude());
    assertEquals(1.5f, first.getParam1());
    assertEquals(4.0f, first.getParam2());
    assertEquals(0.0f, first.getParam3());
    assertEquals(270.0f, first.getParam4());

    MavlinkMissionItemInt second = item(commandSet, 1);
    assertEnvelope(
        second,
        1,
        MavlinkMissionItemIntFactory.MAV_FRAME_GLOBAL_TERRAIN_ALT_INT,
        MavlinkMissionItemIntFactory.MAV_CMD_NAV_WAYPOINT);
    assertEquals(45.0f, second.getAltitude());
    assertEquals(0.0f, second.getParam1());
    assertEquals(2.0f, second.getParam2());
    assertEquals(0.0f, second.getParam3());
    assertTrue(Float.isNaN(second.getParam4()));

    MavlinkMissionItemInt third = item(commandSet, 2);
    assertEnvelope(
        third,
        2,
        MavlinkMissionItemIntFactory.MAV_FRAME_GLOBAL_INT,
        MavlinkMissionItemIntFactory.MAV_CMD_NAV_LOITER_TIME);
    assertEquals(10.25f, third.getParam1());
    assertEquals(0.0f, third.getParam2());
    assertEquals(30.0f, third.getParam3());
    assertTrue(Float.isNaN(third.getParam4()));

    MavlinkMissionItemInt fourth = item(commandSet, 3);
    assertEnvelope(
        fourth,
        3,
        MavlinkMissionItemIntFactory.MAV_FRAME_MISSION,
        MavlinkMissionItemIntFactory.MAV_CMD_NAV_RETURN_TO_LAUNCH);
    assertEquals(0, fourth.getLatitude());
    assertEquals(0, fourth.getLongitude());
    assertEquals(0.0f, fourth.getAltitude());
  }

  @Test
  void planItemAltitudeOverrideIsCompiledAsMslAltitude() {
    MissionPlan missionPlan =
        new MissionPlan(
            List.of(
                waypoint(
                    new GeoPosition(-33.8688d, 151.2093d, null, 40.0d),
                    null,
                    null,
                    null,
                    150.0d)));

    MavlinkMissionItemInt item =
        item(model.buildMission(context(), missionPlan), 0);

    assertEquals(MavlinkMissionItemIntFactory.MAV_FRAME_GLOBAL_INT, item.getFrame());
    assertEquals(150.0f, item.getAltitude());
  }

  @Test
  void zeroDurationLoiterCompilesAsUnlimitedLoiter() {
    MissionPlan missionPlan =
        new MissionPlan(
            List.of(
                loiter(
                    new GeoPosition(-33.8688d, 151.2093d, 120.0d, null),
                    Duration.ZERO,
                    25.0d)));

    MavlinkMissionItemInt item =
        item(model.buildMission(context(), missionPlan), 0);

    assertEquals(MavlinkMissionItemIntFactory.MAV_CMD_NAV_LOITER_UNLIM, item.getCommand());
    assertEquals(25.0f, item.getParam3());
  }

  private static PlanItem waypoint(
      GeoPosition position,
      Duration holdDuration,
      Double radiusMeters,
      Float yawDegrees,
      Double altitudeMeters) {
    return new PlanItem(
        PlanItemType.WAYPOINT,
        position,
        holdDuration,
        radiusMeters,
        yawDegrees,
        null,
        altitudeMeters,
        null);
  }

  private static PlanItem loiter(
      GeoPosition position,
      Duration duration,
      Double radiusMeters) {
    return new PlanItem(
        PlanItemType.LOITER,
        position,
        duration,
        radiusMeters,
        null,
        null,
        null,
        null);
  }

  private static PlanItem returnToHome() {
    return new PlanItem(
        PlanItemType.RETURN_TO_HOME,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  private static MavlinkMissionItemInt item(
      UxvModelCommandSet commandSet,
      int index) {
    return (MavlinkMissionItemInt) commandSet.messages().get(index);
  }

  private static void assertEnvelope(
      MavlinkMissionItemInt item,
      int expectedMissionSequence,
      int expectedFrame,
      int expectedCommand) {
    assertEquals(TARGET_SYSTEM, item.getTargetSystem());
    assertEquals(TARGET_COMPONENT, item.getTargetComponent());
    assertEquals(expectedMissionSequence, item.getMissionSequence());
    assertEquals(expectedFrame, item.getFrame());
    assertEquals(expectedCommand, item.getCommand());
    assertEquals(0, item.getCurrent());
    assertEquals(1, item.getAutocontinue());
    assertEquals(MavlinkMissionItemIntFactory.MAV_MISSION_TYPE_MISSION, item.getMissionType());
  }
  private static UxvCommandContext context() {
    UxvCommandContext context = mock(UxvCommandContext.class);
    when(context.targetSystem()).thenReturn(TARGET_SYSTEM);
    when(context.targetComponent()).thenReturn(TARGET_COMPONENT);
    when(context.sequence()).thenReturn(7);
    return context;
  }

}
