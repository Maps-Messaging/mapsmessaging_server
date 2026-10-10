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
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class IndexedCounterBulkRecoveryTest {
  @TempDir Path root;

  @Test
  void unpublishedStagingFilesAreNotAnActiveStore() throws Exception {
    Path staged = root.resolve(".pending.staging");
    IndexedCounterBulkLoader.initialize(staged,
        List.of(new IndexedCounterBulkLoader.Entry("sender", 7)));
    assertFalse(Files.exists(root.resolve("active")));
    try (IndexedCounterStore recovered = new IndexedCounterStore(staged.resolve("counters"))) {
      assertEquals(7, recovered.highWaterMark("sender"));
    }
  }

  @Test
  void existingCommittedStoreSurvivesRejectedBulkReplacement() throws Exception {
    Path active = root.resolve("active");
    IndexedCounterBulkLoader.initialize(active,
        List.of(new IndexedCounterBulkLoader.Entry("sender", 51)));
    assertThrows(IOException.class, () -> IndexedCounterBulkLoader.initialize(active,
        List.of(new IndexedCounterBulkLoader.Entry("sender", 1))));
    try (IndexedCounterStore reopened = new IndexedCounterStore(active.resolve("counters"))) {
      assertEquals(51, reopened.highWaterMark("sender"));
    }
  }

  @Test
  void corruptBothPublishedSlotsMustFailOnReopen() throws Exception {
    Path active = root.resolve("active");
    IndexedCounterBulkLoader.initialize(active,
        List.of(new IndexedCounterBulkLoader.Entry("sender", 17)));
    Path data = active.resolve("counters.dat");
    try (FileChannel channel = FileChannel.open(data, StandardOpenOption.WRITE)) {
      channel.write(ByteBuffer.wrap(new byte[] {(byte) 0xff}), 8);
      channel.write(ByteBuffer.wrap(new byte[] {(byte) 0xff}), 32);
      channel.force(true);
    }
    assertThrows(IOException.class, () -> new IndexedCounterStore(active.resolve("counters")));
  }

  @Test
  void truncatedPublishedIndexMustFailOnReopen() throws Exception {
    Path active = root.resolve("active");
    IndexedCounterBulkLoader.initialize(active,
        List.of(new IndexedCounterBulkLoader.Entry("sender", 17)));
    Path index = active.resolve("counters.idx");
    try (FileChannel channel = FileChannel.open(index, StandardOpenOption.WRITE)) {
      channel.truncate(channel.size() - 1);
      channel.force(true);
    }
    assertThrows(IOException.class, () -> new IndexedCounterStore(active.resolve("counters")));
  }
}
