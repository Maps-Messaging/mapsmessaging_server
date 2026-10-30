/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.storage.counter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;
import java.util.Properties;
import java.util.Random;

/**
 * Opt-in comparison of STRICT indexed counter updates against a synthetic
 * Properties full-file rewrite baseline. Not run by JUnit or the regular build.
 *
 * Args: [keys=1000] [updates=2000] [warmup=100]. Run on the target filesystem.
 * Both update paths force their file before acknowledging an update, but the
 * Properties baseline is NOT crash-atomic and is not the deleted legacy class.
 * Reported write amplification counts application-level bytes submitted to the
 * filesystem; it is not physical SSD write amplification or filesystem I/O.
 */
public final class CounterStoreComparisonBenchmark {
  private static final int COUNTER_SLOT_BYTES = 24;

  private CounterStoreComparisonBenchmark() {}

  public static void main(String[] args) throws Exception {
    int keys = args.length > 0 ? Integer.parseInt(args[0]) : 1_000;
    int updates = args.length > 1 ? Integer.parseInt(args[1]) : 2_000;
    int warmup = args.length > 2 ? Integer.parseInt(args[2]) : 100;
    if (keys < 1 || keys > 1_000_000 || updates < 1 || warmup < 0
        || (long) updates + warmup > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("Invalid keys, updates or warmup");
    }

    Path directory = Files.createTempDirectory("counter-comparison-");
    Path indexedDirectory = directory.resolve("indexed");
    Path propertiesFile = directory.resolve("properties-counters.properties");
    String[] names = new String[keys];
    for (int i = 0; i < keys; i++) names[i] = "rx/sender-" + i + "/scheme-0";

    IndexedCounterBulkLoader.initialize(indexedDirectory, entries(names));
    Properties properties = new Properties();
    for (String name : names) properties.setProperty(name, "1");
    long[] indexedValues = new long[keys];
    long[] propertyValues = new long[keys];
    Arrays.fill(indexedValues, 1);
    Arrays.fill(propertyValues, 1);

    int[] selected = new int[warmup + updates];
    Random random = new Random(409);
    for (int i = 0; i < selected.length; i++) selected[i] = random.nextInt(keys);

    long indexedNanos;
    long propertiesNanos;
    long propertiesBytes = 0;
    try (IndexedCounterStore indexed = new IndexedCounterStore(
             indexedDirectory.resolve("counters"), CounterDurability.STRICT, 1, 0);
         FileChannel baseline = FileChannel.open(propertiesFile,
             StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
      ByteArrayOutputStream serialized = new ByteArrayOutputStream();
      writeProperties(properties, baseline, serialized);

      for (int i = 0; i < warmup; i++) {
        int id = selected[i];
        if (!indexed.accept(names[id], ++indexedValues[id])) {
          throw new IllegalStateException("Indexed warmup counter not advanced");
        }
        properties.setProperty(names[id], Long.toString(++propertyValues[id]));
        writeProperties(properties, baseline, serialized);
      }

      long start = System.nanoTime();
      for (int i = warmup; i < selected.length; i++) {
        int id = selected[i];
        if (!indexed.accept(names[id], ++indexedValues[id])) {
          throw new IllegalStateException("Indexed counter not advanced");
        }
      }
      indexedNanos = System.nanoTime() - start;

      start = System.nanoTime();
      for (int i = warmup; i < selected.length; i++) {
        int id = selected[i];
        properties.setProperty(names[id], Long.toString(++propertyValues[id]));
        propertiesBytes += writeProperties(properties, baseline, serialized);
      }
      propertiesNanos = System.nanoTime() - start;
    }

    // Verify that both workloads actually persisted the same monotonic values.
    Properties reread = new Properties();
    try (InputStream input = Files.newInputStream(propertiesFile);
         IndexedCounterStore reopened = new IndexedCounterStore(indexedDirectory.resolve("counters"))) {
      reread.load(input);
      for (int i = 0; i < keys; i++) {
        if (indexedValues[i] != propertyValues[i]
            || reopened.highWaterMark(names[i]) != indexedValues[i]
            || Long.parseLong(reread.getProperty(names[i])) != propertyValues[i]) {
          throw new IllegalStateException("Counter mismatch for " + names[i]);
        }
      }
    }

    long indexedBytes = (long) COUNTER_SLOT_BYTES * updates;
    System.out.println("Java=" + System.getProperty("java.version") + ",directory=" + directory);
    System.out.println("Both workloads issue one force per measured update; Properties is a synthetic, non-crash-atomic baseline.");
    System.out.println("Logical bytes submitted exclude metadata, fsync amplification, and physical device writes.");
    System.out.println("implementation,keys,updates,elapsed_ms,updates_per_sec,logical_bytes_written,bytes_per_update,stored_bytes");
    print("indexed_strict", keys, updates, indexedNanos, indexedBytes,
        Files.size(indexedDirectory.resolve("counters.idx"))
            + Files.size(indexedDirectory.resolve("counters.dat")));
    print("properties_full_rewrite", keys, updates, propertiesNanos, propertiesBytes,
        Files.size(propertiesFile));
  }

  private static Iterable<IndexedCounterBulkLoader.Entry> entries(String[] names) {
    return () -> new Iterator<>() {
      private int position;
      @Override public boolean hasNext() { return position < names.length; }
      @Override public IndexedCounterBulkLoader.Entry next() {
        return new IndexedCounterBulkLoader.Entry(names[position++], 1);
      }
    };
  }

  private static long writeProperties(Properties properties, FileChannel channel,
      ByteArrayOutputStream bytes) throws IOException {
    bytes.reset();
    properties.store(bytes, null);
    ByteBuffer buffer = ByteBuffer.wrap(bytes.toByteArray());
    int length = buffer.remaining();
    channel.position(0);
    while (buffer.hasRemaining()) {
      if (channel.write(buffer) <= 0) throw new IOException("Properties rewrite failed");
    }
    channel.truncate(length);
    channel.force(true);
    return length;
  }

  private static void print(String scenario, int keys, int updates, long nanos,
      long bytes, long storedBytes) {
    double seconds = nanos / 1_000_000_000.0;
    System.out.printf(Locale.ROOT, "%s,%d,%d,%.3f,%.1f,%d,%.1f,%d%n",
        scenario, keys, updates, nanos / 1_000_000.0, updates / seconds,
        bytes, (double) bytes / updates, storedBytes);
  }
}
