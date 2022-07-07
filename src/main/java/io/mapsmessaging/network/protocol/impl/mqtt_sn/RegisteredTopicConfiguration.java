/*
 *
 *   Copyright [ 2020 - 2022 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package io.mapsmessaging.network.protocol.impl.mqtt_sn;

import io.mapsmessaging.utilities.configuration.ConfigurationProperties;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.StringTokenizer;

public class RegisteredTopicConfiguration {

  private final HashMap<Integer, List<TopicConfiguration>> topicConfigById;
  private final HashMap<String, List<TopicConfiguration>> topicConfigByName;


  public RegisteredTopicConfiguration(ConfigurationProperties properties) {
    String registeredTopics = properties.getProperty("registered", "");
    topicConfigById = new LinkedHashMap<>();
    topicConfigByName = new LinkedHashMap<>();
    parse(registeredTopics);
    Object predefined = properties.get("preDefinedTopics");
    if (predefined instanceof List) {
      List<ConfigurationProperties> predefinedList = (List<ConfigurationProperties>) predefined;
      for (ConfigurationProperties props : predefinedList) {
        int id = props.getIntProperty("id", 0);
        String topic = props.getProperty("topic", "");
        String address = props.getProperty("address", "*");
        List<TopicConfiguration> list = topicConfigById.computeIfAbsent(id, k -> new ArrayList<>());
        list.add(new TopicConfiguration(address, id, topic));

        List<TopicConfiguration> list1 = topicConfigByName.computeIfAbsent(topic, k -> new ArrayList<>());
        list1.add(new TopicConfiguration(address, id, topic));

      }
    }
  }

  public String getTopic(SocketAddress from, int id) {
    List<TopicConfiguration> list = topicConfigById.get(id);
    if (list != null) {
      // Search for explicit address mapping
      for (TopicConfiguration tc : list) {
        if (from instanceof InetSocketAddress) {
          InetSocketAddress inetAddress = (InetSocketAddress) from;
          if (inetAddress.getAddress().getHostAddress().equals(tc.address) || inetAddress.getHostName().equals(tc.address)) {
            return tc.topic;
          }
        }
      }

      // OK we have no address match, lets now check for a "*"
      for (TopicConfiguration tc : list) {
        if (tc.address.equals("*") || tc.address.equals("0.0.0.0")) {
          return tc.topic; // No checks
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
    List<TopicConfiguration> list = topicConfigByName.get(destinationName);

    if (list != null) {
      for (TopicConfiguration tc : list) {
        if (from instanceof InetSocketAddress) {
          InetSocketAddress inetAddress = (InetSocketAddress) from;
          if (inetAddress.getAddress().getHostAddress().equals(tc.address) || inetAddress.getHostName().equals(tc.address)) {
            return tc.id;
          }
        }
      }

      // OK we have no address match, lets now check for a "*"
      for (TopicConfiguration tc : list) {
        if (tc.address.equals("*") || tc.address.equals("0.0.0.0")) {
          return tc.id; // No checks
        }
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
