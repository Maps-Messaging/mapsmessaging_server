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
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.Queue;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
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
      Future<?> first = executor.submit((Callable<Void>) () -> {
        fixture.executeNextReaper();
        return null;
      });
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
    fixture.destination.complete(402);
    fixture.requestFallback();
    assertEquals(1, attempts.get(), "New completions must respect the rejected submission's delay");
    assertEquals(0, fixture.queuedReapers());
    fixture.advanceRetry();
    assertEquals(1, fixture.queuedReapers(), "Rejected scheduling must not leave the queued flag stuck");
    fixture.executeNextReaper();
    assertEquals(1, fixture.queuedRemovals());
    fixture.executeNextRemoval();
    verify(fixture.resource).remove(401L);
    verify(fixture.resource).remove(402L);
  }

  @Test
  void failed_interest_lookup_preserves_the_batch_without_immediate_retry() throws Exception {
    Fixture fixture = new Fixture();
    when(fixture.subscriptionManager.getAll()).thenThrow(new IllegalStateException("Lookup failed")).thenReturn(new LinkedList<>());
    fixture.destination.complete(11L);

    assertThrows(IllegalStateException.class, fixture::executeNextReaper);
    fixture.destination.complete(12L);
    fixture.requestFallback();
    assertEquals(0, fixture.queuedReapers());
    assertEquals(0, fixture.queuedRemovals());

    fixture.advanceRetry();
    fixture.executeNextReaper();
    fixture.executeNextRemoval();
    verify(fixture.resource).remove(11L);
    verify(fixture.resource).remove(12L);
    assertEquals(0, fixture.queuedTasks.size());
  }

  @Test
  void rejected_removal_submission_restores_the_batch() throws Exception {
    Fixture fixture = new Fixture();
    AtomicBoolean reject = new AtomicBoolean(true);
    doAnswer(invocation -> {
      Callable<?> task = invocation.getArgument(0);
      if (task instanceof BulkRemoveMessageTask && reject.getAndSet(false)) {
        throw new IllegalStateException("Rejected removal");
      }
      fixture.queuedTasks.add(task);
      return null;
    }).when(fixture.subscriptionTaskQueue).submit(any(Callable.class));
    fixture.destination.complete(21L);

    assertThrows(IllegalStateException.class, fixture::executeNextReaper);
    assertEquals(0, fixture.queuedTasks.size());
    fixture.advanceRetry();
    fixture.executeNextReaper();
    fixture.executeNextRemoval();
    verify(fixture.resource).remove(21L);
  }

  @Test
  void isolated_poison_id_does_not_block_healthy_work_or_retry_early() throws Exception {
    Fixture fixture = new Fixture();
    doThrow(new IOException("Poison ID")).when(fixture.resource).remove(2L);
    fixture.destination.complete(1L);
    fixture.destination.complete(2L);
    fixture.destination.complete(3L);
    fixture.executeNextReaper();
    fixture.executeNextRemoval();
    verify(fixture.resource).remove(1L);
    verify(fixture.resource).remove(3L);
    assertEquals(0, fixture.queuedTasks.size());

    fixture.destination.complete(4L);
    fixture.destination.complete(2L);
    fixture.executeNextReaper();
    fixture.executeNextRemoval();
    verify(fixture.resource).remove(4L);
    verify(fixture.resource, times(1)).remove(2L);

    fixture.advanceRetry();
    fixture.executeNextReaper();
    fixture.executeNextRemoval();
    verify(fixture.resource, times(2)).remove(2L);
    assertEquals(0, fixture.queuedTasks.size(), "Permanent failure must wait for another tick");

    doNothing().when(fixture.resource).remove(2L);
    fixture.advanceRetry();
    fixture.executeNextReaper();
    fixture.executeNextRemoval();
    verify(fixture.resource, times(3)).remove(2L);
    verify(fixture.resource, times(1)).remove(1L);
    verify(fixture.resource, times(1)).remove(3L);
  }

  @Test
  void two_failures_pause_the_batch_and_keep_new_completions_for_later() throws Exception {
    Fixture fixture = new Fixture();
    doThrow(new IOException("First")).when(fixture.resource).remove(2L);
    doThrow(new IOException("Second")).when(fixture.resource).remove(3L);
    for (long messageId = 1; messageId <= 5; messageId++) {
      fixture.destination.complete(messageId);
    }
    fixture.executeNextReaper();
    fixture.executeNextRemoval();
    verify(fixture.resource, never()).remove(4L);
    verify(fixture.resource, never()).remove(5L);
    fixture.destination.complete(6L);
    fixture.requestFallback();
    assertEquals(0, fixture.queuedTasks.size());

    doNothing().when(fixture.resource).remove(2L);
    doNothing().when(fixture.resource).remove(3L);
    fixture.advanceRetry();
    fixture.executeNextReaper();
    fixture.executeNextRemoval();
    verify(fixture.resource, times(1)).remove(1L);
    verify(fixture.resource, times(2)).remove(2L);
    verify(fixture.resource, times(2)).remove(3L);
    verify(fixture.resource).remove(4L);
    verify(fixture.resource).remove(5L);
    verify(fixture.resource).remove(6L);
  }

  @Test
  void delayed_retry_rechecks_interest_before_deleting() throws Exception {
    Fixture fixture = new Fixture();
    doThrow(new IOException("Failed")).when(fixture.resource).remove(31L);
    fixture.destination.complete(31L);
    fixture.executeNextReaper();
    fixture.executeNextRemoval();
    when(fixture.subscriptionManager.getAll()).thenReturn(new LinkedList<>(List.of(31L)));

    fixture.advanceRetry();
    fixture.executeNextReaper();

    assertEquals(0, fixture.queuedRemovals());
    verify(fixture.resource, times(1)).remove(31L);
  }

  @Test
  void already_queued_removal_cannot_bypass_a_later_deferral() throws Exception {
    Fixture fixture = new Fixture();
    fixture.destination.holdCompletedMessage(41L);
    BulkRemoveMessageTask queued = new BulkRemoveMessageTask(fixture.destination, new LinkedList<>(List.of(41L, 42L)));
    queued.call();
    verify(fixture.resource, never()).remove(41L);
    verify(fixture.resource).remove(42L);

    fixture.destination.deferCompletedMessages(new LinkedList<>(List.of(41L)), true);
    new BulkRemoveMessageTask(fixture.destination, new LinkedList<>(List.of(43L))).call();
    verify(fixture.resource, never()).remove(43L);
    fixture.advanceRetry();
    fixture.executeNextReaper();
    fixture.executeNextRemoval();
    verify(fixture.resource).remove(41L);
    verify(fixture.resource).remove(43L);
  }

  @Test
  void periodic_submission_failure_does_not_cancel_the_timer() throws Exception {
    Fixture fixture = new Fixture();
    fixture.completions.add(51L);
    AtomicBoolean reject = new AtomicBoolean(true);
    doAnswer(invocation -> {
      if (reject.getAndSet(false)) {
        throw new IllegalStateException("Rejected periodic submission");
      }
      fixture.queuedTasks.add(invocation.getArgument(0));
      return null;
    }).when(fixture.subscriptionTaskQueue).submit(any(Callable.class));
    ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();
    CountDownLatch ticks = new CountDownLatch(3);
    ScheduledFuture<?> future = timer.scheduleAtFixedRate(() -> {
      try {
        fixture.requestSafeFallback();
      } catch (Exception failure) {
        throw new AssertionError(failure);
      } finally {
        ticks.countDown();
      }
    }, 0, 1, TimeUnit.MILLISECONDS);
    try {
      assertTrue(ticks.await(5, TimeUnit.SECONDS));
      assertFalse(future.isDone(), "A rejected submission must not suppress future ticks");
    } finally {
      timer.shutdownNow();
      assertTrue(timer.awaitTermination(5, TimeUnit.SECONDS));
    }
    assertEquals(0, fixture.queuedReapers());
    fixture.advanceRetry();
    fixture.executeNextReaper();
    fixture.executeNextRemoval();
    verify(fixture.resource).remove(51L);
  }

  @Test
  void completions_during_a_failed_lookup_are_preserved_and_coalesced() throws Exception {
    Fixture fixture = new Fixture();
    CountDownLatch entered = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    AtomicBoolean fail = new AtomicBoolean(true);
    when(fixture.subscriptionManager.getAll()).thenAnswer(invocation -> {
      if (fail.getAndSet(false)) {
        entered.countDown();
        await(release);
        throw new IllegalStateException("Lookup failed");
      }
      return new LinkedList<Long>();
    });
    fixture.destination.complete(61L);
    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      Future<?> running = executor.submit((Callable<Void>) () -> {
        fixture.executeNextReaper();
        return null;
      });
      assertTrue(entered.await(5, TimeUnit.SECONDS));
      fixture.destination.complete(62L);
      fixture.destination.complete(63L);
      release.countDown();
      assertThrows(ExecutionException.class, () -> running.get(5, TimeUnit.SECONDS));
      assertEquals(0, fixture.queuedTasks.size());
      fixture.advanceRetry();
      assertEquals(1, fixture.queuedReapers());
      fixture.executeNextReaper();
      fixture.executeNextRemoval();
      verify(fixture.resource).remove(61L);
      verify(fixture.resource).remove(62L);
      verify(fixture.resource).remove(63L);
    } finally {
      release.countDown();
      executor.shutdownNow();
    }
  }

  @Test
  void close_cancels_deferred_retries() throws Exception {
    Fixture fixture = new Fixture();
    fixture.destination.deferCompletedMessages(new LinkedList<>(List.of(71L)), true);
    fixture.destination.close();
    fixture.advanceRetry();
    assertEquals(0, fixture.queuedTasks.size());
    verify(fixture.resource, never()).remove(71L);
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
    private final AtomicLong retryClock = new AtomicLong();
    private final EventReaperQueue completions = new EventReaperQueue();
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
      set("reaperRetries", new ReaperRetryQueue(retryClock::get));
      set("completionQueue", completions);
      set("stats", stats);
      set("resource", resource);
      set("retainManager", retainManager);
      set("subscriptionManager", subscriptionManager);
      set("schemaSubscriptionManager", schemaSubscriptionManager);
      set("subscriptionTaskQueue", subscriptionTaskQueue);
      set("reaperFuture", reaperFuture);
      when(retainManager.current()).thenReturn(null);
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

    private void advanceRetry() throws Exception {
      retryClock.addAndGet(TimeUnit.SECONDS.toNanos(5));
      requestFallback();
    }

    private void requestSafeFallback() throws Exception {
      Method method = DestinationImpl.class.getDeclaredMethod("requestReaperSafely");
      method.setAccessible(true);
      method.invoke(destination);
    }

    private void executeNextRemoval() throws Exception {
      for (Callable<?> task : queuedTasks) {
        if (task instanceof BulkRemoveMessageTask && queuedTasks.remove(task)) {
          task.call();
          return;
        }
      }
      fail("Expected a queued bulk removal task");
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
