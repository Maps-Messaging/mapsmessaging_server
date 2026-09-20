package io.mapsmessaging.network.protocol.impl.satellite.protocol;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

class MessageQueueTest {

  @Test
  void variableWidthUnsignedIntegersRoundTripAcrossSupportedWidths() {
    TestQueue queue = new TestQueue();

    assertEquals(0x7f, queue.roundTrip(0x7f, 1));
    assertEquals(0xbeef, queue.roundTrip(0xbeef, 2));
    assertEquals(0xabcdef, queue.roundTrip(0xabcdef, 3));
    assertEquals(0x12345678, queue.roundTrip(0x12345678, 4));
  }

  @Test
  void invalidWidthsAreRejectedForReadWriteAndValidation() {
    TestQueue queue = new TestQueue();

    assertThrows(IllegalArgumentException.class, () -> queue.write(1, 0));
    assertThrows(IllegalArgumentException.class, () -> queue.write(1, 5));
    assertThrows(IllegalArgumentException.class, () -> queue.read(new byte[]{1}, 0));
    assertThrows(IllegalArgumentException.class, () -> queue.require(1, 5));
  }

  @Test
  void valuesOutsideSelectedUnsignedWidthAreRejected() {
    TestQueue queue = new TestQueue();

    assertThrows(IllegalArgumentException.class, () -> queue.write(256, 1));
    assertThrows(IllegalArgumentException.class, () -> queue.require(65536, 2));
    assertThrows(IllegalArgumentException.class, () -> queue.write(-1, 4));
  }

  private static final class TestQueue extends MessageQueue {
    int roundTrip(int value, int width) {
      ByteBuffer buffer = ByteBuffer.allocate(width);
      putVarUInt(buffer, value, width);
      buffer.flip();
      return getVarUInt(buffer, width);
    }

    void write(int value, int width) {
      putVarUInt(ByteBuffer.allocate(8), value, width);
    }

    int read(byte[] data, int width) {
      return getVarUInt(ByteBuffer.wrap(data), width);
    }

    void require(int value, int width) {
      requireUnsignedInRange(value, width, "value");
    }
  }
}