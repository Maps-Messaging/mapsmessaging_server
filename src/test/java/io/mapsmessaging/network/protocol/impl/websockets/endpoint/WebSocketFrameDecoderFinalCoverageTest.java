package io.mapsmessaging.network.protocol.impl.websockets.endpoint;

import io.mapsmessaging.network.io.Packet;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebSocketFrameDecoderFinalCoverageTest {

  @Test
  void fragmentedTextMessageIsReassembledAcrossContinuationFrame() throws Exception {
    WebSocketFrameDecoder decoder = new WebSocketFrameDecoder();
    Packet destination = new Packet(32, false);

    int first = decoder.decode(
        new Packet(ByteBuffer.wrap(maskedFrame(0x01, "hel".getBytes(StandardCharsets.UTF_8)))),
        destination,
        null);
    assertEquals(0, first);

    int second = decoder.decode(
        new Packet(ByteBuffer.wrap(maskedFrame(0x80, "lo".getBytes(StandardCharsets.UTF_8)))),
        destination,
        null);
    assertEquals(5, second);

    destination.flip();
    byte[] value = new byte[destination.available()];
    destination.get(value);
    assertEquals("hello", new String(value, StandardCharsets.UTF_8));
  }

  @Test
  void pongPayloadIsDeliveredToListenerWithoutApplicationOutput() throws Exception {
    WebSocketFrameDecoder decoder = new WebSocketFrameDecoder();
    WebSocketFrameDecoder.Listener listener = mock(WebSocketFrameDecoder.Listener.class);
    Packet destination = new Packet(8, false);

    assertEquals(
        0,
        decoder.decode(
            new Packet(ByteBuffer.wrap(maskedFrame(0x8A, new byte[]{4, 5, 6}))),
            destination,
            listener));

    verify(listener).onPong(new byte[]{4, 5, 6});
    assertEquals(0, destination.position());
  }

  @Test
  void validCloseFrameSetsCloseStateAndNotifiesListener() throws Exception {
    WebSocketFrameDecoder decoder = new WebSocketFrameDecoder();
    WebSocketFrameDecoder.Listener listener = mock(WebSocketFrameDecoder.Listener.class);
    byte[] payload = new byte[]{0x03, (byte) 0xE8, 'b', 'y', 'e'};

    decoder.decode(
        new Packet(ByteBuffer.wrap(maskedFrame(0x88, payload))),
        new Packet(8, false),
        listener);

    assertTrue(decoder.isCloseReceived());
    verify(listener).onClose(payload);
  }

  @Test
  void oneByteClosePayloadIsProtocolError() {
    WebSocketFrameDecoder decoder = new WebSocketFrameDecoder();

    WebSocketProtocolException failure = assertThrows(
        WebSocketProtocolException.class,
        () -> decoder.decode(
            new Packet(ByteBuffer.wrap(maskedFrame(0x88, new byte[]{1}))),
            new Packet(8, false),
            null));

    assertEquals(1002, failure.getCloseCode());
  }

  private static byte[] maskedFrame(int firstByte, byte[] payload) {
    byte[] mask = new byte[]{1, 2, 3, 4};
    byte[] frame = new byte[6 + payload.length];
    frame[0] = (byte) firstByte;
    frame[1] = (byte) (0x80 | payload.length);
    System.arraycopy(mask, 0, frame, 2, 4);
    for (int i = 0; i < payload.length; i++) {
      frame[6 + i] = (byte) (payload[i] ^ mask[i & 3]);
    }
    return frame;
  }
}