/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.engine.destination.subscription.modes;

import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.subscription.Subscription;
import io.mapsmessaging.engine.tasks.Response;
import io.mapsmessaging.engine.tasks.VoidResponse;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Verify lifecycle operations are queued on destination ownership domains, not called by session threads. */
class SubscriptionLifecycleQueueTest {

  @Test
  void twoDestinationCloseCompletesOnlyAfterBothQueuedTasksRun() throws Exception {
    NormalSubscriptionModeManager manager = new NormalSubscriptionModeManager();
    Queue<Callable<Response>> firstTasks = new ArrayDeque<>();
    Queue<Callable<Response>> secondTasks = new ArrayDeque<>();
    DestinationImpl firstDestination = destination(firstTasks);
    DestinationImpl secondDestination = destination(secondTasks);
    Subscription first = mock(Subscription.class);
    Subscription second = mock(Subscription.class);
    manager.add(first, firstDestination);
    manager.add(second, secondDestination);

    CompletableFuture<Void> finished = manager.closeAsync(false);
    assertFalse(finished.isDone());
    verify(first, never()).delete();
    verify(second, never()).delete();

    secondTasks.remove().call();
    assertFalse(finished.isDone());
    verify(first, never()).delete();
    verify(second).delete();

    firstTasks.remove().call();
    assertNull(finished.get(5, TimeUnit.SECONDS));
    verify(first).delete();
    assertNull(manager.get(firstDestination));
    assertNull(manager.get(secondDestination));
  }

  @Test
  void hibernationRunsOnlyOnEachDestinationTaskAndKeepsSubscriptions() throws Exception {
    NormalSubscriptionModeManager manager = new NormalSubscriptionModeManager();
    Queue<Callable<Response>> tasks = new ArrayDeque<>();
    DestinationImpl destination = destination(tasks);
    Subscription subscription = mock(Subscription.class);
    manager.add(subscription, destination);

    CompletableFuture<Void> finished = manager.hibernateAsync();
    assertFalse(finished.isDone());
    verify(subscription, never()).hibernate();

    tasks.remove().call();
    assertNull(finished.get(5, TimeUnit.SECONDS));
    verify(subscription).hibernate();
    assertTrue(manager.get(destination) == subscription);
  }

  @Test
  void failedQueuedCleanupDoesNotClaimSuccessfulClosure() throws Exception {
    NormalSubscriptionModeManager manager = new NormalSubscriptionModeManager();
    Queue<Callable<Response>> tasks = new ArrayDeque<>();
    DestinationImpl destination = destination(tasks);
    Subscription subscription = mock(Subscription.class);
    manager.add(subscription, destination);
    doThrow(new IOException("cleanup failed")).when(subscription).delete();

    CompletableFuture<Void> finished = manager.closeAsync(false);
    tasks.remove().call();

    assertThrows(ExecutionException.class, () -> finished.get(5, TimeUnit.SECONDS));
    assertTrue(manager.get(destination) == subscription, "Do not discard failed cleanup ownership");
  }

  @Test
  void rejectedDestinationSubmissionFailsClosureWithoutDirectDelete() throws Exception {
    NormalSubscriptionModeManager manager = new NormalSubscriptionModeManager();
    DestinationImpl destination = mock(DestinationImpl.class);
    when(destination.submit(any(Callable.class))).thenThrow(new RejectedExecutionException("closed queue"));
    Subscription subscription = mock(Subscription.class);
    manager.add(subscription, destination);

    CompletableFuture<Void> finished = manager.closeAsync(false);

    assertThrows(ExecutionException.class, () -> finished.get(5, TimeUnit.SECONDS));
    verify(subscription, never()).delete();
    assertTrue(manager.get(destination) == subscription);
  }

  private static DestinationImpl destination(Queue<Callable<Response>> tasks) {
    DestinationImpl destination = mock(DestinationImpl.class);
    when(destination.submit(any(Callable.class))).thenAnswer(invocation -> {
      tasks.add(invocation.getArgument(0));
      return CompletableFuture.completedFuture(new VoidResponse());
    });
    return destination;
  }
}
