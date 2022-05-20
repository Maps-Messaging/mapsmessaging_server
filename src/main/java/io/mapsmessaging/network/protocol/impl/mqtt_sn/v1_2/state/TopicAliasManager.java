package io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.state;

import static io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.MQTT_SNPacket.TOPIC_NAME;
import static io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.MQTT_SNPacket.TOPIC_PRE_DEFINED_ID;

import io.mapsmessaging.network.protocol.impl.mqtt_sn.DefaultConstants;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.RegisteredTopicConfiguration;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class TopicAliasManager {
  private final HashMap<String, Short> topicAlias;
  private final RegisteredTopicConfiguration registeredTopicConfiguration;
  private final AtomicInteger aliasGenerator;

  public TopicAliasManager(RegisteredTopicConfiguration registeredTopicConfiguration){
    topicAlias = new LinkedHashMap<>();
    aliasGenerator = new AtomicInteger(1);
    this.registeredTopicConfiguration = registeredTopicConfiguration;
  }

  public void clear(){
    topicAlias.clear();
  }

  public short getTopicAlias(String name) {
    Short alias = topicAlias.get(name);
    if (alias == null && topicAlias.size() < DefaultConstants.MAX_REGISTERED_SIZE) {
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
    if(topicType == TOPIC_NAME) {
      for (Map.Entry<String, Short> entries : topicAlias.entrySet()) {
        if (entries.getValue() == alias) {
          return entries.getKey();
        }
      }
    }
    else{
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
    if(topicAlias.containsKey(destinationName)){
      return TOPIC_NAME;
    }
    return TOPIC_PRE_DEFINED_ID;
  }

  public int findRegisteredTopicAlias(SocketAddress key, String destinationName) {
    return registeredTopicConfiguration.getRegisteredTopicAliasType(key, destinationName);
  }
}
