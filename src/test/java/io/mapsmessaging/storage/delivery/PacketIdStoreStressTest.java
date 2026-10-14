/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.storage.delivery;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(60)
class PacketIdStoreStressTest {
  private static final int THREADS = 16;
  private static final int CAPACITY = 31;
  @TempDir(factory = TargetDataTempDirFactory.class) Path directory;

  @Test void memory_handles_many_topics_with_continuous_wrap_and_reuse() throws Exception {
    try (PacketIdStore store = PacketIdStore.memory(CAPACITY)) {
      churn(store, 10000, false);
      assertEquals(THREADS, store.size());
    }
  }

  @Test void batched_journal_handles_parallel_topics_flush_and_compaction_then_recovers() throws Exception {
    persistentChurn(PacketIdStore.Durability.BATCHED, 5000, true);
  }

  @Test void synchronous_journal_handles_parallel_topics_then_recovers() throws Exception {
    persistentChurn(PacketIdStore.Durability.SYNC, 100, false);
  }

  @Test void simultaneous_same_delivery_allocations_create_exactly_one_mapping() throws Exception {
    try (PacketIdStore store = PacketIdStore.open(directory.resolve("shared"), CAPACITY,
        PacketIdStore.Durability.BATCHED); var executor = Executors.newFixedThreadPool(THREADS)) {
      for (int round = 0; round < 128; round++) {
        final long messageId = round;
        CyclicBarrier barrier = new CyclicBarrier(THREADS);
        var tasks = new ArrayList<Callable<PacketIdStore.Allocation>>();
        for (int worker = 0; worker < THREADS; worker++) {
          tasks.add(() -> { barrier.await(5, TimeUnit.SECONDS); return store.allocate("same/topic", messageId); });
        }
        var futures = executor.invokeAll(tasks, 10, TimeUnit.SECONDS);
        var ids = new HashSet<Integer>();
        int fresh = 0;
        for (var future : futures) {
          var allocation = future.get();
          ids.add(allocation.packetId());
          if (!allocation.existing()) fresh++;
        }
        assertEquals(1, fresh);
        assertEquals(1, ids.size());
        assertEquals(1, store.size());
        assertTrue(store.clear("same/topic", messageId));
      }
      store.flush();
    }
    try (PacketIdStore recovered = PacketIdStore.open(directory.resolve("shared"), CAPACITY,
        PacketIdStore.Durability.BATCHED)) { assertEquals(0, recovered.size()); }
  }

  @Test void concurrent_full_capacity_allocations_have_unique_ids_and_fail_without_overwriting() throws Exception {
    try (PacketIdStore store = PacketIdStore.memory(CAPACITY); var executor = Executors.newFixedThreadPool(THREADS)) {
      var tasks = new ArrayList<Callable<Integer>>();
      for (int index = 0; index < CAPACITY; index++) {
        final long messageId = index;
        tasks.add(() -> store.allocate("capacity", messageId).packetId());
      }
      var ids = new HashSet<Integer>();
      for (var future : executor.invokeAll(tasks, 10, TimeUnit.SECONDS)) assertTrue(ids.add(future.get()));
      Map<Integer, PacketIdStore.Delivery> before = store.outstanding();
      var failures = new ArrayList<Callable<Boolean>>();
      for (int index = 0; index < THREADS; index++) {
        final long messageId = CAPACITY + index;
        failures.add(() -> { assertThrows(java.io.IOException.class, () -> store.allocate("capacity", messageId)); return true; });
      }
      for (var future : executor.invokeAll(failures, 10, TimeUnit.SECONDS)) assertTrue(future.get());
      assertEquals(before, store.outstanding());
    }
  }

  private void persistentChurn(PacketIdStore.Durability durability, int iterations, boolean maintenance) throws Exception {
    Path file = directory.resolve(durability.name());
    Map<Integer, PacketIdStore.Delivery> expected;
    long historyBytes;
    try (PacketIdStore store = PacketIdStore.open(file, CAPACITY, durability)) {
      churn(store, iterations, maintenance);
      expected = store.outstanding();
      historyBytes = Files.size(file);
      store.flush();
    }
    try (PacketIdStore recovered = PacketIdStore.open(file, CAPACITY, durability)) {
      assertEquals(expected, recovered.outstanding());
      for (var entry : expected.entrySet()) {
        var delivery = entry.getValue();
        var allocation = recovered.allocate(delivery.topic(), delivery.messageId());
        assertTrue(allocation.existing());
        assertEquals(entry.getKey().intValue(), allocation.packetId());
      }
      recovered.compact();
      assertTrue(Files.size(file) < historyBytes);
      System.out.printf("%s journal: %,d bytes before final compaction; %,d after%n", durability, historyBytes, Files.size(file));
    }
    try (PacketIdStore recovered = PacketIdStore.open(file, CAPACITY, durability)) {
      assertEquals(expected, recovered.outstanding());
    }
  }

  private void churn(PacketIdStore store, int iterations, boolean maintenance) throws Exception {
    CountDownLatch start = new CountDownLatch(1);
    CountDownLatch finished = new CountDownLatch(THREADS);
    CountDownLatch initialAllocated = new CountDownLatch(THREADS);
    CountDownLatch initialCompacted = new CountDownLatch(1);
    long began = System.nanoTime();
    try (var executor = Executors.newFixedThreadPool(THREADS + 1)) {
      var futures = new ArrayList<Future<?>>();
      for (int worker = 0; worker < THREADS; worker++) {
        final String topic = "sensor/" + worker;
        futures.add(executor.submit(() -> {
          try {
            assertTrue(start.await(5, TimeUnit.SECONDS));
            for (int iteration = 0; iteration < iterations; iteration++) {
              // Same long IDs across all topics exercise composite-key isolation.
              long messageId = iteration;
              var allocation = store.allocate(topic, messageId);
              assertFalse(allocation.existing());
              if (maintenance && iteration == 0) {
                initialAllocated.countDown();
                assertTrue(initialCompacted.await(10, TimeUnit.SECONDS));
              }
              assertTrue(allocation.packetId() > 0 && allocation.packetId() <= CAPACITY);
              assertEquals(allocation.packetId(), store.getPacketId(topic, messageId));
              assertEquals(new PacketIdStore.Delivery(topic, messageId), store.getDelivery(allocation.packetId()));
              var duplicate = store.allocate(topic, messageId);
              assertTrue(duplicate.existing());
              assertEquals(allocation.packetId(), duplicate.packetId());
              boolean cleared = iteration % 2 == 0 ? store.clear(allocation.packetId()) : store.clear(topic, messageId);
              assertTrue(cleared);
              assertNull(store.getPacketId(topic, messageId));
            }
            assertFalse(store.allocate(topic, iterations).existing());
          } finally { finished.countDown(); }
          return null;
        }));
      }
      if (maintenance) futures.add(executor.submit(() -> {
        assertTrue(start.await(5, TimeUnit.SECONDS));
        assertTrue(initialAllocated.await(10, TimeUnit.SECONDS));
        var held = store.outstanding();
        assertEquals(THREADS, held.size());
        assertEquals(THREADS, finished.getCount());
        store.compact();
        assertEquals(held, store.outstanding(), "Compaction must preserve all worker-held mappings");
        initialCompacted.countDown();
        for (int scan = 0; scan < 100 && finished.getCount() > 0; scan++) {
          var snapshot = store.outstanding();
          assertEquals(snapshot.size(), new HashSet<>(snapshot.values()).size());
          store.flush();
          if (scan % 10 == 0) store.compact();
        }
        return null;
      }));
      start.countDown();
      try { for (var future : futures) future.get(45, TimeUnit.SECONDS); }
      finally { executor.shutdownNow(); }
    }
    double seconds = (System.nanoTime() - began) / 1_000_000_000.0;
    long cycles = (long) THREADS * iterations;
    System.out.printf("%,d concurrent allocate/lookup/reallocate/clear cycles in %.3f s (%,.0f cycles/s)%n", cycles, seconds, cycles / seconds);
    assertEquals(THREADS, store.size());
  }
}
