/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.storage.counter;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class IndexedCounterCompactorTest {
  @TempDir Path root;

  private Path source() {
    return root.resolve("source");
  }

  private void seed() throws IOException {
    try (IndexedCounterStore store = new IndexedCounterStore(source())) {
      store.accept("rx/a/0", 51);
      store.accept("rx/b/0", 82);
      store.reserve("tx", 100);
    }
  }

  @Test
  void happyCompactPreservesAllCountersAndOriginal() throws Exception {
    seed();
    Path destination = root.resolve("compact");
    IndexedCounterCompactor.compact(source(), destination, Set.of());
    try (IndexedCounterStore copy = new IndexedCounterStore(destination.resolve("counters"))) {
      assertEquals(51, copy.highWaterMark("rx/a/0"));
      assertEquals(82, copy.highWaterMark("rx/b/0"));
      assertEquals(100, copy.highWaterMark("tx"));
      assertFalse(copy.accept("rx/a/0", 51));
      assertEquals(new CounterStore.Range(101, 102), copy.reserve("tx", 2));
    }
    try (IndexedCounterStore original = new IndexedCounterStore(source())) {
      assertEquals(51, original.highWaterMark("rx/a/0"));
      assertEquals(100, original.highWaterMark("tx"));
    }
  }

  @Test
  void happyRetiredIdentityIsAbsentOnlyFromNewSnapshot() throws Exception {
    seed();
    Path destination = root.resolve("compacted");
    IndexedCounterCompactor.compact(source(), destination, Set.of("rx/b/0"));
    try (IndexedCounterStore copy = new IndexedCounterStore(destination.resolve("counters"))) {
      assertEquals(51, copy.highWaterMark("rx/a/0"));
      assertEquals(0, copy.highWaterMark("rx/b/0"));
      assertEquals(100, copy.highWaterMark("tx"));
    }
    try (IndexedCounterStore original = new IndexedCounterStore(source())) {
      assertEquals(82, original.highWaterMark("rx/b/0"));
    }
  }

  @Test
  void sadExistingDestinationNotReplaced() throws Exception {
    seed();
    Path destination = root.resolve("keep");
    Files.createDirectory(destination);
    assertThrows(IOException.class, () ->
        IndexedCounterCompactor.compact(source(), destination, Set.of()));
    try (IndexedCounterStore original = new IndexedCounterStore(source())) {
      assertEquals(82, original.highWaterMark("rx/b/0"));
    }
  }

  @Test
  void sadConcurrentWriterPreventsSnapshot() throws Exception {
    seed();
    try (IndexedCounterStore writer = new IndexedCounterStore(source())) {
      assertThrows(IOException.class, () ->
          IndexedCounterCompactor.compact(source(), root.resolve("blocked"), Set.of()));
      assertFalse(Files.exists(root.resolve("blocked")));
    }
  }

  @Test
  void murphyCorruptSourceDoesNotPublishSnapshot() throws Exception {
    seed();
    Path index = root.resolve("source.idx");
    try (FileChannel channel = FileChannel.open(index, StandardOpenOption.WRITE)) {
      channel.write(ByteBuffer.wrap(new byte[] {(byte) 0xff}), 8);
      channel.force(true);
    }
    Path destination = root.resolve("no-partial-store");
    assertThrows(IOException.class, () ->
        IndexedCounterCompactor.compact(source(), destination, Set.of()));
    assertFalse(Files.exists(destination));
  }
}
