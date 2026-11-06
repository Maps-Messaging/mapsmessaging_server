package io.mapsmessaging.network.io.connection.state;

import io.mapsmessaging.network.io.connection.EndPointConnection;
import io.mapsmessaging.network.route.link.LinkState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConnectedTest {

  @Test
  void executeSchedulesConfiguredEstablishingState() {
    EndPointConnection connection = mock(EndPointConnection.class);
    State establishing = mock(State.class);
    when(connection.getEstablishingState()).thenReturn(establishing);

    Connected state = new Connected(connection);
    state.execute();

    verify(connection).scheduleState(establishing);
  }

  @Test
  void metadataReflectsTransitionalConnectedState() {
    Connected state = new Connected(mock(EndPointConnection.class));

    assertEquals("Connected", state.getName());
    assertEquals(LinkState.CONNECTING, state.getLinkState());
  }
}
