/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.storage.counter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.zip.CRC32C;

/**
 * Creates a compact, atomically published NEW store from an existing store.
 * The source is held under the normal single-writer lock while the snapshot
 * is copied; it is never mutated, renamed or deleted.
 *
 * This is an offline snapshot operation, NOT hot compaction. Keys may only be
 * excluded after the caller has permanently retired the corresponding
 * identity/credential. Exclusion intentionally removes replay history.
 */
public final class IndexedCounterCompactor {
  private static final int MAGIC = 0x43495831;
  private static final int HEADER = 8;
  private static final int MAX_KEY_BYTES = 4096;

  private IndexedCounterCompactor() {}

  public static void compact(Path sourceBase, Path newDirectory,
      Set<String> permanentlyRetiredKeys) throws IOException {
    Objects.requireNonNull(sourceBase, "sourceBase");
    Objects.requireNonNull(newDirectory, "newDirectory");
    Objects.requireNonNull(permanentlyRetiredKeys, "permanentlyRetiredKeys");
    Path absolute = sourceBase.toAbsolutePath().normalize();
    Path target = newDirectory.toAbsolutePath().normalize();
    if (Files.exists(target)) throw new IOException("Compaction destination already exists");
    if (target.equals(absolute.getParent()) || target.equals(absolute)
        || target.startsWith(absolute)) {
      throw new IOException("Compaction destination overlaps source");
    }
    Set<String> retired = Set.copyOf(permanentlyRetiredKeys);
    try (IndexedCounterStore source = new IndexedCounterStore(absolute);
         FileChannel index = FileChannel.open(absolute.resolveSibling(
             absolute.getFileName() + ".idx"), StandardOpenOption.READ)) {
      // Explicitly synchronize any dirty state and hold ownership while copying.
      source.flush();
      Iterable<IndexedCounterBulkLoader.Entry> records = () -> new IndexIterator(index, source, retired);
      try {
        IndexedCounterBulkLoader.initialize(target, records);
      } catch (UncheckedCompactionException error) {
        throw error.io;
      }
    }
  }

  private static final class IndexIterator implements Iterator<IndexedCounterBulkLoader.Entry> {
    private final FileChannel channel;
    private final IndexedCounterStore source;
    private final Set<String> retired;
    private final Set<String> seen = new HashSet<>();
    private final long length;
    private long offset = HEADER;
    private IndexedCounterBulkLoader.Entry next;

    private IndexIterator(FileChannel channel, IndexedCounterStore source, Set<String> retired) {
      this.channel = channel;
      this.source = source;
      this.retired = retired;
      try {
        length = channel.size();
        ByteBuffer header = read(channel, HEADER, 0);
        if (header.getInt(0) != MAGIC || header.getInt(4) != 1) {
          throw new IOException("Unsupported counter index header");
        }
        advance();
      } catch (IOException e) {
        throw new UncheckedCompactionException(e);
      }
    }

    @Override public boolean hasNext() { return next != null; }

    @Override public IndexedCounterBulkLoader.Entry next() {
      if (next == null) throw new java.util.NoSuchElementException();
      IndexedCounterBulkLoader.Entry current = next;
      try {
        advance();
      } catch (IOException e) {
        throw new UncheckedCompactionException(e);
      }
      return current;
    }

    private void advance() throws IOException {
      next = null;
      while (offset < length) {
        if (length - offset < 4) throw new IOException("Truncated counter index");
        int keyLength = read(channel, 4, offset).getInt();
        if (keyLength <= 0 || keyLength > MAX_KEY_BYTES || length - offset < keyLength + 16L) {
          throw new IOException("Corrupt counter index entry");
        }
        ByteBuffer record = read(channel, keyLength + 16, offset);
        byte[] data = record.array();
        CRC32C crc = new CRC32C();
        crc.update(data, 0, data.length - 4);
        if (record.getInt(data.length - 4) != (int) crc.getValue()) {
          throw new IOException("Counter index checksum mismatch");
        }
        byte[] keyBytes = new byte[keyLength];
        record.position(4);
        record.get(keyBytes);
        String key;
        try {
          key = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(keyBytes)).toString();
        } catch (CharacterCodingException bad) {
          throw new IOException("Invalid UTF-8 counter key", bad);
        }
        if (!seen.add(key)) throw new IOException("Duplicate counter key");
        offset += data.length;
        if (!retired.contains(key)) {
          next = new IndexedCounterBulkLoader.Entry(key, source.highWaterMark(key));
          return;
        }
      }
    }
  }

  private static ByteBuffer read(FileChannel channel, int length, long offset) throws IOException {
    ByteBuffer buffer = ByteBuffer.allocate(length).order(ByteOrder.BIG_ENDIAN);
    while (buffer.hasRemaining()) {
      int n = channel.read(buffer, offset + buffer.position());
      if (n <= 0) throw new IOException("Truncated counter file");
    }
    buffer.flip();
    return buffer;
  }

  private static final class UncheckedCompactionException extends RuntimeException {
    private final IOException io;
    private UncheckedCompactionException(IOException io) {
      super(io);
      this.io = io;
    }
  }
}
