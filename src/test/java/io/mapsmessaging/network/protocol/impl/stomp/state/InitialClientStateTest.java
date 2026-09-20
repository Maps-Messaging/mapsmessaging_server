package io.mapsmessaging.network.protocol.impl.stomp.state;

import io.mapsmessaging.network.protocol.impl.stomp.StompProtocolException;
import io.mapsmessaging.network.protocol.impl.stomp.frames.Connected;
import io.mapsmessaging.network.protocol.impl.stomp.frames.Frame;
import io.mapsmessaging.network.protocol.impl.stomp.listener.FrameListener;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InitialClientStateTest {

  @Test
  void connectedFrameIsDispatchedAndPostProcessed() throws Exception {
    SessionState engine = mock(SessionState.class);
    Connected frame = mock(Connected.class);
    FrameListener listener = mock(FrameListener.class);
    when(frame.getFrameListener()).thenReturn(listener);

    new InitialClientState().handleFrame(engine, frame, true);

    verify(listener).frameEvent(frame, engine, true);
    verify(listener).postFrameHandling(frame, engine);
  }

  @Test
  void unexpectedFrameIsRejected() {
    SessionState engine = mock(SessionState.class);
    Frame frame = mock(Frame.class);

    assertThrows(
        StompProtocolException.class,
        () -> new InitialClientState().handleFrame(engine, frame, false)
    );
  }

  @Test
  void messagesCannotBeSentBeforeClientSessionIsEstablished() {
    assertFalse(new InitialClientState().sendMessage(null, null, null, null, null));
  }
}
