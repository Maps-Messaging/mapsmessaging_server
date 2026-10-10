/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0;

import java.io.IOException;
import io.mapsmessaging.storage.alias.TopicAliasRegistry;
import java.util.Objects;
import java.util.function.IntFunction;

/**
 * MQTT-SN 2.0 topic aliases; storage owned by this protocol instance until attached to session persistence.
 * CSD01: 0=session alias, 1=predefined alias, 2=reserved, 3=topic name.
 * Caller supplies configured predefined topic resolution separately.
 */
public final class MqttSn2TopicAliases {
  private TopicAliasRegistry registry;

  public MqttSn2TopicAliases() {
    this(new TopicAliasRegistry(65535));
  }

  public MqttSn2TopicAliases(TopicAliasRegistry registry) {
    this.registry = Objects.requireNonNull(registry);
  }

  public synchronized void attach(TopicAliasRegistry persistentSessionRegistry) {
    registry = Objects.requireNonNull(persistentSessionRegistry);
  }

  public synchronized int register(String name) throws IOException {
    return registry.register(name);
  }

  public synchronized String resolve(int type, int alias, String topicName,
      IntFunction<String> predefinedLookup) throws IOException {
    Objects.requireNonNull(predefinedLookup, "predefinedLookup");
    String name = switch (type) {
      case 0 -> registry.topic(alias);
      case 1 -> predefinedLookup.apply(alias);
      case 3 -> topicName;
      default -> throw new IOException("Reserved MQTT-SN 2.0 topic type");
    };
    if (name == null || name.isEmpty()) {
      throw new IOException("Unknown MQTT-SN 2.0 topic alias");
    }
    return name;
  }

  public synchronized void clear() throws IOException {
    registry.clear();
  }
}
