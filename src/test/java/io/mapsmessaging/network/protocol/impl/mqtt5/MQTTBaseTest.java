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

import io.mapsmessaging.test.BaseTestConfig;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.junit.jupiter.params.provider.Arguments;

public class MQTTBaseTest extends BaseTestConfig {

  public static final int MQTT_5_0 = 5;

  int[] tcp_ports = {1883, 1882};
  int[] ssl_ports = {1892, 1893};


  public MqttConnectionOptions getOptions() throws IOException {
    MqttConnectionOptions options = new MqttConnectionOptions();
    options.setUserName("admin");
    options.setPassword(getPassword("admin").getBytes());
    return options;
  }

  static Stream<Arguments> mqttGetAuthUrls() {
    List<Arguments> argumentsList = new ArrayList<>();
    String[] connectionTypes = {"tcp", "ssl", "ws", "wss"};

    for (String connectionType : connectionTypes) {
      argumentsList.add(Arguments.of(MQTT_5_0, connectionType));
    }
    return argumentsList.stream();
  }

  static Stream<Arguments> mqttTestParameters() {
    List<Arguments> argumentsList = new ArrayList<>();
    String[] connectionTypes = {"tcp", "ssl", "ws", "wss"};
    boolean[] authOptions = {false, true};
    for (String connectionType : connectionTypes) {
      for (boolean auth : authOptions) {
        argumentsList.add(Arguments.of(MQTT_5_0, connectionType, auth));
      }
    }
    return argumentsList.stream();
  }


  static Stream<Arguments> mqttPublishTestParameters() {
    List<Arguments> argumentsList = new ArrayList<>();
    String[] connectionTypes = {"tcp", "ssl", "ws", "wss"};
    boolean[] authOptions = {false, true};
    int[] qosLevels = {0, 1, 2};

    for (String connectionType : connectionTypes) {
      for (boolean auth : authOptions) {
        for (int qos : qosLevels) {
          argumentsList.add(Arguments.of(MQTT_5_0, connectionType, auth, qos));
        }
      }
    }
    return argumentsList.stream();
  }

  public SaslClient setForSasl(MqttConnectionOptions options, String username, String password) throws IOException {
    String mechanism = "CRAM-MD5";
    Map<String, String> props = new HashMap<>();
    props.put(Sasl.QOP, "auth");
    String[] mechanisms = {mechanism};
    ClientCallbackHandler clientHandler = new ClientCallbackHandler(username, password, "servername");
    SaslClient saslClient = Sasl.createSaslClient(mechanisms, "authorizationId", "MQTT", "serverName", props, clientHandler);
    if (options != null) {
      options.setAuthMethod(mechanism);
      options.setAuthData(new byte[0]);
    }
    return saslClient;
  }

  public String getUrl(String protocol, boolean auth) {
    boolean isSsl = protocol.equalsIgnoreCase("ssl") || protocol.equalsIgnoreCase("wss");
    int[] ports = isSsl ? ssl_ports : tcp_ports;
    int portIndex = auth ? 1 : 0;
    return protocol + "://localhost:" + ports[portIndex];
  }

  public int getPort(String protocol, boolean auth) {
    boolean isSsl = protocol.equalsIgnoreCase("ssl") || protocol.equalsIgnoreCase("wss");
    int portIndex = auth ? 1 : 0;
    int[] ports = isSsl ? ssl_ports : tcp_ports;
    return  ports[portIndex];

  }

  public MqttConnectionOptions getOptions(boolean auth) throws IOException {
    MqttConnectionOptions options = new MqttConnectionOptions();
    if (auth) {
      options.setUserName("admin");
      options.setPassword(getPassword("admin").getBytes());
    }
    options.setReceiveMaximum(100);
    options.setTopicAliasMaximum(8192);
    return options;
  }


}
