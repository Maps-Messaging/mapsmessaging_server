/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.engine.destination.subscription;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.message.Message;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Forces membership changes during dispatch without relying on thread scheduling luck.
 * The engine must either serialize updates or iterate a stable membership snapshot.
 */
class DestinationSubscriptionConcurrentMutationRegressionTest {

  @Test
  void subscribingDuringDispatchMustNotCorruptIteration() throws Exception {
    Fixture f = fixture();
    Subscribable joining = mock(Subscribable.class);
    try {
      Future<Integer> publish = f.pool.submit(() -> f.manager.register(f.message));
      assertTrue(f.entered.await(5, TimeUnit.SECONDS));
      f.manager.put("joining", joining);
      f.release.countDown();

      assertEquals(1, publish.get(5, TimeUnit.SECONDS));
      assertTrue(f.manager.hasSubscriptions());
    } finally {
      f.close();
    }
  }

  @Test
  void unsubscribingDuringDispatchMustNotCorruptIteration() throws Exception {
    Fixture f = fixture();
    try {
      Future<Integer> publish = f.pool.submit(() -> f.manager.register(f.message));
      assertTrue(f.entered.await(5, TimeUnit.SECONDS));
      assertNotNull(f.manager.remove("first"));
      f.release.countDown();

      assertEquals(1, publish.get(5, TimeUnit.SECONDS));
      assertFalse(f.manager.hasSubscriptions());
    } finally {
      f.close();
    }
  }

  private static Fixture fixture() {
    DestinationSubscriptionManager manager = new DestinationSubscriptionManager("topic/concurrent");
    Subscribable first = mock(Subscribable.class);
    Message message = new MessageBuilder().setId(5L).build();
    CountDownLatch entered = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    when(first.register(message)).thenAnswer(invocation -> {
      entered.countDown();
      if (!release.await(5, TimeUnit.SECONDS)) {
        throw new AssertionError("Dispatch was not released");
      }
      return 1;
    });
    manager.put("first", first);
    return new Fixture(manager, message, entered, release, Executors.newSingleThreadExecutor());
  }

  private record Fixture(DestinationSubscriptionManager manager, Message message,
      CountDownLatch entered, CountDownLatch release, ExecutorService pool) {
    void close() throws InterruptedException {
      release.countDown();
      pool.shutdownNow();
      assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
    }
  }
}
