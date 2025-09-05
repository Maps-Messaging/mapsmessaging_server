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

import io.mapsmessaging.security.uuid.UuidGenerator;
import java.io.IOException;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class MQTTConnectionTest extends MQTTBaseTest {


  void justRun(int version, String protocol, boolean auth) throws MqttException, IOException {
    MqttConnectOptions options = getOptions(auth, version);
    MqttClient client = new MqttClient(getUrl(protocol, auth), getClientId(UuidGenerator.getInstance().generate().toString(), version), new MemoryPersistence());
    client.connect(options);
    byte[] payload = new byte[1024];
    for(int x=0;x<payload.length;x++){
      payload[x] = (byte)(x%128);
    }
    while(true){
      MqttMessage mqttMessage = new MqttMessage();
      mqttMessage.setQos(1);
      mqttMessage.setPayload(payload);
      client.publish("/test", mqttMessage);
    }
  }

  @DisplayName("Test valid username/password MQTT client connection")
  @ParameterizedTest
  @MethodSource("mqttTestParameters")
  void testValidUser(int version, String protocol, boolean auth) throws MqttException, IOException {
    MqttConnectOptions options = getOptions(auth, version);
    MqttClient client = new MqttClient(getUrl(protocol, auth), getClientId(UuidGenerator.getInstance().generate().toString(), version), new MemoryPersistence());

    options.setWill("/topic/will", "this is my will msg".getBytes(), 2, true);
    client.connect(options);
    Assertions.assertTrue(client.isConnected());
    client.disconnect();
    Assertions.assertFalse(client.isConnected());
    client.close();
  }

  @DisplayName("Test valid user/password MQTT client connection with a reset session set")
  @ParameterizedTest
  @MethodSource("mqttTestParameters")
  void testValidUserResetState(int version, String protocol, boolean auth) throws MqttException, IOException {
    MqttConnectOptions options = getOptions(auth, version);
    MqttClient client = new MqttClient(getUrl(protocol, auth), getClientId(UuidGenerator.getInstance().generate().toString(), version), new MemoryPersistence());
    options.setCleanSession(true);

    client.connect(options);
    Assertions.assertTrue(client.isConnected());
    client.disconnect();
    Assertions.assertFalse(client.isConnected());
    client.close();

    options = getOptions(auth, version);
    client = new MqttClient(getUrl(protocol, auth), getClientId(UuidGenerator.getInstance().generate().toString(), version), new MemoryPersistence());
    options.setCleanSession(false);

    client.connect(options);
    Assertions.assertTrue(client.isConnected());
    client.disconnect();
    Assertions.assertFalse(client.isConnected());
    client.close();

  }

  @DisplayName("Test invalid MQTT client connection")
  @ParameterizedTest
  @MethodSource("mqttTestParameters")
  void testInvalidUser(int version, String protocol, boolean auth) throws MqttException, IOException {
    MqttConnectOptions options = getOptions(auth, version);
    MqttClient client = new MqttClient(getUrl(protocol, auth), getClientId(UuidGenerator.getInstance().generate().toString(), version), new MemoryPersistence());
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
    options.setConnectionTimeout(5);
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
