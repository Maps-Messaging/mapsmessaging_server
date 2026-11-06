package io.mapsmessaging.network.protocol.impl.nats.state;

import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.network.protocol.impl.nats.frames.MsgFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ClientConnectedStateTest {

  @Test
  void outboundMessageIsWrappedAsNatsMsgAndCompletionIsRetained() {
    SessionState engine = mock(SessionState.class);
    Message message = mock(Message.class);
    byte[] payload = new byte[]{1, 2, 3};
    when(message.getOpaqueData()).thenReturn(payload);
    when(engine.send(any(NatsFrame.class))).thenReturn(true);
    AtomicBoolean completed = new AtomicBoolean(false);

    boolean sent = new ClientConnectedState().sendMessage(
        engine,
        "vehicle.state",
        null,
        message,
        () -> completed.set(true)
    );

    assertTrue(sent);
    ArgumentCaptor<NatsFrame> captor = ArgumentCaptor.forClass(NatsFrame.class);
    verify(engine).send(captor.capture());
    MsgFrame frame = assertInstanceOf(MsgFrame.class, captor.getValue());
    assertEquals("vehicle.state", frame.getSubject());
    assertArrayEquals(payload, frame.getPayload());

    frame.complete();
    assertTrue(completed.get());
  }

  @Test
  void engineSendResultIsPropagated() {
    SessionState engine = mock(SessionState.class);
    Message message = mock(Message.class);
    when(message.getOpaqueData()).thenReturn(new byte[0]);
    when(engine.send(any(NatsFrame.class))).thenReturn(false);

    assertFalse(
        new ClientConnectedState().sendMessage(
            engine, "subject", null, message, () -> {}
        )
    );
  }
}
