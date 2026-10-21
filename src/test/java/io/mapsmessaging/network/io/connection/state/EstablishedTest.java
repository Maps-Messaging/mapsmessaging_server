package io.mapsmessaging.network.io.connection.state;

import io.mapsmessaging.network.io.connection.EndPointConnection;
import io.mapsmessaging.network.route.link.LinkState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EstablishedTest {

  @Test
  void executeIsNoOpUntilExternalDisconnect() {
    EndPointConnection connection = mock(EndPointConnection.class);
    Established state = new Established(connection);

    assertDoesNotThrow(state::execute);

    verifyNoInteractions(connection);
  }

  @Test
  void metadataReflectsConnectedLink() {
    Established state = new Established(mock(EndPointConnection.class));

    assertEquals("Established", state.getName());
    assertEquals(LinkState.CONNECTED, state.getLinkState());
  }
}
