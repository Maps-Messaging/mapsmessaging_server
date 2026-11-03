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
import io.mapsmessaging.network.io.connection.state.Connected;
import io.mapsmessaging.network.io.connection.state.Disconnected;
import io.mapsmessaging.network.io.connection.state.Established;
import io.mapsmessaging.network.io.connection.state.State;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.route.select.SelectionOrchestrator;
import java.util.ArrayDeque;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
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

  // Only transport completion and scheduling are controlled. Route selection, Connected,
  // Hold/Holding and subscription establishment all execute their production implementations.
  private static final class ControlledConnection {
    private final EndPointConnection connection = mock(EndPointConnection.class);
    private final AtomicReference<State> state = new AtomicReference<>();
    private final AtomicReference<State> establishingState = new AtomicReference<>();
    private final ArrayDeque<State> scheduledStates = new ArrayDeque<>();
    private final AtomicInteger subscriptionRequests = new AtomicInteger();

    private ControlledConnection(String name) throws Exception {
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
        State nextState = invocation.getArgument(0);
        state.set(nextState);
        scheduledStates.clear();
        scheduledStates.add(nextState);
        return null;
      }).when(connection).scheduleState(any(State.class));
      doAnswer(invocation -> {
        subscriptionRequests.incrementAndGet();
        return null;
      }).when(protocol).subscribeRemote(anyString(), anyString(), any(), any(), any(), any(), any());
      state.set(new Disconnected(connection));
    }

    private void transportDisconnected() {
      scheduledStates.clear();
      state.set(new Disconnected(connection));
    }

    private void transportConnected() {
      connection.scheduleState(new Connected(connection));
      runScheduledStates();
    }

    private void runScheduledStates() {
      int transitions = 0;
      while (!scheduledStates.isEmpty()) {
        assertTrue(++transitions <= 20, "State transitions must settle without an infinite loop");
        scheduledStates.removeFirst().execute();
      }
    }
  }
}
