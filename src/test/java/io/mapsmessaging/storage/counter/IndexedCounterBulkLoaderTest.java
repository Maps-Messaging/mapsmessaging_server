/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.storage.counter;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class IndexedCounterBulkLoaderTest {
  @TempDir Path directory;

  @Test
  void happyBulkLoadAndUpdateAcrossRestart() throws Exception {
    Path target = directory.resolve("bulk");
    IndexedCounterBulkLoader.initialize(target,
        IntStream.range(0, 1_000)
            .mapToObj(i -> new IndexedCounterBulkLoader.Entry("session/" + i, i + 1L))
            .toList());
    try (IndexedCounterStore store = new IndexedCounterStore(target.resolve("counters"))) {
      assertEquals(1, store.highWaterMark("session/0"));
      assertEquals(1000, store.highWaterMark("session/999"));
      assertTrue(store.accept("session/999", 1001));
    }
    try (IndexedCounterStore reopened = new IndexedCounterStore(target.resolve("counters"))) {
      assertEquals(1001, reopened.highWaterMark("session/999"));
    }
  }

  @Test
  void happyEmptyBulkStoreOpensAndAcceptsNewKeys() throws Exception {
    Path target = directory.resolve("empty");
    IndexedCounterBulkLoader.initialize(target, List.of());
    try (IndexedCounterStore store = new IndexedCounterStore(target.resolve("counters"))) {
      assertEquals(0, store.highWaterMark("new"));
      assertTrue(store.accept("new", 1));
    }
  }

  @Test
  void sadExistingDestinationNeverOverwritten() throws Exception {
    Path target = directory.resolve("already-present");
    Files.createDirectory(target);
    assertThrows(IOException.class, () ->
        IndexedCounterBulkLoader.initialize(target, List.of(
            new IndexedCounterBulkLoader.Entry("a", 1))));
    assertTrue(Files.exists(target));
  }

  @Test
  void sadDuplicateKeyLeavesNoPublishedStore() {
    Path target = directory.resolve("duplicate");
    assertThrows(IOException.class, () -> IndexedCounterBulkLoader.initialize(target,
        List.of(new IndexedCounterBulkLoader.Entry("same", 1),
            new IndexedCounterBulkLoader.Entry("same", 2))));
    assertFalse(Files.exists(target));
  }

  @Test
  void murphyInvalidLaterEntryDoesNotPublishPartialStore() {
    Path target = directory.resolve("invalid-later");
    assertThrows(IOException.class, () -> IndexedCounterBulkLoader.initialize(target,
        List.of(new IndexedCounterBulkLoader.Entry("good", 1),
            new IndexedCounterBulkLoader.Entry("bad", -1))));
    assertFalse(Files.exists(target));
  }

  @Test
  void murphyRejectedInvalidAndOversizeKeys() {
    assertThrows(IOException.class, () -> IndexedCounterBulkLoader.initialize(
        directory.resolve("blank"), List.of(new IndexedCounterBulkLoader.Entry("", 1))));
    assertThrows(IOException.class, () -> IndexedCounterBulkLoader.initialize(
        directory.resolve("oversize"), List.of(new IndexedCounterBulkLoader.Entry("x".repeat(4097), 1))));
    assertFalse(Files.exists(directory.resolve("blank")));
    assertFalse(Files.exists(directory.resolve("oversize")));
  }
}
