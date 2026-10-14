/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.storage.delivery;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Session-owned, bidirectional packet identifier allocation. Topics may instead
 * be stable subscription keys when separate deliveries share a topic.
 * Normal disconnect retains this store; close releases ownership, not records.
 * The owner removes records on ACK, expiry, eviction or permanent session removal.
 * Mapping presence means allocation already occurred, not proof of a socket write.
 */
public final class PacketIdStore implements AutoCloseable {
  public enum Durability { SYNC, BATCHED }
  public record Delivery(String topic, long messageId) {}
  public record Allocation(int packetId, boolean existing) {}

  private final Map<Delivery, Integer> byDelivery = new HashMap<>();
  private final Map<Integer, Delivery> byPacket = new HashMap<>();
  private final int maximum;
  private final PacketIdJournal journal;
  private int cursor;
  private boolean closed;

  private PacketIdStore(int maximum, PacketIdJournal journal) {
    if (maximum < 1 || maximum > 0xffff) throw new IllegalArgumentException("Packet identifier capacity must be 1..65535");
    this.maximum = maximum;
    this.journal = journal;
  }

  public static PacketIdStore memory(int maximum) {
    return new PacketIdStore(maximum, null);
  }

  /**
   * Opens persistent state exclusively. BATCHED mutations require flush() before
   * protocol actions that rely on crash durability; close also flushes.
   */
  public static PacketIdStore open(Path file, int maximum, Durability durability) throws IOException {
    if (maximum < 1 || maximum > 0xffff) throw new IllegalArgumentException("Packet identifier capacity must be 1..65535");
    PacketIdJournal journal = new PacketIdJournal(file, maximum, Objects.requireNonNull(durability));
    PacketIdStore persistent = new PacketIdStore(maximum, journal);
    try {
      journal.recover(persistent.byPacket, persistent.byDelivery);
      return persistent;
    } catch (IOException | RuntimeException failure) {
      try { journal.close(); } catch (IOException closeFailure) { failure.addSuppressed(closeFailure); }
      throw failure;
    }
  }

  public synchronized Allocation allocate(String topic, long messageId) throws IOException {
    ensureOpen();
    Delivery delivery = key(topic, messageId);
    Integer existing = byDelivery.get(delivery);
    if (existing != null) return new Allocation(existing, true);
    validateTopic(topic);
    if (byPacket.size() == maximum) throw new IOException("All packet identifiers are outstanding");
    int candidate = cursor;
    do { candidate = candidate == maximum ? 1 : candidate + 1; } while (byPacket.containsKey(candidate));
    if (journal != null) journal.allocate(candidate, delivery);
    byPacket.put(candidate, delivery);
    byDelivery.put(delivery, candidate);
    cursor = candidate;
    return new Allocation(candidate, false);
  }

  /** Null indicates no outstanding delivery. No disk read is required. */
  public synchronized Integer getPacketId(String topic, long messageId) throws IOException {
    ensureOpen();
    return byDelivery.get(key(topic, messageId));
  }

  public synchronized Delivery getDelivery(int packetId) throws IOException {
    ensureOpen();
    validateId(packetId);
    return byPacket.get(packetId);
  }

  public synchronized boolean clear(int packetId) throws IOException {
    ensureOpen();
    validateId(packetId);
    Delivery delivery = byPacket.get(packetId);
    if (delivery == null) return false;
    if (journal != null) journal.clear(packetId);
    byPacket.remove(packetId);
    byDelivery.remove(delivery);
    return true;
  }

  public synchronized boolean clear(String topic, long messageId) throws IOException {
    ensureOpen();
    Integer packetId = byDelivery.get(key(topic, messageId));
    return packetId != null && clear(packetId);
  }

  /** Stable snapshot for reconnect recovery; callers cannot mutate the indexes. */
  public synchronized Map<Integer, Delivery> outstanding() throws IOException {
    ensureOpen();
    return Map.copyOf(byPacket);
  }

  public synchronized int size() throws IOException {
    ensureOpen();
    return byPacket.size();
  }

  public synchronized void flush() throws IOException {
    ensureOpen();
    if (journal != null) journal.flush();
  }

  /** Explicit maintenance operation; ordinary ACKs do not rewrite the store. */
  public synchronized void compact() throws IOException {
    ensureOpen();
    if (journal != null) journal.compact(byPacket);
  }

  @Override public synchronized void close() throws IOException {
    if (closed) return;
    closed = true;
    if (journal != null) journal.close();
    byPacket.clear();
    byDelivery.clear();
  }

  private Delivery key(String topic, long messageId) {
    Objects.requireNonNull(topic, "topic");
    if (topic.isEmpty()) throw new IllegalArgumentException("Topic must not be empty");
    return new Delivery(topic, messageId);
  }

  private void validateTopic(String topic) {
    if (topic.getBytes(StandardCharsets.UTF_8).length > PacketIdJournal.MAX_TOPIC_BYTES
        || !StandardCharsets.UTF_8.newEncoder().canEncode(topic)) {
      throw new IllegalArgumentException("Topic must be valid UTF-8 and contain 1..65535 bytes");
    }
  }

  private void validateId(int packetId) {
    if (packetId < 1 || packetId > maximum) throw new IllegalArgumentException("Packet identifier outside capacity");
  }

  private void ensureOpen() throws IOException {
    if (closed) throw new IOException("Packet identifier store is closed");
    if (journal != null) journal.ensureUsable();
  }
}
