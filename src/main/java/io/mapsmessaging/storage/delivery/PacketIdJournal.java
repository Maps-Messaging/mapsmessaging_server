/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.storage.delivery;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.UUID;
import java.util.zip.CRC32C;

/** Append-only records; lock is separate so atomic compaction retains ownership. */
final class PacketIdJournal implements AutoCloseable {
  static final int MAX_TOPIC_BYTES = 65535;
  private static final int MAGIC = 0x50494431;
  private static final int HEADER_SIZE = 16;
  private static final int ALLOCATE = 1;
  private static final int CLEAR = 2;
  private static final int STAGE = 3;
  private static final int QOS = 4;
  private static final long COMPACTION_MIN_BYTES = 1024L * 1024;
  private long recordCount;
  private final Path file;
  private final int maximum;
  private final PacketIdStore.Durability durability;
  private FileChannel channel;
  private FileChannel owner;
  private FileLock lock;
  private FileLock dataLock;
  private IOException failure;

  PacketIdJournal(Path file, int maximum, PacketIdStore.Durability durability) throws IOException {
    this.file = file.toAbsolutePath().normalize();
    if (Files.isSymbolicLink(this.file)) throw new IOException("Symbolic-link delivery journals are unsupported");
    this.maximum = maximum;
    this.durability = durability;
    Files.createDirectories(this.file.getParent());
    try {
      owner = FileChannel.open(this.file.resolveSibling(this.file.getFileName() + ".lock"),
          StandardOpenOption.CREATE, StandardOpenOption.WRITE);
      lock = owner.tryLock();
      if (lock == null) throw new IOException("Packet identifier store already owned");
      boolean exists = Files.exists(this.file);
      channel = FileChannel.open(this.file, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE, java.nio.file.LinkOption.NOFOLLOW_LINKS);
      dataLock = channel.tryLock();
      if (dataLock == null) throw new IOException("Packet identifier journal already owned");
      if (!exists) {
        writeFully(channel, header());
        channel.force(true);
        forceDirectory();
      }
    } catch (IOException | RuntimeException error) {
      IOException cleanup = release();
      if (cleanup != null) error.addSuppressed(cleanup);
      if (error instanceof OverlappingFileLockException) throw new IOException("Packet identifier store already owned", error);
      throw error;
    }
  }

  void ensureUsable() throws IOException {
    if (failure != null) throw new IOException("Packet identifier persistence failed", failure);
  }

  void recover(Map<Integer, PacketIdStore.Delivery> byPacket, Map<PacketIdStore.Delivery, Integer> byDelivery, Map<Integer, Integer> stages, Map<Integer, Integer> qosLevels) throws IOException {
    channel.position(0);
    if (channel.size() < HEADER_SIZE) throw new IOException("Truncated packet identifier header");
    ByteBuffer header = read(HEADER_SIZE);
    if (header.getInt() != MAGIC || header.getInt() != 1 || header.getInt() != maximum
        || header.getInt() != checksum(header.array(), 12)) throw new IOException("Invalid packet identifier header/capacity");
    while (channel.position() < channel.size()) {
      long start = channel.position();
      if (channel.size() - start < 8) {
        truncateTail(start);
        break;
      }
      ByteBuffer framing = read(8);
      int length = framing.getInt();
      if (framing.getInt() != ~length) throw new IOException("Delivery record length checksum mismatch");
      if (length < 9 || length > MAX_TOPIC_BYTES + 21) throw new IOException("Invalid delivery record length");
      if (channel.size() - channel.position() < length) {
        truncateTail(start);
        break;
      }
      ByteBuffer record = read(length);
      if (record.getInt(length - 4) != checksum(record.array(), length - 4)) throw new IOException("Delivery record checksum mismatch");
      apply(record, byPacket, byDelivery, stages, qosLevels);
      recordCount++;
    }
    channel.position(channel.size());
  }

  private void apply(ByteBuffer record, Map<Integer, PacketIdStore.Delivery> byPacket,
      Map<PacketIdStore.Delivery, Integer> byDelivery, Map<Integer, Integer> stages, Map<Integer, Integer> qosLevels) throws IOException {
    int type = record.get() & 0xff;
    int packetId = record.getInt();
    if (packetId < 1 || packetId > maximum) throw new IOException("Invalid persisted packet identifier");
    if (type == CLEAR && record.limit() == 9) {
      PacketIdStore.Delivery removed = byPacket.remove(packetId);
      if (removed == null) throw new IOException("Completion without allocation");
      byDelivery.remove(removed);
      stages.remove(packetId);
      qosLevels.remove(packetId);
    } else if (type == STAGE && record.limit() == 10) {
      int stage = record.get() & 0xff;
      if (!byPacket.containsKey(packetId) || stage != 1 || stages.containsKey(packetId) || qosLevels.getOrDefault(packetId, 0) == 1) {
        throw new IOException("Invalid persisted delivery stage transition");
      }
      stages.put(packetId, stage);
    } else if (type == QOS && record.limit() == 10) {
      int qos = record.get() & 0xff;
      if (!byPacket.containsKey(packetId) || (qos != 1 && qos != 2) || qosLevels.containsKey(packetId)
          || (qos == 1 && stages.getOrDefault(packetId, 0) == 1)) {
        throw new IOException("Invalid persisted delivery QoS");
      }
      qosLevels.put(packetId, qos);
    } else if (type == ALLOCATE && record.limit() >= 22) {
      long messageId = record.getLong();
      int topicLength = record.getInt();
      if (topicLength < 1 || topicLength != record.limit() - 21) throw new IOException("Invalid persisted topic length");
      byte[] topic = new byte[topicLength];
      record.get(topic);
      String decoded;
      try {
        decoded = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(topic)).toString();
      } catch (CharacterCodingException error) { throw new IOException("Invalid persisted topic encoding", error); }
      PacketIdStore.Delivery delivery = new PacketIdStore.Delivery(decoded, messageId);
      if (byPacket.containsKey(packetId) || byDelivery.containsKey(delivery)) throw new IOException("Duplicate outstanding delivery");
      byPacket.put(packetId, delivery);
      byDelivery.put(delivery, packetId);
    } else { throw new IOException("Invalid delivery record type/size"); }
  }

  void allocate(int packetId, PacketIdStore.Delivery delivery) throws IOException {
    append(allocation(packetId, delivery));
  }

  void clear(int packetId) throws IOException {
    ByteBuffer record = ByteBuffer.allocate(9);
    record.put((byte) CLEAR).putInt(packetId);
    finish(record);
    append(record);
  }

  void setStage(int packetId, int stage) throws IOException {
    append(stageUpdate(packetId, stage));
  }

  private ByteBuffer stageUpdate(int packetId, int stage) {
    ByteBuffer record = ByteBuffer.allocate(10);
    record.put((byte) STAGE).putInt(packetId).put((byte) stage);
    finish(record);
    return record;
  }

  void setQos(int packetId, int qos) throws IOException {
    append(qosUpdate(packetId, qos));
  }

  private ByteBuffer qosUpdate(int packetId, int qos) {
    ByteBuffer record = ByteBuffer.allocate(10);
    record.put((byte) QOS).putInt(packetId).put((byte) qos);
    finish(record);
    return record;
  }

  private ByteBuffer allocation(int packetId, PacketIdStore.Delivery delivery) {
    byte[] topic = delivery.topic().getBytes(StandardCharsets.UTF_8);
    ByteBuffer record = ByteBuffer.allocate(21 + topic.length);
    record.put((byte) ALLOCATE).putInt(packetId).putLong(delivery.messageId()).putInt(topic.length).put(topic);
    finish(record);
    return record;
  }

  private static void finish(ByteBuffer record) {
    record.putInt(checksum(record.array(), record.position()));
    record.flip();
  }

  private void append(ByteBuffer record) throws IOException {
    ensureUsable();
    try {
      writeRecord(channel, record);
      recordCount++;
      if (durability == PacketIdStore.Durability.SYNC) channel.force(false);
    } catch (IOException error) { failure = error; throw error; }
  }

  private static void writeRecord(FileChannel target, ByteBuffer record) throws IOException {
    ByteBuffer framed = ByteBuffer.allocate(8 + record.remaining());
    framed.putInt(record.remaining()).putInt(~record.remaining()).put(record).flip();
    writeFully(target, framed);
  }

  /** Constant-time gate; conservative live-record estimate avoids frequent rewrites. */
  boolean needsCompaction(int liveDeliveries) throws IOException {
    ensureUsable();
    return recordCount >= 4L * (liveDeliveries * 3L + 1)
        && channel.size() >= COMPACTION_MIN_BYTES;
  }

  void flush() throws IOException {
    ensureUsable();
    try { channel.force(false); } catch (IOException error) { failure = error; throw error; }
  }

  void compact(Map<Integer, PacketIdStore.Delivery> outstanding, Map<Integer, Integer> stages, Map<Integer, Integer> qosLevels) throws IOException {
    ensureUsable();
    Path temporary = file.resolveSibling(file.getFileName() + "." + UUID.randomUUID() + ".tmp");
    try {
      try (FileChannel replacement = FileChannel.open(temporary, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
        writeFully(replacement, header());
        for (Map.Entry<Integer, PacketIdStore.Delivery> entry : outstanding.entrySet()) {
          writeRecord(replacement, allocation(entry.getKey(), entry.getValue()));
          Integer qos = qosLevels.get(entry.getKey());
          if (qos != null) writeRecord(replacement, qosUpdate(entry.getKey(), qos));
          Integer stage = stages.get(entry.getKey());
          if (stage != null) writeRecord(replacement, stageUpdate(entry.getKey(), stage));
        }
        replacement.force(true);
      }
      // Once replacement begins, any failure poisons this view until reopen.
      channel.close();
      Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      forceDirectory();
      channel = FileChannel.open(file, StandardOpenOption.READ, StandardOpenOption.WRITE, java.nio.file.LinkOption.NOFOLLOW_LINKS);
      dataLock = channel.tryLock();
      if (dataLock == null) throw new IOException("Replacement delivery journal already owned");
      channel.position(channel.size());
      recordCount = (long) outstanding.size() + stages.size() + qosLevels.size();
    } catch (IOException error) { failure = error; throw error;
    } finally { Files.deleteIfExists(temporary); }
  }

  private ByteBuffer header() {
    ByteBuffer header = ByteBuffer.allocate(HEADER_SIZE);
    header.putInt(MAGIC).putInt(1).putInt(maximum);
    finish(header);
    return header;
  }

  private void truncateTail(long offset) throws IOException {
    channel.truncate(offset);
    channel.force(true);
  }

  private ByteBuffer read(int length) throws IOException {
    ByteBuffer buffer = ByteBuffer.allocate(length);
    while (buffer.hasRemaining()) if (channel.read(buffer) <= 0) throw new IOException("Incomplete delivery record");
    return buffer.flip();
  }

  private static void writeFully(FileChannel target, ByteBuffer buffer) throws IOException {
    while (buffer.hasRemaining()) if (target.write(buffer) <= 0) throw new IOException("Unable to write delivery record");
  }

  private static int checksum(byte[] bytes, int length) {
    CRC32C crc = new CRC32C();
    crc.update(bytes, 0, length);
    return (int) crc.getValue();
  }

  private void forceDirectory() throws IOException {
    // Windows NIO cannot open a directory channel. File contents are still
    // forced, but directory-entry power-loss durability is platform-dependent.
    if (System.getProperty("os.name", "").startsWith("Windows")) return;
    try (FileChannel directory = FileChannel.open(file.getParent(), StandardOpenOption.READ)) { directory.force(true); }
  }

  @Override
  public void close() throws IOException {
    IOException error = null;
    if (channel != null && channel.isOpen() && failure == null) {
      try { flush(); } catch (IOException flushFailure) { error = flushFailure; }
    }
    IOException cleanup = release();
    if (error == null) error = cleanup;
    else if (cleanup != null) error.addSuppressed(cleanup);
    if (error != null) throw error;
  }

  private IOException release() {
    IOException error = null;
    for (AutoCloseable resource : new AutoCloseable[]{dataLock, channel, lock, owner}) {
      if (resource == null || (resource instanceof FileLock fileLock && !fileLock.isValid())) continue;
      try { resource.close(); } catch (Exception closeFailure) {
        if (error == null) error = new IOException("Failed to close packet identifier persistence", closeFailure);
        else error.addSuppressed(closeFailure);
      }
    }
    return error;
  }
}
