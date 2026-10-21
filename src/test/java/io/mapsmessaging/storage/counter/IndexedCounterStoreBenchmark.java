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
 * test classpath and optionally pass maximum keys, sample count and durability mode.
 *
 * Example: java ... IndexedCounterStoreBenchmark 1000000 10000 BATCHED
 *
 * WARNING: new-key registration still durably commits the index and values;
 * even in BATCHED mode, a million new keys can take a very long time.
 * No assertions about disk throughput should be inferred from warm cache runs.
 */
public final class IndexedCounterStoreBenchmark {
  private static final int[] SIZES = {1, 10, 100, 1_000, 10_000, 100_000, 1_000_000};

  private IndexedCounterStoreBenchmark() {}

  public static void main(String[] args) throws Exception {
    int maximum = args.length > 0 ? Integer.parseInt(args[0]) : 10_000;
    int samples = args.length > 1 ? Integer.parseInt(args[1]) : 1_000;
    CounterDurability durability = args.length > 2
        ? CounterDurability.valueOf(args[2].toUpperCase(Locale.ROOT)) : CounterDurability.STRICT;
    if (durability == CounterDurability.RESERVED_RANGE) {
      throw new IllegalArgumentException("RESERVED_RANGE requires a reservation benchmark, not inbound accept()");
    }
    if (maximum < 1 || maximum > 1_000_000 || samples < 1) {
      throw new IllegalArgumentException("maximum must be 1..1000000 and samples positive");
    }

    Path directory = Files.createTempDirectory("indexed-counter-bench-");
    Path base = directory.resolve("counters");
    System.out.println("Store: " + base);
    System.out.println("JVM: " + System.getProperty("java.version"));
    System.out.println("Durability: " + durability);
    System.out.println("Sessions,add_us_per_key,lookup_us_per_op,update_us_per_op,flush_ms,reopen_ms,index_bytes,value_bytes");
    int count = 0;
    int updatesPerCheckpoint = Math.min(samples, 100);
    Random random = new Random(324);
    List<Integer> checkpoints = new ArrayList<>();
    for (int size : SIZES) if (size <= maximum) checkpoints.add(size);
    if (checkpoints.isEmpty() || checkpoints.getLast() != maximum) checkpoints.add(maximum);

    for (int target : checkpoints) {
      try (IndexedCounterStore store = new IndexedCounterStore(base, durability, 1_000, 100)) {
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

        // Distribute updates across the established key space. Repeated updates
        // target existing keys, so index append cost is excluded.
        int updates = updatesPerCheckpoint;
        int[] chosen = new int[updates];
        for (int i = 0; i < updates; i++) {
          chosen[i] = random.nextInt(count);
        }
        // Advance each chosen key from its actual current value.
        started = System.nanoTime();
        for (int id : chosen) {
          String key = "session/" + id + "/scheme/0";
          store.accept(key, store.highWaterMark(key) + 1);
        }
        long updateNanos = System.nanoTime() - started;
        started = System.nanoTime();
        store.flush();
        long flushNanos = System.nanoTime() - started;

        System.out.printf(Locale.ROOT, "%d,%.2f,%.2f,%.2f,%.3f,",
            count, addNanos / (double) added / 1_000.0,
            lookupNanos / (double) samples / 1_000.0,
            updateNanos / (double) updates / 1_000.0,
            flushNanos / 1_000_000.0);
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
