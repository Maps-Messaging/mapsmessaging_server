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
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import java.util.List;
import org.junit.jupiter.api.Test;

class GenericPx4UavModelRepeatMissionTest {


  private final GenericPx4UavModel model = new GenericPx4UavModel();

  @Test
  void oneIterationDoesNotAppendJump() {
    UxvModelCommandSet commandSet =
        model.buildMission(
            context(),
            new MissionPlan(List.of(waypoint(-33.8688d, 151.2093d, 100.0d))));

    assertEquals(1, commandSet.messages().size());
    assertFalse(
        commandSet.messages().stream()
            .anyMatch(
                message ->
                    message.getCommand()
                        == MavlinkMissionItemIntFactory.MAV_CMD_DO_JUMP));
  }

  @Test
  void multipleIterationsAppendJumpToFirstMissionItem() {
    UxvModelCommandSet commandSet =
        model.buildMission(
            context(),
            new MissionPlan(
                List.of(
                    waypoint(-33.8688d, 151.2093d, 100.0d),
                    waypoint(-33.8695d, 151.2102d, 110.0d)),
                3));

    assertEquals(3, commandSet.messages().size());

    MavlinkMissionItemInt first = item(commandSet, 0);
    MavlinkMissionItemInt second = item(commandSet, 1);
    MavlinkMissionItemInt jump = item(commandSet, 2);

    assertEquals(MavlinkMissionItemIntFactory.MAV_CMD_NAV_WAYPOINT, first.getCommand());
    assertEquals(0, first.getMissionSequence());
    assertEquals(-338_688_000, first.getLatitude());
    assertEquals(1_512_093_000, first.getLongitude());
    assertEquals(100.0f, first.getAltitude());

    assertEquals(MavlinkMissionItemIntFactory.MAV_CMD_NAV_WAYPOINT, second.getCommand());
    assertEquals(1, second.getMissionSequence());
    assertEquals(-338_695_000, second.getLatitude());
    assertEquals(1_512_102_000, second.getLongitude());
    assertEquals(110.0f, second.getAltitude());

    assertEquals(MavlinkMissionItemIntFactory.MAV_CMD_DO_JUMP, jump.getCommand());
    assertEquals(2, jump.getMissionSequence());
    assertEquals(MavlinkMissionItemIntFactory.MAV_FRAME_MISSION, jump.getFrame());
    assertEquals(0.0f, jump.getParam1());
    assertEquals(2.0f, jump.getParam2());
  }

  @Test
  void twoIterationsOfSinglePointProduceOneAdditionalRepeat() {
    UxvModelCommandSet commandSet =
        model.buildMission(
            context(),
            new MissionPlan(
                List.of(waypoint(-33.8688d, 151.2093d, 100.0d)),
                2));

    assertEquals(2, commandSet.messages().size());

    MavlinkMissionItemInt jump = item(commandSet, 1);
    assertEquals(1, jump.getMissionSequence());
    assertEquals(0.0f, jump.getParam1());
    assertEquals(1.0f, jump.getParam2());
  }

  @Test
  void repeatingMissionRejectsInlineReturnToHome() {
    MissionPlan missionPlan =
        new MissionPlan(
            List.of(
                waypoint(-33.8688d, 151.2093d, 100.0d),
                new PlanItem(
                    PlanItemType.RETURN_TO_HOME,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null)),
            2);

    assertFalse(model.validateMission(missionPlan).valid());
    assertThrows(
        IllegalArgumentException.class,
        () -> model.buildMission(context(), missionPlan));
  }

  private static PlanItem waypoint(
      double latitude,
      double longitude,
      double altitudeMeters) {
    return new PlanItem(
        PlanItemType.WAYPOINT,
        new GeoPosition(latitude, longitude, altitudeMeters, null),
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
  private static UxvCommandContext context() {
    UxvCommandContext context = mock(UxvCommandContext.class);
    when(context.targetSystem()).thenReturn(2);
    when(context.targetComponent()).thenReturn(1);
    when(context.sequence()).thenReturn(7);
    return context;
  }

}
