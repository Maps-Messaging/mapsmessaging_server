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
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.zip.CRC32C;

/**
 * Generation-managed counter store. The CRC-protected manifest is the only
 * authority for selecting an active generation. Incomplete/new generations
 * remain unreferenced and cannot replace the last committed generation.
 *
 * Retirement explicitly discards replay protection and must only be used
 * AFTER permanent revocation of the identity/key associated with a counter.
 * A retirement marker is durably written before changing the active data.
 */
public final class GenerationCounterStore implements CounterStore {
  private static final int MAGIC = 0x43474d31; // CGM1
  private static final int RECORD_MAGIC = 0x43525431; // CRT1
  private final Path root;
  private final Path manifest;
  private final Path retirements;
  private final FileChannel lockChannel;
  private final FileLock lock;
  private final CounterDurability durability;
  private final int flushWrites;
  private final long flushMillis;
  private final double compactThreshold;
  private final Set<String> retired = new HashSet<>();
  private IndexedCounterStore active;
  private String generation;
  private boolean closed;

  public GenerationCounterStore(Path root, CounterDurability durability, int flushWrites,
      long flushMillis, double compactThreshold) throws IOException {
    if (compactThreshold <= 0 || compactThreshold > 1 || !Double.isFinite(compactThreshold)) {
      throw new IllegalArgumentException("Compaction threshold must be in (0,1]");
    }
    this.root = root.toAbsolutePath().normalize();
    this.manifest = this.root.resolve("ACTIVE");
    this.retirements = this.root.resolve("RETIRED");
    this.durability = durability;
    this.flushWrites = flushWrites;
    this.flushMillis = flushMillis;
    this.compactThreshold = compactThreshold;
    Files.createDirectories(this.root);
    lockChannel = FileChannel.open(this.root.resolve("owner.lock"),
        StandardOpenOption.CREATE, StandardOpenOption.WRITE);
    FileLock acquired;
    try {
      acquired = lockChannel.tryLock();
      if (acquired == null) throw new IOException("Counter generations already have an owner");
    } catch (OverlappingFileLockException e) {
      lockChannel.close();
      throw new IOException("Counter generations already have an owner", e);
    }
    lock = acquired;
    try {
      if (Files.exists(manifest)) {
        generation = decodeManifest(Files.readAllBytes(manifest));
      } else {
        // A missing manifest with existing generations is corruption, not a new store.
        try (var children = Files.list(this.root)) {
          if (children.anyMatch(p -> p.getFileName().toString().startsWith("gen-"))) {
            throw new IOException("Missing counter generation manifest");
          }
        }
        generation = "gen-" + UUID.randomUUID();
        IndexedCounterBulkLoader.initialize(this.root.resolve(generation), java.util.List.of());
        writeManifest(generation);
      }
      readRetirements();
      active = openGeneration(generation);
    } catch (IOException | RuntimeException error) {
      acquired.release();
      lockChannel.close();
      throw error;
    }
  }

  private IndexedCounterStore openGeneration(String name) throws IOException {
    if (!name.matches("gen-[a-zA-Z0-9-]+")) throw new IOException("Invalid generation name");
    Path base = root.resolve(name).resolve("counters");
    if (!Files.isRegularFile(base.resolveSibling("counters.idx"))
        || !Files.isRegularFile(base.resolveSibling("counters.dat"))) {
      throw new IOException("Active generation files missing");
    }
    return new IndexedCounterStore(base, durability, flushWrites, flushMillis);
  }

  @Override public synchronized boolean accept(String key, long value) throws IOException {
    requireOpen();
    if (retired.contains(key)) throw new IOException("Counter identity permanently retired");
    return active.accept(key, value);
  }

  @Override public synchronized Range reserve(String key, int count) throws IOException {
    requireOpen();
    if (retired.contains(key)) throw new IOException("Counter identity permanently retired");
    return active.reserve(key, count);
  }

  @Override public synchronized long highWaterMark(String key) throws IOException {
    requireOpen();
    if (retired.contains(key)) throw new IOException("Counter identity permanently retired");
    return active.highWaterMark(key);
  }

  /**
   * Caller MUST have revoked all credentials associated with this key.
   * Tombstones are durable before retirement succeeds; a retired identity
   * cannot be silently recreated after a restart or compaction.
   */
  public synchronized void retire(String key, boolean credentialRevoked) throws IOException {
    requireOpen();
    if (!credentialRevoked) throw new IOException("Permanent credential revocation required");
    if (key == null || key.isBlank() || key.getBytes(StandardCharsets.UTF_8).length > 4096) {
      throw new IllegalArgumentException("Invalid retirement key");
    }
    if (retired.contains(key)) return;
    byte[] bytes = key.getBytes(StandardCharsets.UTF_8);
    ByteBuffer b = ByteBuffer.allocate(12 + bytes.length).order(ByteOrder.BIG_ENDIAN);
    b.putInt(RECORD_MAGIC).putInt(bytes.length).put(bytes);
    b.putInt(crc(b.array(), 0, b.position())).flip();
    try (FileChannel channel = FileChannel.open(retirements,
        StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
      while (b.hasRemaining()) channel.write(b);
      channel.force(true);
    }
    retired.add(key);
    int activeCount = countActive();
    if ((double) retired.size() / Math.max(1, activeCount) >= compactThreshold) compact();
  }

  /** Writes and validates a NEW generation, then changes one durable manifest. */
  public synchronized void compact() throws IOException {
    requireOpen();
    active.flush();
    String next = "gen-" + UUID.randomUUID();
    Path target = root.resolve(next);
    IndexedCounterCompactor.compact(root.resolve(generation).resolve("counters"), target, retired);
    try (IndexedCounterStore check = new IndexedCounterStore(target.resolve("counters"))) {
      // Opening validates every index record and both-slot recoverability.
    }
    // Ensure no dirty old values remain before making a new generation active.
    active.flush();
    writeManifest(next);
    // After the manifest has switched, failures must be terminal rather than
    // continue writing the old generation.
    IndexedCounterStore old = active;
    closed = true;
    try {
      old.close();
      active = openGeneration(next);
      generation = next;
      closed = false;
    } catch (IOException failure) {
      throw new IOException("Generation switched but new store could not open; reopen required", failure);
    }
  }

  private int countActive() throws IOException {
    // A count is sufficient for a deletion threshold; only called at retirement.
    Path idx = root.resolve(generation).resolve("counters.idx");
    try (FileChannel channel = FileChannel.open(idx, StandardOpenOption.READ)) {
      long pos = 8;
      int count = 0;
      while (pos < channel.size()) {
        ByteBuffer len = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN);
        if (channel.read(len, pos) != 4) throw new IOException("Corrupt index length");
        int n = len.flip().getInt();
        if (n <= 0 || n > 4096 || pos + n + 16L > channel.size()) throw new IOException("Corrupt index entry");
        pos += n + 16L;
        count++;
      }
      return count;
    }
  }

  private void readRetirements() throws IOException {
    if (!Files.exists(retirements)) return;
    byte[] data = Files.readAllBytes(retirements);
    ByteBuffer b = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN);
    while (b.hasRemaining()) {
      int position = b.position();
      if (b.remaining() < 12 || b.getInt() != RECORD_MAGIC) throw new IOException("Corrupt retirement journal");
      int len = b.getInt();
      if (len < 1 || len > 4096 || b.remaining() < len + 4) throw new IOException("Truncated retirement record");
      byte[] key = new byte[len];
      b.get(key);
      if (b.getInt() != crc(data, position, 8 + len)) throw new IOException("Retirement checksum failure");
      retired.add(new String(key, StandardCharsets.UTF_8));
    }
  }

  private void writeManifest(String name) throws IOException {
    byte[] raw = name.getBytes(StandardCharsets.UTF_8);
    ByteBuffer b = ByteBuffer.allocate(12 + raw.length).order(ByteOrder.BIG_ENDIAN);
    b.putInt(MAGIC).putInt(raw.length).put(raw);
    b.putInt(crc(b.array(), 0, b.position())).flip();
    Path temp = root.resolve(".ACTIVE-" + UUID.randomUUID());
    try {
      Files.write(temp, b.array(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
      try (FileChannel f = FileChannel.open(temp, StandardOpenOption.WRITE)) { f.force(true); }
      Files.move(temp, manifest, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      try (FileChannel dir = FileChannel.open(root, StandardOpenOption.READ)) { dir.force(true); }
    } finally {
      Files.deleteIfExists(temp);
    }
  }

  private static String decodeManifest(byte[] bytes) throws IOException {
    ByteBuffer b = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
    if (bytes.length < 12 || b.getInt() != MAGIC) throw new IOException("Corrupt generation manifest");
    int length = b.getInt();
    if (length <= 0 || length > 128 || bytes.length != length + 12
        || ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getInt(8 + length)
            != crc(bytes, 0, 8 + length)) {
      throw new IOException("Corrupt generation manifest");
    }
    byte[] raw = new byte[length];
    b.get(raw);
    String result = new String(raw, StandardCharsets.UTF_8);
    if (!result.matches("gen-[a-zA-Z0-9-]+")) throw new IOException("Invalid generation name");
    return result;
  }

  private static int crc(byte[] bytes, int offset, int length) {
    CRC32C crc = new CRC32C();
    crc.update(bytes, offset, length);
    return (int) crc.getValue();
  }

  private void requireOpen() throws IOException {
    if (closed) throw new IOException("Generation store is closed");
  }

  @Override public synchronized void flush() throws IOException {
    requireOpen();
    active.flush();
  }

  @Override public synchronized void close() throws IOException {
    if (closed) return;
    closed = true;
    try { active.close(); }
    finally {
      try { lock.release(); }
      finally { lockChannel.close(); }
    }
  }
}
