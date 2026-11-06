package io.mapsmessaging.network.io.connection.state;

import io.mapsmessaging.network.EndPointURL;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.connection.EndPointConnection;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.route.link.LinkState;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class ConnectingTest {

  @Test
  void udpAndSerialHandExistingProtocolEndpointToConnection() throws Exception {
    for (String scheme : new String[]{"udp", "serial"}) {
      EndPointConnection connection = mock(EndPointConnection.class);
      Protocol protocol = mock(Protocol.class);
      EndPoint endpoint = mock(EndPoint.class);

      when(connection.getUrl()).thenReturn(new EndPointURL(scheme + "://localhost:1000"));
      when(connection.getProtocol()).thenReturn(protocol);
      when(protocol.getEndPoint()).thenReturn(endpoint);

      new Connecting(connection).execute();

      verify(connection).handleNewEndPoint(endpoint);
      verify(connection, never()).scheduleState(any(State.class));
    }
  }

  @Test
  void endpointFailureSchedulesDisconnectedState() throws Exception {
    EndPointConnection connection = mock(EndPointConnection.class);
    Protocol protocol = mock(Protocol.class);
    EndPoint endpoint = mock(EndPoint.class);

    when(connection.getUrl()).thenReturn(new EndPointURL("udp://localhost:1000"));
    when(connection.getProtocol()).thenReturn(protocol);
    when(protocol.getEndPoint()).thenReturn(endpoint);
    doThrow(new IOException("boom")).when(connection).handleNewEndPoint(endpoint);

    new Connecting(connection).execute();

    verify(connection).scheduleState(argThat(state -> state instanceof Disconnected));
  }

  @Test
  void noopProtocolIsMarkedConnectedImmediately() {
    EndPointConnection connection = mock(EndPointConnection.class);
    Protocol protocol = mock(Protocol.class);

    when(connection.getUrl()).thenReturn(new EndPointURL("noop://localhost"));
    when(connection.getProtocol()).thenReturn(protocol);

    new Connecting(connection).execute();

    verify(protocol).setConnected(true);
  }

  @Test
  void otherProtocolsWaitWithoutForcingTransition() throws Exception {
    EndPointConnection connection = mock(EndPointConnection.class);
    Protocol protocol = mock(Protocol.class);

    when(connection.getUrl()).thenReturn(new EndPointURL("tcp://localhost:1883"));
    when(connection.getProtocol()).thenReturn(protocol);

    new Connecting(connection).execute();

    verify(connection, never()).handleNewEndPoint(any());
    verify(connection, never()).scheduleState(any(State.class));
    verify(protocol, never()).setConnected(anyBoolean());
  }

  @Test
  void metadataReflectsConnectingState() {
    Connecting state = new Connecting(mock(EndPointConnection.class));

    assertEquals("Connecting", state.getName());
    assertEquals(LinkState.CONNECTING, state.getLinkState());
  }
}
