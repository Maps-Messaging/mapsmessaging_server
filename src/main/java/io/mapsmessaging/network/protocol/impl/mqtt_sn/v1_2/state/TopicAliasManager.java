/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.state;

import io.mapsmessaging.network.protocol.impl.mqtt_sn.RegisteredTopicConfiguration;
import io.mapsmessaging.storage.alias.TopicAliasRegistry;
import java.io.IOException;
import java.net.SocketAddress;

import static io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.MQTT_SNPacket.TOPIC_NAME;
import static io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.MQTT_SNPacket.TOPIC_PRE_DEFINED_ID;

/**
 * MQTT-SN 1.2 protocol adapter for a per-state-engine alias dictionary.
 * Keeps legacy allocation behaviour (first allocated ID is 2).
 */
public class TopicAliasManager {
  private TopicAliasRegistry aliases = new TopicAliasRegistry(65535);
  private final RegisteredTopicConfiguration registeredTopicConfiguration;
  private final int maxSize;
  private int nextAlias = 2;

  public TopicAliasManager(RegisteredTopicConfiguration registeredTopicConfiguration, int maxSize) {
    this.registeredTopicConfiguration = registeredTopicConfiguration;
    this.maxSize = maxSize;
  }

  public synchronized void attach(TopicAliasRegistry registry) {
    aliases = java.util.Objects.requireNonNull(registry);
  }

  public synchronized void clear() {
    try {
      aliases.clear();
      nextAlias = 2;
    } catch (IOException e) {
      throw new IllegalStateException("MQTT-SN 1.2 alias state unavailable", e);
    }
  }

  public synchronized short getTopicAlias(String name) {
    try {
      Integer alias = aliases.alias(name);
      if (alias != null) return (short) (int) alias;
      if (aliases.size() >= maxSize || nextAlias > 65535) return -1;
      while (nextAlias <= 65535 && aliases.topic(nextAlias) != null) nextAlias++;
      if (nextAlias > 65535) return -1;
      int id = nextAlias++;
      aliases.register(id, name);
      return (short) id;
    } catch (IOException e) {
      return -1;
    }
  }

  public synchronized String getTopic(int alias) {
    try {
      return aliases.topic(Short.toUnsignedInt((short) alias));
    } catch (IOException e) {
      throw new IllegalStateException("MQTT-SN 1.2 alias lookup failed", e);
    }
  }

  public synchronized String getTopic(SocketAddress address, int alias, int topicType) {
    return topicType == TOPIC_NAME ? getTopic(alias)
        : registeredTopicConfiguration.getTopic(address, alias);
  }

  public synchronized short findTopicAlias(String name) {
    try {
      Integer alias = aliases.alias(name);
      return alias == null ? -1 : (short) (int) alias;
    } catch (IOException e) {
      throw new IllegalStateException("MQTT-SN 1.2 alias lookup failed", e);
    }
  }

  public synchronized int getTopicAliasType(String destinationName) {
    return findTopicAlias(destinationName) != -1 ? TOPIC_NAME : TOPIC_PRE_DEFINED_ID;
  }

  public int findRegisteredTopicAlias(SocketAddress key, String destinationName) {
    return registeredTopicConfiguration.getRegisteredTopicAliasType(key, destinationName);
  }
}
