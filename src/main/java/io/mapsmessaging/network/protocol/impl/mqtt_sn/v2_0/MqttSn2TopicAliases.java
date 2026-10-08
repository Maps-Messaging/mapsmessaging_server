/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.IntFunction;

/**
 * Connection-scoped MQTT-SN 2.0 topic aliases.
 * CSD01: 0=session alias, 1=predefined alias, 2=reserved, 3=topic name.
 * Caller supplies configured predefined topic resolution separately.
 */
public final class MqttSn2TopicAliases {
  private final Map<String, Integer> nameToAlias = new HashMap<>();
  private final Map<Integer, String> aliasToName = new HashMap<>();
  private int nextAlias = 1;

  public synchronized int register(String name) throws IOException {
    if (name == null || name.isEmpty()) {
      throw new IOException("MQTT-SN 2.0 empty Topic Name");
    }
    Integer existing = nameToAlias.get(name);
    if (existing != null) return existing;
    if (nextAlias > 0xffff) {
      throw new IOException("MQTT-SN 2.0 session alias space exhausted");
    }
    int value = nextAlias++;
    nameToAlias.put(name, value);
    aliasToName.put(value, name);
    return value;
  }

  public synchronized String resolve(int type, int alias, String topicName,
      IntFunction<String> predefinedLookup) throws IOException {
    Objects.requireNonNull(predefinedLookup, "predefinedLookup");
    String name = switch (type) {
      case 0 -> aliasToName.get(alias);
      case 1 -> predefinedLookup.apply(alias);
      case 3 -> topicName;
      default -> throw new IOException("Reserved MQTT-SN 2.0 topic type");
    };
    if (name == null || name.isEmpty()) {
      throw new IOException("Unknown MQTT-SN 2.0 topic alias");
    }
    return name;
  }

  public synchronized void clear() {
    nameToAlias.clear();
    aliasToName.clear();
    nextAlias = 1;
  }
}
