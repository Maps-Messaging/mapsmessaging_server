/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.storage.delivery;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
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
  public enum Durability {
    SYNC,
    BATCHED
  }
  public record Delivery(String topic, long messageId) {
  }
  public record Allocation(int packetId, boolean existing) {
  }

  private final Map<Delivery, Integer> byDelivery = new HashMap<>();
  private final Map<Integer, Delivery> byPacket = new HashMap<>();
  private final Map<Integer, Integer> stages = new HashMap<>();
  private final Map<Integer, Integer> qosLevels = new HashMap<>();
  private final Set<Integer> reserved = new HashSet<>();
  private final int maximum;
  private final PacketIdJournal journal;
  private int cursor;
  private boolean closed;

  private PacketIdStore(int maximum, PacketIdJournal journal) {
    if (maximum < 1 || maximum > 0xffff) {
      throw new IllegalArgumentException("Packet identifier capacity must be 1..65535");
    }
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
  @SuppressWarnings("java:S2095") // Successful return transfers journal ownership to PacketIdStore; recovery failures close it.
  public static PacketIdStore open(Path file, int maximum, Durability durability) throws IOException {
    if (maximum < 1 || maximum > 0xffff) {
      throw new IllegalArgumentException("Packet identifier capacity must be 1..65535");
    }
    PacketIdJournal journal = new PacketIdJournal(file, maximum, Objects.requireNonNull(durability));
    PacketIdStore persistent = new PacketIdStore(maximum, journal);
    try {
      journal.recover(persistent.byPacket, persistent.byDelivery, persistent.stages, persistent.qosLevels);
      return persistent;
    } catch (IOException | RuntimeException failure) {
      try {
        journal.close();
      } catch (IOException closeFailure) {
        failure.addSuppressed(closeFailure);
      }
      throw failure;
    }
  }

  public synchronized Allocation allocate(String topic, long messageId) throws IOException {
    ensureOpen();
    Delivery delivery = key(topic, messageId);
    Integer existing = byDelivery.get(delivery);
    if (existing != null) {
      return new Allocation(existing, true);
    }
    validateTopic(topic);
    int candidate = nextAvailableId();
    if (journal != null) journal.allocate(candidate, delivery);
    byPacket.put(candidate, delivery);
    byDelivery.put(delivery, candidate);
    cursor = candidate;
    return new Allocation(candidate, false);
  }

  /** Reserves an identifier for connection-scoped control traffic without persistence. */
  public synchronized int reservePacketId() throws IOException {
    ensureOpen();
    int packetId = nextAvailableId();
    reserved.add(packetId);
    cursor = packetId;
    return packetId;
  }

  /** Releases only a control reservation, never an outstanding delivery. */
  public synchronized boolean releaseReservedPacketId(int packetId) {
    if (closed) {
      throw new IllegalStateException("Packet identifier store is closed");
    }
    validateId(packetId);
    return reserved.remove(packetId);
  }

  /** Stage 0 publishes; stage 1 awaits PUBCOMP. Unknown identifiers default to 0. */
  public synchronized int getStage(int packetId) throws IOException {
    ensureOpen();
    validateId(packetId);
    return stages.getOrDefault(packetId, 0);
  }

  /** Advances an outstanding delivery; repeated updates are idempotent. */
  public synchronized void setStage(int packetId, int stage) throws IOException {
    ensureOpen();
    validateId(packetId);
    if (!byPacket.containsKey(packetId)) {
      throw new IllegalArgumentException("No outstanding delivery for packet identifier");
    }
    int current = stages.getOrDefault(packetId, 0);
    if (stage < current || stage > 1 || stage < 0) {
      throw new IllegalArgumentException("Invalid delivery stage transition");
    }
    if (stage == 1 && qosLevels.getOrDefault(packetId, 0) == 1) {
      throw new IllegalArgumentException("QoS 1 delivery cannot await PUBCOMP");
    }
    if (stage == current) {
      return;
    }
    if (journal != null) journal.setStage(packetId, stage);
    stages.put(packetId, stage);
  }

  /** Original wire QoS, or 0 when unspecified; legacy PUBREL stages imply QoS 2. */
  public synchronized int getQos(int packetId) throws IOException {
    ensureOpen();
    validateId(packetId);
    return qosLevels.getOrDefault(packetId, stages.getOrDefault(packetId, 0) == 1 ? 2 : 0);
  }

  /** Records the immutable original QoS before a delivery reaches the wire. */
  public synchronized void setQos(int packetId, int qos) throws IOException {
    ensureOpen();
    validateId(packetId);
    if (!byPacket.containsKey(packetId)) {
      throw new IllegalArgumentException("No outstanding delivery for packet identifier");
    }
    if (qos != 1 && qos != 2) {
      throw new IllegalArgumentException("Delivery QoS must be 1 or 2");
    }
    int current = getQos(packetId);
    if (current != 0 && current != qos) {
      throw new IllegalArgumentException("Delivery QoS cannot change");
    }
    if (qosLevels.containsKey(packetId)) {
      return;
    }
    if (journal != null) journal.setQos(packetId, qos);
    qosLevels.put(packetId, qos);
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
    if (delivery == null) {
      return false;
    }
    if (journal != null) {
      journal.clear(packetId);
    }
    byPacket.remove(packetId);
    stages.remove(packetId);
    qosLevels.remove(packetId);
    byDelivery.remove(delivery);
    return true;
  }

  public synchronized boolean clear(String topic, long messageId) throws IOException {
    ensureOpen();
    Integer packetId = byDelivery.get(key(topic, messageId));
    return packetId != null && clear(packetId);
  }

  /** Atomically clears only a matching delivery still at the PUBLISH stage. */
  public synchronized boolean clearPendingDelivery(String topic, long messageId) throws IOException {
    ensureOpen();
    Integer packetId = byDelivery.get(key(topic, messageId));
    return packetId != null && stages.getOrDefault(packetId, 0) == 0 && clear(packetId);
  }

  /** Removes every delivery for an exact subscription key while preventing ID reuse. */
  public synchronized int clearDeliveries(String topic) throws IOException {
    ensureOpen();
    key(topic, 0);
    List<Integer> packetIds = new ArrayList<>();
    for (Map.Entry<Integer, Delivery> entry : byPacket.entrySet()) {
      if (entry.getValue().topic().equals(topic)) {
        packetIds.add(entry.getKey());
      }
    }
    for (int packetId : packetIds) {
      clear(packetId);
    }
    return packetIds.size();
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

  /** Durability barrier; large journals with substantial dead history are compacted. */
  public synchronized void flush() throws IOException {
    ensureOpen();
    if (journal != null) {
      if (journal.needsCompaction(byPacket.size())) journal.compact(byPacket, stages, qosLevels);
      else journal.flush();
    }
  }

  /** Explicit maintenance operation; ordinary ACKs do not rewrite the store. */
  public synchronized void compact() throws IOException {
    ensureOpen();
    if (journal != null) {
      journal.compact(byPacket, stages, qosLevels);
    }
  }

  @Override
  public synchronized void close() throws IOException {
    if (closed) {
      return;
    }
    closed = true;
    if (journal != null) {
      journal.close();
    }
    byPacket.clear();
    byDelivery.clear();
    stages.clear();
    qosLevels.clear();
    reserved.clear();
  }

  private int nextAvailableId() throws IOException {
    if (byPacket.size() + reserved.size() == maximum) {
      throw new IOException("All packet identifiers are outstanding or reserved");
    }
    int candidate = cursor;
    do { candidate = candidate == maximum ? 1 : candidate + 1; }
    while (byPacket.containsKey(candidate) || reserved.contains(candidate));
    return candidate;
  }

  private Delivery key(String topic, long messageId) {
    Objects.requireNonNull(topic, "topic");
    if (topic.isEmpty()) {
      throw new IllegalArgumentException("Topic must not be empty");
    }
    return new Delivery(topic, messageId);
  }

  private void validateTopic(String topic) {
    if (topic.getBytes(StandardCharsets.UTF_8).length > PacketIdJournal.MAX_TOPIC_BYTES
        || !StandardCharsets.UTF_8.newEncoder().canEncode(topic)) {
      throw new IllegalArgumentException("Topic must be valid UTF-8 and contain 1..65535 bytes");
    }
  }

  private void validateId(int packetId) {
    if (packetId < 1 || packetId > maximum) {
      throw new IllegalArgumentException("Packet identifier outside capacity");
    }
  }

  private void ensureOpen() throws IOException {
    if (closed) {
      throw new IOException("Packet identifier store is closed");
    }
    if (journal != null) {
      journal.ensureUsable();
    }
  }
}
