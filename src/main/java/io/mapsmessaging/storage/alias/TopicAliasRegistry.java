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
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.zip.CRC32C;

/**
 * A single owner's topic alias dictionary. No global state or filesystem I/O.
 *
 * Persistence belongs to the owning session: call snapshot() on mutation,
 * commit through session persistence, and restore the snapshot before
 * processing packets on a recovered session. MQTT 5 connection owners must
 * not persist their mappings across network connections.
 */
public final class TopicAliasRegistry {
  private static final int MAGIC = 0x54415231; // TAR1
  private static final int VERSION = 1;
  private static final int HEADER_BYTES = 16;
  private static final int MAX_TOPIC_BYTES = 65_535;
  private final int maximum;
  private final Map<Integer, String> byAlias = new HashMap<>();
  private final Map<String, Integer> byTopic = new HashMap<>();
  private int nextAlias = 1;

  public TopicAliasRegistry(int maximum) {
    if (maximum < 1 || maximum > 65535) {
      throw new IllegalArgumentException("Alias maximum must be 1..65535");
    }
    this.maximum = maximum;
  }

  public synchronized int register(String topic) throws IOException {
    validateTopic(topic);
    Integer existing = byTopic.get(topic);
    if (existing != null) return existing;
    if (nextAlias > maximum) throw new IOException("Topic alias space exhausted");
    int allocated = nextAlias++;
    byAlias.put(allocated, topic);
    byTopic.put(topic, allocated);
    return allocated;
  }

  /** Add a server-assigned numeric alias; conflicting mappings fail closed. */
  public synchronized void register(int alias, String topic) throws IOException {
    validateTopic(topic);
    if (alias < 1 || alias > maximum) throw new IOException("Topic alias out of range");
    String existingTopic = byAlias.get(alias);
    Integer existingAlias = byTopic.get(topic);
    if ((existingTopic != null && !existingTopic.equals(topic))
        || (existingAlias != null && existingAlias != alias)) {
      throw new IOException("Conflicting topic alias mapping");
    }
    if (existingTopic != null) return;
    byAlias.put(alias, topic);
    byTopic.put(topic, alias);
    if (alias >= nextAlias) nextAlias = alias + 1;
  }

  public synchronized String topic(int alias) {
    return byAlias.get(alias);
  }

  /** Returns null if a topic has no session alias. */
  public synchronized Integer alias(String topic) {
    return byTopic.get(topic);
  }

  public synchronized int size() {
    return byAlias.size();
  }

  public synchronized void clear() {
    byAlias.clear();
    byTopic.clear();
    nextAlias = 1;
  }

  /** Versioned, checksum-protected deterministic snapshot. No storage side effects. */
  public synchronized byte[] snapshot() {
    int length = HEADER_BYTES + 4;
    for (String topic : byTopic.keySet()) {
      length += 6 + topic.getBytes(StandardCharsets.UTF_8).length;
    }
    ByteBuffer buffer = ByteBuffer.allocate(length).order(ByteOrder.BIG_ENDIAN);
    buffer.putInt(MAGIC).putInt(VERSION).putInt(nextAlias).putInt(byAlias.size());
    byAlias.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
      byte[] topic = entry.getValue().getBytes(StandardCharsets.UTF_8);
      buffer.putInt(entry.getKey()).putShort((short) topic.length).put(topic);
    });
    buffer.putInt(crc(buffer.array(), 0, buffer.position()));
    return buffer.array();
  }

  /** Restore atomically: a corrupt snapshot never alters this registry. */
  public synchronized void restore(byte[] snapshot) throws IOException {
    Objects.requireNonNull(snapshot, "snapshot");
    if (snapshot.length < HEADER_BYTES + 4) throw new IOException("Truncated alias snapshot");
    ByteBuffer b = ByteBuffer.wrap(snapshot).order(ByteOrder.BIG_ENDIAN);
    if (b.getInt() != MAGIC || b.getInt() != VERSION) throw new IOException("Unsupported alias snapshot");
    int candidateNext = b.getInt();
    int entries = b.getInt();
    if (candidateNext < 1 || candidateNext > maximum + 1
        || entries < 0 || entries > maximum || entries > (snapshot.length - 20) / 7
        || b.getInt(snapshot.length - 4) != crc(snapshot, 0, snapshot.length - 4)) {
      throw new IOException("Invalid alias snapshot header or checksum");
    }
    Map<Integer, String> ids = new HashMap<>();
    Map<String, Integer> names = new HashMap<>();
    int highest = 0;
    for (int i = 0; i < entries; i++) {
      if (b.remaining() < 10) throw new IOException("Truncated alias mapping");
      int alias = b.getInt();
      int size = Short.toUnsignedInt(b.getShort());
      if (alias < 1 || alias > maximum || size < 1 || size > MAX_TOPIC_BYTES
          || b.remaining() < size + 4) throw new IOException("Invalid alias mapping");
      byte[] encoded = new byte[size];
      b.get(encoded);
      String topic;
      try {
        topic = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(encoded)).toString();
      } catch (CharacterCodingException e) {
        throw new IOException("Malformed alias topic UTF-8", e);
      }
      validateTopic(topic);
      if (ids.putIfAbsent(alias, topic) != null || names.putIfAbsent(topic, alias) != null) {
        throw new IOException("Duplicate alias mapping");
      }
      highest = Math.max(highest, alias);
    }
    if (b.position() != snapshot.length - 4 || candidateNext <= highest) {
      throw new IOException("Inconsistent alias snapshot");
    }
    byAlias.clear();
    byAlias.putAll(ids);
    byTopic.clear();
    byTopic.putAll(names);
    nextAlias = candidateNext;
  }

  private static void validateTopic(String topic) throws IOException {
    if (topic == null || topic.isEmpty() || topic.getBytes(StandardCharsets.UTF_8).length > MAX_TOPIC_BYTES) {
      throw new IOException("Invalid topic name");
    }
  }

  private static int crc(byte[] bytes, int start, int count) {
    CRC32C crc = new CRC32C();
    crc.update(bytes, start, count);
    return (int) crc.getValue();
  }
}
