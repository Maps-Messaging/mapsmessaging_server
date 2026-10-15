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

package io.mapsmessaging.engine.destination;

import io.mapsmessaging.engine.destination.subscription.DestinationSubscriptionManager;
import io.mapsmessaging.engine.destination.tasks.BulkRemoveMessageTask;
import io.mapsmessaging.engine.resources.Resource;
import io.mapsmessaging.utilities.queue.EventReaperQueue;
import io.mapsmessaging.utilities.threads.tasks.TaskScheduler;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Deterministic scheduler interleavings for MSG-413. The destination is a constructor-bypassing
 * fixture because actual task submission must be held and replayed explicitly; every field
 * accessed by the exercised methods is initialized below.
 */
class DestinationReaperInterleavingTest {

  @Test
  void concurrentCompletionsCoalesceIntoOneQueuedReaper() throws Exception {
    Fixture fixture = new Fixture();
    int count = 64;
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(8);
    try {
      Future<?>[] futures = new Future<?>[count];
      for (int index = 0; index < count; index++) {
        final long messageId = index + 1L;
        futures[index] = executor.submit(() -> {
          await(start);
          fixture.destination.complete(messageId);
        });
      }
      start.countDown();
      for (Future<?> future : futures) {
        future.get(10, TimeUnit.SECONDS);
      }
      assertEquals(1, fixture.queuedReapers());
      fixture.executeNextReaper();
      assertEquals(0, fixture.queuedReapers());
      assertEquals(1, fixture.queuedRemovals());
      verify(fixture.stats, times(count)).deliveredMessage();
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void completionDuringRunningReaperAndFallbackQueuesOnlyOneFollowUp() throws Exception {
    Fixture fixture = new Fixture();
    fixture.destination.complete(101);
    CountDownLatch entered = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    when(fixture.subscriptionManager.getAll()).thenAnswer(invocation -> {
      entered.countDown();
      await(release);
      return new LinkedList<Long>();
    });

    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      Future<?> first = executor.submit(() -> fixture.executeNextReaper());
      assertTrue(entered.await(10, TimeUnit.SECONDS), "First reaper must reach the controlled processing point");

      fixture.destination.complete(102);
      fixture.destination.complete(103);
      fixture.requestFallback();
      fixture.requestFallback();
      assertEquals(0, fixture.queuedReapers(), "A running reaper must not queue a competing reaper");

      release.countDown();
      first.get(10, TimeUnit.SECONDS);
      assertEquals(1, fixture.queuedReapers(), "Pending completions and fallback must coalesce");
      fixture.executeNextReaper();
      assertEquals(0, fixture.queuedReapers());
      assertEquals(2, fixture.queuedRemovals(), "Both batches must be forwarded to removal");
      verify(fixture.stats, times(3)).deliveredMessage();
    } finally {
      release.countDown();
      executor.shutdownNow();
    }
  }

  @Test
  void fallbackDoesNotDuplicateAlreadyQueuedWork() throws Exception {
    Fixture fixture = new Fixture();
    fixture.destination.complete(201);
    fixture.requestFallback();
    fixture.requestFallback();
    assertEquals(1, fixture.queuedReapers());
    fixture.executeNextReaper();
    assertEquals(0, fixture.queuedReapers());
    assertEquals(1, fixture.queuedRemovals());
  }

  @Test
  void closePreventsFurtherReaperSubmissions() throws Exception {
    Fixture fixture = new Fixture();
    fixture.destination.complete(301);
    assertEquals(1, fixture.queuedReapers());
    fixture.destination.close();
    fixture.destination.complete(302);
    fixture.requestFallback();
    assertEquals(1, fixture.queuedReapers(), "Close must not submit a new reaper");
    fixture.executeNextReaper();
    assertEquals(0, fixture.queuedRemovals(), "Queued reaper must skip processing after close");
    verify(fixture.reaperFuture).cancel(false);
  }

  @Test
  void thousandCompletedQueueMessagesAreForwardedAsOneBatch() throws Exception {
    Fixture fixture = new Fixture();
    for (long messageId = 1; messageId <= 1000; messageId++) {
      fixture.destination.complete(messageId);
    }
    assertEquals(1, fixture.queuedReapers());
    fixture.executeNextReaper();
    assertEquals(1, fixture.queuedRemovals());
    Callable<?> removal = fixture.queuedTasks.remove();
    assertInstanceOf(BulkRemoveMessageTask.class, removal);
    removal.call();
    verify(fixture.resource, times(1000)).remove(anyLong());
    assertEquals(0, fixture.queuedTasks.size());
  }

  @Test
  void reaperSubmissionFailureCanBeRetried() throws Exception {
    Fixture fixture = new Fixture();
    AtomicInteger attempts = new AtomicInteger();
    doAnswer(invocation -> {
      if (attempts.getAndIncrement() == 0) {
        throw new IllegalStateException("Rejected submission");
      }
      fixture.queuedTasks.add(invocation.getArgument(0));
      return null;
    }).when(fixture.subscriptionTaskQueue).submit(any(Callable.class));

    assertThrows(IllegalStateException.class, () -> fixture.destination.complete(401));
    fixture.requestFallback();
    assertEquals(1, fixture.queuedReapers(), "Rejected scheduling must not leave the queued flag stuck");
    fixture.executeNextReaper();
    assertEquals(1, fixture.queuedRemovals());
  }

  private static void await(CountDownLatch latch) {
    try {
      if (!latch.await(10, TimeUnit.SECONDS)) {
        throw new AssertionError("Timed out waiting for controlled interleaving");
      }
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      throw new AssertionError("Interrupted during controlled interleaving", interrupted);
    }
  }

  private static final class Fixture {
    private final DestinationImpl destination = mock(DestinationImpl.class, CALLS_REAL_METHODS);
    private final DestinationStats stats = mock(DestinationStats.class);
    private final Resource resource = mock(Resource.class);
    private final RetainManager retainManager = mock(RetainManager.class);
    private final DestinationSubscriptionManager subscriptionManager = mock(DestinationSubscriptionManager.class);
    private final DestinationSubscriptionManager schemaSubscriptionManager = mock(DestinationSubscriptionManager.class);
    private final TaskScheduler subscriptionTaskQueue = mock(TaskScheduler.class);
    private final ScheduledFuture<?> reaperFuture = mock(ScheduledFuture.class);
    private final Queue<Callable<?>> queuedTasks = new java.util.concurrent.ConcurrentLinkedQueue<>();

    private Fixture() throws Exception {
      set("reaperLock", new Object());
      set("completionQueue", new EventReaperQueue());
      set("stats", stats);
      set("resource", resource);
      set("retainManager", retainManager);
      set("subscriptionManager", subscriptionManager);
      set("schemaSubscriptionManager", schemaSubscriptionManager);
      set("subscriptionTaskQueue", subscriptionTaskQueue);
      set("reaperFuture", reaperFuture);
      when(retainManager.current()).thenReturn(-1L);
      when(subscriptionManager.getAll()).thenReturn(new LinkedList<>());
      doAnswer(invocation -> {
        queuedTasks.add(invocation.getArgument(0));
        return null;
      }).when(subscriptionTaskQueue).submit(any(Callable.class));
    }

    private void set(String name, Object value) throws Exception {
      Field field = DestinationImpl.class.getDeclaredField(name);
      field.setAccessible(true);
      field.set(destination, value);
    }

    private void requestFallback() throws Exception {
      Method method = DestinationImpl.class.getDeclaredMethod("requestReaper");
      method.setAccessible(true);
      method.invoke(destination);
    }

    private int queuedReapers() {
      int count = 0;
      for (Callable<?> task : queuedTasks) {
        if (task.getClass().getSimpleName().equals("EventReaper")) {
          count++;
        }
      }
      return count;
    }

    private int queuedRemovals() {
      int count = 0;
      for (Callable<?> task : queuedTasks) {
        if (task instanceof BulkRemoveMessageTask) {
          count++;
        }
      }
      return count;
    }

    private void executeNextReaper() throws Exception {
      for (Callable<?> task : queuedTasks) {
        if (task.getClass().getSimpleName().equals("EventReaper") && queuedTasks.remove(task)) {
          task.call();
          return;
        }
      }
      fail("Expected a queued EventReaper task");
    }
  }
}
