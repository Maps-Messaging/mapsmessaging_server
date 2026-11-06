package io.mapsmessaging.network.io.connection.state;

import io.mapsmessaging.network.io.connection.EndPointConnection;
import io.mapsmessaging.network.route.link.LinkState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HoldingTest {

  @Test
  void executeIsNoOpWhileConnectionIsHeld() {
    EndPointConnection connection = mock(EndPointConnection.class);
    Holding state = new Holding(connection);

    assertDoesNotThrow(state::execute);

    verifyNoInteractions(connection);
  }

  @Test
  void metadataReflectsHoldingLink() {
    Holding state = new Holding(mock(EndPointConnection.class));

    assertEquals("Holding", state.getName());
    assertEquals(LinkState.HOLDING, state.getLinkState());
  }
}
