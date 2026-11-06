package io.mapsmessaging.network.io.connection.route;

import io.mapsmessaging.network.io.connection.EndPointConnection;
import io.mapsmessaging.network.route.link.LinkState;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EndPointLinkTest {

  @Test
  void linkProjectsIdentityUriStateMetricsAndCostFromConnection() {
    EndPointConnection connection = mock(EndPointConnection.class, RETURNS_DEEP_STUBS);
    when(connection.getConfigName()).thenReturn("primary-uplink");
    when(connection.getProtocol().getEndPoint().getConfig().getUrl())
        .thenReturn("tcp://edge.example:1883");
    when(connection.getState().getLinkState()).thenReturn(LinkState.CONNECTED);
    when(connection.getProperties().getCost()).thenReturn(3);

    EndPointLink link = new EndPointLink(connection);

    assertEquals("primary-uplink", link.getLinkId().value());
    assertEquals(URI.create("tcp://edge.example:1883"), link.getRemoteUri());
    assertEquals(LinkState.CONNECTED, link.getState());
    assertTrue(link.isAvailable());
    assertSame(link.getMetrics(), link.getMetrics());
    assertEquals(3.0, link.getBaseCost().orElseThrow(), 0.0);
  }

  @Test
  void nonConnectedStatesAreUnavailableAndLifecycleHooksAreSafe() {
    EndPointConnection connection = mock(EndPointConnection.class, RETURNS_DEEP_STUBS);
    when(connection.getState().getLinkState()).thenReturn(LinkState.DEGRADED);

    EndPointLink link = new EndPointLink(connection);

    assertFalse(link.isAvailable());
    assertDoesNotThrow(link::connect);
    assertDoesNotThrow(link::disconnect);
  }
}