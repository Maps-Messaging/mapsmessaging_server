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

import io.mapsmessaging.state.drone.model.GeoPosition;
import io.mapsmessaging.state.mavlink.model.impl.AbstractMissionUxvModel;
import io.mapsmessaging.state.mavlink.model.impl.uav.GenericPx4FixedWingUavModel;
import io.mapsmessaging.state.mavlink.model.impl.uav.GenericPx4UavModel;
import io.mapsmessaging.state.mavlink.model.impl.ugv.GenericPx4UgvModel;
import io.mapsmessaging.state.mavlink.model.impl.usv.SticklebackArdupilotUsvModel;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MavlinkModelValidationMessageTest {

  private static final UxvCommandContext CONTEXT =
      new UxvCommandContext(UUID.randomUUID(), 2, 1, 255, 190, 7);
  private static final GeoPosition POSITION =
      new GeoPosition(-33.8688, 151.2093, 120.0, null);

  @ParameterizedTest
  @MethodSource("models")
  void missing_context_preserves_diagnostic(AbstractMissionUxvModel model) {
    assertEquals("context must not be null",
        assertThrows(NullPointerException.class, () -> model.arm(null)).getMessage());
    assertEquals("context must not be null",
        assertThrows(NullPointerException.class, () -> model.startMission(null)).getMessage());
    assertEquals("context must not be null",
        assertThrows(NullPointerException.class, () -> model.returnToHome(null)).getMessage());
  }

  @ParameterizedTest
  @MethodSource("models")
  void missing_requests_and_plans_preserve_diagnostics(AbstractMissionUxvModel model) {
    assertEquals("request must not be null",
        assertThrows(NullPointerException.class, () -> model.setHome(CONTEXT, null)).getMessage());
    assertEquals("missionPlan must not be null",
        assertThrows(NullPointerException.class, () -> model.buildMission(CONTEXT, null)).getMessage());
    assertEquals("missionPlan must not be null",
        assertThrows(NullPointerException.class, () -> model.validateMission(null)).getMessage());
  }

  @ParameterizedTest
  @MethodSource("depthCases")
  void unsupported_depth_preserves_mission_index_and_text(
      AbstractMissionUxvModel model, String expectedMessage) {
    PlanItem item = new PlanItem(PlanItemType.WAYPOINT, POSITION,
        null, null, null, null, null, 2.0);

    PlanValidation validation = model.validateMission(new MissionPlan(List.of(item)));

    assertTrue(validation.issues().stream().anyMatch(issue ->
        issue.operation() == UxvOperation.BUILD_MISSION
            && issue.message().equals(expectedMessage)));
  }

  @Test
  void invalid_radius_preserves_parameter_name() {
    GenericPx4UavModel model = new GenericPx4UavModel();
    LoiterRequest request = new LoiterRequest(POSITION, -1.0, Duration.ZERO,
        null, null, null);

    assertEquals("radiusMeters must not be negative",
        assertThrows(IllegalArgumentException.class,
            () -> model.loiter(CONTEXT, request)).getMessage());
  }

  private static Stream<AbstractMissionUxvModel> models() {
    return Stream.of(new GenericPx4UavModel(), new GenericPx4FixedWingUavModel(),
        new GenericPx4UgvModel(), new SticklebackArdupilotUsvModel());
  }

  private static Stream<Arguments> depthCases() {
    return Stream.of(
        Arguments.of(new GenericPx4UavModel(),
            "Mission item 0 contains depthMeters, which is not valid for a UAV model"),
        Arguments.of(new GenericPx4FixedWingUavModel(),
            "Mission item 0 contains depthMeters, which is not valid for a UAV model"),
        Arguments.of(new GenericPx4UgvModel(),
            "Mission item 0 depthMeters is not valid for a UGV model"),
        Arguments.of(new SticklebackArdupilotUsvModel(),
            "Mission item 0 depthMeters is not valid for this Stickleback ArduPilot USV model"));
  }
}
