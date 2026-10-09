/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.storage.counter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class IndexedCounterStoreTest {

  @TempDir Path directory;

  private Path base() {
    return directory.resolve("replay-state");
  }

  private Path index() {
    return directory.resolve("replay-state.idx");
  }

  private Path values() {
    return directory.resolve("replay-state.dat");
  }

  // Happy paths: monotonic acceptance, independent identities and persistent reservations.

  @Test
  void happyAcceptAndRejectReplayAcrossRestart() throws Exception {
    try (CounterStore store = new IndexedCounterStore(base())) {
      assertEquals(0, store.highWaterMark("rx/sender-a/0"));
      assertTrue(store.accept("rx/sender-a/0", 15));
      assertFalse(store.accept("rx/sender-a/0", 15));
      assertFalse(store.accept("rx/sender-a/0", 12));
      assertTrue(store.accept("rx/sender-a/0", 16));
      assertTrue(store.accept("rx/sender-a/1", 2));
      assertTrue(store.accept("rx/sender-b/0", 8));
      assertEquals(16, store.highWaterMark("rx/sender-a/0"));
    }
    try (CounterStore reopened = new IndexedCounterStore(base())) {
      assertEquals(16, reopened.highWaterMark("rx/sender-a/0"));
      assertEquals(2, reopened.highWaterMark("rx/sender-a/1"));
      assertEquals(8, reopened.highWaterMark("rx/sender-b/0"));
      assertFalse(reopened.accept("rx/sender-a/0", 16));
      assertTrue(reopened.accept("rx/sender-a/0", 17));
    }
  }

  @Test
  void happyReserveRangesNeverReissueAcrossRestart() throws Exception {
    try (CounterStore store = new IndexedCounterStore(base())) {
      assertEquals(new CounterStore.Range(1, 32), store.reserve("tx", 32));
      assertEquals(new CounterStore.Range(33, 64), store.reserve("tx", 32));
      assertEquals(64, store.highWaterMark("tx"));
    }
    try (CounterStore reopened = new IndexedCounterStore(base())) {
      assertEquals(new CounterStore.Range(65, 80), reopened.reserve("tx", 16));
    }
  }

  @Test
  void happyVariableLengthUtf8KeyAndEmptyStore() throws Exception {
    try (CounterStore store = new IndexedCounterStore(base())) {
      assertEquals(0, store.highWaterMark("not-registered"));
      assertTrue(store.accept("sensor/温度/1", 7));
    }
    try (CounterStore store = new IndexedCounterStore(base())) {
      assertEquals(7, store.highWaterMark("sensor/温度/1"));
    }
  }

  // Sad paths: reject invalid input, file pairs, invalid headers and concurrent ownership.

  @Test
  void sadRejectInvalidInputWithoutAdvancingCounter() throws Exception {
    try (CounterStore store = new IndexedCounterStore(base())) {
      assertThrows(IllegalArgumentException.class, () -> store.accept("key", 0));
      assertThrows(IllegalArgumentException.class, () -> store.accept("key", -1));
      assertThrows(IllegalArgumentException.class, () -> store.reserve("key", 0));
      assertThrows(IllegalArgumentException.class, () -> store.reserve("key", -10));
      assertThrows(IllegalArgumentException.class, () -> store.reserve("", 1));
      assertThrows(IllegalArgumentException.class, () -> store.reserve(" ", 1));
      assertThrows(IllegalArgumentException.class, () -> store.reserve(null, 1));
      assertThrows(IllegalArgumentException.class, () -> store.reserve("a".repeat(4097), 1));
      assertEquals(0, store.highWaterMark("key"));
    }
  }

  @Test
  void sadRejectIncompleteFilePair() throws Exception {
    try (CounterStore ignored = new IndexedCounterStore(base())) { }
    Files.delete(index());
    assertThrows(IOException.class, () -> new IndexedCounterStore(base()));
  }

  @Test
  void sadRejectCorruptIndexHeader() throws Exception {
    try (CounterStore ignored = new IndexedCounterStore(base())) { }
    overwrite(index(), 0, new byte[] {0, 0, 0, 0});
    assertThrows(IOException.class, () -> new IndexedCounterStore(base()));
  }

  @Test
  void sadRejectConcurrentStoreInstance() throws Exception {
    try (CounterStore store = new IndexedCounterStore(base())) {
      assertThrows(IOException.class, () -> new IndexedCounterStore(base()));
      assertTrue(store.accept("rx", 1));
    }
  }

  @Test
  void sadCloseRejectsSubsequentOperations() throws Exception {
    CounterStore store = new IndexedCounterStore(base());
    store.close();
    store.close();
    assertThrows(IOException.class, () -> store.accept("rx", 1));
    assertThrows(IOException.class, () -> store.reserve("tx", 1));
    assertThrows(IOException.class, () -> store.highWaterMark("rx"));
  }

  // Murphy paths: interrupted writes, corrupted data, truncation and concurrency.

  @Test
  void murphyTornNewestSlotRecoversPreviousCommittedGeneration() throws Exception {
    try (CounterStore store = new IndexedCounterStore(base())) {
      assertTrue(store.accept("rx", 41));
      assertTrue(store.accept("rx", 42));
    }
    // Value header=8, slot A=24, slot B starts at 32. The second update
    // committed into slot A; corrupt that slot to model an interrupted write.
    overwrite(values(), 8, new byte[] {(byte) 0xFF, (byte) 0xFF});
    try (CounterStore recovered = new IndexedCounterStore(base())) {
      assertEquals(41, recovered.highWaterMark("rx"));
      assertFalse(recovered.accept("rx", 41));
      assertTrue(recovered.accept("rx", 43));
    }
  }

  @Test
  void murphyBothSlotsCorruptedFailClosedAtStartup() throws Exception {
    try (CounterStore store = new IndexedCounterStore(base())) {
      store.accept("rx", 41);
    }
    overwrite(values(), 8 + 16, new byte[] {0, 0, 0, 0});
    overwrite(values(), 8 + 24 + 16, new byte[] {0, 0, 0, 0});
    assertThrows(IOException.class, () -> new IndexedCounterStore(base()));
  }

  @Test
  void murphyTruncatedIndexRecordFailsClosed() throws Exception {
    try (CounterStore store = new IndexedCounterStore(base())) {
      store.accept("rx/a", 2);
    }
    try (FileChannel channel = FileChannel.open(index(), StandardOpenOption.WRITE)) {
      channel.truncate(channel.size() - 1);
    }
    assertThrows(IOException.class, () -> new IndexedCounterStore(base()));
  }

  @Test
  void murphyTruncatedValueFileFailsClosed() throws Exception {
    try (CounterStore store = new IndexedCounterStore(base())) {
      store.accept("rx/a", 2);
    }
    try (FileChannel channel = FileChannel.open(values(), StandardOpenOption.WRITE)) {
      channel.truncate(channel.size() - 1);
    }
    assertThrows(IOException.class, () -> new IndexedCounterStore(base()));
  }

  @Test
  void murphyConcurrentReservationsNeverOverlap() throws Exception {
    try (CounterStore store = new IndexedCounterStore(base());
         var executor = Executors.newFixedThreadPool(4)) {
      List<Callable<CounterStore.Range>> tasks = new ArrayList<>();
      for (int i = 0; i < 12; i++) {
        tasks.add(() -> store.reserve("tx", 3));
      }
      List<Future<CounterStore.Range>> futures = executor.invokeAll(tasks);
      Set<Long> issued = new HashSet<>();
      for (Future<CounterStore.Range> future : futures) {
        CounterStore.Range range = future.get();
        for (long value = range.first(); value <= range.last(); value++) {
          assertTrue(issued.add(value), "Counter " + value + " issued twice");
        }
      }
      assertEquals(36, issued.size());
      assertEquals(36, store.highWaterMark("tx"));
    }
    try (CounterStore reopened = new IndexedCounterStore(base())) {
      assertEquals(new CounterStore.Range(37, 37), reopened.reserve("tx", 1));
    }
  }

  private static void overwrite(Path file, long position, byte[] bytes) throws IOException {
    try (FileChannel channel = FileChannel.open(file, StandardOpenOption.WRITE)) {
      ByteBuffer data = ByteBuffer.wrap(bytes);
      while (data.hasRemaining()) {
        int written = channel.write(data, position + data.position());
        if (written <= 0) throw new IOException("Could not corrupt test fixture");
      }
      channel.force(true);
    }
  }
}
