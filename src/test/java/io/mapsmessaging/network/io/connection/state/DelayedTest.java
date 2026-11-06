package io.mapsmessaging.network.io.connection.state;

import io.mapsmessaging.network.io.connection.Constants;
import io.mapsmessaging.network.io.connection.EndPointConnection;
import io.mapsmessaging.network.route.link.LinkState;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DelayedTest {

  @Test
  void executeSchedulesDisconnectedAfterConfiguredDelay() {
    EndPointConnection connection = mock(EndPointConnection.class);
    Delayed state = new Delayed(connection);

    state.execute();

    ArgumentCaptor<State> stateCaptor = ArgumentCaptor.forClass(State.class);
    verify(connection).scheduleState(stateCaptor.capture(), eq(Constants.DELAYED_TIME));
    assertInstanceOf(Disconnected.class, stateCaptor.getValue());
    assertSame(connection, stateCaptor.getValue().getEndPointConnection());
  }

  @Test
  void metadataReflectsFailedLinkDuringBackoff() {
    Delayed state = new Delayed(mock(EndPointConnection.class));

    assertEquals("Delayed", state.getName());
    assertEquals(LinkState.FAILED, state.getLinkState());
  }
}
