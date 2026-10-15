/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.storage.counter;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class IndexedCounterDurabilityTest {
  @TempDir Path directory;

  private Path base() {
    return directory.resolve("counters");
  }

  @Test
  void happyStrictDefaultSurvivesRestart() throws Exception {
    try (IndexedCounterStore store = new IndexedCounterStore(base())) {
      assertTrue(store.accept("rx", 11));
      store.flush();
    }
    try (IndexedCounterStore reopened = new IndexedCounterStore(base())) {
      assertEquals(11, reopened.highWaterMark("rx"));
      assertFalse(reopened.accept("rx", 11));
    }
  }

  @Test
  void happyBatchedCountAndExplicitFlushSurviveRestart() throws Exception {
    try (IndexedCounterStore store = new IndexedCounterStore(
        base(), CounterDurability.BATCHED, 3, 0)) {
      for (int value = 1; value <= 7; value++) assertTrue(store.accept("rx", value));
      assertEquals(7, store.highWaterMark("rx"));
      store.flush();
    }
    try (IndexedCounterStore reopened = new IndexedCounterStore(base())) {
      assertEquals(7, reopened.highWaterMark("rx"));
    }
  }

  @Test
  void happyBatchedTimeAndOrderlyCloseFlush() throws Exception {
    try (IndexedCounterStore store = new IndexedCounterStore(
        base(), CounterDurability.BATCHED, 1_000, 20)) {
      assertTrue(store.accept("rx", 37));
      Thread.sleep(80);
      store.flush();
      assertEquals(37, store.highWaterMark("rx"));
    }
    try (IndexedCounterStore reopened = new IndexedCounterStore(base())) {
      assertEquals(37, reopened.highWaterMark("rx"));
    }
  }

  @Test
  void happyRangeReservationsNeverRepeatAfterRestart() throws Exception {
    try (IndexedCounterStore store = new IndexedCounterStore(
        base(), CounterDurability.RESERVED_RANGE, 100, 0)) {
      assertEquals(new CounterStore.Range(1, 1024), store.reserve("tx", 1024));
      assertEquals(new CounterStore.Range(1025, 2048), store.reserve("tx", 1024));
    }
    try (IndexedCounterStore reopened = new IndexedCounterStore(
        base(), CounterDurability.RESERVED_RANGE, 100, 0)) {
      assertEquals(new CounterStore.Range(2049, 3072), reopened.reserve("tx", 1024));
    }
  }

  @Test
  void sadReservedStoreMustNotAcceptInboundReplayCounters() throws Exception {
    try (IndexedCounterStore store = new IndexedCounterStore(
        base(), CounterDurability.RESERVED_RANGE, 100, 0)) {
      assertThrows(IOException.class, () -> store.accept("rx", 1));
      assertEquals(0, store.highWaterMark("rx"));
    }
  }

  @Test
  void sadInvalidBatchConfigurationRejected() {
    assertThrows(IllegalArgumentException.class, () ->
        new IndexedCounterStore(base(), CounterDurability.BATCHED, 0, 10));
    assertThrows(IllegalArgumentException.class, () ->
        new IndexedCounterStore(base(), CounterDurability.BATCHED, 10, -1));
  }

  @Test
  void murphyMixedBatchAndReservationsAlwaysKeepHighWaterMark() throws Exception {
    try (IndexedCounterStore store = new IndexedCounterStore(
        base(), CounterDurability.BATCHED, 100_000, 0)) {
      assertTrue(store.accept("telemetry", 5));
      assertEquals(new CounterStore.Range(1, 64), store.reserve("tx", 64));
      assertTrue(store.accept("telemetry", 6));
      // close must force remaining dirty writes
    }
    try (IndexedCounterStore reopened = new IndexedCounterStore(base())) {
      assertEquals(64, reopened.highWaterMark("tx"));
      assertEquals(6, reopened.highWaterMark("telemetry"));
    }
  }

  @Test
  void murphyClosedStoreCannotFlush() throws Exception {
    IndexedCounterStore store = new IndexedCounterStore(base());
    store.close();
    assertThrows(IOException.class, store::flush);
  }
}
