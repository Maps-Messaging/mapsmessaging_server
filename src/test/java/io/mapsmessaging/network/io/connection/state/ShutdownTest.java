package io.mapsmessaging.network.io.connection.state;

import io.mapsmessaging.network.io.connection.EndPointConnection;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.route.link.LinkState;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ShutdownTest {

  @Test
  void executeClosesAttachedProtocol() throws Exception {
    EndPointConnection connection = mock(EndPointConnection.class);
    Protocol protocol = mock(Protocol.class);
    when(connection.getProtocol()).thenReturn(protocol);

    new Shutdown(connection).execute();

    verify(protocol).close();
  }

  @Test
  void executeIsSafeWithoutProtocol() {
    EndPointConnection connection = mock(EndPointConnection.class);
    when(connection.getProtocol()).thenReturn(null);

    assertDoesNotThrow(() -> new Shutdown(connection).execute());
  }

  @Test
  void closeFailureIsContainedDuringShutdown() throws Exception {
    EndPointConnection connection = mock(EndPointConnection.class);
    Protocol protocol = mock(Protocol.class);
    when(connection.getProtocol()).thenReturn(protocol);
    doThrow(new IOException("close failed")).when(protocol).close();

    assertDoesNotThrow(() -> new Shutdown(connection).execute());
  }

  @Test
  void metadataReflectsTerminalFailedLinkState() {
    Shutdown state = new Shutdown(mock(EndPointConnection.class));

    assertEquals("Shutdown", state.getName());
    assertEquals(LinkState.FAILED, state.getLinkState());
  }
}
