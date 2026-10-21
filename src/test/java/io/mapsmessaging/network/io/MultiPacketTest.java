package io.mapsmessaging.network.io;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

class MultiPacketTest {

  @Test
  void sizeConstructorUsesRequestedCapacityAndPacketOperations() {
    MultiPacket packet = new MultiPacket(8, false);
    packet.put(new byte[]{1, 2, 3});
    packet.flip();

    byte[] restored = new byte[3];
    packet.get(restored);

    assertEquals(8, packet.capacity());
    assertArrayEquals(new byte[]{1, 2, 3}, restored);
  }

  @Test
  void byteBufferConstructorUsesProvidedBuffer() {
    ByteBuffer buffer = ByteBuffer.allocate(4);
    buffer.put((byte) 7).flip();

    MultiPacket packet = new MultiPacket(buffer);

    assertSame(buffer, packet.getRawBuffer());
    assertEquals(7, packet.getByte());
  }

  @Test
  void copyConstructorSharesUnderlyingPacketBufferAndAddress() {
    Packet source = new Packet(4, false);
    source.putByte(9);

    TestMultiPacket copy = new TestMultiPacket(source);

    assertSame(source.getRawBuffer(), copy.getRawBuffer());
    assertEquals(source.getFromAddress(), copy.getFromAddress());
  }

  private static final class TestMultiPacket extends MultiPacket {
    TestMultiPacket(Packet packet) {
      super(packet);
    }
  }
}