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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.mapsmessaging.state.drone.model.GeoPosition;
import io.mapsmessaging.state.mavlink.messages.MavlinkMissionItemInt;
import io.mapsmessaging.state.mavlink.messages.MavlinkMissionItemIntFactory;
import io.mapsmessaging.state.mavlink.model.impl.uav.GenericPx4UavModel;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MissionPlanRepeatTest {

  private static final UxvCommandContext CONTEXT = new UxvCommandContext(UUID.randomUUID(), 10, 1, 255, 190, 7);
  private static final PlanItem WAYPOINT = new PlanItem(PlanItemType.WAYPOINT, new GeoPosition(59.4673d, 24.828353d, 25.0d, null), null, null, null, null, null, null);

  @Test
  void repeatIndefinitelyAddsForeverJump() {
    GenericPx4UavModel model = new GenericPx4UavModel();
    MissionPlan plan = MissionPlan.repeatIndefinitely(List.of(WAYPOINT));

    UxvModelCommandSet commandSet = model.buildMission(CONTEXT, plan);

    assertTrue(plan.repeats());
    assertTrue(plan.repeatIndefinitely());
    assertEquals(2, commandSet.messages().size());
    MavlinkMissionItemInt jump = (MavlinkMissionItemInt) commandSet.messages().get(1);
    assertEquals(MavlinkMissionItemIntFactory.MAV_CMD_DO_JUMP, jump.getCommand());
    assertEquals(0.0f, jump.getParam1());
    assertEquals(MavlinkMissionItemIntFactory.MAV_CMD_DO_JUMP_REPEAT_FOREVER, (int) jump.getParam2());
  }

  @Test
  void finiteIterationsAddFiniteJumpCount() {
    GenericPx4UavModel model = new GenericPx4UavModel();
    MissionPlan plan = new MissionPlan(List.of(WAYPOINT), 3);

    UxvModelCommandSet commandSet = model.buildMission(CONTEXT, plan);

    assertTrue(plan.repeats());
    assertFalse(plan.repeatIndefinitely());
    MavlinkMissionItemInt jump = (MavlinkMissionItemInt) commandSet.messages().get(1);
    assertEquals(2.0f, jump.getParam2());
  }

  @Test
  void oneShotMissionDoesNotAddJump() {
    GenericPx4UavModel model = new GenericPx4UavModel();

    UxvModelCommandSet commandSet = model.buildMission(CONTEXT, new MissionPlan(List.of(WAYPOINT)));

    assertEquals(1, commandSet.messages().size());
  }

  @Test
  void repeatingMissionRejectsReturnToHome() {
    GenericPx4UavModel model = new GenericPx4UavModel();
    PlanItem returnToHome = new PlanItem(PlanItemType.RETURN_TO_HOME, null, null, null, null, null, null, null);

    assertThrows(IllegalArgumentException.class, () -> model.buildMission(CONTEXT, MissionPlan.repeatIndefinitely(List.of(returnToHome))));
  }

  @Test
  void jumpRejectsValuesBelowRepeatForever() {
    assertThrows(IllegalArgumentException.class, () -> MavlinkMissionItemIntFactory.jump(10, 1, 1, 0, -2));
  }
}
