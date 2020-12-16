/*
 *  Copyright [2020] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.maps.network.protocol.impl.mqtt_sn;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Properties;
import java.util.StringTokenizer;

public class RegisteredTopicConfiguration {

  private final HashMap<Integer, TopicConfiguration> topicConfigById;


  public RegisteredTopicConfiguration(Properties properties) {
    String registeredTopics = properties.getProperty("registered", "");
    topicConfigById = new LinkedHashMap<>();
    parse(registeredTopics);
  }

  public String getTopic(SocketAddress from, int id) {
    TopicConfiguration tc = topicConfigById.get(id);
    if (tc != null) {
      if (tc.address.equals("*") || tc.address.equals("0.0.0.0")) {
        return tc.topic; // No checks
      } else {
        if (from instanceof InetSocketAddress) {
          InetSocketAddress inetAddress = (InetSocketAddress) from;
          if (inetAddress.getAddress().getHostAddress().equals(tc.address) || inetAddress.getHostName().equals(tc.address)) {
            return tc.topic;
          }
        }
      }
    }
    return null;
  }


  private void parse(String config) {
    StringTokenizer st = new StringTokenizer(config, ":");
    while (st.hasMoreElements()) {
      TopicConfiguration tc = new TopicConfiguration(st.nextElement().toString());
      topicConfigById.put(tc.id, tc);
    }
  }


  private static final class TopicConfiguration {

    private final String address;
    private final int id;
    private final String topic;

    TopicConfiguration(String config) {
      StringTokenizer st = new StringTokenizer(config, ",");
      address = st.nextElement().toString();
      id = Integer.parseInt(st.nextElement().toString());
      topic = st.nextElement().toString();
    }
  }
}
