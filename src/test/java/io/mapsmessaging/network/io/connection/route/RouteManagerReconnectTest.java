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
import io.mapsmessaging.network.io.connection.state.Establishing;
import io.mapsmessaging.network.io.connection.state.Holding;
import io.mapsmessaging.network.io.connection.state.State;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.impl.mqtt.PacketIdManager;
import io.mapsmessaging.network.protocol.impl.mqtt.RemoteSubscriptionProtocol;
import io.mapsmessaging.network.protocol.impl.mqtt.RemoteSubscriptionTracker;
import io.mapsmessaging.network.route.select.SelectionOrchestrator;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class RouteManagerReconnectTest {

  @Test
  void selected_link_reconnect_restores_subscription_when_other_link_is_down() throws Exception {
    try (TimedRoute route = new TimedRoute()) {
      route.establishPrimary();
      route.primary.transportDisconnected();
      route.scan();
      route.primary.transportConnected();
      route.scan();
      route.advance(Duration.ofSeconds(3));
      route.assertPrimaryRecovered();
      assertEquals(0, route.backup.subscriptionRequests.get());
    }
  }

  @Test
  void returning_link_restores_subscription_after_selection_switched_to_backup() throws Exception {
    try (TimedRoute route = new TimedRoute()) {
      ControlledConnection primary = route.establish(true, true);
      primary.transportDisconnected();
      route.scan();
      route.assertDeliveryOn(route.backup, 1);
      route.backup.transportDisconnected();
      route.scan();
      primary.transportConnected();
      route.scan();
      route.advance(Duration.ofSeconds(3));
      route.assertPrimaryRecovered();
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

  @ParameterizedTest(name = "startup A={0}, B={1}")
  @CsvSource({"false,false", "true,false", "false,true", "true,true"})
  void startup_selects_only_an_available_stable_link(boolean primaryUp, boolean backupUp) throws Exception {
    try (TimedRoute route = new TimedRoute()) {
      route.primary.connection.getProperties().setCost(10);
      route.backup.connection.getProperties().setCost(100);
      if (primaryUp) {
        route.primary.transportConnected();
      }
      if (backupUp) {
        route.backup.transportConnected();
      }
      route.scan();
      assertEquals(0, route.primary.subscriptionRequests.get(), "Startup must respect warmup");
      assertEquals(0, route.backup.subscriptionRequests.get(), "Startup must respect warmup");
      route.advance(Duration.ofSeconds(3));
      if (primaryUp || backupUp) {
        ControlledConnection expected = primaryUp ? route.primary : route.backup;
        route.assertDeliveryOn(expected, 1);
      } else {
        assertNull(route.manager.getCurrentLink());
        route.assertNoDelivery();
      }
    }
  }

  @ParameterizedTest(name = "first link after offline startup is A={0}")
  @ValueSource(booleans = {true, false})
  void offline_startup_recovers_when_either_link_becomes_available(boolean firstIsPrimary) throws Exception {
    try (TimedRoute route = new TimedRoute()) {
      route.scan();
      assertNull(route.manager.getCurrentLink());
      route.assertNoDelivery();
      ControlledConnection first = route.link(firstIsPrimary);
      first.transportConnected();
      route.scan();
      assertEquals(0, first.subscriptionRequests.get());
      route.advance(Duration.ofMillis(2999));
      route.assertNoDelivery();
      route.advance(Duration.ofMillis(1));
      route.assertDeliveryOn(first, 1);
    }
  }

  @ParameterizedTest(name = "selected A={0}")
  @ValueSource(booleans = {true, false})
  void both_up_selected_failure_activates_stable_standby(boolean selectedIsPrimary) throws Exception {
    try (TimedRoute route = new TimedRoute()) {
      ControlledConnection selected = route.establish(selectedIsPrimary, true);
      ControlledConnection standby = route.other(selected);
      assertInstanceOf(Holding.class, standby.state.get());
      selected.transportDisconnected();
      route.scan();
      route.assertDeliveryOn(standby, 1);
      assertEquals(1, selected.subscriptionRequests.get());
      route.advance(Duration.ofSeconds(10));
      route.assertDeliveryOn(standby, 1);
    }
  }

  @ParameterizedTest(name = "selected A={0}")
  @ValueSource(booleans = {true, false})
  void standby_disconnect_and_reconnect_does_not_disturb_selected_link(boolean selectedIsPrimary) throws Exception {
    try (TimedRoute route = new TimedRoute()) {
      ControlledConnection selected = route.establish(selectedIsPrimary, true);
      ControlledConnection standby = route.other(selected);
      int selectedUnsubscribes = selected.unsubscribeRequests.get();
      standby.transportDisconnected();
      route.scan();
      route.assertDeliveryOn(selected, 1);
      standby.transportConnected();
      route.scan();
      route.advance(Duration.ofSeconds(3));
      route.advance(Duration.ofMinutes(5));
      route.assertDeliveryOn(selected, 1);
      assertInstanceOf(Holding.class, standby.state.get());
      assertEquals(0, standby.subscriptionRequests.get());
      assertEquals(selectedUnsubscribes, selected.unsubscribeRequests.get());
    }
  }

  @ParameterizedTest(name = "selected A={0}, returning cost={1}, switch back={2}")
  @CsvSource({"true,1,true", "true,10,false", "true,100,false", "false,1,true", "false,10,false", "false,100,false"})
  void former_selected_link_returns_while_replacement_remains_healthy(boolean selectedIsPrimary, int returningCost, boolean switchBack) throws Exception {
    try (TimedRoute route = new TimedRoute()) {
      ControlledConnection former = route.establish(selectedIsPrimary, true);
      ControlledConnection replacement = route.other(former);
      former.transportDisconnected();
      route.scan();
      route.assertDeliveryOn(replacement, 1);
      replacement.connection.getProperties().setCost(10);
      former.connection.getProperties().setCost(returningCost);
      former.transportConnected();
      route.scan();
      route.advance(Duration.ofSeconds(3));
      route.assertDeliveryOn(replacement, 1); // Existing cooldown/flap policy still applies.
      int replacementUnsubscribes = replacement.unsubscribeRequests.get();
      route.advance(Duration.ofMinutes(5));
      if (switchBack) {
        route.assertDeliveryOn(former, 2);
        assertInstanceOf(Holding.class, replacement.state.get());
        assertEquals(replacementUnsubscribes + 1, replacement.unsubscribeRequests.get());
      } else {
        route.assertDeliveryOn(replacement, 1);
        assertInstanceOf(Holding.class, former.state.get());
        assertEquals(1, former.subscriptionRequests.get());
        assertEquals(replacementUnsubscribes, replacement.unsubscribeRequests.get());
      }
      route.advance(Duration.ofSeconds(10));
      route.assertDeliveryOn(switchBack ? former : replacement, switchBack ? 2 : 1);
    }
  }

  @ParameterizedTest(name = "selected A={0}, selected returns first={1}")
  @CsvSource({"true,true", "true,false", "false,true", "false,false"})
  void both_links_down_recover_in_either_order(boolean selectedIsPrimary, boolean selectedReturnsFirst) throws Exception {
    try (TimedRoute route = new TimedRoute()) {
      ControlledConnection selected = route.establish(selectedIsPrimary, true);
      ControlledConnection standby = route.other(selected);
      standby.transportDisconnected();
      selected.transportDisconnected();
      route.scan();
      route.assertNoDelivery();
      ControlledConnection first = selectedReturnsFirst ? selected : standby;
      ControlledConnection second = route.other(first);
      first.connection.getProperties().setCost(10);
      second.connection.getProperties().setCost(100);
      first.transportConnected();
      route.scan();
      route.advance(Duration.ofMillis(2999));
      route.assertNoDelivery();
      route.advance(Duration.ofMillis(1));
      int expectedSubscriptions = first == selected ? 2 : 1;
      route.assertDeliveryOn(first, expectedSubscriptions);
      second.transportConnected();
      route.scan();
      route.advance(Duration.ofSeconds(3));
      route.advance(Duration.ofMinutes(5));
      route.assertDeliveryOn(first, expectedSubscriptions);
      assertInstanceOf(Holding.class, second.state.get());
    }
  }

  @ParameterizedTest(name = "selected A={0}")
  @ValueSource(booleans = {true, false})
  void simultaneous_recovery_establishes_only_one_link(boolean selectedIsPrimary) throws Exception {
    try (TimedRoute route = new TimedRoute()) {
      ControlledConnection selected = route.establish(selectedIsPrimary, true);
      ControlledConnection standby = route.other(selected);
      selected.transportDisconnected();
      standby.transportDisconnected();
      route.scan();
      selected.transportConnected();
      standby.transportConnected();
      route.scan();
      route.advance(Duration.ofSeconds(3));
      route.assertDeliveryOn(selected, 2); // The original lower-cost link remains preferred.
      assertEquals(0, standby.subscriptionRequests.get());
      route.advance(Duration.ofMinutes(5));
      route.assertDeliveryOn(selected, 2);
    }
  }

  @ParameterizedTest(name = "only available link A={0}")
  @ValueSource(booleans = {true, false})
  void only_available_selected_link_reconnects_after_warmup(boolean selectedIsPrimary) throws Exception {
    try (TimedRoute route = new TimedRoute()) {
      ControlledConnection selected = route.establish(selectedIsPrimary, false);
      selected.transportDisconnected();
      route.scan();
      route.assertNoDelivery();
      selected.transportConnected();
      route.scan();
      route.advance(Duration.ofMillis(2999));
      route.assertNoDelivery();
      assertEquals(1, selected.subscriptionRequests.get());
      route.advance(Duration.ofMillis(1));
      route.assertDeliveryOn(selected, 2);
    }
  }

  @ParameterizedTest(name = "selected A={0}, disconnect after {1} ms")
  @CsvSource({"true,2999", "true,3000", "true,3001", "false,2999", "false,3000", "false,3001"})
  void disconnect_before_at_or_after_warmup_cannot_reuse_old_recovery(boolean selectedIsPrimary, long disconnectMillis) throws Exception {
    try (TimedRoute route = new TimedRoute()) {
      ControlledConnection selected = route.establish(selectedIsPrimary, false);
      selected.transportDisconnected();
      route.scan();
      selected.transportConnected();
      route.scan();
      if (disconnectMillis > 3000) {
        route.advance(Duration.ofSeconds(3));
        route.assertDeliveryOn(selected, 2);
        route.advance(Duration.ofMillis(disconnectMillis - 3000));
      } else {
        route.elapseWithoutScan(Duration.ofMillis(disconnectMillis));
      }
      selected.transportDisconnected(); // At the boundary, disconnect is delivered before the due scan.
      route.scan();
      route.advance(Duration.ofSeconds(3));
      route.assertNoDelivery();
      int expectedSubscriptions = disconnectMillis > 3000 ? 3 : 2;
      assertEquals(expectedSubscriptions - 1, selected.subscriptionRequests.get());
      selected.transportConnected();
      route.scan();
      route.advance(Duration.ofMillis(2999));
      route.assertNoDelivery();
      route.advance(Duration.ofMillis(1));
      route.assertDeliveryOn(selected, expectedSubscriptions);
    }
  }

  @ParameterizedTest(name = "selected A={0}")
  @ValueSource(booleans = {true, false})
  void repeated_flapping_requires_a_new_uninterrupted_warmup_for_either_link(boolean selectedIsPrimary) throws Exception {
    try (TimedRoute route = new TimedRoute()) {
      ControlledConnection selected = route.establish(selectedIsPrimary, false);
      for (int attempt = 0; attempt < 4; attempt++) {
        selected.transportDisconnected();
        route.scan();
        selected.transportConnected();
        route.scan();
        route.advance(Duration.ofSeconds(2));
        route.assertNoDelivery();
        assertEquals(1, selected.subscriptionRequests.get());
      }
      route.advance(Duration.ofSeconds(1));
      route.assertDeliveryOn(selected, 2);
      route.advance(Duration.ofMinutes(5));
      route.assertDeliveryOn(selected, 2);
    }
  }

  @ParameterizedTest(name = "initial selected A={0}")
  @ValueSource(booleans = {true, false})
  void failed_recovering_standby_cannot_activate_after_selected_link_returns(boolean selectedIsPrimary) throws Exception {
    try (TimedRoute route = new TimedRoute()) {
      ControlledConnection selected = route.establish(selectedIsPrimary, false);
      ControlledConnection standby = route.other(selected);
      selected.transportDisconnected();
      route.scan();
      standby.transportConnected();
      route.scan();
      route.advance(Duration.ofMillis(1500));
      assertEquals(0, standby.subscriptionRequests.get(), "Recovering standby must still be warming up");
      standby.transportDisconnected();
      selected.transportConnected();
      route.scan();
      route.advance(Duration.ofMillis(1500)); // The abandoned standby warmup expires here.
      route.assertNoDelivery();
      assertEquals(0, standby.subscriptionRequests.get());
      route.advance(Duration.ofMillis(1500));
      route.assertDeliveryOn(selected, 2);
      assertEquals(0, standby.subscriptionRequests.get(), "No stale standby establishment may execute");
    }
  }

  @ParameterizedTest(name = "selected A={0}")
  @ValueSource(booleans = {true, false})
  void disconnect_between_scans_resets_reconnect_warmup(boolean selectedIsPrimary) throws Exception {
    try (TimedRoute route = new TimedRoute()) {
      ControlledConnection selected = route.establish(selectedIsPrimary, false);
      selected.transportDisconnected();
      selected.transportConnected();
      route.scan();
      route.advance(Duration.ofSeconds(2));
      selected.transportDisconnected();
      selected.transportConnected();
      route.scan();
      route.advance(Duration.ofSeconds(1));
      route.assertNoDelivery();
      route.advance(Duration.ofMillis(1999));
      route.assertNoDelivery();
      route.advance(Duration.ofMillis(1));
      route.assertDeliveryOn(selected, 2);
    }
  }

  @ParameterizedTest(name = "selected A={0}")
  @ValueSource(booleans = {true, false})
  void cheaper_returning_link_cannot_bounce_back_during_flap_window(boolean selectedIsPrimary) throws Exception {
    try (TimedRoute route = new TimedRoute()) {
      ControlledConnection original = route.establish(selectedIsPrimary, true);
      ControlledConnection replacement = route.other(original);
      original.transportDisconnected();
      route.scan();
      route.assertDeliveryOn(replacement, 1);
      replacement.connection.getProperties().setCost(100);
      original.connection.getProperties().setCost(1);
      original.transportConnected();
      route.scan();
      route.advance(Duration.ofSeconds(3));
      route.assertDeliveryOn(replacement, 1);
      route.advance(Duration.ofSeconds(7));
      route.assertDeliveryOn(replacement, 1); // Cooldown elapsed, but flap window still applies.
      route.advance(Duration.ofMillis(289999));
      route.assertDeliveryOn(replacement, 1);
      route.advance(Duration.ofMillis(2));
      route.assertDeliveryOn(original, 2);
    }
  }

  @ParameterizedTest(name = "selected A={0}, acknowledgement succeeds={1}")
  @CsvSource({"true,true", "true,false", "false,true", "false,false"})
  void pending_subscription_does_not_trigger_reselection_but_disconnect_still_fails_over(boolean selectedIsPrimary, boolean acknowledged) throws Exception {
    try (TimedRoute route = new TimedRoute()) {
      ControlledConnection selected = route.link(selectedIsPrimary);
      ControlledConnection standby = route.other(selected);
      selected.connection.getProperties().setCost(10);
      standby.connection.getProperties().setCost(100);
      RemoteSubscriptionTracker tracker = new RemoteSubscriptionTracker(new PacketIdManager());
      int packetId = tracker.register(List.of("remote/topic"));
      Protocol protocol = mock(Protocol.class, withSettings().extraInterfaces(RemoteSubscriptionProtocol.class));
      when(((RemoteSubscriptionProtocol) protocol).getRemoteSubscriptionTracker()).thenReturn(tracker);
      when(selected.connection.getProtocol()).thenReturn(protocol);
      doAnswer(invocation -> {
        selected.subscriptionRequests.incrementAndGet();
        return null;
      }).when(protocol).subscribeRemote(anyString(), anyString(), any(), any(), any(), any(), any());
      try {
        selected.transportConnected();
        standby.transportConnected();
        route.scan();
        route.advance(Duration.ofSeconds(3));
        assertInstanceOf(Establishing.class, selected.state.get());
        for (int scan = 0; scan < 5; scan++) {
          route.advance(Duration.ofSeconds(1));
          assertSame(route.manager.getRouteList().getLink(selected.connection), route.manager.getCurrentLink());
          assertInstanceOf(Establishing.class, selected.state.get());
          assertEquals(1, selected.subscriptionRequests.get(), "Pending subscription must not be restarted");
          assertEquals(0, standby.subscriptionRequests.get());
        }
        if (acknowledged) {
          tracker.acknowledge(packetId, new int[] {1});
          selected.subscriptionActive.set(true); // The simulated peer has accepted the subscription.
          route.scan();
          route.assertDeliveryOn(selected, 1);
        } else {
          selected.transportDisconnected();
          tracker.close(); // A late failure from the abandoned establishment must not win.
          route.scan();
          route.assertDeliveryOn(standby, 1);
          assertEquals(1, selected.subscriptionRequests.get());
        }
      } finally {
        tracker.close();
      }
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

    private ControlledConnection link(boolean primaryLink) {
      return primaryLink ? primary : backup;
    }

    private ControlledConnection other(ControlledConnection link) {
      return link == primary ? backup : primary;
    }

    private ControlledConnection establish(boolean selectedIsPrimary, boolean standbyUp) {
      ControlledConnection selected = link(selectedIsPrimary);
      ControlledConnection standby = other(selected);
      selected.connection.getProperties().setCost(10);
      standby.connection.getProperties().setCost(100);
      selected.transportConnected();
      if (standbyUp) {
        standby.transportConnected();
      }
      scan();
      advance(Duration.ofSeconds(3));
      assertDeliveryOn(selected, 1);
      return selected;
    }

    private void assertDeliveryOn(ControlledConnection selected, int subscriptionCount) {
      ControlledConnection standby = other(selected);
      assertAll(
          () -> assertSame(manager.getRouteList().getLink(selected.connection), manager.getCurrentLink()),
          () -> assertInstanceOf(Established.class, selected.state.get()),
          () -> assertEquals(subscriptionCount, selected.subscriptionRequests.get(), "Unexpected duplicate or missing subscription"),
          () -> assertTrue(selected.subscriptionActive.get(), "Selected link must have an active subscription"),
          () -> assertFalse(standby.subscriptionActive.get(), "Standby must not deliver concurrently"));
    }

    private void assertNoDelivery() {
      assertFalse(primary.subscriptionActive.get(), "A must not deliver without an established eligible route");
      assertFalse(backup.subscriptionActive.get(), "B must not deliver without an established eligible route");
    }

    private void elapseWithoutScan(Duration duration) {
      now.updateAndGet(previous -> previous.plus(duration));
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
    private final AtomicInteger unsubscribeRequests = new AtomicInteger();
    private final AtomicBoolean subscriptionActive = new AtomicBoolean();

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
        subscriptionActive.set(true);
        return null;
      }).when(protocol).subscribeRemote(anyString(), anyString(), any(), any(), any(), any(), any());
      doAnswer(invocation -> {
        unsubscribeRequests.incrementAndGet();
        subscriptionActive.set(false);
        return null;
      }).when(protocol).unsubscribeRemote(anyString());
      state.set(new Disconnected(connection));
    }

    private void transportDisconnected() {
      subscriptionActive.set(false);
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
