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

package io.mapsmessaging.state.mavlink;

import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.mavlink.bootstrap.DroneTwinMissingState;
import io.mapsmessaging.state.mavlink.bootstrap.DroneTwinReadinessEvaluator;
import io.mapsmessaging.state.mavlink.bootstrap.DroneTwinReadinessResult;
import io.mapsmessaging.state.mavlink.bootstrap.DroneTwinReadinessState;
import io.mapsmessaging.state.mavlink.bootstrap.MavlinkBootstrapEvent;
import io.mapsmessaging.state.mavlink.bootstrap.MavlinkBootstrapEventPublisher;
import io.mapsmessaging.state.mavlink.bootstrap.MavlinkBootstrapStateEngine;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MavlinkDroneMonitorTest {

  private static final Instant NOW = Instant.parse("2026-07-28T00:00:00Z");

  @Test
  void repeated_registration_and_readiness_write_trigger_one_bootstrap_evaluation() {
    TwinManager twinManager = new TwinManager(false, 10_000L, 5_000L, 120_000L, null);
    DroneTwinReadinessEvaluator evaluator = mock(DroneTwinReadinessEvaluator.class);
    MavlinkBootstrapStateEngine stateEngine = mock(MavlinkBootstrapStateEngine.class);
    MavlinkBootstrapEventPublisher publisher = mock(MavlinkBootstrapEventPublisher.class);
    MavlinkDroneMonitor monitor = new MavlinkDroneMonitor(twinManager, evaluator, stateEngine, publisher);
    TwinUpdateContext context = context();
    DroneTwinReadinessResult result = result(DroneTwinReadinessState.REGISTRATION_READY, true, false);
    MavlinkBootstrapEvent event = MavlinkBootstrapEvent.readinessChanged(
        "drone-1",
        DroneTwinReadinessState.DISCOVERED,
        DroneTwinReadinessState.REGISTRATION_READY
    );
    when(evaluator.evaluate(any(DroneTwin.class), same(context))).thenReturn(result);
    when(stateEngine.update(any(DroneTwin.class), same(result), same(context))).thenReturn(List.of(event));

    twinManager.addObserver(monitor);
    twinManager.addObserver(monitor);
    DroneTwin twin = mavlinkTwin();
    twinManager.registerTwin(twin, context);

    verify(evaluator, times(1)).evaluate(same(twin), same(context));
    verify(stateEngine, times(1)).update(same(twin), same(result), same(context));
    verify(publisher, times(1)).publish(same(event));
    assertEquals(DroneTwinReadinessState.REGISTRATION_READY.name(), twin.getReadinessState());
    assertTrue(twin.getRegistrationReady());
    assertFalse(twin.getCommandReady());
  }

  @Test
  void unchanged_readiness_does_not_write_twin_but_still_advances_bootstrap() {
    TwinManager twinManager = mock(TwinManager.class);
    DroneTwinReadinessEvaluator evaluator = mock(DroneTwinReadinessEvaluator.class);
    MavlinkBootstrapStateEngine stateEngine = mock(MavlinkBootstrapStateEngine.class);
    MavlinkBootstrapEventPublisher publisher = mock(MavlinkBootstrapEventPublisher.class);
    MavlinkDroneMonitor monitor = new MavlinkDroneMonitor(twinManager, evaluator, stateEngine, publisher);
    TwinUpdateContext context = context();
    DroneTwin twin = mavlinkTwin();
    DroneTwinReadinessResult result = result(DroneTwinReadinessState.COMMAND_READY, true, true);
    twin.setReadinessState(DroneTwinReadinessState.COMMAND_READY.name());
    twin.setRegistrationReady(true);
    twin.setCommandReady(true);
    twin.setMissingReadinessItems(List.of());
    twin.setDegradedReadinessItems(List.of());
    twin.setBlockingReadinessItems(List.of());
    when(evaluator.evaluate(twin, context)).thenReturn(result);
    when(stateEngine.update(twin, result, context)).thenReturn(List.of());

    monitor.onTwinUpdated(twin.getTwinId(), twin, context);

    verify(twinManager, never()).updateTwin(anyString(), any(), any());
    verify(stateEngine).update(twin, result, context);
  }

  @Test
  void removal_cancels_bootstrap_state() {
    MavlinkBootstrapStateEngine stateEngine = mock(MavlinkBootstrapStateEngine.class);
    MavlinkDroneMonitor monitor = new MavlinkDroneMonitor(
        mock(TwinManager.class),
        mock(DroneTwinReadinessEvaluator.class),
        stateEngine,
        null
    );

    monitor.onTwinRemoved(mavlinkTwin(), context());

    verify(stateEngine).remove("drone-1");
  }

  @Test
  void close_is_idempotent_and_late_callbacks_are_ignored() {
    TwinManager twinManager = mock(TwinManager.class);
    DroneTwinReadinessEvaluator evaluator = mock(DroneTwinReadinessEvaluator.class);
    MavlinkBootstrapStateEngine stateEngine = mock(MavlinkBootstrapStateEngine.class);
    MavlinkBootstrapEventPublisher publisher = mock(MavlinkBootstrapEventPublisher.class);
    MavlinkDroneMonitor monitor = new MavlinkDroneMonitor(twinManager, evaluator, stateEngine, publisher);
    DroneTwin twin = mavlinkTwin();

    monitor.close();
    monitor.close();
    clearInvocations(evaluator, stateEngine, publisher);
    monitor.onTwinUpdated(twin.getTwinId(), twin, context());
    monitor.onTwinRemoved(twin, context());

    verify(twinManager, times(1)).removeObserver(monitor);
    verifyNoInteractions(evaluator, stateEngine, publisher);
  }

  @Test
  void non_mavlink_twin_is_ignored() {
    DroneTwinReadinessEvaluator evaluator = mock(DroneTwinReadinessEvaluator.class);
    MavlinkBootstrapStateEngine stateEngine = mock(MavlinkBootstrapStateEngine.class);
    MavlinkDroneMonitor monitor = new MavlinkDroneMonitor(
        mock(TwinManager.class),
        evaluator,
        stateEngine,
        null
    );
    DroneTwin twin = new DroneTwin("unidentified");

    monitor.onTwinAdded(twin, context());

    verifyNoInteractions(evaluator, stateEngine);
  }

  private DroneTwin mavlinkTwin() {
    DroneTwin twin = new DroneTwin("drone-1");
    twin.setSystemId(17);
    twin.setComponentId(42);
    return twin;
  }

  private TwinUpdateContext context() {
    TwinUpdateContext context = new TwinUpdateContext();
    context.setReceivedTime(NOW);
    return context;
  }

  private DroneTwinReadinessResult result(
      DroneTwinReadinessState state,
      boolean registrationReady,
      boolean commandReady
  ) {
    DroneTwinReadinessResult result = new DroneTwinReadinessResult("drone-1");
    result.setReadinessState(state);
    result.setRegistrationReady(registrationReady);
    result.setCommandReady(commandReady);
    result.setMissingStates(EnumSet.noneOf(DroneTwinMissingState.class));
    result.setDegradedStates(EnumSet.noneOf(DroneTwinMissingState.class));
    result.setBlockingStates(EnumSet.noneOf(DroneTwinMissingState.class));
    result.setEvaluatedAt(NOW);
    return result;
  }
}
