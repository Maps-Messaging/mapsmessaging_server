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

package io.mapsmessaging.state.mavlink.bootstrap;

import io.mapsmessaging.state.config.VehicleClass;
import io.mapsmessaging.state.drone.core.TwinLifecycleStatus;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.model.BatteryState;
import io.mapsmessaging.state.drone.model.FixInfo;
import io.mapsmessaging.state.drone.model.GeoPosition;
import io.mapsmessaging.state.drone.model.LinkState;
import io.mapsmessaging.state.drone.model.SystemState;
import io.mapsmessaging.state.drone.model.autopilot.AutopilotState;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DroneTwinReadinessEvaluatorTest {

  private static final Instant NOW = Instant.parse("2026-07-28T00:00:00Z");

  private final DroneTwinReadinessEvaluator evaluator = new DroneTwinReadinessEvaluator();

  @Test
  void fully_populated_fresh_twin_is_command_ready() {
    DroneTwinReadinessResult result = evaluate(readyTwin());

    assertEquals(DroneTwinReadinessState.COMMAND_READY, result.getReadinessState());
    assertTrue(result.isRegistrationReady());
    assertTrue(result.isCommandReady());
    assertTrue(result.getMissingStates().isEmpty());
  }

  @Test
  void null_twin_is_unknown_and_not_ready() {
    DroneTwinReadinessResult result = evaluator.evaluate(null, contextAt(NOW));

    assertEquals(DroneTwinReadinessState.UNKNOWN, result.getReadinessState());
    assertFalse(result.isRegistrationReady());
    assertFalse(result.isCommandReady());
  }

  @Test
  void missing_identity_fields_block_registration() {
    DroneTwin twin = readyTwin();
    twin.setSystemId(null);
    twin.setComponentId(null);
    twin.setVehicleClass(null);

    DroneTwinReadinessResult result = evaluate(twin);

    assertFalse(result.isRegistrationReady());
    assertFalse(result.isCommandReady());
    assertTrue(result.getBlockingStates().containsAll(List.of(
        DroneTwinMissingState.MISSING_SYSTEM_ID,
        DroneTwinMissingState.MISSING_COMPONENT_ID,
        DroneTwinMissingState.MISSING_VEHICLE_CLASS
    )));
  }

  @Test
  void missing_autopilot_type_blocks_registration_even_with_valid_position() {
    DroneTwin twin = readyTwin();
    twin.getAutopilotState().setAutopilotType(null);

    DroneTwinReadinessResult result = evaluate(twin);

    assertFalse(result.isRegistrationReady());
    assertFalse(result.isCommandReady());
    assertTrue(result.getMissingStates().contains(DroneTwinMissingState.MISSING_AUTOPILOT_TYPE));
    assertEquals(DroneTwinReadinessState.POSITIONED, result.getReadinessState());
  }

  @Test
  void disconnected_or_stale_heartbeat_reports_stale() {
    DroneTwin disconnected = readyTwin();
    disconnected.getLinkState().setConnected(false);

    DroneTwinReadinessResult disconnectedResult = evaluate(disconnected);
    assertEquals(DroneTwinReadinessState.STALE, disconnectedResult.getReadinessState());
    assertTrue(disconnectedResult.getBlockingStates().contains(DroneTwinMissingState.STALE_HEARTBEAT));

    DroneTwin stale = readyTwin();
    stale.setConnectivityUpdatedAt(NOW.minusSeconds(11));

    DroneTwinReadinessResult staleResult = evaluate(stale);
    assertEquals(DroneTwinReadinessState.STALE, staleResult.getReadinessState());
    assertFalse(staleResult.isRegistrationReady());
  }

  @Test
  void missing_or_stale_position_blocks_commands_but_not_registration() {
    DroneTwin missing = readyTwin();
    missing.setGeoPosition(null);

    DroneTwinReadinessResult missingResult = evaluate(missing);
    assertTrue(missingResult.isRegistrationReady());
    assertFalse(missingResult.isCommandReady());
    assertEquals(DroneTwinReadinessState.POSITION_PARTIAL, missingResult.getReadinessState());
    assertTrue(missingResult.getBlockingStates().contains(DroneTwinMissingState.MISSING_GLOBAL_POSITION));

    DroneTwin stale = readyTwin();
    stale.setNavigationUpdatedAt(NOW.minusSeconds(11));

    DroneTwinReadinessResult staleResult = evaluate(stale);
    assertTrue(staleResult.isRegistrationReady());
    assertFalse(staleResult.isCommandReady());
    assertTrue(staleResult.getBlockingStates().contains(DroneTwinMissingState.STALE_POSITION));
  }

  @Test
  void non_finite_or_out_of_range_position_never_becomes_ready() {
    List<GeoPosition> invalidPositions = List.of(
        new GeoPosition(Double.NaN, 151.2, 10.0, null),
        new GeoPosition(-33.8, Double.POSITIVE_INFINITY, 10.0, null),
        new GeoPosition(90.0001, 151.2, 10.0, null),
        new GeoPosition(-33.8, -180.0001, 10.0, null)
    );

    for (GeoPosition invalidPosition : invalidPositions) {
      DroneTwin twin = readyTwin();
      twin.setGeoPosition(invalidPosition);

      DroneTwinReadinessResult result = evaluate(twin);

      assertFalse(result.isCommandReady(), invalidPosition.toString());
      assertTrue(result.getBlockingStates().contains(DroneTwinMissingState.MISSING_GLOBAL_POSITION), invalidPosition.toString());
    }
  }

  @Test
  void gps_flag_and_fix_type_must_both_represent_a_valid_fix() {
    DroneTwin invalidFlag = readyTwin();
    invalidFlag.setGpsValid(false);
    assertFalse(evaluate(invalidFlag).isCommandReady());

    for (String invalidFixType : List.of("NO_GPS", "NO_FIX", "UNKNOWN", " ")) {
      DroneTwin twin = readyTwin();
      twin.getFixInfo().setFixType(invalidFixType);

      DroneTwinReadinessResult result = evaluate(twin);

      assertFalse(result.isCommandReady(), invalidFixType);
      assertTrue(result.getBlockingStates().contains(DroneTwinMissingState.MISSING_GPS_FIX), invalidFixType);
    }
  }

  @Test
  void missing_stale_or_invalid_battery_state_is_health_partial() {
    DroneTwin missing = readyTwin();
    missing.setBatteryState(null);
    assertHealthPartial(evaluate(missing), DroneTwinMissingState.MISSING_BATTERY_STATE);

    DroneTwin stale = readyTwin();
    stale.setPowerUpdatedAt(NOW.minusSeconds(31));
    assertHealthPartial(evaluate(stale), DroneTwinMissingState.STALE_POWER);

    DroneTwin invalid = readyTwin();
    BatteryState batteryState = new BatteryState();
    batteryState.setPercentage(Double.NaN);
    batteryState.setVoltageVolts(Double.POSITIVE_INFINITY);
    batteryState.setCurrentAmps(Double.NaN);
    invalid.setBatteryState(batteryState);
    assertHealthPartial(evaluate(invalid), DroneTwinMissingState.MISSING_BATTERY_STATE);
  }

  @Test
  void missing_capabilities_is_capability_partial() {
    DroneTwin twin = readyTwin();
    twin.getAutopilotState().setCapabilities(null);

    DroneTwinReadinessResult result = evaluate(twin);

    assertTrue(result.isRegistrationReady());
    assertFalse(result.isCommandReady());
    assertEquals(DroneTwinReadinessState.CAPABILITY_PARTIAL, result.getReadinessState());
    assertTrue(result.getMissingStates().contains(DroneTwinMissingState.MISSING_CAPABILITIES));
  }

  @Test
  void missing_home_and_system_health_are_degraded_but_not_command_blocking() {
    DroneTwin twin = readyTwin();
    twin.setHomePosition(null);
    twin.setSystemState(null);

    DroneTwinReadinessResult result = evaluate(twin);

    assertTrue(result.isCommandReady());
    assertEquals(DroneTwinReadinessState.COMMAND_READY, result.getReadinessState());
    assertTrue(result.getDegradedStates().contains(DroneTwinMissingState.MISSING_HOME_POSITION));
    assertTrue(result.getDegradedStates().contains(DroneTwinMissingState.MISSING_SYSTEM_STATE));
  }

  @Test
  void freshness_thresholds_are_inclusive() {
    DroneTwin twin = readyTwin();
    twin.setConnectivityUpdatedAt(NOW.minusSeconds(10));
    twin.setNavigationUpdatedAt(NOW.minusSeconds(10));
    twin.setPowerUpdatedAt(NOW.minusSeconds(30));

    assertTrue(evaluate(twin).isCommandReady());

    twin.setPowerUpdatedAt(NOW.minusSeconds(30).minusMillis(1));
    assertFalse(evaluate(twin).isCommandReady());
  }

  @Test
  void stale_lifecycle_blocks_readiness_even_with_fresh_link_timestamp() {
    DroneTwin twin = readyTwin();
    twin.setLifecycleStatus(TwinLifecycleStatus.STALE);

    DroneTwinReadinessResult result = evaluate(twin);

    assertEquals(DroneTwinReadinessState.STALE, result.getReadinessState());
    assertFalse(result.isRegistrationReady());
    assertFalse(result.isCommandReady());
  }

  private void assertHealthPartial(
      DroneTwinReadinessResult result,
      DroneTwinMissingState expectedMissingState
  ) {
    assertTrue(result.isRegistrationReady());
    assertFalse(result.isCommandReady());
    assertEquals(DroneTwinReadinessState.HEALTH_PARTIAL, result.getReadinessState());
    assertTrue(result.getMissingStates().contains(expectedMissingState));
  }

  private DroneTwinReadinessResult evaluate(DroneTwin twin) {
    return evaluator.evaluate(twin, contextAt(NOW));
  }

  private TwinUpdateContext contextAt(Instant instant) {
    TwinUpdateContext context = new TwinUpdateContext();
    context.setReceivedTime(instant);
    return context;
  }

  private DroneTwin readyTwin() {
    DroneTwin twin = new DroneTwin("drone-1");
    twin.setSystemId(1);
    twin.setComponentId(1);
    twin.setVehicleClass(VehicleClass.UAV);

    TestAutopilotState autopilotState = new TestAutopilotState();
    autopilotState.setAutopilotType("PX4");
    autopilotState.setUid(1L);
    autopilotState.setCapabilities(1L);
    twin.setAutopilotState(autopilotState);

    LinkState linkState = new LinkState();
    linkState.setConnected(true);
    linkState.setState("CONNECTED");
    twin.setLinkState(linkState);
    twin.setConnectivityUpdatedAt(NOW);

    twin.setGeoPosition(new GeoPosition(-33.8688, 151.2093, 30.0, null));
    twin.setNavigationUpdatedAt(NOW);

    FixInfo fixInfo = new FixInfo();
    fixInfo.setFixType("3D");
    fixInfo.setSatelliteCount(12);
    twin.setFixInfo(fixInfo);
    twin.setGpsValid(true);

    twin.setHomePosition(new GeoPosition(-33.8688, 151.2093, 20.0, null));

    BatteryState batteryState = new BatteryState();
    batteryState.setPercentage(75.0);
    twin.setBatteryState(batteryState);
    twin.setPowerUpdatedAt(NOW);

    twin.setSystemState(new SystemState());
    twin.setLifecycleStatus(TwinLifecycleStatus.ACTIVE);
    return twin;
  }

  private static final class TestAutopilotState extends AutopilotState {
  }
}
