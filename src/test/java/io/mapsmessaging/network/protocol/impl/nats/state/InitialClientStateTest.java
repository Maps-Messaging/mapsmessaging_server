package io.mapsmessaging.network.protocol.impl.nats.state;

import io.mapsmessaging.network.protocol.impl.nats.NatsProtocolException;
import io.mapsmessaging.network.protocol.impl.nats.frames.ConnectFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.listener.FrameListener;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InitialClientStateTest {

  @Test
  void connectFrameIsDispatchedAndPostProcessed() throws Exception {
    SessionState engine = mock(SessionState.class);
    ConnectFrame frame = mock(ConnectFrame.class);
    FrameListener listener = mock(FrameListener.class);
    when(frame.getListener()).thenReturn(listener);

    new InitialClientState().handleFrame(engine, frame, true);

    verify(listener).frameEvent(frame, engine, true);
    verify(listener).postFrameHandling(frame, engine);
  }

  @Test
  void unexpectedFrameIsRejected() {
    SessionState engine = mock(SessionState.class);
    NatsFrame frame = mock(NatsFrame.class);

    assertThrows(
        NatsProtocolException.class,
        () -> new InitialClientState().handleFrame(engine, frame, false)
    );
  }

  @Test
  void messagesCannotBeSentBeforeConnectCompletes() {
    assertFalse(new InitialClientState().sendMessage(null, null, null, null, null));
  }
}
