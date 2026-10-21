package io.mapsmessaging.network.io.connection.route;

import io.mapsmessaging.network.io.connection.EndPointConnection;
import io.mapsmessaging.network.io.connection.state.Establishing;
import io.mapsmessaging.network.io.connection.state.Hold;
import io.mapsmessaging.network.io.connection.state.State;
import io.mapsmessaging.network.route.link.Link;
import io.mapsmessaging.network.route.link.LinkState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class RouteManagerTest {

  @Test
  void addingConnectionInstallsHoldEstablishmentAndRegistersLink() {
    RouteManager manager = new RouteManager("route-a");
    try {
      EndPointConnection connection = connection("primary", "Connecting", LinkState.CONNECTING);

      manager.addEndPointConnection(connection);

      verify(connection).setEstablishingState(argThat(state -> state instanceof Hold));
      verify(connection).addStateChangeListener(manager);
      assertNotNull(manager.getRouteList().getLink(connection));
    } finally {
      manager.getOrchestrator().close();
    }
  }

  @Test
  void switchingToNewLinkEstablishesItAndRepeatedSwitchIsNoOp() {
    RouteManager manager = new RouteManager("route-b");
    try {
      EndPointConnection connection = connection("primary", "Connecting", LinkState.CONNECTING);
      manager.addEndPointConnection(connection);
      Link link = manager.getRouteList().getLink(connection);
      clearInvocations(connection);

      assertTrue(manager.switchTo(link, "test"));
      assertSame(link, manager.getCurrentLink());
      verify(connection).scheduleState(argThat(state -> state instanceof Establishing));

      clearInvocations(connection);
      assertFalse(manager.switchTo(link, "same"));
      verifyNoInteractions(connection);
    } finally {
      manager.getOrchestrator().close();
    }
  }

  @Test
  void switchingAwayFromConnectedLinkPlacesOldLinkOnHold() {
    RouteManager manager = new RouteManager("route-c");
    try {
      EndPointConnection first = connection("first", "Established", LinkState.CONNECTED);
      EndPointConnection second = connection("second", "Established", LinkState.CONNECTED);
      manager.addEndPointConnection(first);
      manager.addEndPointConnection(second);

      Link firstLink = manager.getRouteList().getLink(first);
      Link secondLink = manager.getRouteList().getLink(second);
      assertTrue(manager.switchTo(firstLink, "initial"));

      clearInvocations(first, second);
      assertTrue(manager.switchTo(secondLink, "better"));

      verify(first).scheduleState(argThat(state -> state instanceof Hold));
      verify(second, never()).scheduleState(argThat(state -> state instanceof Establishing));
      assertSame(secondLink, manager.getCurrentLink());
    } finally {
      manager.getOrchestrator().close();
    }
  }

  @Test
  void startAndStopManageMetricsScheduler() {
    RouteManager manager = new RouteManager("route-d");
    try {
      manager.start();
      assertNotNull(manager.getMetricsUpdater());
      assertFalse(manager.getMetricsUpdater().isShutdown());

      manager.stop();
      assertTrue(manager.getMetricsUpdater().isShutdown());
    } finally {
      manager.getOrchestrator().close();
      if (manager.getMetricsUpdater() != null) {
        manager.getMetricsUpdater().shutdownNow();
      }
    }
  }

  private static EndPointConnection connection(
      String name,
      String stateName,
      LinkState linkState
  ) {
    EndPointConnection connection = mock(EndPointConnection.class);
    State state = mock(State.class);
    when(connection.getConfigName()).thenReturn(name);
    when(connection.getState()).thenReturn(state);
    when(state.getName()).thenReturn(stateName);
    when(state.getLinkState()).thenReturn(linkState);
    return connection;
  }
}
