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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.mapsmessaging.state.drone.model.GeoPosition;
import io.mapsmessaging.state.mavlink.messages.MavlinkMissionItemInt;
import io.mapsmessaging.state.mavlink.messages.MavlinkMissionItemIntFactory;
import io.mapsmessaging.state.mavlink.model.FixedWingUavModel;
import io.mapsmessaging.state.mavlink.model.MissionPlan;
import io.mapsmessaging.state.mavlink.model.PlanItem;
import io.mapsmessaging.state.mavlink.model.PlanItemType;
import io.mapsmessaging.state.mavlink.model.UxvCommandContext;
import io.mapsmessaging.state.mavlink.model.UxvModelCommandSet;
import io.mapsmessaging.state.mavlink.model.UxvVehicleType;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class GenericPx4FixedWingUavModelTest {

  private static final int TARGET_SYSTEM = 2;
  private static final int TARGET_COMPONENT = 1;

  private final GenericPx4FixedWingUavModel model =
      new GenericPx4FixedWingUavModel();

  @Test
  void identifiesAsFixedWingUavModel() {
    assertInstanceOf(FixedWingUavModel.class, model);
    assertEquals(UxvVehicleType.UAV, model.getVehicleType());
    assertEquals(GenericPx4FixedWingUavModel.MODEL_NAME, model.getModelName());
    assertEquals(
        GenericPx4FixedWingUavModel.DEFAULT_LOITER_RADIUS_METERS,
        model.getDefaultLoiterRadiusMeters());
  }

  @Test
  void waypointWithoutHoldRemainsWaypoint() {
    MavlinkMissionItemInt item =
        onlyMissionItem(
            new MissionPlan(
                List.of(
                    waypoint(
                        new GeoPosition(-33.8688d, 151.2093d, 120.0d, null),
                        Duration.ZERO,
                        12.0d,
                        -90.0f,
                        null))));

    assertEnvelope(
        item,
        0,
        MavlinkMissionItemIntFactory.MAV_FRAME_GLOBAL_INT,
        MavlinkMissionItemIntFactory.MAV_CMD_NAV_WAYPOINT);
    assertEquals(0.0f, item.getParam1());
    assertEquals(12.0f, item.getParam2());
    assertEquals(0.0f, item.getParam3());
    assertEquals(270.0f, item.getParam4());
  }

  @Test
  void timedWaypointBecomesTimedLoiter() {
    MavlinkMissionItemInt item =
        onlyMissionItem(
            new MissionPlan(
                List.of(
                    waypoint(
                        new GeoPosition(-33.8688d, 151.2093d, 120.0d, null),
                        Duration.ofSeconds(30),
                        null,
                        null,
                        null))));

    assertEnvelope(
        item,
        0,
        MavlinkMissionItemIntFactory.MAV_FRAME_GLOBAL_INT,
        MavlinkMissionItemIntFactory.MAV_CMD_NAV_LOITER_TIME);
    assertEquals(30.0f, item.getParam1());
    assertEquals(0.0f, item.getParam2());
    assertEquals(
        (float) GenericPx4FixedWingUavModel.DEFAULT_LOITER_RADIUS_METERS,
        item.getParam3());
    assertTrue(Float.isNaN(item.getParam4()));
  }

  @Test
  void timedWaypointUsesExplicitLoiterRadius() {
    MavlinkMissionItemInt item =
        onlyMissionItem(
            new MissionPlan(
                List.of(
                    waypoint(
                        new GeoPosition(-33.8688d, 151.2093d, 120.0d, null),
                        Duration.ofMillis(1500),
                        75.0d,
                        null,
                        null))));

    assertEquals(MavlinkMissionItemIntFactory.MAV_CMD_NAV_LOITER_TIME, item.getCommand());
    assertEquals(1.5f, item.getParam1());
    assertEquals(75.0f, item.getParam3());
  }

  @Test
  void timedWaypointUsesAglFrameWhenAglAltitudeIsSupplied() {
    MavlinkMissionItemInt item =
        onlyMissionItem(
            new MissionPlan(
                List.of(
                    waypoint(
                        new GeoPosition(-33.8688d, 151.2093d, null, 45.0d),
                        Duration.ofSeconds(20),
                        60.0d,
                        null,
                        null))));

    assertEquals(
        MavlinkMissionItemIntFactory.MAV_FRAME_GLOBAL_TERRAIN_ALT_INT,
        item.getFrame());
    assertEquals(45.0f, item.getAltitude());
  }

  @Test
  void planItemAltitudeOverrideUsesMslFrame() {
    MavlinkMissionItemInt item =
        onlyMissionItem(
            new MissionPlan(
                List.of(
                    waypoint(
                        new GeoPosition(-33.8688d, 151.2093d, null, 45.0d),
                        Duration.ofSeconds(20),
                        60.0d,
                        null,
                        150.0d))));

    assertEquals(MavlinkMissionItemIntFactory.MAV_FRAME_GLOBAL_INT, item.getFrame());
    assertEquals(150.0f, item.getAltitude());
  }

  @Test
  void loiterWithoutDurationUsesUnlimitedLoiter() {
    MavlinkMissionItemInt item =
        onlyMissionItem(
            new MissionPlan(
                List.of(
                    loiter(
                        Duration.ZERO,
                        null,
                        null))));

    assertEquals(
        MavlinkMissionItemIntFactory.MAV_CMD_NAV_LOITER_UNLIM,
        item.getCommand());
    assertEquals(
        (float) GenericPx4FixedWingUavModel.DEFAULT_LOITER_RADIUS_METERS,
        item.getParam3());
    assertTrue(Float.isNaN(item.getParam4()));
  }

  @Test
  void timedLoiterUsesDurationAndRadius() {
    MavlinkMissionItemInt item =
        onlyMissionItem(
            new MissionPlan(
                List.of(
                    loiter(
                        Duration.ofMillis(1500),
                        80.0d,
                        null))));

    assertEquals(
        MavlinkMissionItemIntFactory.MAV_CMD_NAV_LOITER_TIME,
        item.getCommand());
    assertEquals(1.5f, item.getParam1());
    assertEquals(80.0f, item.getParam3());
  }

  @Test
  void repeatingFixedWingMissionAppendsJump() {
    UxvModelCommandSet commandSet =
        model.buildMission(
            context(),
            new MissionPlan(
                List.of(
                    waypoint(
                        new GeoPosition(-33.8688d, 151.2093d, 120.0d, null),
                        Duration.ZERO,
                        null,
                        null,
                        null),
                    waypoint(
                        new GeoPosition(-33.8695d, 151.2102d, 130.0d, null),
                        Duration.ofSeconds(10),
                        60.0d,
                        null,
                        null)),
                2));

    assertEquals(3, commandSet.messages().size());

    MavlinkMissionItemInt first = item(commandSet, 0);
    MavlinkMissionItemInt second = item(commandSet, 1);
    MavlinkMissionItemInt jump = item(commandSet, 2);

    assertEquals(MavlinkMissionItemIntFactory.MAV_CMD_NAV_WAYPOINT, first.getCommand());
    assertEquals(0, first.getMissionSequence());

    assertEquals(MavlinkMissionItemIntFactory.MAV_CMD_NAV_LOITER_TIME, second.getCommand());
    assertEquals(1, second.getMissionSequence());
    assertEquals(10.0f, second.getParam1());
    assertEquals(60.0f, second.getParam3());

    assertEquals(MavlinkMissionItemIntFactory.MAV_CMD_DO_JUMP, jump.getCommand());
    assertEquals(2, jump.getMissionSequence());
    assertEquals(0.0f, jump.getParam1());
    assertEquals(1.0f, jump.getParam2());
  }

  @Test
  void timedWaypointRejectsYaw() {
    MissionPlan missionPlan =
        new MissionPlan(
            List.of(
                waypoint(
                    new GeoPosition(-33.8688d, 151.2093d, 120.0d, null),
                    Duration.ofSeconds(30),
                    50.0d,
                    90.0f,
                    null)));

    assertFalse(model.validateMission(missionPlan).valid());
    assertThrows(
        IllegalArgumentException.class,
        () -> model.buildMission(context(), missionPlan));
  }

  @Test
  void loiterRejectsYawForBothTimedAndUnlimitedForms() {
    assertFalse(
        model
            .validateMission(
                new MissionPlan(
                    List.of(
                        loiter(
                            Duration.ZERO,
                            50.0d,
                            90.0f))))
            .valid());

    assertFalse(
        model
            .validateMission(
                new MissionPlan(
                    List.of(
                        loiter(
                            Duration.ofSeconds(30),
                            50.0d,
                            90.0f))))
            .valid());
  }

  @Test
  void timedWaypointRejectsZeroRadius() {
    MissionPlan missionPlan =
        new MissionPlan(
            List.of(
                waypoint(
                    new GeoPosition(-33.8688d, 151.2093d, 120.0d, null),
                    Duration.ofSeconds(30),
                    0.0d,
                    null,
                    null)));

    assertFalse(model.validateMission(missionPlan).valid());
  }

  @Test
  void fixedWingLoiterRejectsZeroAndNegativeRadius() {
    assertFalse(
        model
            .validateMission(
                new MissionPlan(
                    List.of(
                        waypoint(
                            new GeoPosition(-33.8688d, 151.2093d, 120.0d, null),
                            Duration.ofSeconds(30),
                            0.0d,
                            null,
                            null))))
            .valid());

    assertFalse(
        model
            .validateMission(
                new MissionPlan(
                    List.of(
                        loiter(
                            Duration.ZERO,
                            -1.0d,
                            null))))
            .valid());
  }

  @Test
  void fixedWingMissionRejectsMissingAltitude() {
    MissionPlan missionPlan =
        new MissionPlan(
            List.of(
                waypoint(
                    new GeoPosition(-33.8688d, 151.2093d, null, null),
                    Duration.ZERO,
                    null,
                    null,
                    null)));

    assertFalse(model.validateMission(missionPlan).valid());
  }

  @Test
  void constructorRejectsInvalidDefaultRadius() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new GenericPx4FixedWingUavModel(0.0d));
    assertThrows(
        IllegalArgumentException.class,
        () -> new GenericPx4FixedWingUavModel(-1.0d));
    assertThrows(
        IllegalArgumentException.class,
        () -> new GenericPx4FixedWingUavModel(Double.NaN));
    assertThrows(
        IllegalArgumentException.class,
        () -> new GenericPx4FixedWingUavModel(Double.POSITIVE_INFINITY));
  }

  private MavlinkMissionItemInt onlyMissionItem(MissionPlan missionPlan) {
    UxvModelCommandSet commandSet = model.buildMission(context(), missionPlan);

    assertEquals(1, commandSet.messages().size());
    return item(commandSet, 0);
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
      Duration holdDuration,
      Double radiusMeters,
      Float yawDegrees) {
    return new PlanItem(
        PlanItemType.LOITER,
        new GeoPosition(-33.8688d, 151.2093d, 120.0d, null),
        holdDuration,
        radiusMeters,
        yawDegrees,
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
      int expectedSequence,
      int expectedFrame,
      int expectedCommand) {
    assertEquals(TARGET_SYSTEM, item.getTargetSystem());
    assertEquals(TARGET_COMPONENT, item.getTargetComponent());
    assertEquals(expectedSequence, item.getMissionSequence());
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
