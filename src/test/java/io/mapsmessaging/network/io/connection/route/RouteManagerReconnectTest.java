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

package io.mapsmessaging.network.io.connection.route;

import io.mapsmessaging.dto.rest.config.network.EndPointConnectionServerConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.LinkConfigDTO;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.network.io.connection.EndPointConnection;
import io.mapsmessaging.network.io.connection.StateChangeListener;
import io.mapsmessaging.network.io.connection.state.Connected;
import io.mapsmessaging.network.io.connection.state.Disconnected;
import io.mapsmessaging.network.io.connection.state.Established;
import io.mapsmessaging.network.io.connection.state.State;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.route.select.SelectionOrchestrator;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class RouteManagerReconnectTest {

  @Test
  void selected_link_reconnect_restores_subscription_when_other_link_is_down() throws Exception {
    try (MockedStatic<SelectionOrchestrator> scheduling = mockStatic(SelectionOrchestrator.class)) {
      scheduling.when(() -> SelectionOrchestrator.start(any(), any())).thenReturn(mock(SelectionOrchestrator.class));
      RouteManager manager = new RouteManager("reconnect-selected");
      ControlledConnection primary = new ControlledConnection("A");
      ControlledConnection backup = new ControlledConnection("B");
      manager.addEndPointConnection(primary.connection);
      manager.addEndPointConnection(backup.connection);

      primary.transportConnected();
      manager.getSelector().evaluateOnce();
      primary.runScheduledStates();
      assertInstanceOf(Established.class, primary.state.get());
      assertEquals(1, primary.subscriptionRequests.get());
      assertSame(manager.getRouteList().getLink(primary.connection), manager.getCurrentLink());

      primary.transportDisconnected();
      manager.getSelector().evaluateOnce();
      assertInstanceOf(Disconnected.class, backup.state.get());

      primary.transportConnected();
      for (int scan = 0; scan < 10; scan++) {
        manager.getSelector().evaluateOnce();
        primary.runScheduledStates();
        backup.runScheduledStates();
      }

      assertAll(
          () -> assertInstanceOf(Established.class, primary.state.get(), "A must resume forwarding after reconnect"),
          () -> assertEquals(2, primary.subscriptionRequests.get(), "A must issue a new subscription after reconnect"),
          () -> assertEquals(0, backup.subscriptionRequests.get(), "B remains unavailable"));
    }
  }

  @Test
  void returning_link_restores_subscription_after_selection_switched_to_backup() throws Exception {
    try (MockedStatic<SelectionOrchestrator> scheduling = mockStatic(SelectionOrchestrator.class)) {
      scheduling.when(() -> SelectionOrchestrator.start(any(), any())).thenReturn(mock(SelectionOrchestrator.class));
      RouteManager manager = new RouteManager("reconnect-after-failover");
      ControlledConnection primary = new ControlledConnection("A");
      ControlledConnection backup = new ControlledConnection("B");
      manager.addEndPointConnection(primary.connection);
      manager.addEndPointConnection(backup.connection);

      primary.transportConnected();
      manager.getSelector().evaluateOnce();
      primary.runScheduledStates();
      assertInstanceOf(Established.class, primary.state.get());
      assertEquals(1, primary.subscriptionRequests.get());

      primary.transportDisconnected();
      backup.transportConnected();
      manager.getSelector().evaluateOnce();
      backup.runScheduledStates();
      assertSame(manager.getRouteList().getLink(backup.connection), manager.getCurrentLink());
      assertInstanceOf(Established.class, backup.state.get());
      assertEquals(1, backup.subscriptionRequests.get());

      backup.transportDisconnected();
      manager.getSelector().evaluateOnce();
      primary.transportConnected();
      manager.getSelector().evaluateOnce();
      primary.runScheduledStates();

      assertSame(manager.getRouteList().getLink(primary.connection), manager.getCurrentLink());
      assertInstanceOf(Established.class, primary.state.get());
      assertEquals(2, primary.subscriptionRequests.get());
    }
  }

  @Test
  void selected_reconnected_link_waits_until_warmup_boundary_before_resubscribing() throws Exception {
    try (TimedRoute route = new TimedRoute()) {
      route.establishPrimary();
      route.primary.transportDisconnected();
      route.scan();
      route.primary.transportConnected();
      route.scan();
      route.assertPrimaryWaiting();

      route.advance(Duration.ofMillis(2999));
      route.assertPrimaryWaiting();
      route.advance(Duration.ofMillis(1));
      route.assertPrimaryRecovered();
      route.advance(Duration.ofSeconds(10));
      route.assertPrimaryRecovered();
    }
  }

  @Test
  void disconnect_during_warmup_requires_a_full_new_stable_interval() throws Exception {
    try (TimedRoute route = new TimedRoute()) {
      route.establishPrimary();
      route.primary.transportDisconnected();
      route.scan();
      route.primary.transportConnected();
      route.scan();
      route.advance(Duration.ofSeconds(2));
      route.assertPrimaryWaiting();

      route.primary.transportDisconnected();
      route.scan();
      route.advance(Duration.ofMillis(500));
      route.primary.transportConnected();
      route.scan();
      route.advance(Duration.ofMillis(500)); // The first reconnect's deadline must not resume delivery.
      route.assertPrimaryWaiting();
      route.advance(Duration.ofMillis(2499));
      route.assertPrimaryWaiting();
      route.advance(Duration.ofMillis(1));
      route.assertPrimaryRecovered();
    }
  }

  @Test
  void repeated_short_reconnects_do_not_accumulate_warmup_time() throws Exception {
    try (TimedRoute route = new TimedRoute()) {
      route.establishPrimary();
      for (int reconnect = 0; reconnect < 3; reconnect++) {
        route.primary.transportDisconnected();
        route.scan();
        route.primary.transportConnected();
        route.scan();
        route.advance(Duration.ofSeconds(2));
        route.assertPrimaryWaiting();
      }

      route.primary.transportDisconnected();
      route.scan();
      route.primary.transportConnected();
      route.scan();
      route.advance(Duration.ofMillis(2999));
      route.assertPrimaryWaiting();
      route.advance(Duration.ofMillis(1));
      route.assertPrimaryRecovered();
    }
  }

  @Test
  void returning_nonselected_link_waits_for_warmup_when_selected_backup_is_down() throws Exception {
    try (TimedRoute route = new TimedRoute()) {
      route.establishPrimary();
      route.primary.transportDisconnected();
      route.backup.transportConnected();
      route.scan();
      route.advance(Duration.ofSeconds(3));
      assertInstanceOf(Established.class, route.backup.state.get());
      assertEquals(1, route.backup.subscriptionRequests.get());
      assertSame(route.manager.getRouteList().getLink(route.backup.connection), route.manager.getCurrentLink());

      route.backup.transportDisconnected();
      route.scan();
      route.primary.transportConnected();
      route.scan();
      route.assertPrimaryWaiting();
      route.advance(Duration.ofMillis(2999));
      route.assertPrimaryWaiting();
      route.advance(Duration.ofMillis(1));
      route.assertPrimaryRecovered();
    }
  }

  // The clock advances only when the test requests it. Selector evaluations stand in for
  // periodic/event-driven scans; the production selector and route/state logic remain real.
  private static final class TimedRoute implements AutoCloseable {
    private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-11-03T00:00:00Z"));
    private final MockedStatic<Instant> clock = mockStatic(Instant.class, CALLS_REAL_METHODS);
    private final MockedStatic<SelectionOrchestrator> scheduling = mockStatic(SelectionOrchestrator.class);
    private final RouteManager manager;
    private final ControlledConnection primary;
    private final ControlledConnection backup;

    private TimedRoute() throws Exception {
      clock.when(Instant::now).thenAnswer(invocation -> now.get());
      scheduling.when(() -> SelectionOrchestrator.start(any(), any())).thenReturn(mock(SelectionOrchestrator.class));
      manager = new RouteManager("timed-reconnect");
      primary = new ControlledConnection("A", () -> now.get().toEpochMilli());
      backup = new ControlledConnection("B", () -> now.get().toEpochMilli());
      manager.addEndPointConnection(primary.connection);
      manager.addEndPointConnection(backup.connection);
    }

    private void establishPrimary() {
      primary.transportConnected();
      scan();
      advance(Duration.ofSeconds(3));
      assertInstanceOf(Established.class, primary.state.get());
      assertEquals(1, primary.subscriptionRequests.get());
      assertSame(manager.getRouteList().getLink(primary.connection), manager.getCurrentLink());
    }

    private void advance(Duration duration) {
      now.updateAndGet(previous -> previous.plus(duration));
      scan();
    }

    private void scan() {
      primary.runScheduledStates();
      backup.runScheduledStates();
      manager.getSelector().evaluateOnce();
      primary.runScheduledStates();
      backup.runScheduledStates();
    }

    private void assertPrimaryWaiting() {
      assertEquals(1, primary.subscriptionRequests.get(), "A must not resubscribe before a continuous 3-second warmup");
      assertFalse(primary.state.get() instanceof Established, "A must not establish early");
    }

    private void assertPrimaryRecovered() {
      assertAll(
          () -> assertInstanceOf(Established.class, primary.state.get(), "A must establish once the warmup expires"),
          () -> assertEquals(2, primary.subscriptionRequests.get(), "A must resubscribe exactly once after warmup"),
          () -> assertSame(manager.getRouteList().getLink(primary.connection), manager.getCurrentLink()));
    }

    @Override
    public void close() {
      scheduling.close();
      clock.close();
    }
  }

  // Only transport completion and scheduling are controlled. Route selection, Connected,
  // Hold/Holding and subscription establishment all execute their production implementations.
  private static final class ControlledConnection {
    private final EndPointConnection connection = mock(EndPointConnection.class);
    private final AtomicReference<State> state = new AtomicReference<>();
    private final AtomicReference<State> establishingState = new AtomicReference<>();
    private final ArrayDeque<ScheduledState> scheduledStates = new ArrayDeque<>();
    private final LongSupplier currentMillis;
    private StateChangeListener stateChangeListener;
    private final AtomicInteger subscriptionRequests = new AtomicInteger();

    private ControlledConnection(String name) throws Exception {
      this(name, () -> 0L);
    }

    private ControlledConnection(String name, LongSupplier currentMillis) throws Exception {
      this.currentMillis = currentMillis;
      EndPointConnectionServerConfigDTO config = new EndPointConnectionServerConfigDTO();
      LinkConfigDTO subscription = new LinkConfigDTO();
      subscription.setDirection("pull");
      subscription.setLocalNamespace("local/topic");
      subscription.setRemoteNamespace("remote/topic");
      config.setLinkConfigs(List.of(subscription));
      Protocol protocol = mock(Protocol.class);
      when(connection.getConfigName()).thenReturn(name);
      when(connection.getProperties()).thenReturn(config);
      when(connection.getProtocol()).thenReturn(protocol);
      when(connection.getLogger()).thenReturn(mock(Logger.class));
      when(connection.getState()).thenAnswer(invocation -> state.get());
      when(connection.getEstablishingState()).thenAnswer(invocation -> establishingState.get());
      doAnswer(invocation -> {
        establishingState.set(invocation.getArgument(0));
        return null;
      }).when(connection).setEstablishingState(any(State.class));
      doAnswer(invocation -> {
        stateChangeListener = invocation.getArgument(0);
        return null;
      }).when(connection).addStateChangeListener(any(StateChangeListener.class));
      doAnswer(invocation -> {
        schedule(invocation.getArgument(0), 0L);
        return null;
      }).when(connection).scheduleState(any(State.class));
      doAnswer(invocation -> {
        schedule(invocation.getArgument(0), invocation.getArgument(1));
        return null;
      }).when(connection).scheduleState(any(State.class), anyLong());
      doAnswer(invocation -> {
        subscriptionRequests.incrementAndGet();
        return null;
      }).when(protocol).subscribeRemote(anyString(), anyString(), any(), any(), any(), any(), any());
      state.set(new Disconnected(connection));
    }

    private void transportDisconnected() {
      scheduledStates.clear();
      State previous = state.getAndSet(new Disconnected(connection));
      if (stateChangeListener != null) {
        stateChangeListener.changeState(previous, state.get());
      }
    }

    private void transportConnected() {
      connection.scheduleState(new Connected(connection));
      runScheduledStates();
    }

    private void runScheduledStates() {
      int transitions = 0;
      while (!scheduledStates.isEmpty() && scheduledStates.peekFirst().dueMillis() <= currentMillis.getAsLong()) {
        assertTrue(++transitions <= 20, "State transitions must settle without an infinite loop");
        ScheduledState scheduled = scheduledStates.removeFirst();
        if (stateChangeListener != null) {
          stateChangeListener.changeState(scheduled.previous(), scheduled.next());
        }
        if (state.get() == scheduled.next()) {
          scheduled.next().execute();
        }
      }
    }

    private void schedule(State nextState, long delayMillis) {
      State previous = state.getAndSet(nextState);
      scheduledStates.clear();
      scheduledStates.add(new ScheduledState(previous, nextState, currentMillis.getAsLong() + delayMillis));
    }

    private record ScheduledState(State previous, State next, long dueMillis) {
    }
  }
}
