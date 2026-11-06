package io.mapsmessaging.network.io.connection.state;

import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.connection.EndPointConnection;
import io.mapsmessaging.network.protocol.Protocol;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StateMonitorBranchCoverageTest {

  @Test
  void resolveEndpointPrefersConnectionEndpointThenFallsBackToProtocol() throws Exception {
    EndPointConnection connection = mock(EndPointConnection.class);
    StateMonitor monitor = new StateMonitor(connection);
    Method resolve = StateMonitor.class.getDeclaredMethod("resolveEndPoint");
    resolve.setAccessible(true);

    EndPoint direct = mock(EndPoint.class);
    when(connection.getEndPoint()).thenReturn(direct);
    assertSame(direct, resolve.invoke(monitor));

    reset(connection);
    Protocol protocol = mock(Protocol.class);
    EndPoint fallback = mock(EndPoint.class);
    when(connection.getProtocol()).thenReturn(protocol);
    when(protocol.getEndPoint()).thenReturn(fallback);
    assertSame(fallback, resolve.invoke(monitor));

    reset(connection);
    assertNull(resolve.invoke(monitor));
  }

  @Test
  void runBeforeStartIsNoOpAndCloseIsIdempotent() {
    EndPointConnection connection = mock(EndPointConnection.class);
    StateMonitor monitor = new StateMonitor(connection);

    assertDoesNotThrow(monitor::run);
    assertDoesNotThrow(monitor::close);
    assertDoesNotThrow(monitor::close);
    assertDoesNotThrow(monitor::start);

    verify(connection, never()).getState();
  }
}