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
import io.mapsmessaging.network.io.EndPointStatus;
import io.mapsmessaging.network.io.Packet;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TcpBufferLifecycleTest {

  @Nested
  class HappyPath {

    @Test
    void fragmentedStreamDataIsPreservedAcrossReads() throws Exception {
      TestHarness harness = new TestHarness(8, bytes(1, 2), bytes(3, 4));
      List<byte[]> observed = new ArrayList<>();

      when(harness.callback.processPacket(any(Packet.class))).thenAnswer(invocation -> {
        Packet packet = invocation.getArgument(0);
        byte[] visible = remaining(packet);
        observed.add(visible);
        if (observed.size() == 1) {
          assertArrayEquals(bytes(1, 2), visible);
          packet.get();
          return false;
        }
        assertArrayEquals(bytes(2, 3, 4), visible);
        while (packet.hasRemaining()) {
          packet.get();
        }
        return true;
      });

      harness.task.read();
      harness.task.read();

      assertEquals(2, observed.size());
    }

    @Test
    void coalescedBytesRemainAvailableInOneRead() throws Exception {
      TestHarness harness = new TestHarness(16, bytes(10, 11, 12, 13));

      when(harness.callback.processPacket(any(Packet.class))).thenAnswer(invocation -> {
        Packet packet = invocation.getArgument(0);
        assertArrayEquals(bytes(10, 11, 12, 13), remaining(packet));
        while (packet.hasRemaining()) {
          packet.get();
        }
        return true;
      });

      harness.task.read();

      verify(harness.callback, times(1)).processPacket(any(Packet.class));
    }

    @Test
    void exactCapacityReadCanBeConsumedAndBufferReused() throws Exception {
      TestHarness harness = new TestHarness(4, bytes(1, 2, 3, 4), bytes(5, 6, 7, 8));

      when(harness.callback.processPacket(any(Packet.class))).thenAnswer(invocation -> {
        Packet packet = invocation.getArgument(0);
        assertEquals(4, packet.available());
        while (packet.hasRemaining()) {
          packet.get();
        }
        return true;
      });

      harness.task.read();
      harness.task.read();

      verify(harness.callback, times(2)).processPacket(any(Packet.class));
    }
  }

  @Nested
  class SadPath {

    @Test
    void negativeReadClosesProtocol() throws Exception {
      SelectorCallback callback = mock(SelectorCallback.class);
      EndPoint endPoint = mock(EndPoint.class);
      Logger logger = mock(Logger.class);

      when(callback.getEndPoint()).thenReturn(endPoint);
      when(endPoint.readPacket(any(Packet.class))).thenReturn(-1);

      ReadTask task = new ReadTask(callback, 8, logger, -1, -1);
      task.read();

      verify(callback).close();
      verify(callback, never()).processPacket(any(Packet.class));
    }

    @Test
    void zeroByteReadDoesNotInventApplicationData() throws Exception {
      SelectorCallback callback = mock(SelectorCallback.class);
      EndPoint endPoint = mock(EndPoint.class);
      Logger logger = mock(Logger.class);

      when(callback.getEndPoint()).thenReturn(endPoint);
      when(endPoint.readPacket(any(Packet.class))).thenReturn(0);

      ReadTask task = new ReadTask(callback, 8, logger, -1, -1);
      task.read();

      verify(callback, never()).processPacket(any(Packet.class));
      verify(callback, never()).close();
    }
  }

  @Nested
  class Murphy {

    @Test
    void manySingleByteReadsDoNotLoseOrDuplicateData() throws Exception {
      byte[][] chunks = new byte[32][];
      for (int i = 0; i < chunks.length; i++) {
        chunks[i] = bytes(i);
      }
      TestHarness harness = new TestHarness(64, chunks);
      List<Byte> received = new ArrayList<>();

      when(harness.callback.processPacket(any(Packet.class))).thenAnswer(invocation -> {
        Packet packet = invocation.getArgument(0);
        while (packet.hasRemaining()) {
          received.add(packet.get());
        }
        return true;
      });

      for (int i = 0; i < chunks.length; i++) {
        harness.task.read();
      }

      assertEquals(32, received.size());
      for (int i = 0; i < received.size(); i++) {
        assertEquals((byte) i, received.get(i));
      }
    }

    @Test
    void repeatedPartialConsumptionPreservesOnlyUnreadTail() throws Exception {
      TestHarness harness = new TestHarness(16, bytes(1, 2, 3), bytes(4), bytes(5));
      List<byte[]> observed = new ArrayList<>();

      when(harness.callback.processPacket(any(Packet.class))).thenAnswer(invocation -> {
        Packet packet = invocation.getArgument(0);
        observed.add(remaining(packet));
        packet.get();
        return false;
      });

      harness.task.read();
      harness.task.read();
      harness.task.read();

      assertArrayEquals(bytes(1, 2, 3), observed.get(0));
      assertArrayEquals(bytes(2, 3, 4), observed.get(1));
      assertArrayEquals(bytes(3, 4, 5), observed.get(2));
    }
  }

  private static final class TestHarness {
    private final SelectorCallback callback = mock(SelectorCallback.class);
    private final EndPoint endPoint = mock(EndPoint.class);
    private final EndPointStatus status = mock(EndPointStatus.class);
    private final Logger logger = mock(Logger.class);
    private final ReadTask task;

    private TestHarness(int bufferSize, byte[]... chunks) throws IOException {
      Deque<byte[]> pending = new ArrayDeque<>(List.of(chunks));
      when(callback.getEndPoint()).thenReturn(endPoint);
      when(endPoint.getEndPointStatus()).thenReturn(status);
      when(endPoint.readPacket(any(Packet.class))).thenAnswer(invocation -> {
        Packet packet = invocation.getArgument(0);
        byte[] chunk = pending.pollFirst();
        if (chunk == null) {
          return 0;
        }
        packet.put(chunk);
        return chunk.length;
      });
      task = new ReadTask(callback, bufferSize, logger, -1, -1);
    }
  }

  private static byte[] remaining(Packet packet) {
    int position = packet.position();
    byte[] bytes = new byte[packet.available()];
    packet.get(bytes);
    packet.position(position);
    return bytes;
  }

  private static byte[] bytes(int... values) {
    byte[] bytes = new byte[values.length];
    for (int i = 0; i < values.length; i++) {
      bytes[i] = (byte) values[i];
    }
    return bytes;
  }
}
