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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FrameHandlerDeadlockTest {

  @Test
  void completion_callback_runs_without_frame_handler_monitor() throws Exception {
    WriteFixture fixture = new WriteFixture();
    TestPacket packet = new TestPacket(new byte[]{1, 2, 3});
    AtomicReference<Boolean> monitorHeld = new AtomicReference<>();
    packet.onComplete = () -> monitorHeld.set(Thread.holdsLock(fixture.writeTask.frameHandler));

    fixture.writeTask.push(packet);
    fixture.runNextCallback();

    Assertions.assertEquals(Boolean.FALSE, monitorHeld.get());
    Assertions.assertEquals(1, packet.completed.get());
  }

  @Test
  void reciprocal_completion_callbacks_do_not_deadlock() throws Exception {
    WriteFixture first = new WriteFixture();
    WriteFixture second = new WriteFixture();
    TestPacket firstInitial = new TestPacket(new byte[]{1});
    TestPacket secondInitial = new TestPacket(new byte[]{2});
    TestPacket firstFollowUp = new TestPacket(new byte[]{3});
    TestPacket secondFollowUp = new TestPacket(new byte[]{4});
    CountDownLatch completionsEntered = new CountDownLatch(2);
    CountDownLatch releaseCompletions = new CountDownLatch(1);
    CountDownLatch writersFinished = new CountDownLatch(2);

    firstInitial.onComplete = () -> {
      completionsEntered.countDown();
      await(releaseCompletions);
      second.writeTask.push(secondFollowUp);
    };
    secondInitial.onComplete = () -> {
      completionsEntered.countDown();
      await(releaseCompletions);
      first.writeTask.push(firstFollowUp);
    };

    first.writeTask.push(firstInitial);
    second.writeTask.push(secondInitial);

    Thread firstWriter = daemonThread(() -> {
      try {
        first.runNextCallback();
      } finally {
        writersFinished.countDown();
      }
    });
    Thread secondWriter = daemonThread(() -> {
      try {
        second.runNextCallback();
      } finally {
        writersFinished.countDown();
      }
    });
    firstWriter.start();
    secondWriter.start();

    Assertions.assertTrue(completionsEntered.await(2, TimeUnit.SECONDS), "Both completion callbacks must be entered");
    releaseCompletions.countDown();
    Assertions.assertTrue(writersFinished.await(2, TimeUnit.SECONDS), "Reciprocal completion callbacks deadlocked");

    first.runUntilIdle();
    second.runUntilIdle();

    Assertions.assertArrayEquals(new byte[]{1, 3}, first.output.toByteArray());
    Assertions.assertArrayEquals(new byte[]{2, 4}, second.output.toByteArray());
    Assertions.assertEquals(1, firstInitial.completed.get());
    Assertions.assertEquals(1, secondInitial.completed.get());
    Assertions.assertEquals(1, firstFollowUp.completed.get());
    Assertions.assertEquals(1, secondFollowUp.completed.get());
  }

  private static Thread daemonThread(Runnable runnable) {
    Thread thread = new Thread(runnable);
    thread.setDaemon(true);
    return thread;
  }

  private static void await(CountDownLatch latch) {
    try {
      if (!latch.await(2, TimeUnit.SECONDS)) {
        throw new AssertionError("Timed out waiting for reciprocal completion callback");
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new AssertionError("Interrupted while coordinating completion callback", e);
    }
  }

  private static class WriteFixture {
    private static final int MAXIMUM_CALLBACKS = 100;

    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final Deque<Runnable> callbacks = new ConcurrentLinkedDeque<>();
    private final WriteTask writeTask;

    private WriteFixture() throws Exception {
      SelectorCallback callback = mock(SelectorCallback.class);
      SelectorTask selectorTask = mock(SelectorTask.class);
      Logger logger = mock(Logger.class);
      EndPoint endPoint = mock(EndPoint.class);
      EndPointStatus endPointStatus = mock(EndPointStatus.class);
      AtomicReference<WriteTask> taskReference = new AtomicReference<>();

      when(callback.getEndPoint()).thenReturn(endPoint);
      when(endPoint.getEndPointStatus()).thenReturn(endPointStatus);
      when(endPoint.sendPacket(any(Packet.class))).thenAnswer(invocation -> {
        Packet packet = invocation.getArgument(0);
        ByteBuffer buffer = packet.getRawBuffer();
        int length = buffer.remaining();
        byte[] data = new byte[length];
        buffer.get(data);
        output.writeBytes(data);
        return length;
      });
      doAnswer(invocation -> {
        callbacks.add(() -> taskReference.get().handleWrite());
        return null;
      }).when(selectorTask).register(SelectionKey.OP_WRITE);

      writeTask = new WriteTask(callback, 32, selectorTask, logger);
      taskReference.set(writeTask);
    }

    private void runNextCallback() {
      Runnable callback = callbacks.poll();
      Assertions.assertNotNull(callback, "Expected a registered write callback");
      callback.run();
    }

    private void runUntilIdle() {
      int count = 0;
      while (!callbacks.isEmpty() && count++ < MAXIMUM_CALLBACKS) {
        runNextCallback();
      }
      Assertions.assertTrue(callbacks.isEmpty(), "Write callbacks did not drain");
    }
  }

  private static class TestPacket implements ServerPacket {
    private final byte[] data;
    private final AtomicInteger completed = new AtomicInteger();
    private Runnable onComplete;

    private TestPacket(byte[] data) {
      this.data = data;
    }

    @Override
    public int packFrame(Packet packet) {
      packet.put(data);
      return data.length;
    }

    @Override
    public void complete() {
      completed.incrementAndGet();
      if (onComplete != null) {
        onComplete.run();
      }
    }

    @Override
    public SocketAddress getFromAddress() {
      return null;
    }
  }
}
