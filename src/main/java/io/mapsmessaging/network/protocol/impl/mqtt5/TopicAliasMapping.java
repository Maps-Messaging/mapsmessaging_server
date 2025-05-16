/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
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

import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class TopicAliasMapping {

  private final Logger logger;

  private final String aliasName;
  private final HashMap<String, Alias> keyByString;
  private final HashMap<Integer, Alias> keyByInteger;
  private final BitSet available;

  private int aliasMaximum;

  public TopicAliasMapping(String name) {
    logger = LoggerFactory.getLogger(TopicAliasMapping.class);
    this.aliasName = name;
    keyByInteger = new LinkedHashMap<>();
    keyByString = new LinkedHashMap<>();
    aliasMaximum = DefaultConstants.SERVER_RECEIVE_MAXIMUM;
    available = new BitSet(DefaultConstants.SERVER_RECEIVE_MAXIMUM + 1);
    available.set(0); // illegal value
  }

  public int size() {
    return keyByInteger.size();
  }

  public synchronized boolean add(String name, TopicAlias topicAlias) {
    if (keyByInteger.size() >= aliasMaximum) {
      logger.log(ServerLogMessages.MQTT5_TOPIC_ALIAS_EXCEEDED_MAXIMUM);
      return false;
    }
    if (topicAlias.getTopicAlias() == 0) {
      logger.log(ServerLogMessages.MQTT5_TOPIC_ALIAS_INVALID_VALUE, topicAlias.getTopicAlias());
      return false;
    }
    if (keyByString.containsKey(name)) {
      logger.log(ServerLogMessages.MQTT5_TOPIC_ALIAS_ALREADY_EXISTS, name);
      return false;
    }
    Alias alias = new Alias(name, topicAlias);
    available.set(topicAlias.getTopicAlias());
    keyByString.put(name, alias);
    keyByInteger.put(alias.getTopicAlias(), alias);
    logger.log(ServerLogMessages.MQTT5_TOPIC_ALIAS_ADD, name, alias.topicAlias);
    return true;
  }

  public synchronized String find(int aliasId) {
    Alias alias = keyByInteger.get(aliasId);
    if (alias != null) {
      return alias.getName();
    }
    return null;
  }

  public synchronized TopicAlias find(String aliasId) {
    Alias alias = keyByString.get(aliasId);
    if (alias != null) {
      return alias.getAliasProperty();
    }
    return null;
  }

  public synchronized void clearAll() {
    keyByInteger.clear();
    keyByString.clear();
    available.clear();
  }

  public int getMaximum() {
    return aliasMaximum;
  }

  public void setMaximum(int topicAliasMaximum) {
    if (topicAliasMaximum <= DefaultConstants.SERVER_TOPIC_ALIAS_MAX) {
      aliasMaximum = topicAliasMaximum;
      logger.log(ServerLogMessages.MQTT5_TOPIC_ALIAS_SET_MAXIMUM, aliasName, topicAliasMaximum);
    }
  }

  public TopicAlias create(String destinationName) {
    TopicAlias topicAlias = new TopicAlias(available.nextClearBit(0));
    if (!add(destinationName, topicAlias)) {
      return null;
    }
    return topicAlias;
  }

  private static class Alias {

    private final TopicAlias aliasProperty;
    private final int topicAlias;
    private final String name;

    public Alias(String name, TopicAlias aliasProperty) {
      this.aliasProperty = aliasProperty;
      this.topicAlias = aliasProperty.getTopicAlias();
      this.name = name;
    }

    public TopicAlias getAliasProperty() {
      return aliasProperty;
    }

    public int getTopicAlias() {
      return topicAlias;
    }

    public String getName() {
      return name;
    }
  }
}
