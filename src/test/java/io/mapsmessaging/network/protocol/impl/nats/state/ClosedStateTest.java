package io.mapsmessaging.network.protocol.impl.nats.state;

import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ClosedStateTest {

  @Test
  void closedStateRejectsIncomingFrames() {
    IOException failure = assertThrows(
        IOException.class,
        () -> new ClosedState().handleFrame(
            mock(SessionState.class),
            mock(NatsFrame.class),
            true
        )
    );

    assertTrue(failure.getMessage().contains("closed"));
  }

  @Test
  void closedStateNeverSendsMessages() {
    assertFalse(
        new ClosedState().sendMessage(
            mock(SessionState.class),
            "subject",
            null,
            null,
            () -> {}
        )
    );
  }
}
