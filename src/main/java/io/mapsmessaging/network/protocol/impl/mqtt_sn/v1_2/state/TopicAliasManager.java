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

package io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.state;

import io.mapsmessaging.network.protocol.impl.mqtt_sn.RegisteredTopicConfiguration;

import java.net.SocketAddress;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.MQTT_SNPacket.TOPIC_NAME;
import static io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.MQTT_SNPacket.TOPIC_PRE_DEFINED_ID;

public class TopicAliasManager {

  private final HashMap<String, Short> topicAlias;
  private final RegisteredTopicConfiguration registeredTopicConfiguration;
  private final AtomicInteger aliasGenerator;
  private final int maxSize;

  public TopicAliasManager(RegisteredTopicConfiguration registeredTopicConfiguration, int maxSize) {
    this.maxSize = maxSize;
    topicAlias = new LinkedHashMap<>();
    aliasGenerator = new AtomicInteger(1);
    this.registeredTopicConfiguration = registeredTopicConfiguration;
  }

  public void clear() {
    aliasGenerator.set(1);
    topicAlias.clear();
  }

  public short getTopicAlias(String name) {
    Short alias = topicAlias.get(name);
    if (alias == null && topicAlias.size() < maxSize) {
      alias = (short) aliasGenerator.incrementAndGet();
      topicAlias.put(name, alias);
    }
    if (alias == null) {
      return -1;
    }
    return alias;
  }

  public String getTopic(int alias) {
    for (Map.Entry<String, Short> entries : topicAlias.entrySet()) {
      if (entries.getValue() == alias) {
        return entries.getKey();
      }
    }
    return null;
  }

  public String getTopic(SocketAddress address, int alias, int topicType) {
    if (topicType == TOPIC_NAME) {
      for (Map.Entry<String, Short> entries : topicAlias.entrySet()) {
        if (entries.getValue() == alias) {
          return entries.getKey();
        }
      }
    } else {
      return registeredTopicConfiguration.getTopic(address, alias);
    }
    return null;
  }

  public short findTopicAlias(String name) {
    Short alias = topicAlias.get(name);
    if (alias == null) {
      return -1;
    }
    return alias;
  }

  public int getTopicAliasType(String destinationName) {
    if (topicAlias.containsKey(destinationName)) {
      return TOPIC_NAME;
    }
    return TOPIC_PRE_DEFINED_ID;
  }

  public int findRegisteredTopicAlias(SocketAddress key, String destinationName) {
    return registeredTopicConfiguration.getRegisteredTopicAliasType(key, destinationName);
  }
}
