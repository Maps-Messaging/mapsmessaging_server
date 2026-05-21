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

package io.mapsmessaging.network.protocol.impl.local;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class LoopbackEngineExecutorTest {

  @Test
  void submitRunsOnDifferentThread() throws Exception {
    LoopbackEngineExecutor executor =
        new LoopbackEngineExecutor(1, 32, "test-loopback", false);

    try {
      long callerThreadId = Thread.currentThread().getId();
      CountDownLatch latch = new CountDownLatch(1);
      AtomicReference<Long> executionThreadId = new AtomicReference<>();

      boolean accepted = executor.submit(() -> {
        executionThreadId.set(Thread.currentThread().getId());
        latch.countDown();
      });

      assertTrue(accepted);
      assertTrue(latch.await(2, TimeUnit.SECONDS));
      assertNotNull(executionThreadId.get());
      assertNotEquals(callerThreadId, executionThreadId.get());
      assertTrue(executor.awaitIdle(2, TimeUnit.SECONDS));
      assertEquals(1L, executor.getExecutedCount());
    } finally {
      executor.shutdown();
    }
  }

  @Test
  void awaitIdleWaitsForAllTasks() throws Exception {
    int poolSize = 2;
    LoopbackEngineExecutor executor =
        new LoopbackEngineExecutor(poolSize, 128, "test-loopback", false);

    try {
      int taskCount = 100;

      CountDownLatch firstWorkersStarted = new CountDownLatch(poolSize);
      CountDownLatch release = new CountDownLatch(1);
      AtomicInteger ran = new AtomicInteger();

      for (int i = 0; i < taskCount; i++) {
        boolean accepted = executor.submit(() -> {
          firstWorkersStarted.countDown();
          try {
            release.await(2, TimeUnit.SECONDS);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
          ran.incrementAndGet();
        });
        assertTrue(accepted);
      }

      assertTrue(firstWorkersStarted.await(2, TimeUnit.SECONDS));
      release.countDown();

      assertTrue(executor.awaitIdle(5, TimeUnit.SECONDS));
      assertEquals(taskCount, ran.get());
      assertEquals(taskCount, executor.getExecutedCount());
      assertEquals(0L, executor.getRejectedCount());
    } finally {
      executor.shutdown();
    }
  }

  @Test
  void rejectsWhenQueueIsFull() throws Exception {
    LoopbackEngineExecutor executor = new LoopbackEngineExecutor(1, 1, "test-loopback", false);
    try {
      CountDownLatch task1Started = new CountDownLatch(1);
      CountDownLatch holdWorker = new CountDownLatch(1);
      boolean accepted1 = executor.submit(() -> {
        task1Started.countDown();
        try {
          holdWorker.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      });
      assertTrue(accepted1);
      assertTrue(task1Started.await(2, TimeUnit.SECONDS));

      CountDownLatch task2Created = new CountDownLatch(1);
      boolean accepted2 = executor.submit(task2Created::countDown);
      assertTrue(accepted2);

      assertTrue(waitForQueueSize(executor, 1, 2, TimeUnit.SECONDS));

      boolean accepted3 = executor.submit(() -> { });
      assertFalse(accepted3);
      assertTrue(executor.getRejectedCount() >= 1L);

      holdWorker.countDown();

      assertTrue(executor.awaitIdle(3, TimeUnit.SECONDS));
      assertTrue(executor.getExecutedCount() >= 2L);
    } finally {
      executor.shutdown();
    }
  }

  private static boolean waitForQueueSize(LoopbackEngineExecutor executor,
                                          int expectedSize,
                                          long timeout,
                                          TimeUnit unit) throws InterruptedException {
    long deadlineNanos = System.nanoTime() + unit.toNanos(timeout);
    while (System.nanoTime() < deadlineNanos) {
      if (executor.getQueueSize() == expectedSize) {
        return true;
      }
      Thread.yield();
    }
    return executor.getQueueSize() == expectedSize;
  }

  @Test
  void exceptionDoesNotKillExecutorThreads() throws Exception {
    LoopbackEngineExecutor executor =
        new LoopbackEngineExecutor(1, 32, "test-loopback", false);

    try {
      CountDownLatch okLatch = new CountDownLatch(1);

      boolean accepted1 = executor.submit(() -> {
        throw new RuntimeException("boom");
      });
      assertTrue(accepted1);

      boolean accepted2 = executor.submit(okLatch::countDown);
      assertTrue(accepted2);

      assertTrue(okLatch.await(2, TimeUnit.SECONDS));
      assertTrue(executor.awaitIdle(2, TimeUnit.SECONDS));

      assertTrue(executor.getFailedCount() >= 1L);
      assertTrue(executor.getExecutedCount() >= 2L);
    } finally {
      executor.shutdown();
    }
  }
}
