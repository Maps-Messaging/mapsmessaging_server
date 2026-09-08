/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.network.io.impl;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointStatus;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.ServerPacket;
import io.mapsmessaging.network.io.ServerPublishPacket;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FrameHandlerTest {

  @Test
  void blocked_and_partial_coalesced_write_is_retained_until_complete() throws Exception {
    TestPacket frame = new TestPacket(new byte[]{1, 2, 3, 4, 5, 6, 7});
    WriteFixture fixture = new WriteFixture(16, new AlternatingBlockedWriter(2));

    fixture.writeTask.push(frame);
    fixture.runUntilIdle();

    Assertions.assertArrayEquals(frame.data, fixture.output.toByteArray());
    Assertions.assertEquals(1, frame.completed.get());
    Assertions.assertTrue(fixture.registrationCount.get() > 1);
    verify(fixture.selectorTask).cancel(SelectionKey.OP_WRITE);
  }

  @Test
  void advanced_segments_survive_blocking_at_every_boundary() throws Exception {
    byte[] header = new byte[]{10, 11, 12, 13};
    byte[] payload = new byte[]{20, 21, 22, 23, 24, 25, 26};
    SegmentedTestPacket frame = new SegmentedTestPacket(header, payload, new byte[]{0});
    WriteFixture fixture = new WriteFixture(8, new AlternatingBlockedWriter(2));

    fixture.writeTask.push(frame);
    fixture.runUntilIdle();

    Assertions.assertArrayEquals(join(header, payload, new byte[]{0}), fixture.output.toByteArray());
    Assertions.assertEquals(1, frame.completed.get());
  }

  @Test
  void mixed_standard_and_advanced_frames_preserve_fifo_and_completion_order() throws Exception {
    List<Integer> completionOrder = new ArrayList<>();
    TestPacket first = new TestPacket(new byte[]{1, 2}, 1, completionOrder);
    TestPacket second = new TestPacket(new byte[]{3}, 2, completionOrder);
    SegmentedTestPacket publish = new SegmentedTestPacket(new byte[]{4, 5}, new byte[]{6, 7, 8}, new byte[0], 3, completionOrder);
    TestPacket last = new TestPacket(new byte[]{9, 10}, 4, completionOrder);
    WriteFixture fixture = new WriteFixture(8, new MaximumWriteWriter(3));

    fixture.writeTask.push(first);
    fixture.writeTask.push(second);
    fixture.writeTask.push(publish);
    fixture.writeTask.push(last);
    fixture.runUntilIdle();

    Assertions.assertArrayEquals(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, fixture.output.toByteArray());
    Assertions.assertEquals(List.of(1, 2, 3, 4), completionOrder);
  }

  @Test
  void multiple_advanced_frames_progress_with_one_shot_write_callbacks() throws Exception {
    WriteFixture fixture = new WriteFixture(4, new MaximumWriteWriter(Integer.MAX_VALUE));
    List<SegmentedTestPacket> frames = new ArrayList<>();
    for (int index = 0; index < 250; index++) {
      SegmentedTestPacket frame = new SegmentedTestPacket(new byte[]{(byte) index}, new byte[]{(byte) (index + 1)}, new byte[0]);
      frames.add(frame);
      fixture.writeTask.push(frame);
    }

    fixture.runUntilIdle();

    Assertions.assertEquals(500, fixture.output.size());
    Assertions.assertTrue(frames.stream().allMatch(frame -> frame.completed.get() == 1));
    Assertions.assertTrue(fixture.registrationCount.get() >= 3);
  }

  @Test
  void replay_larger_than_reported_stall_threshold_is_fully_drained() throws Exception {
    int frameCount = 384;
    int payloadSize = 8 * 1024;
    ByteArrayOutputStream expected = new ByteArrayOutputStream(frameCount * (payloadSize + 2));
    List<SegmentedTestPacket> frames = new ArrayList<>();
    WriteFixture fixture = new WriteFixture(32, new AlternatingBlockedWriter(4096));
    for (int index = 0; index < frameCount; index++) {
      byte[] header = new byte[]{(byte) (index >>> 8), (byte) index};
      byte[] payload = new byte[payloadSize];
      java.util.Arrays.fill(payload, (byte) index);
      expected.writeBytes(header);
      expected.writeBytes(payload);
      SegmentedTestPacket frame = new SegmentedTestPacket(header, payload, new byte[0]);
      frames.add(frame);
      fixture.writeTask.push(frame);
    }

    fixture.runUntilIdle();

    Assertions.assertTrue(fixture.output.size() > 2_500_000);
    Assertions.assertArrayEquals(expected.toByteArray(), fixture.output.toByteArray());
    Assertions.assertTrue(frames.stream().allMatch(frame -> frame.completed.get() == 1));
  }

  @Test
  void advanced_header_larger_than_configured_buffer_is_grown_and_sent() throws Exception {
    byte[] header = sequence(73, 11);
    byte[] payload = sequence(31, 101);
    SegmentedTestPacket frame = new SegmentedTestPacket(header, payload, new byte[0]);
    WriteFixture fixture = new WriteFixture(8, new MaximumWriteWriter(7));

    fixture.writeTask.push(frame);
    fixture.runUntilIdle();

    Assertions.assertArrayEquals(join(header, payload), fixture.output.toByteArray());
    Assertions.assertEquals(1, frame.completed.get());
    verify(fixture.endPointStatus, times(4)).incrementOverFlow();
  }

  @Test
  void standard_frame_larger_than_configured_buffer_is_grown_and_sent() throws Exception {
    byte[] data = sequence(73, 21);
    TestPacket frame = new TestPacket(data);
    WriteFixture fixture = new WriteFixture(8, new MaximumWriteWriter(9));

    fixture.writeTask.push(frame);
    fixture.runUntilIdle();

    Assertions.assertArrayEquals(data, fixture.output.toByteArray());
    Assertions.assertEquals(1, frame.completed.get());
    verify(fixture.endPointStatus, times(4)).incrementOverFlow();
  }

  @Test
  void coalescing_overflow_requeues_the_frame_without_reordering() throws Exception {
    TestPacket first = new TestPacket(sequence(6, 1));
    TestPacket second = new TestPacket(sequence(6, 21));
    TestPacket third = new TestPacket(sequence(2, 41));
    WriteFixture fixture = new WriteFixture(8, new MaximumWriteWriter(Integer.MAX_VALUE));

    fixture.writeTask.push(first);
    fixture.writeTask.push(second);
    fixture.writeTask.push(third);
    fixture.runUntilIdle();

    Assertions.assertArrayEquals(join(first.data, second.data, third.data), fixture.output.toByteArray());
    Assertions.assertEquals(1, first.completed.get());
    Assertions.assertEquals(1, second.completed.get());
    Assertions.assertEquals(1, third.completed.get());
    verify(fixture.endPointStatus).incrementOverFlow();
  }

  @Test
  void frame_enqueued_by_completion_callback_is_not_stranded() throws Exception {
    WriteFixture fixture = new WriteFixture(8, new MaximumWriteWriter(Integer.MAX_VALUE));
    TestPacket second = new TestPacket(new byte[]{3, 4});
    TestPacket first = new TestPacket(new byte[]{1, 2});
    first.onComplete = () -> fixture.writeTask.push(second);

    fixture.writeTask.push(first);
    fixture.runUntilIdle();

    Assertions.assertArrayEquals(new byte[]{1, 2, 3, 4}, fixture.output.toByteArray());
    Assertions.assertEquals(1, first.completed.get());
    Assertions.assertEquals(1, second.completed.get());
  }

  @Test
  void completion_failure_does_not_prevent_later_completions_or_writes() throws Exception {
    TestPacket failing = new TestPacket(new byte[]{1});
    failing.onComplete = () -> {
      throw new IllegalStateException("completion failure");
    };
    TestPacket second = new TestPacket(new byte[]{2});
    WriteFixture fixture = new WriteFixture(8, new MaximumWriteWriter(Integer.MAX_VALUE));

    fixture.writeTask.push(failing);
    fixture.writeTask.push(second);
    fixture.runUntilIdle();

    Assertions.assertArrayEquals(new byte[]{1, 2}, fixture.output.toByteArray());
    Assertions.assertEquals(1, failing.completed.get());
    Assertions.assertEquals(1, second.completed.get());
  }

  @Test
  void io_failure_closes_callback_cancels_write_and_does_not_complete_frame() throws Exception {
    TestPacket frame = new TestPacket(new byte[]{1, 2, 3});
    WriteFixture fixture = new WriteFixture(8, (packet, output, attempt) -> {
      throw new IOException("write failed");
    });

    fixture.writeTask.push(frame);
    fixture.runNextCallback();

    Assertions.assertEquals(0, frame.completed.get());
    verify(fixture.callback).close();
    verify(fixture.selectorTask).cancel(SelectionKey.OP_WRITE);
  }

  @Test
  void io_failure_after_partial_write_does_not_complete_frame() throws Exception {
    TestPacket frame = new TestPacket(new byte[]{1, 2, 3, 4});
    WriteFixture fixture = new WriteFixture(8, (packet, output, attempt) -> {
      if (attempt != 0) {
        throw new IOException("write failed");
      }
      return new MaximumWriteWriter(2).write(packet, output, attempt);
    });

    fixture.writeTask.push(frame);
    fixture.runNextCallback();
    fixture.runNextCallback();

    Assertions.assertArrayEquals(new byte[]{1, 2}, fixture.output.toByteArray());
    Assertions.assertEquals(0, frame.completed.get());
    verify(fixture.callback).close();
    verify(fixture.selectorTask).cancel(SelectionKey.OP_WRITE);
  }

  @Test
  void packing_failure_closes_callback_cancels_write_and_does_not_complete_frame() throws Exception {
    TestPacket frame = new TestPacket(new byte[]{1}) {
      @Override
      public int packFrame(Packet packet) {
        throw new IllegalArgumentException("invalid frame");
      }
    };
    WriteFixture fixture = new WriteFixture(8, new MaximumWriteWriter(Integer.MAX_VALUE));

    fixture.writeTask.push(frame);
    fixture.runNextCallback();

    Assertions.assertEquals(0, frame.completed.get());
    verify(fixture.callback).close();
    verify(fixture.selectorTask).cancel(SelectionKey.OP_WRITE);
  }

  @Test
  void drained_handler_registers_again_for_later_frame() throws Exception {
    WriteFixture fixture = new WriteFixture(8, new MaximumWriteWriter(Integer.MAX_VALUE));
    TestPacket first = new TestPacket(new byte[]{1});
    TestPacket second = new TestPacket(new byte[]{2});

    fixture.writeTask.push(first);
    fixture.runUntilIdle();
    fixture.writeTask.push(second);
    fixture.runUntilIdle();

    Assertions.assertArrayEquals(new byte[]{1, 2}, fixture.output.toByteArray());
    Assertions.assertEquals(2, fixture.registrationCount.get());
    verify(fixture.selectorTask, times(2)).cancel(SelectionKey.OP_WRITE);
  }

  @Test
  void explicit_cancel_preserves_shared_transport_lifecycle_contract() throws Exception {
    WriteFixture fixture = new WriteFixture(32, new MaximumWriteWriter(Integer.MAX_VALUE));

    fixture.writeTask.frameHandler.registerWrite();
    fixture.writeTask.frameHandler.cancel();

    verify(fixture.selectorTask).register(SelectionKey.OP_WRITE);
    verify(fixture.selectorTask).cancel(SelectionKey.OP_WRITE);

    fixture.writeTask.frameHandler.registerWrite();
    verify(fixture.selectorTask, times(2)).register(SelectionKey.OP_WRITE);
  }

  @Test
  void frame_enqueued_while_write_is_being_cancelled_is_registered() throws Exception {
    WriteFixture fixture = new WriteFixture(8, new MaximumWriteWriter(Integer.MAX_VALUE));
    TestPacket first = new TestPacket(new byte[]{1});
    TestPacket second = new TestPacket(new byte[]{2});
    CountDownLatch cancelEntered = new CountDownLatch(1);
    CountDownLatch allowCancel = new CountDownLatch(1);
    doAnswer(invocation -> {
      cancelEntered.countDown();
      Assertions.assertTrue(allowCancel.await(5, TimeUnit.SECONDS));
      return null;
    }).when(fixture.selectorTask).cancel(SelectionKey.OP_WRITE);

    fixture.writeTask.push(first);
    Thread writer = new Thread(fixture::runNextCallback);
    writer.start();
    Assertions.assertTrue(cancelEntered.await(5, TimeUnit.SECONDS));

    Thread producer = new Thread(() -> fixture.writeTask.push(second));
    producer.start();
    allowCancel.countDown();
    writer.join(5_000);
    producer.join(5_000);
    Assertions.assertFalse(writer.isAlive());
    Assertions.assertFalse(producer.isAlive());
    fixture.runUntilIdle();

    Assertions.assertArrayEquals(new byte[]{1, 2}, fixture.output.toByteArray());
    Assertions.assertEquals(1, second.completed.get());
    Assertions.assertTrue(fixture.registrationCount.get() >= 2);
  }

  private static byte[] sequence(int length, int offset) {
    byte[] result = new byte[length];
    for (int index = 0; index < length; index++) {
      result[index] = (byte) (index + offset);
    }
    return result;
  }

  private static byte[] join(byte[]... values) {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    for (byte[] value : values) {
      output.writeBytes(value);
    }
    return output.toByteArray();
  }

  @FunctionalInterface
  private interface PacketWriter {
    int write(Packet packet, ByteArrayOutputStream output, int attempt) throws IOException;
  }

  private static class MaximumWriteWriter implements PacketWriter {
    private final int maximumWriteSize;

    private MaximumWriteWriter(int maximumWriteSize) {
      this.maximumWriteSize = maximumWriteSize;
    }

    @Override
    public int write(Packet packet, ByteArrayOutputStream output, int attempt) {
      ByteBuffer buffer = packet.getRawBuffer();
      int length = Math.min(maximumWriteSize, buffer.remaining());
      byte[] data = new byte[length];
      buffer.get(data);
      output.writeBytes(data);
      return length;
    }
  }

  private static class AlternatingBlockedWriter extends MaximumWriteWriter {
    private AlternatingBlockedWriter(int maximumWriteSize) {
      super(maximumWriteSize);
    }

    @Override
    public int write(Packet packet, ByteArrayOutputStream output, int attempt) {
      if ((attempt & 1) == 0) {
        return 0;
      }
      return super.write(packet, output, attempt);
    }
  }

  private static class WriteFixture {
    private static final int MAXIMUM_CALLBACKS = 10_000;

    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final AtomicInteger writeAttempts = new AtomicInteger();
    private final AtomicInteger registrationCount = new AtomicInteger();
    private final Deque<Runnable> callbacks = new ConcurrentLinkedDeque<>();
    private final SelectorCallback callback;
    private final SelectorTask selectorTask;
    private final EndPointStatus endPointStatus;
    private final WriteTask writeTask;

    private WriteFixture(int bufferSize, PacketWriter packetWriter) throws Exception {
      callback = mock(SelectorCallback.class);
      selectorTask = mock(SelectorTask.class);
      Logger logger = mock(Logger.class);
      EndPoint endPoint = mock(EndPoint.class);
      endPointStatus = mock(EndPointStatus.class);
      AtomicReference<WriteTask> taskReference = new AtomicReference<>();

      when(callback.getEndPoint()).thenReturn(endPoint);
      when(endPoint.getEndPointStatus()).thenReturn(endPointStatus);
      when(endPoint.sendPacket(any(Packet.class))).thenAnswer(invocation -> packetWriter.write(
          invocation.getArgument(0),
          output,
          writeAttempts.getAndIncrement()));
      doAnswer(invocation -> {
        registrationCount.incrementAndGet();
        callbacks.add(() -> taskReference.get().handleWrite());
        return null;
      }).when(selectorTask).register(SelectionKey.OP_WRITE);

      writeTask = new WriteTask(callback, bufferSize, selectorTask, logger);
      taskReference.set(writeTask);
    }

    private void runNextCallback() {
      Runnable callbackTask = callbacks.poll();
      Assertions.assertNotNull(callbackTask, "Expected a registered write callback");
      callbackTask.run();
    }

    private void runUntilIdle() {
      int callbacksRun = 0;
      while (!callbacks.isEmpty() && callbacksRun < MAXIMUM_CALLBACKS) {
        callbacksRun++;
        runNextCallback();
      }
      Assertions.assertTrue(callbacks.isEmpty(), "Write callbacks did not make progress");
    }
  }

  private static class TestPacket implements ServerPacket {
    protected final byte[] data;
    protected final AtomicInteger completed = new AtomicInteger();
    private final int completionId;
    private final List<Integer> completionOrder;
    private Runnable onComplete;

    private TestPacket(byte[] data) {
      this(data, -1, null);
    }

    private TestPacket(byte[] data, int completionId, List<Integer> completionOrder) {
      this.data = data;
      this.completionId = completionId;
      this.completionOrder = completionOrder;
    }

    @Override
    public int packFrame(Packet packet) {
      packet.put(data);
      return data.length;
    }

    @Override
    public void complete() {
      completed.incrementAndGet();
      if (completionOrder != null) {
        completionOrder.add(completionId);
      }
      if (onComplete != null) {
        onComplete.run();
      }
    }

    @Override
    public SocketAddress getFromAddress() {
      return null;
    }
  }

  private static class SegmentedTestPacket extends TestPacket implements ServerPublishPacket {
    private final byte[] payload;
    private final byte[] trailer;

    private SegmentedTestPacket(byte[] header, byte[] payload, byte[] trailer) {
      this(header, payload, trailer, -1, null);
    }

    private SegmentedTestPacket(byte[] header, byte[] payload, byte[] trailer, int completionId, List<Integer> completionOrder) {
      super(header, completionId, completionOrder);
      this.payload = payload;
      this.trailer = trailer;
    }

    @Override
    public Packet[] packAdvancedFrame(Packet packet) {
      packet.put(data);
      List<Packet> packets = new ArrayList<>();
      packets.add(packet);
      packets.add(new Packet(ByteBuffer.wrap(payload)));
      if (trailer.length != 0) {
        packets.add(new Packet(ByteBuffer.wrap(trailer)));
      }
      return packets.toArray(new Packet[0]);
    }
  }
}
