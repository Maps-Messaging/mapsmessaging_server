/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 */

package io.mapsmessaging.network.protocol;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.ServerPacket;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.PingReq;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.PingReq5;
import io.mapsmessaging.network.protocol.impl.nats.frames.PingFrame;
import io.mapsmessaging.network.protocol.impl.stomp.frames.ClientHeartBeat;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.BufferOverflowException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProtocolFrameSerializationContractTest {

  @Nested
  class HappyPath {

    @Test
    void representativeFramesAppendFromNonZeroPositionWithoutTouchingPrefix() {
      for (ServerPacket frame : representativeFrames()) {
        Packet packet = new Packet(64, false);
        packet.put(new byte[]{99, 98, 97});
        int start = packet.position();

        int encoded = frame.packFrame(packet);

        assertTrue(encoded > 0, frame.getClass().getSimpleName());
        assertEquals(start + encoded, packet.position(), frame.getClass().getSimpleName());
        assertEquals((byte) 99, packet.get(0));
        assertEquals((byte) 98, packet.get(1));
        assertEquals((byte) 97, packet.get(2));
        assertEquals(packet.capacity(), packet.limit(),
            "serializer must leave supplied Packet in write mode: " + frame.getClass().getSimpleName());
      }
    }

    @Test
    void twoFramesCanBeCoalescedIntoSamePacketWithoutCorruptingFirst() {
      Packet packet = new Packet(64, false);
      PingReq mqtt3 = new PingReq();
      PingReq5 mqtt5 = new PingReq5();

      int firstStart = packet.position();
      int firstLength = mqtt3.packFrame(packet);
      byte[] first = slice(packet, firstStart, firstLength);

      int secondStart = packet.position();
      int secondLength = mqtt5.packFrame(packet);

      assertArrayEquals(first, slice(packet, firstStart, firstLength));
      assertEquals(firstStart + firstLength, secondStart);
      assertEquals(secondStart + secondLength, packet.position());
    }
  }

  @Nested
  class SadPath {

    @Test
    void insufficientCapacityThrowsBufferOverflowAndDoesNotFlipPacket() {
      for (ServerPacket frame : representativeFrames()) {
        Packet probe = new Packet(64, false);
        int encoded = frame.packFrame(probe);

        Packet tooSmall = new Packet(Math.max(0, encoded - 1), false);

        assertThrows(BufferOverflowException.class, () -> frame.packFrame(tooSmall),
            frame.getClass().getSimpleName());
        assertEquals(tooSmall.capacity(), tooSmall.limit(),
            "serializer must not flip/compact caller buffer on overflow");
      }
    }
  }

  @Nested
  class Murphy {

    @Test
    void retryAfterOverflowIntoLargerPacketProducesSameBytesAsFirstAttemptWould() {
      for (ServerPacket frame : representativeFrames()) {
        Packet reference = new Packet(64, false);
        int length = frame.packFrame(reference);
        byte[] expected = slice(reference, 0, length);

        Packet tooSmall = new Packet(Math.max(0, length - 1), false);
        assertThrows(BufferOverflowException.class, () -> frame.packFrame(tooSmall));

        Packet retry = new Packet(64, false);
        int retryLength = frame.packFrame(retry);

        assertEquals(length, retryLength);
        assertArrayEquals(expected, slice(retry, 0, retryLength),
            "serializer must be retry-safe after BufferOverflowException: " + frame.getClass().getSimpleName());
      }
    }

    @Test
    void repeatedCoalescingMaintainsByteOrder() {
      Packet packet = new Packet(512, false);
      List<ServerPacket> frames = representativeFrames();

      int expectedPosition = 0;
      for (int round = 0; round < 20; round++) {
        for (ServerPacket frame : frames) {
          int length = frame.packFrame(packet);
          expectedPosition += length;
          assertEquals(expectedPosition, packet.position());
        }
      }
    }
  }

  private static List<ServerPacket> representativeFrames() {
    return List.of(
        new PingReq(),
        new PingReq5(),
        new ClientHeartBeat(),
        new PingFrame());
  }

  private static byte[] slice(Packet packet, int start, int length) {
    byte[] data = new byte[length];
    for (int i = 0; i < length; i++) {
      data[i] = packet.get(start + i);
    }
    return data;
  }
}
