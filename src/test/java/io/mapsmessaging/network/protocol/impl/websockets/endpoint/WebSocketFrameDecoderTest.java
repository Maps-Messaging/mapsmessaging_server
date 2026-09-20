package io.mapsmessaging.network.protocol.impl.websockets.endpoint;

import io.mapsmessaging.network.io.Packet;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebSocketFrameDecoderTest {

  @Test
  void maskedTextFrameIsUnmaskedIntoDestination() throws Exception {
    WebSocketFrameDecoder decoder = new WebSocketFrameDecoder();
    Packet source = new Packet(ByteBuffer.wrap(maskedFrame(0x81, "hello".getBytes(StandardCharsets.UTF_8))));
    Packet destination = new Packet(32, false);

    assertEquals(5, decoder.decode(source, destination, null));

    destination.flip();
    byte[] output = new byte[destination.available()];
    destination.get(output);
    assertEquals("hello", new String(output, StandardCharsets.UTF_8));
    assertFalse(decoder.hasPendingOutput());
  }

  @Test
  void unmaskedClientFrameIsRejectedAsProtocolError() {
    WebSocketFrameDecoder decoder = new WebSocketFrameDecoder();
    Packet source = new Packet(ByteBuffer.wrap(new byte[]{(byte) 0x81, 0x01, 'x'}));

    WebSocketProtocolException failure = assertThrows(
        WebSocketProtocolException.class,
        () -> decoder.decode(source, new Packet(16, false), null));

    assertEquals(1002, failure.getCloseCode());
  }

  @Test
  void pingPayloadIsDeliveredToListener() throws Exception {
    WebSocketFrameDecoder decoder = new WebSocketFrameDecoder();
    WebSocketFrameDecoder.Listener listener = mock(WebSocketFrameDecoder.Listener.class);
    Packet source = new Packet(ByteBuffer.wrap(maskedFrame(0x89, new byte[]{1,2,3})));

    decoder.decode(source, new Packet(8, false), listener);

    verify(listener).onPing(new byte[]{1,2,3});
  }

  @Test
  void invalidUtf8TextIsRejectedWithInvalidPayloadCode() {
    WebSocketFrameDecoder decoder = new WebSocketFrameDecoder();
    Packet source = new Packet(ByteBuffer.wrap(maskedFrame(0x81, new byte[]{(byte) 0xC0})));

    WebSocketProtocolException failure = assertThrows(
        WebSocketProtocolException.class,
        () -> decoder.decode(source, new Packet(8, false), null));

    assertEquals(1007, failure.getCloseCode());
  }

  private static byte[] maskedFrame(int firstByte, byte[] payload) {
    byte[] mask = new byte[]{1,2,3,4};
    byte[] frame = new byte[2 + 4 + payload.length];
    frame[0] = (byte) firstByte;
    frame[1] = (byte) (0x80 | payload.length);
    System.arraycopy(mask, 0, frame, 2, 4);
    for (int i = 0; i < payload.length; i++) {
      frame[6 + i] = (byte) (payload[i] ^ mask[i & 3]);
    }
    return frame;
  }
}
