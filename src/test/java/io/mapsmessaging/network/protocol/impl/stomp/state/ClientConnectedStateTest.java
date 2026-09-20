package io.mapsmessaging.network.protocol.impl.stomp.state;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.network.protocol.impl.stomp.StompProtocol;
import io.mapsmessaging.network.protocol.impl.stomp.frames.Frame;
import io.mapsmessaging.network.protocol.impl.stomp.frames.Send;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ClientConnectedStateTest {

  @Test
  void outboundMessageIsWrappedAsStompSendAndCompletionIsRetained() {
    SessionState engine = mock(SessionState.class);
    StompProtocol protocol = mock(StompProtocol.class);
    when(engine.getProtocol()).thenReturn(protocol);
    when(protocol.isBase64Encode()).thenReturn(false);
    when(engine.send(any(Frame.class))).thenReturn(true);

    MessageBuilder builder = new MessageBuilder();
    builder.setOpaqueData(new byte[]{4, 5, 6});
    Message message = builder.build();
    AtomicBoolean completed = new AtomicBoolean(false);

    assertTrue(
        new ClientConnectedState().sendMessage(
            engine,
            "/topic/test",
            null,
            message,
            () -> completed.set(true)
        )
    );

    ArgumentCaptor<Frame> captor = ArgumentCaptor.forClass(Frame.class);
    verify(engine).send(captor.capture());
    Send frame = assertInstanceOf(Send.class, captor.getValue());
    assertTrue(frame.isValid());
    assertEquals("/topic/test", frame.getDestination());
    assertArrayEquals(new byte[]{4, 5, 6}, frame.getData());

    frame.complete();
    assertTrue(completed.get());
  }

  @Test
  void base64ModeIsAppliedToPayloadButDataRoundTrips() {
    SessionState engine = mock(SessionState.class);
    StompProtocol protocol = mock(StompProtocol.class);
    when(engine.getProtocol()).thenReturn(protocol);
    when(protocol.isBase64Encode()).thenReturn(true);
    when(engine.send(any(Frame.class))).thenReturn(true);

    MessageBuilder builder = new MessageBuilder();
    builder.setOpaqueData(new byte[]{1, 2});
    Message message = builder.build();

    new ClientConnectedState().sendMessage(
        engine, "/topic/base64", null, message, () -> {}
    );

    ArgumentCaptor<Frame> captor = ArgumentCaptor.forClass(Frame.class);
    verify(engine).send(captor.capture());
    Send frame = (Send) captor.getValue();
    assertArrayEquals(new byte[]{1, 2}, frame.getData());
  }
}
