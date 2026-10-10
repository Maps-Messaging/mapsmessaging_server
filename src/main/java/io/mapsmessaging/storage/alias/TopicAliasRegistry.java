/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.storage.alias;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.zip.CRC32C;

/**
 * Instance-owned, register-once topic alias dictionary with O(1) lookups.
 * The owner determines whether it lives only on a connection (MQTT 5), or
 * participates in session persistence (MQTT-SN).
 *
 * Snapshots are complete and CRC-protected. The persistence adapter MUST
 * atomically/durably commit each snapshot before returning from save().
 * Ambiguous save failures poison the registry until a fresh load.
 */
public final class TopicAliasRegistry {
  private static final int MAGIC = 0x54415231; // TAR1
  private static final int VERSION = 1;
  private static final int MAX_TOPIC_BYTES = 65_535;

  public enum PersistenceMode { MEMORY_ONLY, SESSION_PERSISTENT }

  public interface SnapshotPersistence {
    /** Return null for a previously uninitialized session. */
    byte[] load() throws IOException;
    /** Commit the entire snapshot durably before returning. */
    void save(byte[] snapshot) throws IOException;
    /** Remove the snapshot when the owning session is permanently discarded. */
    void delete() throws IOException;
  }

  private final int maximum;
  private final PersistenceMode mode;
  private final SnapshotPersistence persistence;
  private final Map<Integer, String> byAlias = new HashMap<>();
  private final Map<String, Integer> byTopic = new HashMap<>();
  private int nextAlias = 1;
  private boolean poisoned;
  private boolean loaded;
  private boolean destroyed;

  public TopicAliasRegistry(int maximum) {
    this(maximum, PersistenceMode.MEMORY_ONLY, null);
  }

  public TopicAliasRegistry(int maximum, PersistenceMode mode, SnapshotPersistence persistence) {
    if (maximum < 1 || maximum > 65_535) {
      throw new IllegalArgumentException("Alias maximum must be 1..65535");
    }
    this.maximum = maximum;
    this.mode = Objects.requireNonNull(mode, "mode");
    if ((mode == PersistenceMode.SESSION_PERSISTENT) != (persistence != null)) {
      throw new IllegalArgumentException("Persistent mode requires an adapter; memory mode forbids one");
    }
    this.persistence = persistence;
    loaded = mode == PersistenceMode.MEMORY_ONLY;
  }

  public PersistenceMode persistenceMode() {
    return mode;
  }

  /** Must complete before any restored session publishes or registers. */
  public synchronized void load() throws IOException {
    if (mode == PersistenceMode.MEMORY_ONLY) return;
    if (loaded) throw new IOException("Alias registry already loaded");
    try {
      byte[] bytes = persistence.load();
      if (bytes != null) decodeInto(bytes);
      loaded = true;
      poisoned = false;
    } catch (IOException | RuntimeException error) {
      poisoned = true;
      throw error;
    }
  }

  /** Allocate one unique alias, returning the existing value on repeat registration. */
  public synchronized int register(String topic) throws IOException {
    checkUsable();
    validateTopic(topic);
    Integer existing = byTopic.get(topic);
    if (existing != null) return existing;
    int allocated = nextFree();
    Map<Integer, String> candidate = new HashMap<>(byAlias);
    candidate.put(allocated, topic);
    commit(candidate, allocated == maximum ? maximum + 1 : allocated + 1);
    byAlias.put(allocated, topic);
    byTopic.put(topic, allocated);
    nextAlias = allocated + 1;
    return allocated;
  }

  /** Server-assigned mapping; conflicting reassignment is rejected. */
  public synchronized void register(int alias, String topic) throws IOException {
    checkUsable();
    validateTopic(topic);
    validateAlias(alias);
    String existing = byAlias.get(alias);
    Integer old = byTopic.get(topic);
    if ((existing != null && !existing.equals(topic))
        || (old != null && old != alias)) throw new IOException("Conflicting topic alias mapping");
    if (existing != null) return;
    Map<Integer, String> candidate = new HashMap<>(byAlias);
    candidate.put(alias, topic);
    int next = Math.max(nextAlias, alias + 1);
    commit(candidate, next);
    byAlias.put(alias, topic);
    byTopic.put(topic, alias);
    nextAlias = next;
  }

  /**
   * MQTT 5 only: a sender may reassign an existing alias on its connection.
   * This deliberately does NOT perform the session-persistent register-once policy.
   */
  public synchronized void rebind(int alias, String topic) throws IOException {
    checkUsable();
    if (mode != PersistenceMode.MEMORY_ONLY) throw new IOException("Rebinding persistent aliases forbidden");
    validateAlias(alias);
    validateTopic(topic);
    String previous = byAlias.put(alias, topic);
    if (previous != null) byTopic.remove(previous);
    Integer oldAlias = byTopic.put(topic, alias);
    if (oldAlias != null && oldAlias != alias) byAlias.remove(oldAlias);
    nextAlias = Math.max(nextAlias, alias + 1);
  }

  public synchronized String topic(int alias) throws IOException {
    checkUsable();
    return byAlias.get(alias);
  }

  public synchronized Integer alias(String topic) throws IOException {
    checkUsable();
    return byTopic.get(topic);
  }

  public synchronized int size() throws IOException {
    checkUsable();
    return byAlias.size();
  }

  /** Permanent session removal/expiry; normal disconnect must not call this. */
  public synchronized void destroy() throws IOException {
    checkUsable();
    if (mode == PersistenceMode.SESSION_PERSISTENT) {
      try {
        persistence.delete();
      } catch (IOException | RuntimeException failure) {
        poisoned = true;
        throw failure;
      }
    }
    byAlias.clear();
    byTopic.clear();
    nextAlias = 1;
    destroyed = true;
  }

  public synchronized void clear() throws IOException {
    checkUsable();
    commit(Map.of(), 1);
    byAlias.clear();
    byTopic.clear();
    nextAlias = 1;
  }

  /** Snapshot in-memory state. MQTT 5 must not save this across connections. */
  public synchronized byte[] snapshot() throws IOException {
    checkUsable();
    return encode(byAlias, nextAlias);
  }

  /** Manual restore is only for memory-only ownership and tests. */
  public synchronized void restore(byte[] bytes) throws IOException {
    checkUsable();
    if (mode == PersistenceMode.SESSION_PERSISTENT) {
      throw new IOException("Persistent registry must restore through owner load()");
    }
    decodeInto(bytes);
  }

  private int nextFree() throws IOException {
    int next = nextAlias;
    while (next <= maximum && byAlias.containsKey(next)) next++;
    if (next > maximum) throw new IOException("Topic alias space exhausted");
    return next;
  }

  private void commit(Map<Integer, String> candidate, int next) throws IOException {
    if (mode != PersistenceMode.SESSION_PERSISTENT) return;
    try {
      persistence.save(encode(candidate, next));
    } catch (IOException | RuntimeException error) {
      poisoned = true;
      throw error;
    }
  }

  private void decodeInto(byte[] bytes) throws IOException {
    if (bytes == null || bytes.length < 20) throw new IOException("Truncated alias snapshot");
    ByteBuffer b = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
    if (b.getInt() != MAGIC || b.getInt() != VERSION) throw new IOException("Unsupported alias snapshot");
    int savedNext = b.getInt();
    int count = b.getInt();
    if (savedNext < 1 || savedNext > maximum + 1 || count < 0 || count > maximum
        || count > (bytes.length - 20) / 7
        || b.getInt(bytes.length - 4) != crc(bytes, bytes.length - 4)) {
      throw new IOException("Corrupt alias snapshot");
    }
    Map<Integer, String> ids = new HashMap<>();
    Map<String, Integer> names = new HashMap<>();
    int highest = 0;
    for (int i = 0; i < count; i++) {
      if (b.remaining() < 10) throw new IOException("Truncated alias entry");
      int id = b.getInt();
      int size = Short.toUnsignedInt(b.getShort());
      if (id < 1 || id > maximum || size < 1 || size > MAX_TOPIC_BYTES
          || b.remaining() < size + 4) throw new IOException("Invalid alias entry");
      byte[] raw = new byte[size];
      b.get(raw);
      String topic;
      try {
        topic = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(raw)).toString();
      } catch (CharacterCodingException e) {
        throw new IOException("Invalid alias topic UTF-8", e);
      }
      validateTopic(topic);
      if (ids.putIfAbsent(id, topic) != null || names.putIfAbsent(topic, id) != null) {
        throw new IOException("Duplicate alias or topic");
      }
      highest = Math.max(highest, id);
    }
    if (b.position() != bytes.length - 4 || highest >= savedNext) {
      throw new IOException("Invalid alias allocation state");
    }
    byAlias.clear();
    byAlias.putAll(ids);
    byTopic.clear();
    byTopic.putAll(names);
    nextAlias = savedNext;
  }

  private void checkUsable() throws IOException {
    if (poisoned || !loaded || destroyed) throw new IOException("Alias registry is not active");
  }

  private void validateAlias(int alias) throws IOException {
    if (alias < 1 || alias > maximum) throw new IOException("Alias out of range");
  }

  private static void validateTopic(String topic) throws IOException {
    if (topic == null || topic.isEmpty()
        || topic.getBytes(StandardCharsets.UTF_8).length > MAX_TOPIC_BYTES) {
      throw new IOException("Invalid alias topic");
    }
  }

  private static byte[] encode(Map<Integer, String> mapping, int next) {
    int size = 20;
    for (String topic : mapping.values()) size += 6 + topic.getBytes(StandardCharsets.UTF_8).length;
    ByteBuffer b = ByteBuffer.allocate(size).order(ByteOrder.BIG_ENDIAN);
    b.putInt(MAGIC).putInt(VERSION).putInt(next).putInt(mapping.size());
    mapping.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
      byte[] bytes = entry.getValue().getBytes(StandardCharsets.UTF_8);
      b.putInt(entry.getKey()).putShort((short) bytes.length).put(bytes);
    });
    b.putInt(crc(b.array(), b.position()));
    return b.array();
  }

  private static int crc(byte[] bytes, int length) {
    CRC32C checksum = new CRC32C();
    checksum.update(bytes, 0, length);
    return (int) checksum.getValue();
  }
}
