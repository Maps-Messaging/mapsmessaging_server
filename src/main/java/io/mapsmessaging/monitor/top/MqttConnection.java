/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.monitor.top;

import io.mapsmessaging.security.uuid.UuidGenerator;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MqttConnection {

  private final MqttClient client;

  public MqttConnection(String url, String username, String password) throws MqttException {
    MqttConnectOptions options = getOptions();
    if(username != null){
      options.setUserName(username);
      options.setPassword(password.toCharArray());
    }
    client = new MqttClient(url, UuidGenerator.getInstance().generate().toString(), new MemoryPersistence());
    client.connect(options);
  }


  public void subscribe(String topicName, IMqttMessageListener listener) throws MqttException {
    client.subscribe(topicName, 0, listener);
  }


  private MqttConnectOptions getOptions() {
    MqttConnectOptions options = new MqttConnectOptions();
    options.setMqttVersion(4); // 3.1.1
    options.setCleanSession(true);
    options.setMaxInflight(10);
    options.setAutomaticReconnect(true);
    options.setHttpsHostnameVerificationEnabled(false);
    options.setSSLHostnameVerifier((hostname, session) -> true);
    options.setExecutorServiceTimeout(20000);
    options.setConnectionTimeout(20000);
    return options;
  }

}
