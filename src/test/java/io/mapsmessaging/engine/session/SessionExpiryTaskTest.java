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

package io.mapsmessaging.engine.session;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class SessionExpiryTaskTest {

  @Test
  void cancelBeforeTimerFiresPreventsPipelineCleanup() {
    ManualExpiryScheduler scheduler = new ManualExpiryScheduler();
    ManualExecutor executor = new ManualExecutor();
    AtomicInteger cleanupCalls = new AtomicInteger();

    SessionExpiryTask task = SessionExpiryTask.schedule(
        scheduler, executor, cleanupCalls::incrementAndGet, 30, TimeUnit.SECONDS);

    assertTrue(task.cancel(false));
    scheduler.fire();

    assertTrue(task.isDone());
    assertTrue(task.isCancelled());
    assertEquals(0, executor.queuedTasks());
    assertEquals(0, cleanupCalls.get());
  }

  @Test
  void cancelAfterTimerFiresCancelsQueuedPipelineCleanup() {
    ManualExpiryScheduler scheduler = new ManualExpiryScheduler();
    ManualExecutor executor = new ManualExecutor();
    AtomicInteger cleanupCalls = new AtomicInteger();

    SessionExpiryTask task = SessionExpiryTask.schedule(
        scheduler, executor, cleanupCalls::incrementAndGet, 30, TimeUnit.SECONDS);

    scheduler.fire();

    assertFalse(task.isDone());
    assertEquals(1, executor.queuedTasks());
    assertTrue(task.cancel(false));

    executor.runNext();

    assertTrue(task.isCancelled());
    assertEquals(0, cleanupCalls.get());
  }

  @Test
  void completionRepresentsPipelineCleanupNotTimerTrigger() throws Exception {
    ManualExpiryScheduler scheduler = new ManualExpiryScheduler();
    ManualExecutor executor = new ManualExecutor();
    AtomicInteger cleanupCalls = new AtomicInteger();

    SessionExpiryTask task = SessionExpiryTask.schedule(
        scheduler, executor, cleanupCalls::incrementAndGet, 30, TimeUnit.SECONDS);

    scheduler.fire();

    assertFalse(task.isDone());
    assertEquals(0, cleanupCalls.get());

    executor.runNext();

    assertTrue(task.isDone());
    assertFalse(task.isCancelled());
    assertEquals(1, cleanupCalls.get());
    assertNull(task.get());
  }

  private static final class ManualExpiryScheduler implements SessionExpiryScheduler {

    private FutureTask<Void> timer;

    @Override
    public Future<?> schedule(Runnable task, long delay, TimeUnit unit) {
      timer = new FutureTask<>(task, null);
      return timer;
    }

    void fire() {
      assertNotNull(timer);
      timer.run();
    }
  }

  private static final class ManualExecutor extends AbstractExecutorService {

    private final Queue<Runnable> tasks = new ConcurrentLinkedQueue<>();
    private boolean shutdown;

    @Override
    public void shutdown() {
      shutdown = true;
    }

    @Override
    public List<Runnable> shutdownNow() {
      shutdown = true;
      List<Runnable> remaining = List.copyOf(tasks);
      tasks.clear();
      return remaining;
    }

    @Override
    public boolean isShutdown() {
      return shutdown;
    }

    @Override
    public boolean isTerminated() {
      return shutdown && tasks.isEmpty();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) {
      return isTerminated();
    }

    @Override
    public void execute(Runnable command) {
      tasks.add(command);
    }

    int queuedTasks() {
      return tasks.size();
    }

    void runNext() {
      Runnable task = tasks.poll();
      assertNotNull(task);
      task.run();
    }
  }
}
