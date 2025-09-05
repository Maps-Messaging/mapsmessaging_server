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

import io.mapsmessaging.security.uuid.UuidGenerator;
import java.io.IOException;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class MQTTConnectionTest extends MQTTBaseTest {

  @DisplayName("Test valid username/password MQTT client connection")
  @ParameterizedTest
  @MethodSource("mqttTestParameters")
  void testValidUser(int version, String protocol, boolean auth) throws MqttException, IOException {
    MqttClient client = new MqttClient(getUrl(protocol, auth), UuidGenerator.getInstance().generate().toString(), new MemoryPersistence());
    MqttConnectionOptions options = getOptions(auth);
    MqttMessage mqttMessage = new MqttMessage();
    mqttMessage.setPayload("this is my will msg".getBytes());
    mqttMessage.setQos(2);
    mqttMessage.setRetained(true);
    options.setWill("/topic/will", mqttMessage);
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
    MqttClient client = new MqttClient(getUrl(protocol, auth), UuidGenerator.getInstance().generate().toString(), new MemoryPersistence());
    MqttConnectionOptions options = getOptions(auth);
    options.setCleanStart(true);

    client.connect(options);
    Assertions.assertTrue(client.isConnected());
    client.disconnect();
    Assertions.assertFalse(client.isConnected());
    client.close();

    client = new MqttClient(getUrl(protocol, auth), UuidGenerator.getInstance().generate().toString(), new MemoryPersistence());
    options = getOptions();
    options.setCleanStart(false);

    client.connect(options);
    Assertions.assertTrue(client.isConnected());
    client.disconnect();
    Assertions.assertFalse(client.isConnected());
    client.close();
  }
}
