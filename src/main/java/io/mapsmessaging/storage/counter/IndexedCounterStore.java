/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.storage.counter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.zip.CRC32C;

/**
 * Indexed fixed-record counter store. An append-only checksummed index points
 * to pairs of checksummed value slots; writing the inactive slot preserves
 * the last committed value through interrupted writes.
 *
 * This initial format has no deletion or compaction. Those operations must
 * be implemented with atomic generation switching, not independent renames.
 */
public final class IndexedCounterStore implements CounterStore {
  private static final int INDEX_MAGIC = 0x43495831; // CIX1
  private static final int VALUE_MAGIC = 0x43564131; // CVA1
  private static final int HEADER_SIZE = 8;
  private static final int SLOT_SIZE = 24;
  private static final int PAIR_SIZE = 2 * SLOT_SIZE;
  private static final int MAX_KEY_BYTES = 4096;

  private final FileChannel index;
  private final FileChannel values;
  private final FileChannel ownershipChannel;
  private final FileLock ownership;
  private final Map<String, Long> offsets = new HashMap<>();
  private boolean closed;

  public IndexedCounterStore(Path base) throws IOException {
    Objects.requireNonNull(base, "base");
    Path absolute = base.toAbsolutePath().normalize();
    Path indexPath = absolute.resolveSibling(absolute.getFileName() + ".idx");
    Path valuesPath = absolute.resolveSibling(absolute.getFileName() + ".dat");
    Path lockPath = absolute.resolveSibling(absolute.getFileName() + ".lock");
    Files.createDirectories(absolute.getParent());
    ownershipChannel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
    FileLock acquired = null;
    FileChannel openedIndex = null;
    FileChannel openedValues = null;
    try {
      acquired = ownershipChannel.tryLock();
      if (acquired == null) throw new IOException("Counter store already has a writer");
      boolean hasIndex = Files.exists(indexPath);
      boolean hasValues = Files.exists(valuesPath);
      if (hasIndex != hasValues) throw new IOException("Counter index/value pair is incomplete");
      openedIndex = FileChannel.open(indexPath, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
      openedValues = FileChannel.open(valuesPath, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
      if (!hasIndex) {
        writeFully(openedIndex, header(INDEX_MAGIC), 0);
        writeFully(openedValues, header(VALUE_MAGIC), 0);
        openedIndex.force(true);
        openedValues.force(true);
      }
      checkHeader(openedIndex, INDEX_MAGIC);
      checkHeader(openedValues, VALUE_MAGIC);
      if ((openedValues.size() - HEADER_SIZE) % PAIR_SIZE != 0) {
        throw new IOException("Counter value file has an incomplete record");
      }
      loadIndex(openedIndex, openedValues);
      index = openedIndex;
      values = openedValues;
      ownership = acquired;
    } catch (IOException | RuntimeException failure) {
      if (openedIndex != null) openedIndex.close();
      if (openedValues != null) openedValues.close();
      if (acquired != null) acquired.close();
      ownershipChannel.close();
      throw failure;
    }
  }

  @Override
  public synchronized boolean accept(String key, long value) throws IOException {
    ensureOpen();
    if (value <= 0) throw new IllegalArgumentException("counter must be positive");
    long offset = locate(key);
    Slot current = readCurrent(offset);
    if (value <= current.value) return false;
    writeNext(offset, current, value);
    return true;
  }

  @Override
  public synchronized Range reserve(String key, int count) throws IOException {
    ensureOpen();
    if (count <= 0) throw new IllegalArgumentException("count must be positive");
    long offset = locate(key);
    Slot current = readCurrent(offset);
    if (current.value > Long.MAX_VALUE - count) throw new IOException("Counter exhausted");
    long next = current.value + count;
    writeNext(offset, current, next);
    return new Range(current.value + 1, next);
  }

  @Override
  public synchronized long highWaterMark(String key) throws IOException {
    ensureOpen();
    Long offset = offsets.get(validateKey(key));
    return offset == null ? 0 : readCurrent(offset).value;
  }

  private long locate(String key) throws IOException {
    key = validateKey(key);
    Long existing = offsets.get(key);
    if (existing != null) return existing;
    byte[] raw = key.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    long offset = values.size();
    // Make initialized value slots durable BEFORE adding the index pointer.
    writeFully(values, slot(0, 0), offset);
    writeFully(values, slot(0, 0), offset + SLOT_SIZE);
    values.force(true);
    ByteBuffer record = ByteBuffer.allocate(4 + raw.length + 8 + 4).order(ByteOrder.BIG_ENDIAN);
    record.putInt(raw.length).put(raw).putLong(offset);
    record.putInt(checksum(record.array(), 0, record.position()));
    record.flip();
    writeFully(index, record, index.size());
    index.force(true);
    offsets.put(key, offset);
    return offset;
  }

  private void writeNext(long offset, Slot current, long value) throws IOException {
    if (current.generation == Long.MAX_VALUE) throw new IOException("Counter generation exhausted");
    int nextSlot = 1 - current.slot;
    writeFully(values, slot(current.generation + 1, value), offset + (long) nextSlot * SLOT_SIZE);
    values.force(false);
  }

  private Slot readCurrent(long offset) throws IOException {
    Slot a = readSlot(offset, 0);
    Slot b = readSlot(offset, 1);
    if (a == null && b == null) throw new IOException("Both counter slots are invalid");
    if (a == null) return b;
    if (b == null) return a;
    return a.generation >= b.generation ? a : b;
  }

  private Slot readSlot(long offset, int slotNumber) throws IOException {
    ByteBuffer buffer = ByteBuffer.allocate(SLOT_SIZE).order(ByteOrder.BIG_ENDIAN);
    readFully(values, buffer, offset + (long) slotNumber * SLOT_SIZE);
    byte[] bytes = buffer.array();
    int stored = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getInt(16);
    if (stored != checksum(bytes, 0, 16)) return null;
    long generation = buffer.getLong(0);
    long value = buffer.getLong(8);
    if (generation < 0 || value < 0) return null;
    return new Slot(slotNumber, generation, value);
  }

  private static ByteBuffer slot(long generation, long value) {
    ByteBuffer buffer = ByteBuffer.allocate(SLOT_SIZE).order(ByteOrder.BIG_ENDIAN);
    buffer.putLong(generation).putLong(value);
    buffer.putInt(checksum(buffer.array(), 0, 16)).putInt(0).flip();
    return buffer;
  }

  private void loadIndex(FileChannel idx, FileChannel data) throws IOException {
    long position = HEADER_SIZE;
    while (position < idx.size()) {
      ByteBuffer size = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN);
      readFully(idx, size, position);
      int length = size.getInt(0);
      if (length <= 0 || length > MAX_KEY_BYTES || position + 16L + length > idx.size()) {
        throw new IOException("Corrupt or truncated counter index");
      }
      ByteBuffer entry = ByteBuffer.allocate(4 + length + 8 + 4).order(ByteOrder.BIG_ENDIAN);
      readFully(idx, entry, position);
      byte[] bytes = entry.array();
      if (entry.getInt(12 + length) != checksum(bytes, 0, 12 + length)) {
        throw new IOException("Counter index checksum mismatch");
      }
      byte[] keyBytes = new byte[length];
      entry.position(4);
      entry.get(keyBytes);
      String key = new String(keyBytes, java.nio.charset.StandardCharsets.UTF_8);
      long offset = entry.getLong();
      if (offset < HEADER_SIZE || (offset - HEADER_SIZE) % PAIR_SIZE != 0
          || offset + PAIR_SIZE > data.size() || offsets.putIfAbsent(key, offset) != null) {
        throw new IOException("Invalid or duplicate counter index entry");
      }
      readCurrentOn(data, offset);
      position += entry.capacity();
    }
  }

  private static void readCurrentOn(FileChannel data, long offset) throws IOException {
    // Validate record readability during recovery; full checks occur before use.
    ByteBuffer record = ByteBuffer.allocate(PAIR_SIZE);
    readFully(data, record, offset);
  }

  private static String validateKey(String key) {
    if (key == null || key.isBlank()
        || key.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MAX_KEY_BYTES) {
      throw new IllegalArgumentException("Invalid counter key");
    }
    return key;
  }

  private static ByteBuffer header(int magic) {
    return ByteBuffer.allocate(HEADER_SIZE).order(ByteOrder.BIG_ENDIAN).putInt(magic).putInt(1).flip();
  }

  private static void checkHeader(FileChannel channel, int magic) throws IOException {
    if (channel.size() < HEADER_SIZE) throw new IOException("Truncated counter store header");
    ByteBuffer header = ByteBuffer.allocate(HEADER_SIZE).order(ByteOrder.BIG_ENDIAN);
    readFully(channel, header, 0);
    if (header.getInt(0) != magic || header.getInt(4) != 1) {
      throw new IOException("Unsupported or corrupt counter store header");
    }
  }

  private static int checksum(byte[] bytes, int start, int length) {
    CRC32C crc = new CRC32C();
    crc.update(bytes, start, length);
    return (int) crc.getValue();
  }

  private static void readFully(FileChannel channel, ByteBuffer buffer, long offset) throws IOException {
    while (buffer.hasRemaining()) {
      int read = channel.read(buffer, offset + buffer.position());
      if (read <= 0) throw new IOException("Truncated or unreadable counter record");
    }
    buffer.flip();
  }

  private static void writeFully(FileChannel channel, ByteBuffer buffer, long offset) throws IOException {
    while (buffer.hasRemaining()) {
      int written = channel.write(buffer, offset + buffer.position());
      if (written <= 0) throw new IOException("Unable to write counter record");
    }
  }

  private void ensureOpen() throws IOException {
    if (closed) throw new IOException("Counter store is closed");
  }

  @Override
  public synchronized void close() throws IOException {
    if (closed) return;
    closed = true;
    try {
      index.close();
      values.close();
    } finally {
      try {
        ownership.release();
      } finally {
        ownershipChannel.close();
      }
    }
  }

  private record Slot(int slot, long generation, long value) {}
}
