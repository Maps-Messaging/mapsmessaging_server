/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 */

package io.mapsmessaging.network.io.impl;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UdpDatagramBoundaryTest {

  @Nested
  class HappyPath {

    @Test
    void completeDatagramIsDeliveredAsSingleProcessingUnit() throws Exception {
      SocketAddress source = new InetSocketAddress("127.0.0.1", 12001);
      TestHarness harness = new TestHarness(datagram(source, 1, 2, 3, 4));

      when(harness.callback.processPacket(any(Packet.class))).thenAnswer(invocation -> {
        Packet packet = invocation.getArgument(0);
        assertEquals(source, packet.getFromAddress());
        assertArrayEquals(bytes(1, 2, 3, 4), remaining(packet));
        consumeAll(packet);
        return true;
      });

      harness.task.read();

      verify(harness.callback, times(1)).processPacket(any(Packet.class));
    }

    @Test
    void consecutiveDatagramsFromSameSourceRemainIndependent() throws Exception {
      SocketAddress source = new InetSocketAddress("127.0.0.1", 12002);
      TestHarness harness = new TestHarness(
          datagram(source, 1, 2),
          datagram(source, 3, 4));

      List<byte[]> observed = new ArrayList<>();
      when(harness.callback.processPacket(any(Packet.class))).thenAnswer(invocation -> {
        Packet packet = invocation.getArgument(0);
        observed.add(remaining(packet));
        consumeAll(packet);
        return true;
      });

      harness.task.read();
      harness.task.read();

      assertEquals(2, observed.size());
      assertArrayEquals(bytes(1, 2), observed.get(0));
      assertArrayEquals(bytes(3, 4), observed.get(1));
    }

    @Test
    void datagramsFromDifferentSourcesRemainIndependent() throws Exception {
      SocketAddress sourceA = new InetSocketAddress("127.0.0.1", 12003);
      SocketAddress sourceB = new InetSocketAddress("127.0.0.1", 12004);
      TestHarness harness = new TestHarness(
          datagram(sourceA, 10, 11),
          datagram(sourceB, 20, 21));

      List<SocketAddress> sources = new ArrayList<>();
      List<byte[]> observed = new ArrayList<>();

      when(harness.callback.processPacket(any(Packet.class))).thenAnswer(invocation -> {
        Packet packet = invocation.getArgument(0);
        sources.add(packet.getFromAddress());
        observed.add(remaining(packet));
        consumeAll(packet);
        return true;
      });

      harness.task.read();
      harness.task.read();

      assertEquals(List.of(sourceA, sourceB), sources);
      assertArrayEquals(bytes(10, 11), observed.get(0));
      assertArrayEquals(bytes(20, 21), observed.get(1));
    }
  }

  @Nested
  class SadPath {

    @Test
    void endpointMustNotRetainUnreadTailForCrossDatagramReassembly() throws Exception {
      SocketAddress source = new InetSocketAddress("127.0.0.1", 12005);
      TestHarness harness = new TestHarness(datagram(source, 1, 2, 3));

      when(harness.callback.processPacket(any(Packet.class))).thenAnswer(invocation -> {
        Packet packet = invocation.getArgument(0);
        packet.get();
        return false;
      });

      harness.task.read();

      assertTrue(outstandingPackets(harness.task).isEmpty(),
          "UDP endpoint layer must not retain an unread datagram tail for concatenation with a later datagram");
    }

    @Test
    void unreadTailFromFirstDatagramMustNotBePrependedToSecondDatagram() throws Exception {
      SocketAddress source = new InetSocketAddress("127.0.0.1", 12006);
      TestHarness harness = new TestHarness(
          datagram(source, 1, 2, 3),
          datagram(source, 9, 8));

      List<byte[]> observed = new ArrayList<>();
      when(harness.callback.processPacket(any(Packet.class))).thenAnswer(invocation -> {
        Packet packet = invocation.getArgument(0);
        observed.add(remaining(packet));
        if (observed.size() == 1) {
          packet.get();
          return false;
        }
        consumeAll(packet);
        return true;
      });

      harness.task.read();
      harness.task.read();

      assertArrayEquals(bytes(1, 2, 3), observed.get(0));
      assertArrayEquals(bytes(9, 8), observed.get(1),
          "second UDP receive must contain only the second datagram");
    }
  }

  @Nested
  class Murphy {

    @Test
    void manySameSourceDatagramsNeverCreateEndpointReassemblyState() throws Exception {
      SocketAddress source = new InetSocketAddress("127.0.0.1", 12007);
      Datagram[] datagrams = new Datagram[32];
      for (int i = 0; i < datagrams.length; i++) {
        datagrams[i] = datagram(source, i, i + 1, i + 2);
      }
      TestHarness harness = new TestHarness(datagrams);

      when(harness.callback.processPacket(any(Packet.class))).thenAnswer(invocation -> {
        Packet packet = invocation.getArgument(0);
        packet.get();
        return false;
      });

      for (int i = 0; i < datagrams.length; i++) {
        harness.task.read();
        assertTrue(outstandingPackets(harness.task).isEmpty(),
            "UDP endpoint must remain stateless between datagrams even when the protocol leaves bytes unread");
      }
    }

    @Test
    void alternatingSourcesCannotCrossContaminateDatagrams() throws Exception {
      SocketAddress sourceA = new InetSocketAddress("127.0.0.1", 12008);
      SocketAddress sourceB = new InetSocketAddress("127.0.0.1", 12009);
      TestHarness harness = new TestHarness(
          datagram(sourceA, 1, 1, 1),
          datagram(sourceB, 2, 2, 2),
          datagram(sourceA, 3, 3, 3),
          datagram(sourceB, 4, 4, 4));

      List<byte[]> observed = new ArrayList<>();
      when(harness.callback.processPacket(any(Packet.class))).thenAnswer(invocation -> {
        Packet packet = invocation.getArgument(0);
        observed.add(remaining(packet));
        consumeAll(packet);
        return true;
      });

      for (int i = 0; i < 4; i++) {
        harness.task.read();
      }

      assertArrayEquals(bytes(1, 1, 1), observed.get(0));
      assertArrayEquals(bytes(2, 2, 2), observed.get(1));
      assertArrayEquals(bytes(3, 3, 3), observed.get(2));
      assertArrayEquals(bytes(4, 4, 4), observed.get(3));
    }
  }

  @SuppressWarnings("unchecked")
  private static Map<SocketAddress, ?> outstandingPackets(UDPReadTask task) throws Exception {
    Field field = UDPReadTask.class.getDeclaredField("outstandingPacketMap");
    field.setAccessible(true);
    return (Map<SocketAddress, ?>) field.get(task);
  }

  private static final class TestHarness {
    private final SelectorCallback callback = mock(SelectorCallback.class);
    private final EndPoint endPoint = mock(EndPoint.class);
    private final Logger logger = mock(Logger.class);
    private final UDPReadTask task;

    private TestHarness(Datagram... datagrams) throws Exception {
      Deque<Datagram> pending = new ArrayDeque<>(List.of(datagrams));
      when(callback.getEndPoint()).thenReturn(endPoint);
      when(endPoint.readPacket(any(Packet.class))).thenAnswer(invocation -> {
        Packet packet = invocation.getArgument(0);
        Datagram datagram = pending.pollFirst();
        if (datagram == null) {
          return 0;
        }
        packet.setFromAddress(datagram.source());
        packet.put(datagram.payload());
        return datagram.payload().length;
      });
      task = new UDPReadTask(callback, 128, 1000, logger);
    }
  }

  private record Datagram(SocketAddress source, byte[] payload) {
  }

  private static Datagram datagram(SocketAddress source, int... values) {
    return new Datagram(source, bytes(values));
  }

  private static byte[] remaining(Packet packet) {
    int position = packet.position();
    byte[] bytes = new byte[packet.available()];
    packet.get(bytes);
    packet.position(position);
    return bytes;
  }

  private static void consumeAll(Packet packet) {
    while (packet.hasRemaining()) {
      packet.get();
    }
  }

  private static byte[] bytes(int... values) {
    byte[] bytes = new byte[values.length];
    for (int i = 0; i < values.length; i++) {
      bytes[i] = (byte) values[i];
    }
    return bytes;
  }
}
