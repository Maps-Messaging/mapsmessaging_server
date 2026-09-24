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
import io.mapsmessaging.network.io.ServerPacket;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.SocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WriteBufferLifecycleTest {

  @Nested
  class HappyPath {

    @Test
    void partialWritePreservesRemainingBytesUntilNextWritableCallback() throws Exception {
      Harness harness = new Harness(16);
      TestPacket frame = new TestPacket(bytes(1, 2, 3, 4, 5, 6));
      AtomicInteger calls = new AtomicInteger();

      when(harness.endPoint.sendPacket(any(Packet.class))).thenAnswer(invocation -> {
        Packet packet = invocation.getArgument(0);
        int maximum = calls.getAndIncrement() == 0 ? 2 : packet.available();
        int sent = Math.min(maximum, packet.available());
        packet.position(packet.position() + sent);
        return sent;
      });

      harness.task.push(frame);
      harness.task.handleWrite();

      assertEquals(0, frame.completions.get(), "frame must not complete after only a partial transport write");

      harness.task.handleWrite();

      assertEquals(1, frame.completions.get());
      verify(harness.endPoint, times(2)).sendPacket(any(Packet.class));
    }

    @Test
    void coalescedFramesCompleteOnlyAfterCombinedBufferIsWritten() throws Exception {
      Harness harness = new Harness(32);
      TestPacket first = new TestPacket(bytes(1, 2));
      TestPacket second = new TestPacket(bytes(3, 4));

      when(harness.endPoint.sendPacket(any(Packet.class))).thenAnswer(invocation -> {
        Packet packet = invocation.getArgument(0);
        int sent = packet.available();
        packet.position(packet.limit());
        return sent;
      });

      harness.task.push(first);
      harness.task.push(second);
      harness.task.handleWrite();

      assertEquals(1, first.completions.get());
      assertEquals(1, second.completions.get());
    }
  }

  @Nested
  class SadPath {

    @Test
    void failedWriteClosesConnectionAndDoesNotCompleteFrame() throws Exception {
      Harness harness = new Harness(16);
      TestPacket frame = new TestPacket(bytes(1, 2, 3));

      when(harness.endPoint.sendPacket(any(Packet.class))).thenThrow(new IOException("forced write failure"));

      harness.task.push(frame);
      harness.task.handleWrite();

      verify(harness.callback).close();
      assertEquals(0, frame.completions.get());
    }

    @Test
    void zeroProgressWriteDoesNotCompleteOrDiscardPendingFrame() throws Exception {
      Harness harness = new Harness(16);
      TestPacket frame = new TestPacket(bytes(1, 2, 3));

      when(harness.endPoint.sendPacket(any(Packet.class))).thenReturn(0);

      harness.task.push(frame);
      harness.task.handleWrite();
      harness.task.handleWrite();

      assertEquals(0, frame.completions.get());
      verify(harness.endPoint, times(2)).sendPacket(any(Packet.class));
    }
  }

  @Nested
  class Murphy {

    @Test
    void oversizedFrameGrowsAssemblyBufferWithoutLosingBytes() throws Exception {
      Harness harness = new Harness(8);
      byte[] payload = new byte[257];
      for (int i = 0; i < payload.length; i++) {
        payload[i] = (byte) i;
      }
      TestPacket frame = new TestPacket(payload);
      List<byte[]> writes = new ArrayList<>();

      when(harness.endPoint.sendPacket(any(Packet.class))).thenAnswer(invocation -> {
        Packet packet = invocation.getArgument(0);
        byte[] data = remaining(packet);
        writes.add(data);
        packet.position(packet.limit());
        return data.length;
      });

      harness.task.push(frame);
      harness.task.handleWrite();

      assertEquals(1, frame.completions.get());
      assertEquals(1, writes.size());
      assertArrayEquals(payload, writes.getFirst());
    }

    @Test
    void repeatedZeroProgressCallbacksNeverSpinInsideOneSelection() throws Exception {
      Harness harness = new Harness(16);
      TestPacket frame = new TestPacket(bytes(1, 2, 3, 4));
      when(harness.endPoint.sendPacket(any(Packet.class))).thenReturn(0);

      harness.task.push(frame);

      assertTimeoutPreemptively(Duration.ofSeconds(1), harness.task::handleWrite);
      assertEquals(0, frame.completions.get());
      verify(harness.endPoint, times(1)).sendPacket(any(Packet.class));
    }

    @Test
    void manySmallFramesAreNeitherDuplicatedNorDropped() throws Exception {
      Harness harness = new Harness(64);
      List<TestPacket> frames = new ArrayList<>();
      List<Byte> transmitted = new ArrayList<>();

      when(harness.endPoint.sendPacket(any(Packet.class))).thenAnswer(invocation -> {
        Packet packet = invocation.getArgument(0);
        while (packet.hasRemaining()) {
          transmitted.add(packet.get());
        }
        return transmitted.size();
      });

      for (int i = 0; i < 100; i++) {
        TestPacket frame = new TestPacket(bytes(i));
        frames.add(frame);
        harness.task.push(frame);
      }

      harness.task.handleWrite();

      assertEquals(100, transmitted.size());
      for (int i = 0; i < 100; i++) {
        assertEquals((byte) i, transmitted.get(i));
        assertEquals(1, frames.get(i).completions.get());
      }
    }
  }

  private static final class Harness {
    private final SelectorCallback callback = mock(SelectorCallback.class);
    private final EndPoint endPoint = mock(EndPoint.class);
    private final SelectorTask selectorTask = mock(SelectorTask.class);
    private final Logger logger = mock(Logger.class);
    private final WriteTask task;

    private Harness(int bufferSize) throws Exception {
      when(callback.getEndPoint()).thenReturn(endPoint);
      task = new WriteTask(callback, bufferSize, selectorTask, logger);
    }
  }

  private static final class TestPacket implements ServerPacket {
    private final byte[] payload;
    private final AtomicInteger completions = new AtomicInteger();

    private TestPacket(byte[] payload) {
      this.payload = payload;
    }

    @Override
    public int packFrame(Packet packet) {
      packet.put(payload);
      return payload.length;
    }

    @Override
    public void complete() {
      completions.incrementAndGet();
    }

    @Override
    public SocketAddress getFromAddress() {
      return null;
    }
  }

  private static byte[] remaining(Packet packet) {
    int position = packet.position();
    byte[] data = new byte[packet.available()];
    packet.get(data);
    packet.position(position);
    return data;
  }

  private static byte[] bytes(int... values) {
    byte[] data = new byte[values.length];
    for (int i = 0; i < values.length; i++) {
      data[i] = (byte) values[i];
    }
    return data;
  }
}
