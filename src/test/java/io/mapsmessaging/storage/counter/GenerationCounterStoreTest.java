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
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GenerationCounterStoreTest {
  @TempDir Path root;

  private GenerationCounterStore open(double threshold) throws IOException {
    return new GenerationCounterStore(root.resolve("managed"),
        CounterDurability.STRICT, 1, 0, threshold);
  }

  @Test
  void happyCompactionRetainsCountersAndReservationsAcrossRestart() throws Exception {
    try (GenerationCounterStore store = open(1.0)) {
      store.accept("rx/one", 41);
      store.accept("rx/two", 50);
      assertEquals(new CounterStore.Range(1, 128), store.reserve("tx", 128));
      store.compact();
      assertEquals(41, store.highWaterMark("rx/one"));
      assertFalse(store.accept("rx/two", 50));
      assertEquals(new CounterStore.Range(129, 130), store.reserve("tx", 2));
    }
    try (GenerationCounterStore store = open(1.0)) {
      assertEquals(41, store.highWaterMark("rx/one"));
      assertEquals(130, store.highWaterMark("tx"));
      assertTrue(store.accept("rx/two", 51));
    }
  }

  @Test
  void sadRetirementRequiresCredentialRevocation() throws Exception {
    try (GenerationCounterStore store = open(1.0)) {
      store.accept("rx/client", 71);
      assertThrows(IOException.class, () -> store.retire("rx/client", false));
      assertEquals(71, store.highWaterMark("rx/client"));
    }
  }

  @Test
  void happyRetirementDoesNotPermitReplayIdentityReuse() throws Exception {
    try (GenerationCounterStore store = open(1.0)) {
      store.accept("rx/client", 71);
      store.retire("rx/client", true);
      store.compact();
      assertThrows(IOException.class, () -> store.accept("rx/client", 1));
    }
    try (GenerationCounterStore reopened = open(1.0)) {
      assertThrows(IOException.class, () -> reopened.accept("rx/client", 72));
      assertThrows(IOException.class, () -> reopened.highWaterMark("rx/client"));
    }
  }

  @Test
  void happyAutomaticCompactionOnThreshold() throws Exception {
    try (GenerationCounterStore store = open(0.5)) {
      store.accept("rx/a", 8);
      store.accept("rx/b", 9);
      store.retire("rx/a", true);
      assertEquals(9, store.highWaterMark("rx/b"));
      assertThrows(IOException.class, () -> store.accept("rx/a", 10));
    }
    try (GenerationCounterStore reopened = open(0.5)) {
      assertEquals(9, reopened.highWaterMark("rx/b"));
    }
  }

  @Test
  void murphyUnreferencedGenerationDoesNotReplaceCommittedState() throws Exception {
    try (GenerationCounterStore store = open(1.0)) {
      store.accept("rx/client", 99);
    }
    Path rootPath = root.resolve("managed");
    Path orphan = rootPath.resolve("gen-orphan");
    IndexedCounterBulkLoader.initialize(orphan,
        List.of(new IndexedCounterBulkLoader.Entry("rx/client", 1)));
    try (GenerationCounterStore reopened = open(1.0)) {
      assertEquals(99, reopened.highWaterMark("rx/client"));
    }
  }

  @Test
  void murphyCorruptManifestFailsClosedRatherThanSelectingOrphan() throws Exception {
    try (GenerationCounterStore store = open(1.0)) {
      store.accept("rx/client", 99);
    }
    Files.write(root.resolve("managed/ACTIVE"), new byte[] {1, 2, 3});
    assertThrows(IOException.class, () -> open(1.0));
  }

  @Test
  void sadConcurrentGenerationManagersCannotWrite() throws Exception {
    try (GenerationCounterStore first = open(1.0)) {
      assertThrows(IOException.class, () -> open(1.0));
      assertTrue(first.accept("rx", 1));
    }
  }

  @Test
  void murphyRetirementJournalCorruptionFailsClosed() throws Exception {
    try (GenerationCounterStore store = open(1.0)) {
      store.accept("rx", 19);
      store.retire("rx", true);
    }
    Path file = root.resolve("managed/RETIRED");
    Files.write(file, new byte[] {1, 2, 3});
    assertThrows(IOException.class, () -> open(1.0));
  }

  @Test
  void happyCompactionProducesValidatedGeneration() throws Exception {
    try (GenerationCounterStore store = open(1.0)) {
      for (int i = 0; i < 15; i++) store.accept("rx/" + i, i + 1);
      store.compact();
      for (int i = 0; i < 15; i++) assertEquals(i + 1, store.highWaterMark("rx/" + i));
    }
    try (Stream<Path> children = Files.list(root.resolve("managed"))) {
      assertTrue(children.anyMatch(p -> p.getFileName().toString().startsWith("gen-")));
    }
  }
}
