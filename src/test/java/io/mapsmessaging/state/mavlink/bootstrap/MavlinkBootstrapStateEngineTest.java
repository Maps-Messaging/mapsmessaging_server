/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at:
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

import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static io.mapsmessaging.state.mavlink.packet.MavlinkMessageIds.BATTERY_STATUS;
import static io.mapsmessaging.state.mavlink.packet.MavlinkMessageIds.GLOBAL_POSITION_INT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MavlinkBootstrapStateEngineTest {

  private static final Instant START = Instant.parse("2026-07-28T00:00:00Z");

  private MavlinkBootstrapStateEngine stateEngine;
  private DroneTwin droneTwin;

  @BeforeEach
  void setUp() {
    stateEngine = new MavlinkBootstrapStateEngine(new MavlinkBootstrapProfile());
    droneTwin = new DroneTwin("drone-1");
    droneTwin.setSystemId(17);
    droneTwin.setComponentId(42);
  }

  @Test
  void initial_partial_state_emits_readiness_change_and_actionable_requests() {
    DroneTwinReadinessResult result = result(
        DroneTwinReadinessState.HEALTH_PARTIAL,
        false,
        DroneTwinMissingState.MISSING_GLOBAL_POSITION,
        DroneTwinMissingState.MISSING_BATTERY_STATE
    );

    List<MavlinkBootstrapEvent> events = stateEngine.update(droneTwin, result, contextAt(0));

    assertEquals(3, events.size());
    assertEquals(MavlinkBootstrapEventType.READINESS_CHANGED, events.get(0).getEventType());
    assertEquals(DroneTwinReadinessState.UNKNOWN, events.get(0).getPreviousReadinessState());
    assertEquals(DroneTwinReadinessState.HEALTH_PARTIAL, events.get(0).getCurrentReadinessState());

    MavlinkBootstrapEvent positionRequest = requestFor(events, DroneTwinMissingState.MISSING_GLOBAL_POSITION);
    assertEquals(MavlinkBootstrapRequestType.SET_MESSAGE_INTERVAL, positionRequest.getRequestType());
    assertEquals(GLOBAL_POSITION_INT, positionRequest.getMavlinkMessageId());
    assertEquals(500_000, positionRequest.getIntervalMicroseconds());

    MavlinkBootstrapEvent batteryRequest = requestFor(events, DroneTwinMissingState.MISSING_BATTERY_STATE);
    assertEquals(MavlinkBootstrapRequestType.REQUEST_MESSAGE, batteryRequest.getRequestType());
    assertEquals(BATTERY_STATUS, batteryRequest.getMavlinkMessageId());
    assertEquals(17, batteryRequest.getTargetSystem());
    assertEquals(42, batteryRequest.getTargetComponent());
  }

  @Test
  void unchanged_partial_state_waits_for_the_full_retry_interval() {
    DroneTwinReadinessResult result = result(
        DroneTwinReadinessState.HEALTH_PARTIAL,
        false,
        DroneTwinMissingState.MISSING_BATTERY_STATE
    );

    stateEngine.update(droneTwin, result, contextAt(0));

    assertTrue(stateEngine.update(droneTwin, result, contextAt(1)).isEmpty());
    assertEquals(1, requests(stateEngine.update(droneTwin, result, contextAt(2))).size());
  }

  @Test
  void retries_exhaust_then_timeout_once_without_sleeps() {
    DroneTwinReadinessResult result = result(
        DroneTwinReadinessState.HEALTH_PARTIAL,
        false,
        DroneTwinMissingState.MISSING_BATTERY_STATE
    );

    assertEquals(1, requests(stateEngine.update(droneTwin, result, contextAt(0))).size());
    assertEquals(1, requests(stateEngine.update(droneTwin, result, contextAt(2))).size());
    assertEquals(1, requests(stateEngine.update(droneTwin, result, contextAt(4))).size());
    assertTrue(stateEngine.update(droneTwin, result, contextAt(6)).isEmpty());

    List<MavlinkBootstrapEvent> timedOut = stateEngine.update(droneTwin, result, contextAt(15));
    assertEquals(1, timedOut.size());
    assertEquals(MavlinkBootstrapEventType.BOOTSTRAP_TIMED_OUT, timedOut.get(0).getEventType());
    assertEquals(DroneTwinMissingState.MISSING_BATTERY_STATE, timedOut.get(0).getMissingState());
    assertTrue(timedOut.get(0).getReason().contains("MISSING_BATTERY_STATE"));

    assertTrue(stateEngine.update(droneTwin, result, contextAt(16)).isEmpty());
  }

  @Test
  void resolved_item_resets_exhausted_tracker_before_it_becomes_missing_again() {
    DroneTwinReadinessResult batteryMissing = result(
        DroneTwinReadinessState.HEALTH_PARTIAL,
        false,
        DroneTwinMissingState.MISSING_BATTERY_STATE
    );

    stateEngine.update(droneTwin, batteryMissing, contextAt(0));
    stateEngine.update(droneTwin, batteryMissing, contextAt(2));
    stateEngine.update(droneTwin, batteryMissing, contextAt(4));
    assertTrue(stateEngine.update(droneTwin, batteryMissing, contextAt(6)).isEmpty());

    DroneTwinReadinessResult batteryRecovered = result(
        DroneTwinReadinessState.CAPABILITY_PARTIAL,
        false,
        DroneTwinMissingState.MISSING_CAPABILITIES
    );
    stateEngine.update(droneTwin, batteryRecovered, contextAt(7));

    DroneTwinReadinessResult batteryMissingAgain = result(
        DroneTwinReadinessState.HEALTH_PARTIAL,
        false,
        DroneTwinMissingState.MISSING_BATTERY_STATE,
        DroneTwinMissingState.MISSING_CAPABILITIES
    );
    List<MavlinkBootstrapEvent> events = stateEngine.update(droneTwin, batteryMissingAgain, contextAt(8));

    assertEquals(1, requests(events).size());
    assertEquals(DroneTwinMissingState.MISSING_BATTERY_STATE, requests(events).get(0).getMissingState());
  }

  @Test
  void remove_cancels_progress_and_next_update_starts_fresh() {
    DroneTwinReadinessResult result = result(
        DroneTwinReadinessState.HEALTH_PARTIAL,
        false,
        DroneTwinMissingState.MISSING_BATTERY_STATE
    );

    stateEngine.update(droneTwin, result, contextAt(0));
    stateEngine.remove(droneTwin.getTwinId());

    List<MavlinkBootstrapEvent> events = stateEngine.update(droneTwin, result, contextAt(1));

    assertEquals(1, requests(events).size());
    assertEquals(MavlinkBootstrapEventType.READINESS_CHANGED, events.get(0).getEventType());
  }

  @Test
  void command_ready_completes_once_and_late_regression_does_not_reopen_requests() {
    DroneTwinReadinessResult ready = result(DroneTwinReadinessState.COMMAND_READY, true);

    List<MavlinkBootstrapEvent> initialEvents = stateEngine.update(droneTwin, ready, contextAt(0));
    assertEquals(2, initialEvents.size());
    assertEquals(MavlinkBootstrapEventType.READINESS_CHANGED, initialEvents.get(0).getEventType());
    assertEquals(MavlinkBootstrapEventType.BOOTSTRAP_COMPLETED, initialEvents.get(1).getEventType());

    assertTrue(stateEngine.update(droneTwin, ready, contextAt(1)).isEmpty());

    DroneTwinReadinessResult latePartial = result(
        DroneTwinReadinessState.HEALTH_PARTIAL,
        false,
        DroneTwinMissingState.MISSING_BATTERY_STATE
    );
    List<MavlinkBootstrapEvent> lateEvents = stateEngine.update(droneTwin, latePartial, contextAt(2));

    assertEquals(1, lateEvents.size());
    assertEquals(MavlinkBootstrapEventType.READINESS_CHANGED, lateEvents.get(0).getEventType());
    assertTrue(requests(lateEvents).isEmpty());
    assertFalse(lateEvents.stream().anyMatch(event -> event.getEventType() == MavlinkBootstrapEventType.BOOTSTRAP_COMPLETED));
  }

  @Test
  void missing_target_identity_suppresses_requests_until_ids_are_available() {
    droneTwin.setSystemId(null);
    DroneTwinReadinessResult result = result(
        DroneTwinReadinessState.DISCOVERED,
        false,
        DroneTwinMissingState.MISSING_BATTERY_STATE
    );

    List<MavlinkBootstrapEvent> events = stateEngine.update(droneTwin, result, contextAt(0));

    assertTrue(requests(events).isEmpty());
    assertEquals(1, events.size());
    assertEquals(MavlinkBootstrapEventType.READINESS_CHANGED, events.get(0).getEventType());
  }

  @Test
  void unrelated_missing_state_does_not_consume_an_actionable_request_budget() {
    DroneTwinReadinessResult unrelated = result(
        DroneTwinReadinessState.IDENTIFIED,
        false,
        DroneTwinMissingState.MISSING_AUTOPILOT_TYPE
    );
    assertTrue(requests(stateEngine.update(droneTwin, unrelated, contextAt(0))).isEmpty());

    DroneTwinReadinessResult batteryMissing = result(
        DroneTwinReadinessState.HEALTH_PARTIAL,
        false,
        DroneTwinMissingState.MISSING_AUTOPILOT_TYPE,
        DroneTwinMissingState.MISSING_BATTERY_STATE
    );
    assertEquals(1, requests(stateEngine.update(droneTwin, batteryMissing, contextAt(0))).size());
  }

  @Test
  void one_timed_out_request_does_not_block_a_new_independent_request() {
    DroneTwinReadinessResult batteryMissing = result(
        DroneTwinReadinessState.HEALTH_PARTIAL,
        false,
        DroneTwinMissingState.MISSING_BATTERY_STATE
    );
    stateEngine.update(droneTwin, batteryMissing, contextAt(0));
    stateEngine.update(droneTwin, batteryMissing, contextAt(2));
    stateEngine.update(droneTwin, batteryMissing, contextAt(4));
    stateEngine.update(droneTwin, batteryMissing, contextAt(15));

    DroneTwinReadinessResult batteryAndPositionMissing = result(
        DroneTwinReadinessState.POSITION_PARTIAL,
        false,
        DroneTwinMissingState.MISSING_GLOBAL_POSITION,
        DroneTwinMissingState.MISSING_BATTERY_STATE
    );
    List<MavlinkBootstrapEvent> events = stateEngine.update(droneTwin, batteryAndPositionMissing, contextAt(16));

    assertEquals(1, requests(events).size());
    assertEquals(DroneTwinMissingState.MISSING_GLOBAL_POSITION, requests(events).get(0).getMissingState());
  }

  @Test
  void null_inputs_and_null_twin_id_are_ignored() {
    DroneTwinReadinessResult result = result(DroneTwinReadinessState.DISCOVERED, false);

    assertTrue(stateEngine.update(null, result, contextAt(0)).isEmpty());
    assertTrue(stateEngine.update(droneTwin, null, contextAt(0)).isEmpty());

    droneTwin.setTwinId(null);
    assertTrue(stateEngine.update(droneTwin, result, contextAt(0)).isEmpty());
  }

  private DroneTwinReadinessResult result(
      DroneTwinReadinessState readinessState,
      boolean commandReady,
      DroneTwinMissingState... missingStates
  ) {
    DroneTwinReadinessResult result = new DroneTwinReadinessResult(droneTwin.getTwinId());
    Set<DroneTwinMissingState> missing = missingStates.length == 0
        ? EnumSet.noneOf(DroneTwinMissingState.class)
        : EnumSet.copyOf(Arrays.asList(missingStates));
    result.setReadinessState(readinessState);
    result.setRegistrationReady(readinessState != DroneTwinReadinessState.UNKNOWN && readinessState != DroneTwinReadinessState.DISCOVERED);
    result.setCommandReady(commandReady);
    result.setMissingStates(missing);
    result.setDegradedStates(EnumSet.noneOf(DroneTwinMissingState.class));
    result.setBlockingStates(EnumSet.noneOf(DroneTwinMissingState.class));
    result.setEvaluatedAt(START);
    return result;
  }

  private TwinUpdateContext contextAt(long seconds) {
    TwinUpdateContext context = new TwinUpdateContext();
    context.setReceivedTime(START.plusSeconds(seconds));
    return context;
  }

  private List<MavlinkBootstrapEvent> requests(List<MavlinkBootstrapEvent> events) {
    return events.stream()
        .filter(event -> event.getEventType() == MavlinkBootstrapEventType.REQUEST)
        .toList();
  }

  private MavlinkBootstrapEvent requestFor(
      List<MavlinkBootstrapEvent> events,
      DroneTwinMissingState missingState
  ) {
    MavlinkBootstrapEvent event = requests(events).stream()
        .filter(candidate -> candidate.getMissingState() == missingState)
        .findFirst()
        .orElse(null);
    assertNotNull(event);
    return event;
  }
}
