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

package io.mapsmessaging.network.protocol.impl.mqtt_sn;

import io.mapsmessaging.config.protocol.PredefinedTopics;
import io.mapsmessaging.config.protocol.impl.MqttSnConfig;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;

public class RegisteredTopicConfiguration {

  private final HashMap<Integer, List<TopicConfiguration>> topicConfigById;
  private final HashMap<String, List<TopicConfiguration>> topicConfigByName;


  public RegisteredTopicConfiguration(MqttSnConfig properties) {
    topicConfigById = new LinkedHashMap<>();
    topicConfigByName = new LinkedHashMap<>();
    parse(properties.getRegisteredTopics());
    for(PredefinedTopics predefined: properties.getPredefinedTopicsList()){

      List<TopicConfiguration> list = topicConfigById.computeIfAbsent(predefined.getId(), k -> new ArrayList<>());
      list.add(new TopicConfiguration(predefined.getAddress(), predefined.getId(), predefined.getTopic()));

      List<TopicConfiguration> list1 = topicConfigByName.computeIfAbsent(predefined.getTopic(), k -> new ArrayList<>());
      list1.add(new TopicConfiguration(predefined.getAddress(), predefined.getId(), predefined.getTopic()));
    }
  }

  public String getTopic(SocketAddress from, int id) {
    String topic = null;
    List<TopicConfiguration> list = topicConfigById.get(id);
    if (list != null) {
      // Search for explicit address mapping
      topic = searchForExplicitMapping(from, list);

      // OK we have no address match, lets now check for a "*"
      if(topic == null){
        topic = searchForWildcard(list);
      }
    }
    return topic;
  }

  private String searchForWildcard(List<TopicConfiguration> list){
    for (TopicConfiguration tc : list) {
      if (tc.address.equals("*") || tc.address.equals("0.0.0.0")) {
        return tc.topic; // No checks
      }
    }
    return null;
  }

  private String searchForExplicitMapping(SocketAddress from, List<TopicConfiguration> list){
    for (TopicConfiguration tc : list) {
      if (from instanceof InetSocketAddress) {
        InetSocketAddress inetAddress = (InetSocketAddress) from;
        String address = inetAddress.getAddress().getHostAddress();
        String hostName = address.startsWith("169.254") ? address : inetAddress.getAddress().getHostName();
        if (address.equals(tc.address) || hostName.equals(tc.address)) {
          return tc.topic;
        }
      }
    }
    return null;
  }

  private void parse(String config) {
    StringTokenizer st = new StringTokenizer(config, ":");
    while (st.hasMoreElements()) {
      TopicConfiguration tc = new TopicConfiguration(st.nextElement().toString());
      List<TopicConfiguration> list = topicConfigById.computeIfAbsent(tc.id, k -> new ArrayList<>());
      list.add(tc);
    }
  }

  public int getRegisteredTopicAliasType(SocketAddress from, String destinationName) {
    int id = -1;
    List<TopicConfiguration> list = topicConfigByName.get(destinationName);
    if (list != null) {
      id = searchForTopicId(from, list);
      if(id == -1){
        id = searchForWildcardTopicId(list);
      }
    }
    return id;
  }

  private int searchForTopicId(SocketAddress from, List<TopicConfiguration> list) {
    for (TopicConfiguration tc : list) {
      if (from instanceof InetSocketAddress) {
        InetSocketAddress inetAddress = (InetSocketAddress) from;
        if (inetAddress.getAddress().getHostAddress().equals(tc.address) || inetAddress.getHostName().equals(tc.address)) {
          return tc.id;
        }
      }
    }
    return -1;
  }

  private int searchForWildcardTopicId(List<TopicConfiguration> list) {
    // OK we have no address match, lets now check for a "*"
    for (TopicConfiguration tc : list) {
      if (tc.address.equals("*") || tc.address.equals("0.0.0.0")) {
        return tc.id; // No checks
      }
    }

    return -1;
  }

  private static final class TopicConfiguration {

    private final String address;
    private final int id;
    private final String topic;

    TopicConfiguration(String address, int id, String topic) {
      this.address = address;
      this.id = id;
      this.topic = topic;
    }

    TopicConfiguration(String config) {
      StringTokenizer st = new StringTokenizer(config, ",");
      address = st.nextElement().toString();
      id = Integer.parseInt(st.nextElement().toString());
      topic = st.nextElement().toString();
    }
  }
}
