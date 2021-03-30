/*
 *
 *   Copyright [ 2020 - 2021 ] [Matthew Buckton]
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

package io.mapsmessaging.network.protocol.impl.mqtt;

import java.util.UUID;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

abstract class  MQTTConnectionTest extends MQTTBaseTest {

  abstract int getVersion();

  @Test
  @DisplayName("Test anonymous MQTT client connection")
  void testAnonymous() throws MqttException {
    MqttConnectOptions options = new MqttConnectOptions();
    options.setMqttVersion(getVersion());
    MqttClient client = new MqttClient("tcp://localhost:8675", getClientId(UUID.randomUUID().toString(), getVersion()), new MemoryPersistence());
    client.connect(options);
    Assertions.assertTrue(client.isConnected());
    client.disconnect();
    Assertions.assertFalse(client.isConnected());
    client.close();
  }

  @Test
  @DisplayName("Test valid username/password MQTT client connection")
  void testValidUser() throws MqttException {
    MqttClient client = new MqttClient("tcp://localhost:2001", getClientId(UUID.randomUUID().toString(), getVersion()), new MemoryPersistence());
    MqttConnectOptions options = new MqttConnectOptions();
    options.setWill("/topic/will", "this is my will msg".getBytes(), 2, true);
    options.setUserName("user1");
    options.setPassword("password1".toCharArray());
    options.setMqttVersion(getVersion());
    client.connect(options);
    Assertions.assertTrue(client.isConnected());
    client.disconnect();
    Assertions.assertFalse(client.isConnected());
    client.close();
  }

  @Test
  @DisplayName("Test valid user/password MQTT client connection with a reset session set")
  void testValidUserResetState() throws MqttException {
    MqttClient client = new MqttClient("tcp://localhost:2001", getClientId(UUID.randomUUID().toString(), getVersion()), new MemoryPersistence());
    MqttConnectOptions options = new MqttConnectOptions();
    options.setCleanSession(true);
    options.setUserName("user1");
    options.setPassword("password1".toCharArray());
    options.setMqttVersion(getVersion());

    client.connect(options);
    Assertions.assertTrue(client.isConnected());
    client.disconnect();
    Assertions.assertFalse(client.isConnected());
    client.close();

    client = new MqttClient("tcp://localhost:2001", getClientId(UUID.randomUUID().toString(), getVersion()), new MemoryPersistence());
    options = new MqttConnectOptions();
    options.setCleanSession(false);
    options.setUserName("user1");
    options.setPassword("password1".toCharArray());
    options.setMqttVersion(getVersion());

    client.connect(options);
    Assertions.assertTrue(client.isConnected());
    client.disconnect();
    Assertions.assertFalse(client.isConnected());
    client.close();

  }

  @Test
  @DisplayName("Test invalid MQTT client connection")
  void testInvalidUser() throws MqttException {
    MqttClient client = new MqttClient("tcp://localhost:2001", getClientId(UUID.randomUUID().toString(), getVersion()), new MemoryPersistence());
    MqttCallback callback = new MqttCallback(){

      @Override
      public void connectionLost(Throwable throwable) {
        throwable.printStackTrace();
      }

      @Override
      public void messageArrived(String s, MqttMessage mqttMessage) {
      }

      @Override
      public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
      }
    };

    client.setCallback(callback);
    client.setTimeToWait(2000);
    MqttConnectOptions options = new MqttConnectOptions();
    options.setUserName("user1");
    options.setPassword("password2".toCharArray());
    options.setConnectionTimeout(5);
    options.setMqttVersion(getVersion());
    try {
      client.connect(options);
    } catch (MqttException e) {
      // This is correct
      return;
    }
    Assertions.assertTrue(client.isConnected());
    client.disconnect();
    Assertions.assertFalse(client.isConnected());
    client.close();
  }
}
