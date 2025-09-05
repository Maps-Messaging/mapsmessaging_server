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

package io.mapsmessaging.network.protocol.impl.mqtt;

import io.mapsmessaging.test.BaseTestConfig;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.junit.jupiter.params.provider.Arguments;

public class MQTTBaseTest extends BaseTestConfig {
  public static final String RESTRICTED_CHARACTERS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
  public static final int MQTT_3_1 = 3;
  public static final int MQTT_3_1_1 = 4;

  int[] tcp_ports = {1883, 1882};
  int[] ssl_ports = {1892, 1893};

  static Stream<Arguments> mqttTestParameters() {

    List<Arguments> argumentsList = new ArrayList<>();
    int[] protocols = {MQTT_3_1, MQTT_3_1_1};
    String[] connectionTypes = {"tcp", "ssl", "ws", "wss"};
    boolean[] authOptions = {false, true};
    for (int protocol : protocols) {
      for (String connectionType : connectionTypes) {
        for (boolean auth : authOptions) {
          argumentsList.add(Arguments.of(protocol, connectionType, auth));
        }
      }
    }
    return argumentsList.stream();
  }

  static Stream<Arguments> mqttPublishTestParameters() {
    List<Arguments> argumentsList = new ArrayList<>();
    int[] protocols = {MQTT_3_1, MQTT_3_1_1};
    String[] connectionTypes = {"tcp", "ssl", "ws", "wss"};
    boolean[] authOptions = {false, true};
    int[] qosLevels = {0, 1, 2};

    for (int protocol : protocols) {
      for (String connectionType : connectionTypes) {
        for (boolean auth : authOptions) {
          for (int qos : qosLevels) {
            argumentsList.add(Arguments.of(protocol, connectionType, auth, qos));
          }
        }
      }
    }
    return argumentsList.stream();
  }


  public MqttConnectOptions getOptions() throws IOException {
    return getOptions(true, MQTT_3_1);
  }

  public String getUrl(String protocol, boolean auth) {
    boolean isSsl = protocol.equalsIgnoreCase("ssl") || protocol.equalsIgnoreCase("wss");
    int[] ports = isSsl ? ssl_ports : tcp_ports;
    int portIndex = auth ? 1 : 0;
    return protocol + "://localhost:" + ports[portIndex];
  }

  public MqttConnectOptions getOptions(boolean auth, int version) throws IOException {
    MqttConnectOptions options = new MqttConnectOptions();
    options.setMqttVersion(version);
    if (auth) {
      options.setUserName("admin");
      options.setPassword(getPassword("admin").toCharArray());
    }
    options.setHttpsHostnameVerificationEnabled(false);
    options.setSSLHostnameVerifier((hostname, session) -> true);
    options.setExecutorServiceTimeout(20000);
    options.setConnectionTimeout(20000);
    return options;
  }


  String getClientId(String proposed, int version){
    if(version == MQTT_3_1){
      StringBuilder sb = new StringBuilder();
      for(int x=0;x<proposed.length();x++){
        char test = proposed.charAt(x);
        if(RESTRICTED_CHARACTERS.indexOf(test) != -1){
          sb.append(test);
          if(sb.length() == 23){
            break;
          }
        }
      }
      return sb.toString();
    }
    else{
      return proposed;
    }
  }
}
