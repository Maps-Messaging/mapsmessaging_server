/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.storage.counter;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Locale;

/**
 * Run each cardinality as a separate JVM process in IntelliJ.
 * Args: <sessions> [existing-store-directory].
 * System.gc is only a measurement aid, not a precise retained-size profiler.
 */
public final class IndexedCounterHeapBenchmark {
  private IndexedCounterHeapBenchmark() {}

  public static void main(String[] args) throws Exception {
    int sessions = args.length > 0 ? Integer.parseInt(args[0]) : 1_000_000;
    if (sessions < 1 || sessions > 1_000_000) {
      throw new IllegalArgumentException("Sessions must be 1..1000000");
    }
    Path directory = args.length > 1
        ? Path.of(args[1]) : Files.createTempDirectory("counter-heap-isolated-");
    Path base = directory.resolve("counters");
    if (!Files.exists(base.resolveSibling("counters.idx"))) {
      if (Files.exists(directory)) {
        // Atomic bulk loader requires an unpublished directory.
        Path target = directory.resolve("initialized");
        IndexedCounterBulkLoader.initialize(target, entries(sessions));
        base = target.resolve("counters");
      }
    }
    fullGc();
    long before = heapUsed();
    IndexedCounterStore store = new IndexedCounterStore(base);
    long started = System.nanoTime();
    fullGc();
    long loaded = heapUsed();
    double lookupSum = 0;
    for (int i = 0; i < 1000; i++) {
      lookupSum += store.highWaterMark("session/" + (i % sessions) + "/scheme/0");
    }
    if (lookupSum != 1000) throw new IllegalStateException("Incorrect index state");
    System.out.printf(Locale.ROOT, "sessions=%d before_bytes=%d loaded_bytes=%d retained_delta_bytes=%d gc_and_probe_ms=%.2f%n",
        sessions, before, loaded, loaded - before, (System.nanoTime() - started) / 1_000_000.0);
    store.close();
    store = null;
    fullGc();
    System.out.printf(Locale.ROOT, "after_close_gc_bytes=%d directory=%s%n", heapUsed(), directory);
  }

  private static Iterable<IndexedCounterBulkLoader.Entry> entries(int sessions) {
    return () -> new Iterator<>() {
      private int next;
      @Override public boolean hasNext() { return next < sessions; }
      @Override public IndexedCounterBulkLoader.Entry next() {
        return new IndexedCounterBulkLoader.Entry("session/" + next++ + "/scheme/0", 1);
      }
    };
  }

  private static long heapUsed() {
    MemoryMXBean bean = ManagementFactory.getMemoryMXBean();
    return bean.getHeapMemoryUsage().getUsed();
  }

  private static void fullGc() throws InterruptedException {
    for (int i = 0; i < 3; i++) {
      System.gc();
      Thread.sleep(80);
    }
  }
}
