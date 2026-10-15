/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.storage.counter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Locale;
import java.util.Random;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

/** Opt-in cardinality benchmark using atomic fresh-store bulk initialization. */
public final class IndexedCounterBulkBenchmark {
  private static final int[] SIZES = {1, 10, 100, 1_000, 10_000, 100_000, 1_000_000};
  private IndexedCounterBulkBenchmark() {}

  public static void main(String[] args) throws Exception {
    int maximum = args.length > 0 ? Integer.parseInt(args[0]) : 1_000_000;
    int samples = args.length > 1 ? Integer.parseInt(args[1]) : 10_000;
    if (maximum < 1 || maximum > 1_000_000 || samples < 1) {
      throw new IllegalArgumentException("Invalid session count or samples");
    }
    Path root = Files.createTempDirectory("counter-bulk-benchmark-");
    System.out.println("JVM: " + System.getProperty("java.version"));
    System.out.println("Sessions,bulk_load_ms,reopen_ms,lookup_us_per_op,update_us_per_op,flush_ms,index_bytes,value_bytes,heap_before_bytes,heap_loaded_bytes,heap_after_close_bytes");
    for (int requested : SIZES) {
      if (requested > maximum) break;
      Path directory = root.resolve("sessions-" + requested);
      Iterable<IndexedCounterBulkLoader.Entry> entries = () -> new Iterator<>() {
        private int cursor;
        @Override public boolean hasNext() { return cursor < requested; }
        @Override public IndexedCounterBulkLoader.Entry next() {
          int id = cursor++;
          return new IndexedCounterBulkLoader.Entry("session/" + id + "/scheme/0", 1);
        }
      };
      long started = System.nanoTime();
      IndexedCounterBulkLoader.initialize(directory, entries);
      double loadMs = (System.nanoTime() - started) / 1_000_000.0;
      Path base = directory.resolve("counters");
      long heapBefore = heapUsed();
      started = System.nanoTime();
      double reopenMs;
      double lookupUs;
      double updateUs;
      double flushMs;
      long heapLoaded;
      Random random = new Random(324);
      try (IndexedCounterStore store = new IndexedCounterStore(
          base, CounterDurability.BATCHED, Integer.MAX_VALUE - 1, 0)) {
        reopenMs = (System.nanoTime() - started) / 1_000_000.0;
        heapLoaded = heapUsed();
        started = System.nanoTime();
        for (int i = 0; i < samples; i++) {
          if (store.highWaterMark("session/" + random.nextInt(requested) + "/scheme/0") < 1) {
            throw new IllegalStateException("Counter lost");
          }
        }
        lookupUs = (System.nanoTime() - started) / (double) samples / 1_000.0;
        int updates = Math.min(samples, 1000);
        started = System.nanoTime();
        for (int i = 0; i < updates; i++) {
          int id = random.nextInt(requested);
          String key = "session/" + id + "/scheme/0";
          store.accept(key, store.highWaterMark(key) + 1);
        }
        updateUs = (System.nanoTime() - started) / (double) updates / 1_000.0;
        started = System.nanoTime();
        store.flush();
        flushMs = (System.nanoTime() - started) / 1_000_000.0;
      }
      long heapAfterClose = heapUsed();
      System.out.printf(Locale.ROOT, "%d,%.2f,%.2f,%.2f,%.2f,%.3f,%d,%d,%d,%d,%d%n",
          requested, loadMs, reopenMs, lookupUs, updateUs, flushMs,
          Files.size(directory.resolve("counters.idx")),
          Files.size(directory.resolve("counters.dat")),
          heapBefore, heapLoaded, heapAfterClose);
    }
    System.out.println("Files retained: " + root);
  }

  private static long heapUsed() {
    MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
    return memory.getHeapMemoryUsage().getUsed();
  }
}
