/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.engine.destination.subscription.state;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.Constants;
import io.mapsmessaging.utilities.collections.bitset.BitSetFactoryImpl;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageStateConcurrentLifecycleTest {

  @Test
  void concurrentAllocationAndExpiryNeverLeaveOrphanedInflightState() throws Exception {
    MessageStateManagerImpl state = new MessageStateManagerImpl("concurrent-state", 1L,
        new BitSetFactoryImpl(Constants.BITSET_BLOCK_SIZE));
    List<Message> messages = new ArrayList<>();
    for (int id = 1; id <= 32; id++) {
      Message message = new MessageBuilder().setId(id).build();
      messages.add(message);
      state.register(message);
    }

    ExecutorService executor = Executors.newFixedThreadPool(2);
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    try {
      Future<?> allocate = executor.submit(() -> {
        ready.countDown();
        await(start);
        for (Message message : messages) {
          state.allocate(message);
        }
      });
      Future<?> expire = executor.submit(() -> {
        ready.countDown();
        await(start);
        for (Message message : messages) {
          state.expired(message.getIdentifier());
        }
      });

      assertTrue(ready.await(5, TimeUnit.SECONDS));
      start.countDown();
      allocate.get(5, TimeUnit.SECONDS);
      expire.get(5, TimeUnit.SECONDS);

      assertEquals(0, state.pending());
      assertFalse(state.hasMessagesInFlight());
      for (Message message : messages) {
        assertFalse(state.hasMessage(message.getIdentifier()));
      }
    } finally {
      start.countDown();
      executor.shutdownNow();
      assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }
  }

  private static void await(CountDownLatch latch) {
    try {
      if (!latch.await(5, TimeUnit.SECONDS)) {
        throw new AssertionError("Concurrent lifecycle start latch timed out");
      }
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      throw new AssertionError("Concurrent lifecycle interrupted", interrupted);
    }
  }
}
