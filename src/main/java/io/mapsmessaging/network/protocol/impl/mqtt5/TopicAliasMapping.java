/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.network.protocol.impl.mqtt5;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.TopicAlias;

import io.mapsmessaging.storage.alias.TopicAliasRegistry;
import java.io.IOException;

public class TopicAliasMapping {

  private final Logger logger;

  private final String aliasName;
  private TopicAliasRegistry registry;

  private int aliasMaximum;

  public TopicAliasMapping(String name) {
    logger = LoggerFactory.getLogger(TopicAliasMapping.class);
    this.aliasName = name;
    aliasMaximum = DefaultConstants.SERVER_RECEIVE_MAXIMUM;
    registry = new TopicAliasRegistry(Math.max(1, Math.min(65535, aliasMaximum)));
  }

  public synchronized int size() {
    try { return registry.size(); }
    catch (IOException e) { throw new IllegalStateException("MQTT 5 alias registry unavailable", e); }
  }

  public synchronized boolean add(String name, TopicAlias topicAlias) {
    if (topicAlias == null || topicAlias.getTopicAlias() < 1
        || topicAlias.getTopicAlias() > aliasMaximum) {
      logger.log(ServerLogMessages.MQTT5_TOPIC_ALIAS_INVALID_VALUE,
          topicAlias == null ? 0 : topicAlias.getTopicAlias());
      return false;
    }
    try {
      // MQTT 5 explicitly permits reassignment of an alias within a connection.
      registry.rebind(topicAlias.getTopicAlias(), name);
      return true;
    } catch (IOException invalid) {
      logger.log(ServerLogMessages.MQTT5_TOPIC_ALIAS_EXCEEDED_MAXIMUM);
      return false;
    }
  }

  public synchronized String find(int aliasId) {
    try { return registry.topic(aliasId); }
    catch (IOException e) { throw new IllegalStateException("MQTT 5 alias registry unavailable", e); }
  }

  public synchronized TopicAlias find(String name) {
    try {
      Integer id = registry.alias(name);
      return id == null ? null : new TopicAlias(id);
    } catch (IOException e) { throw new IllegalStateException("MQTT 5 alias registry unavailable", e); }
  }

  public synchronized void clearAll() {
    try { registry.clear(); }
    catch (IOException e) { throw new IllegalStateException("MQTT 5 alias registry unavailable", e); }
  }

  public int getMaximum() { return aliasMaximum; }

  public synchronized void setMaximum(int requested) {
    if (requested >= 0 && requested <= DefaultConstants.SERVER_TOPIC_ALIAS_MAX) {
      aliasMaximum = requested;
      // Never preserve mappings when the negotiated connection limit changes.
      registry = new TopicAliasRegistry(Math.max(1, Math.min(65535, requested)));
      logger.log(ServerLogMessages.MQTT5_TOPIC_ALIAS_SET_MAXIMUM, aliasName, requested);
    }
  }

  public synchronized TopicAlias create(String destinationName) {
    if (aliasMaximum == 0) return null;
    try {
      Integer existing = registry.alias(destinationName);
      if (existing != null) return new TopicAlias(existing);
      return new TopicAlias(registry.register(destinationName));
    } catch (IOException full) {
      return null;
    }
  }
}
