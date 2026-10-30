/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.storage.counter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;
import java.util.Random;
import java.util.stream.Stream;

/**
 * Opt-in, end-to-end GenerationCounterStore compaction benchmark.
 * Args: [identities=1000000] [samples=1000]. Consider -Xmx4g or more.
 *
 * The initial dataset is built through the production bulk loader and staged
 * into a CLOSED, newly created generation. The offline fixture swap is only to
 * avoid one million per-key fsyncs during setup; it is NOT a supported runtime
 * import procedure. Timings for generation open/compact/reopen exclude seeding.
 * No identities are retired, so this measures rewriting and active-manifest
 * publication at scale, not mass retirement or physical power-loss durability.
 */
public final class MillionKeyCounterCompactionBenchmark {
  private static final String TX = "tx.counter";
  private static final long TX_HIGH_WATER = 2_048;

  private MillionKeyCounterCompactionBenchmark() {}

  public static void main(String[] args) throws Exception {
    int identities = args.length > 0 ? Integer.parseInt(args[0]) : 1_000_000;
    int samples = args.length > 1 ? Integer.parseInt(args[1]) : 1_000;
    if (identities < 1 || identities > 1_000_000 || samples < 1) {
      throw new IllegalArgumentException("Invalid identity or sample count");
    }

    Path root = Files.createTempDirectory("counter-million-compaction-");
    Path managed = root.resolve("managed");
    Path source = root.resolve("bulk-seed");
    try (GenerationCounterStore ignored = open(managed)) {
      // Create a legitimate manifest and empty active generation.
    }
    String previous = singleGeneration(managed);
    Path previousDirectory = managed.resolve(previous);

    long start = System.nanoTime();
    IndexedCounterBulkLoader.initialize(source, entries(identities));
    long seedNanos = System.nanoTime() - start;
    // Benchmark-only fixture construction. No manager or file handles are open.
    for (String suffix : new String[] {"idx", "dat"}) {
      Files.move(source.resolve("counters." + suffix),
          previousDirectory.resolve("counters." + suffix), StandardCopyOption.REPLACE_EXISTING);
    }
    Files.delete(source);
    long originalBytes = fileBytes(previousDirectory);

    long openNanos;
    long compactNanos;
    long firstChecks;
    byte[] activeBefore = Files.readAllBytes(managed.resolve("ACTIVE"));
    start = System.nanoTime();
    try (GenerationCounterStore store = open(managed)) {
      openNanos = System.nanoTime() - start;
      if (store.highWaterMark(TX) != TX_HIGH_WATER
          || store.highWaterMark(key(0)) != 1
          || store.highWaterMark(key(identities - 1)) != identities) {
        throw new IllegalStateException("Invalid seeded counters");
      }
      start = System.nanoTime();
      store.compact();
      compactNanos = System.nanoTime() - start;
      firstChecks = verifySamples(store, identities, samples);
      if (!new CounterStore.Range(TX_HIGH_WATER + 1, TX_HIGH_WATER + 1)
          .equals(store.reserve(TX, 1))) {
        throw new IllegalStateException("Outbound reservation rolled back during compaction");
      }
    }
    byte[] activeAfter = Files.readAllBytes(managed.resolve("ACTIVE"));
    if (Arrays.equals(activeBefore, activeAfter)) {
      throw new IllegalStateException("Compaction did not publish a new generation");
    }
    String next = anotherGeneration(managed, previous);
    long compactedBytes = fileBytes(managed.resolve(next));

    start = System.nanoTime();
    long reopenNanos;
    long reopenedChecks;
    try (GenerationCounterStore reopened = open(managed)) {
      reopenNanos = System.nanoTime() - start;
      reopenedChecks = verifySamples(reopened, identities, samples);
      if (reopened.highWaterMark(TX) != TX_HIGH_WATER + 1
          || !new CounterStore.Range(TX_HIGH_WATER + 2, TX_HIGH_WATER + 2)
              .equals(reopened.reserve(TX, 1))) {
        throw new IllegalStateException("Outbound reservation lost on restart");
      }
    }
    if (!Files.isDirectory(previousDirectory)) {
      throw new IllegalStateException("Old committed generation was deleted");
    }

    System.out.println("Java=" + System.getProperty("java.version") + ",directory=" + root);
    System.out.println("Seed prepared offline; zero retirements; old generation intentionally retained.");
    System.out.println("identities,seed_ms,open_ms,compact_ms,reopen_ms,checked_counters,old_generation_bytes,new_generation_bytes");
    System.out.printf(Locale.ROOT, "%d,%.2f,%.2f,%.2f,%.2f,%d,%d,%d%n",
        identities, seedNanos / 1_000_000.0, openNanos / 1_000_000.0,
        compactNanos / 1_000_000.0, reopenNanos / 1_000_000.0,
        firstChecks + reopenedChecks, originalBytes, compactedBytes);
  }

  private static GenerationCounterStore open(Path root) throws Exception {
    return new GenerationCounterStore(root, CounterDurability.STRICT, 1, 0, 1.0);
  }

  private static String key(int id) {
    return "session/" + id + "/scheme/0";
  }

  private static Iterable<IndexedCounterBulkLoader.Entry> entries(int identities) {
    return () -> new Iterator<>() {
      private int position;
      @Override public boolean hasNext() { return position <= identities; }
      @Override public IndexedCounterBulkLoader.Entry next() {
        if (position == identities) {
          position++;
          return new IndexedCounterBulkLoader.Entry(TX, TX_HIGH_WATER);
        }
        int id = position++;
        return new IndexedCounterBulkLoader.Entry(key(id), id + 1L);
      }
    };
  }

  private static long verifySamples(GenerationCounterStore store, int identities, int samples)
      throws Exception {
    Random random = new Random(409);
    for (int i = 0; i < samples; i++) {
      int id = random.nextInt(identities);
      long expected = id + 1L;
      if (store.highWaterMark(key(id)) != expected || store.accept(key(id), expected)) {
        throw new IllegalStateException("Counter replay regression for " + id);
      }
    }
    return samples;
  }

  private static String singleGeneration(Path managed) throws Exception {
    try (Stream<Path> paths = Files.list(managed)) {
      return paths.filter(Files::isDirectory)
          .map(path -> path.getFileName().toString())
          .filter(name -> name.startsWith("gen-"))
          .findFirst().orElseThrow(() -> new IllegalStateException("No initial generation"));
    }
  }

  private static String anotherGeneration(Path managed, String previous) throws Exception {
    try (Stream<Path> paths = Files.list(managed)) {
      return paths.filter(Files::isDirectory)
          .map(path -> path.getFileName().toString())
          .filter(name -> name.startsWith("gen-") && !name.equals(previous))
          .findFirst().orElseThrow(() -> new IllegalStateException("No compacted generation"));
    }
  }

  private static long fileBytes(Path generationDirectory) throws Exception {
    return Files.size(generationDirectory.resolve("counters.idx"))
        + Files.size(generationDirectory.resolve("counters.dat"));
  }
}
