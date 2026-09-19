package io.mapsmessaging.network.io;

import io.mapsmessaging.network.protocol.EndOfBufferException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

class PacketTest {

  @AfterEach
  void resetEraseBuffer() {
    Packet.setERASE_BUFFER(false);
  }

  @Test
  void primitivePutGetAndPositionOperationsRoundTrip() {
    Packet packet = new Packet(16, false);
    packet.putByte(0xff);
    packet.putShort(0x1234);
    packet.put((byte) 0x55);

    assertEquals(4, packet.position());
    assertTrue(packet.hasData());

    packet.flip();
    assertEquals(4, packet.available());
    assertEquals(0xff, packet.getByte());
    assertEquals(0x1234, packet.getShort());
    assertEquals((byte) 0x55, packet.get());
    assertFalse(packet.hasRemaining());
  }

  @Test
  void peekDoesNotAdvanceAndThrowsAtEndOfBuffer() throws Exception {
    Packet packet = new Packet(ByteBuffer.wrap(new byte[]{10, 20}));

    assertEquals(10, packet.peek());
    assertEquals(0, packet.position());
    assertEquals(10, packet.getByte());
    assertEquals(20, packet.peek());

    packet.get();
    assertThrows(EndOfBufferException.class, packet::peek);
  }

  @Test
  void clearCanErasePreviouslyWrittenBytes() {
    Packet.setERASE_BUFFER(true);
    Packet packet = new Packet(4, false);
    packet.put(new byte[]{1, 2, 3, 4});

    packet.clear();

    assertEquals(0, packet.position());
    for (int index = 0; index < packet.capacity(); index++) {
      assertEquals(0, packet.get(index));
    }
  }

  @Test
  void compactPreservesUnreadDataOrClearsFullyConsumedBuffer() {
    Packet partial = new Packet(ByteBuffer.wrap(new byte[]{1, 2, 3, 4}));
    partial.get();
    partial.get();
    partial.compact();

    assertEquals(2, partial.position());
    partial.flip();
    assertEquals(3, partial.getByte());
    assertEquals(4, partial.getByte());

    Packet consumed = new Packet(ByteBuffer.wrap(new byte[]{9}));
    consumed.get();
    consumed.compact();
    assertEquals(0, consumed.position());
    assertEquals(consumed.capacity(), consumed.limit());
  }

  @Test
  void stringRenderingShowsAddressHexAndPrintableViewWithoutMovingPosition() {
    Packet packet = new Packet(ByteBuffer.wrap(new byte[]{0x41, 0x01, 0x42}));
    packet.setFromAddress(new InetSocketAddress("127.0.0.1", 14550));

    int before = packet.position();
    String text = packet.toString();

    assertEquals(before, packet.position());
    assertTrue(text.contains("From:"));
    assertTrue(text.contains("41,01,42"));
    assertTrue(text.endsWith("[A#B]"));
  }
}
