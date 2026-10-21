/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 */

package io.mapsmessaging.network.io;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

import static org.junit.jupiter.api.Assertions.*;

class PacketBufferContractTest {

  @Nested
  class HappyPath {

    @Test
    void flipExposesExactlyWrittenBytes() {
      Packet packet = new Packet(8, false);
      packet.put(new byte[]{1, 2, 3});

      packet.flip();

      assertEquals(0, packet.position());
      assertEquals(3, packet.limit());
      assertEquals(3, packet.available());
      assertArrayEquals(new byte[]{1, 2, 3}, readRemaining(packet));
    }

    @Test
    void compactPreservesUnreadTailAndReturnsToWriteMode() {
      Packet packet = new Packet(8, false);
      packet.put(new byte[]{1, 2, 3, 4});
      packet.flip();
      assertEquals(1, packet.get());

      packet.compact();

      assertEquals(3, packet.position());
      assertEquals(8, packet.limit());
      packet.put((byte) 5);
      packet.flip();
      assertArrayEquals(new byte[]{2, 3, 4, 5}, readRemaining(packet));
    }

    @Test
    void clearResetsPositionAndLimitWithoutChangingCapacity() {
      Packet packet = new Packet(8, false);
      packet.put(new byte[]{1, 2, 3});
      packet.flip();
      packet.get();

      packet.clear();

      assertEquals(0, packet.position());
      assertEquals(8, packet.limit());
      assertEquals(8, packet.capacity());
    }

    @Test
    void putPacketTransfersOnlySourceRemainingBytesAndConsumesSource() {
      Packet source = new Packet(8, false);
      source.put(new byte[]{1, 2, 3, 4});
      source.flip();
      source.get();

      Packet destination = new Packet(8, false);
      destination.put(source);

      assertFalse(source.hasRemaining());
      destination.flip();
      assertArrayEquals(new byte[]{2, 3, 4}, readRemaining(destination));
    }
  }

  @Nested
  class SadPath {

    @Test
    void putBeyondCapacityThrowsBufferOverflow() {
      Packet packet = new Packet(2, false);
      packet.put(new byte[]{1, 2});

      assertThrows(BufferOverflowException.class, () -> packet.put((byte) 3));
    }

    @Test
    void getBeyondLimitThrowsBufferUnderflow() {
      Packet packet = new Packet(2, false);
      packet.put((byte) 1);
      packet.flip();
      packet.get();

      assertThrows(BufferUnderflowException.class, packet::get);
    }

    @Test
    void invalidPositionIsRejectedByUnderlyingBuffer() {
      Packet packet = new Packet(4, false);
      assertThrows(IllegalArgumentException.class, () -> packet.position(5));
    }
  }

  @Nested
  class Murphy {

    @Test
    void repeatedFlipCompactAppendCyclesPreserveByteOrder() {
      Packet packet = new Packet(16, false);
      packet.put(new byte[]{1, 2, 3, 4});

      for (int round = 0; round < 3; round++) {
        packet.flip();
        assertEquals(round + 1, packet.get() & 0xff);
        packet.compact();
        packet.put((byte) (5 + round));
      }

      packet.flip();
      assertArrayEquals(new byte[]{4, 5, 6, 7}, readRemaining(packet));
    }

    @Test
    void directAndHeapPacketsHaveIdenticalVisibleSemantics() {
      Packet heap = new Packet(8, false);
      Packet direct = new Packet(8, true);

      for (Packet packet : new Packet[]{heap, direct}) {
        packet.put(new byte[]{9, 8, 7});
        packet.flip();
        assertArrayEquals(new byte[]{9, 8, 7}, readRemaining(packet));
      }
    }

    @Test
    void zeroLengthPacketHasNoRemainingData() {
      Packet packet = new Packet(0, false);
      packet.flip();

      assertFalse(packet.hasRemaining());
      assertEquals(0, packet.available());
    }

    @Test
    void compactOnFullyConsumedPacketBehavesLikeClear() {
      Packet packet = new Packet(4, false);
      packet.put(new byte[]{1, 2});
      packet.flip();
      packet.get();
      packet.get();

      packet.compact();

      assertEquals(0, packet.position());
      assertEquals(4, packet.limit());
    }
  }

  private static byte[] readRemaining(Packet packet) {
    byte[] data = new byte[packet.available()];
    packet.get(data);
    return data;
  }
}
