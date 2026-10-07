package io.mapsmessaging.network.protocol.impl.websockets.endpoint;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.conformance.common.ProtocolRequirement;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("conformance")
@Tag("conformance-core")
@Tag("websocket")
class WebSocketFrameConformanceTest {

  private static final String SPEC = "WebSocket RFC 6455";
  private static final String SOURCE = ProtocolRequirement.WEBSOCKET_RFC6455_SOURCE;

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "RFC 6455 §5.1 and §5.3: frames from client to server MUST be masked",
      source = SOURCE)
  void unmaskedClientFrameIsProtocolError1002() {
    WebSocketProtocolException error = assertThrows(WebSocketProtocolException.class,
        () -> decode(new byte[]{(byte)0x81, 0x01, 'x'}, new WebSocketFrameDecoder()));
    assertEquals(1002, error.getCloseCode());
  }

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "RFC 6455 §5.2: RSV1/RSV2/RSV3 MUST be zero unless an extension defining them was negotiated",
      source = SOURCE)
  void reservedBitsWithoutExtensionAreRejected() {
    WebSocketProtocolException error = assertThrows(WebSocketProtocolException.class,
        () -> decode(maskedFrame(0xC1, new byte[]{'x'}), new WebSocketFrameDecoder()));
    assertEquals(1002, error.getCloseCode());
  }

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "RFC 6455 §5.2 and §5.4: unknown opcodes are protocol errors",
      source = SOURCE)
  void reservedOpcodeIsRejected() {
    WebSocketProtocolException error = assertThrows(WebSocketProtocolException.class,
        () -> decode(maskedFrame(0x83, new byte[0]), new WebSocketFrameDecoder()));
    assertEquals(1002, error.getCloseCode());
  }

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "RFC 6455 §5.5: control frames MUST have FIN=1 and payload length <=125",
      source = SOURCE)
  void fragmentedOrOversizedControlFramesAreRejected() {
    assertEquals(1002, assertThrows(WebSocketProtocolException.class,
        () -> decode(maskedFrame(0x09, new byte[]{1}), new WebSocketFrameDecoder())).getCloseCode());
    byte[] oversized = new byte[2 + 2 + 4 + 126];
    oversized[0] = (byte)0x89;
    oversized[1] = (byte)(0x80 | 126);
    oversized[2] = 0;
    oversized[3] = 126;
    assertEquals(1002, assertThrows(WebSocketProtocolException.class,
        () -> decode(oversized, new WebSocketFrameDecoder())).getCloseCode());
  }

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "RFC 6455 §5.2: extended payload lengths MUST use the minimal encoding and 64-bit length MSB MUST be zero",
      source = SOURCE)
  void invalidExtendedLengthsAreRejected() {
    assertEquals(1002, assertThrows(WebSocketProtocolException.class,
        () -> decode(maskedExtended126(125), new WebSocketFrameDecoder())).getCloseCode());
    assertEquals(1002, assertThrows(WebSocketProtocolException.class,
        () -> decode(maskedExtended127(65535, false), new WebSocketFrameDecoder())).getCloseCode());
    assertEquals(1002, assertThrows(WebSocketProtocolException.class,
        () -> decode(maskedExtended127(65536, true), new WebSocketFrameDecoder())).getCloseCode());
  }

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "RFC 6455 §5.4: continuation frame without an open fragmented message is a protocol error",
      source = SOURCE)
  void strayContinuationFrameIsRejected() {
    assertEquals(1002, assertThrows(WebSocketProtocolException.class,
        () -> decode(maskedFrame(0x80, new byte[]{'x'}), new WebSocketFrameDecoder())).getCloseCode());
  }

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "RFC 6455 §5.4 and §5.5: control frames MAY be injected in the middle of a fragmented message",
      source = SOURCE)
  void pingMayInterleaveFragmentedTextMessage() throws Exception {
    WebSocketFrameDecoder decoder = new WebSocketFrameDecoder();
    WebSocketFrameDecoder.Listener listener = mock(WebSocketFrameDecoder.Listener.class);
    Packet destination = new Packet(32, false);

    decoder.decode(new Packet(ByteBuffer.wrap(maskedFrame(0x01, "hel".getBytes(StandardCharsets.UTF_8)))), destination, listener);
    decoder.decode(new Packet(ByteBuffer.wrap(maskedFrame(0x89, "p".getBytes(StandardCharsets.UTF_8)))), destination, listener);
    decoder.decode(new Packet(ByteBuffer.wrap(maskedFrame(0x80, "lo".getBytes(StandardCharsets.UTF_8)))), destination, listener);

    verify(listener).onPing("p".getBytes(StandardCharsets.UTF_8));
    destination.flip();
    byte[] output = new byte[destination.available()];
    destination.get(output);
    assertEquals("hello", new String(output, StandardCharsets.UTF_8));
  }

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "RFC 6455 §8.1: invalid UTF-8 in a text message MUST fail the connection; status 1007 denotes invalid payload data",
      source = SOURCE)
  void invalidUtf8TextMapsTo1007() {
    WebSocketProtocolException error = assertThrows(WebSocketProtocolException.class,
        () -> decode(maskedFrame(0x81, new byte[]{(byte)0xC0}), new WebSocketFrameDecoder()));
    assertEquals(1007, error.getCloseCode());
  }

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "RFC 6455 §5.5.1 and §7.4: Close payload length and status code/reason UTF-8 MUST be valid",
      source = SOURCE)
  void invalidClosePayloadsAreRejected() {
    assertEquals(1002, assertThrows(WebSocketProtocolException.class,
        () -> decode(maskedFrame(0x88, new byte[]{1}), new WebSocketFrameDecoder())).getCloseCode());
    assertEquals(1002, assertThrows(WebSocketProtocolException.class,
        () -> decode(maskedFrame(0x88, new byte[]{0x03, (byte)0xED}), new WebSocketFrameDecoder())).getCloseCode());
    assertEquals(1007, assertThrows(WebSocketProtocolException.class,
        () -> decode(maskedFrame(0x88, new byte[]{0x03, (byte)0xE8, (byte)0xC0}), new WebSocketFrameDecoder())).getCloseCode());
  }

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "RFC 6455 §5.1: server-to-client frames MUST NOT be masked",
      source = SOURCE)
  void serverWriterProducesUnmaskedBinaryFrame() throws Exception {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    WebSocketFrameWriter writer = new WebSocketFrameWriter(packet -> {
      int count = packet.available();
      byte[] data = new byte[count];
      packet.get(data);
      bytes.writeBytes(data);
      return count;
    });
    Packet payload = new Packet(ByteBuffer.wrap(new byte[]{1,2,3}));
    assertEquals(3, writer.writeBinary(payload));
    byte[] wire = bytes.toByteArray();
    assertEquals((byte)0x82, wire[0]);
    assertEquals(0, wire[1] & 0x80);
    assertEquals(3, wire[1] & 0x7f);
  }

  @Test
  @Disabled("Known WebSocket post-CLOSE buffered-data processing gap: MSG-370")
  @ProtocolRequirement(
      specification = SPEC,
      value = "RFC 6455 §7.1.2: after receiving Close, endpoint starts closing handshake and must not continue normal application-data processing",
      source = SOURCE)
  void applicationDataAfterCloseIsNotDelivered() throws Exception {
    WebSocketFrameDecoder decoder = new WebSocketFrameDecoder();
    Packet destination = new Packet(32, false);
    byte[] close = maskedFrame(0x88, new byte[]{0x03, (byte)0xE8});
    byte[] data = maskedFrame(0x82, new byte[]{1,2,3});
    byte[] both = new byte[close.length + data.length];
    System.arraycopy(close, 0, both, 0, close.length);
    System.arraycopy(data, 0, both, close.length, data.length);
    decoder.decode(new Packet(ByteBuffer.wrap(both)), destination, mock(WebSocketFrameDecoder.Listener.class));
    assertEquals(0, destination.position());
  }

  private static int decode(byte[] wire, WebSocketFrameDecoder decoder) throws Exception {
    return decoder.decode(new Packet(ByteBuffer.wrap(wire)), new Packet(1024, false), null);
  }

  private static byte[] maskedFrame(int firstByte, byte[] payload) {
    byte[] mask = {1,2,3,4};
    byte[] frame = new byte[6 + payload.length];
    frame[0] = (byte)firstByte;
    frame[1] = (byte)(0x80 | payload.length);
    System.arraycopy(mask, 0, frame, 2, 4);
    for (int i=0;i<payload.length;i++) frame[6+i] = (byte)(payload[i] ^ mask[i & 3]);
    return frame;
  }

  private static byte[] maskedExtended126(int length) {
    byte[] frame = new byte[8];
    frame[0]=(byte)0x82; frame[1]=(byte)0xFE;
    frame[2]=(byte)(length>>>8); frame[3]=(byte)length;
    return frame;
  }

  private static byte[] maskedExtended127(long length, boolean highBit) {
    byte[] frame = new byte[14];
    frame[0]=(byte)0x82; frame[1]=(byte)0xFF;
    long value = highBit ? (length | (1L << 63)) : length;
    for(int i=0;i<8;i++) frame[2+i]=(byte)(value >>> (56 - 8*i));
    return frame;
  }
}
