/*
 * Copyright [2020] [Matthew Buckton]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.maps.network.protocol.impl.mqtt;

import java.util.UUID;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class SSLMQTTConnectionTest extends MQTTPublishEventTest {

  @Test
  @DisplayName("Test valid user over SSL")
  public void testValidUser() throws MqttException {
    MqttClient client = new MqttClient("ssl://localhost:8444", UUID.randomUUID().toString(),  new MemoryPersistence());
    MqttConnectOptions options = new MqttConnectOptions();
    options.setUserName("user1");
    options.setPassword("password1".toCharArray());
    options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
    client.connect(options);
    Assertions.assertTrue(client.isConnected());
    client.disconnect();
    Assertions.assertFalse(client.isConnected());
    client.close();
  }

  @Test
  @DisplayName("Test valid user over SSL")
  public void testValidSSLUser() throws MqttException {
    MqttClient client = new MqttClient("ssl://localhost:8445", UUID.randomUUID().toString(), new MemoryPersistence());
    MqttConnectOptions options = new MqttConnectOptions();
    options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
    client.connect(options);
    Assertions.assertTrue(client.isConnected());
    client.disconnect();
    Assertions.assertFalse(client.isConnected());
    client.close();
  }
}