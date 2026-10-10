/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.storage.counter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.zip.CRC32C;

/**
 * Bulk initialization for a NEW counter store only. Builds an unpublished
 * directory containing a matching index/value pair, then publishes the
 * directory with one atomic rename. Never changes an existing store.
 *
 * The destination is a directory; callers subsequently open
 * new IndexedCounterStore(destination.resolve("counters")).
 */
public final class IndexedCounterBulkLoader {
  private static final int INDEX_MAGIC = 0x43495831;
  private static final int VALUE_MAGIC = 0x43564131;
  private static final int SLOT_SIZE = 24;
  private static final int HEADER_SIZE = 8;
  private static final int BUFFER_BYTES = 1 << 20;

  public record Entry(String key, long value) {}

  private IndexedCounterBulkLoader() {}

  public static void initialize(Path destination, Iterable<Entry> entries) throws IOException {
    Objects.requireNonNull(destination, "destination");
    Objects.requireNonNull(entries, "entries");
    Path target = destination.toAbsolutePath().normalize();
    if (Files.exists(target)) throw new IOException("Bulk initialization requires a new directory");
    Path parent = target.getParent();
    if (parent == null) throw new IOException("Bulk store needs a parent directory");
    Files.createDirectories(parent);
    Path staging = Files.createDirectory(parent.resolve("." + target.getFileName() + "-"
        + UUID.randomUUID() + ".staging"));
    boolean published = false;
    try {
      Path idx = staging.resolve("counters.idx");
      Path dat = staging.resolve("counters.dat");
      try (FileChannel index = FileChannel.open(idx, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
           FileChannel values = FileChannel.open(dat, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
        BufferedWriter indexWriter = new BufferedWriter(index);
        BufferedWriter valueWriter = new BufferedWriter(values);
        indexWriter.append(header(INDEX_MAGIC));
        valueWriter.append(header(VALUE_MAGIC));
        long offset = HEADER_SIZE;
        Set<String> seen = new HashSet<>();
        for (Entry entry : entries) {
          if (entry == null || entry.value() < 0) throw new IOException("Invalid bulk counter");
          String key = entry.key();
          if (key == null || key.isBlank()) throw new IOException("Invalid bulk key");
          byte[] bytes = key.getBytes(StandardCharsets.UTF_8);
          if (bytes.length > 4096 || !seen.add(key)) throw new IOException("Oversize or duplicate bulk key");
          ByteBuffer record = ByteBuffer.allocate(4 + bytes.length + 8 + 4).order(ByteOrder.BIG_ENDIAN);
          record.putInt(bytes.length).put(bytes).putLong(offset);
          record.putInt(crc(record.array(), 0, record.position())).flip();
          indexWriter.append(record);
          // The two valid generations allow ordinary updates immediately.
          valueWriter.append(slot(0, entry.value()));
          valueWriter.append(slot(0, entry.value()));
          offset += 2L * SLOT_SIZE;
        }
        indexWriter.finish();
        valueWriter.finish();
        values.force(true);
        index.force(true);
      }
      // Publication must be atomic: no partially initialized directory is visible.
      Files.move(staging, target, StandardCopyOption.ATOMIC_MOVE);
      published = true;
      // Directory synchronization is platform-dependent. Fail if Linux cannot
      // provide it; never claim a durable commit without a parent barrier.
      try (FileChannel directory = FileChannel.open(parent, StandardOpenOption.READ)) {
        directory.force(true);
      }
    } finally {
      if (!published) {
        Files.deleteIfExists(staging.resolve("counters.idx"));
        Files.deleteIfExists(staging.resolve("counters.dat"));
        Files.deleteIfExists(staging);
      }
    }
  }

  private static ByteBuffer header(int magic) {
    return ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putInt(magic).putInt(1).flip();
  }

  private static ByteBuffer slot(long generation, long value) {
    ByteBuffer b = ByteBuffer.allocate(SLOT_SIZE).order(ByteOrder.BIG_ENDIAN);
    b.putLong(generation).putLong(value);
    b.putInt(crc(b.array(), 0, 16)).putInt(0).flip();
    return b;
  }

  private static int crc(byte[] data, int offset, int length) {
    CRC32C crc = new CRC32C();
    crc.update(data, offset, length);
    return (int) crc.getValue();
  }

  private static final class BufferedWriter {
    private final FileChannel channel;
    private final ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_BYTES);
    private BufferedWriter(FileChannel channel) { this.channel = channel; }

    private void append(ByteBuffer source) throws IOException {
      while (source.hasRemaining()) {
        if (!buffer.hasRemaining()) drain();
        int bytes = Math.min(source.remaining(), buffer.remaining());
        int oldLimit = source.limit();
        source.limit(source.position() + bytes);
        buffer.put(source);
        source.limit(oldLimit);
      }
    }

    private void drain() throws IOException {
      buffer.flip();
      while (buffer.hasRemaining()) {
        if (channel.write(buffer) <= 0) throw new IOException("Failed bulk counter write");
      }
      buffer.clear();
    }

    private void finish() throws IOException {
      drain();
    }
  }
}
