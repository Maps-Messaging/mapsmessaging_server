/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.storage.counter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Opt-in filesystem microbenchmark; not a JUnit test. Execute with a Java 21
 * test classpath and optionally pass a maximum key count and sample count.
 *
 * Example: java ... IndexedCounterStoreBenchmark 1000000 10000
 *
 * WARNING: registering a key commits BOTH the index and value file with
 * force(true). A million newly registered keys may take a very long time.
 * No assertions about disk throughput should be inferred from warm cache runs.
 */
public final class IndexedCounterStoreBenchmark {
  private static final int[] SIZES = {1, 10, 100, 1_000, 10_000, 100_000, 1_000_000};

  private IndexedCounterStoreBenchmark() {}

  public static void main(String[] args) throws Exception {
    int maximum = args.length > 0 ? Integer.parseInt(args[0]) : 10_000;
    int samples = args.length > 1 ? Integer.parseInt(args[1]) : 1_000;
    if (maximum < 1 || maximum > 1_000_000 || samples < 1) {
      throw new IllegalArgumentException("maximum must be 1..1000000 and samples positive");
    }

    Path directory = Files.createTempDirectory("indexed-counter-bench-");
    Path base = directory.resolve("counters");
    System.out.println("Store: " + base);
    System.out.println("JVM: " + System.getProperty("java.version"));
    System.out.println("Sessions,add_us_per_key,lookup_us_per_op,update_us_per_op,reopen_ms,index_bytes,value_bytes");
    int count = 0;
    long previous = 0;
    Random random = new Random(324);
    List<Integer> checkpoints = new ArrayList<>();
    for (int size : SIZES) if (size <= maximum) checkpoints.add(size);
    if (checkpoints.isEmpty() || checkpoints.getLast() != maximum) checkpoints.add(maximum);

    for (int target : checkpoints) {
      try (IndexedCounterStore store = new IndexedCounterStore(base)) {
        long started = System.nanoTime();
        for (int id = count; id < target; id++) {
          store.accept("session/" + id + "/scheme/0", 1);
        }
        long addNanos = System.nanoTime() - started;
        int added = target - count;
        count = target;

        started = System.nanoTime();
        for (int i = 0; i < samples; i++) {
          if (store.highWaterMark("session/" + random.nextInt(count) + "/scheme/0") < 1) {
            throw new IllegalStateException("Missing or invalid counter");
          }
        }
        long lookupNanos = System.nanoTime() - started;

        // Update a single existing record, to isolate the value-write cost
        // from index appends. The number of updates is intentionally bounded.
        int updates = Math.min(samples, 100);
        started = System.nanoTime();
        for (int i = 0; i < updates; i++) {
          store.accept("session/0/scheme/0", 2L + previous + i);
        }
        long updateNanos = System.nanoTime() - started;
        previous += updates;

        System.out.printf(Locale.ROOT, "%d,%.2f,%.2f,%.2f,",
            count, addNanos / (double) added / 1_000.0,
            lookupNanos / (double) samples / 1_000.0,
            updateNanos / (double) updates / 1_000.0);
      }
      double reopenMs = measureReopen(base, count, samples, random);
      System.out.printf(Locale.ROOT, "%.2f,%d,%d%n", reopenMs,
          Files.size(directory.resolve("counters.idx")),
          Files.size(directory.resolve("counters.dat")));
    }
    System.out.println("Files retained for inspection: " + directory);
  }

  private static double measureReopen(Path base, int count, int samples, Random random) throws Exception {
    long start = System.nanoTime();
    try (IndexedCounterStore reopened = new IndexedCounterStore(base)) {
      double millis = (System.nanoTime() - start) / 1_000_000.0;
      for (int i = 0; i < samples; i++) {
        if (reopened.highWaterMark("session/" + random.nextInt(count) + "/scheme/0") < 1) {
          throw new IllegalStateException("Lost counter during restart");
        }
      }
      return millis;
    }
  }
}
