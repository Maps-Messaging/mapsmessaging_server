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

package io.mapsmessaging.network.protocol.impl.mqtt5;

import java.util.UUID;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

abstract class MQTTPublishEventTest extends MQTTBaseTest {

  abstract int getVersion();

  @Test
  @DisplayName("Test QoS:0 publishing")
  void testPublishQOS0hEvent() throws MqttException, InterruptedException {
    testPublish(0);
  }

  @Test
  @DisplayName("Test QoS:1 publishing")
  void testPublishQOS1hEvent() throws MqttException, InterruptedException {
    testPublish(1);
  }

  @Test
  @DisplayName("Test QoS:2 publishing")
  void testPublishQOS2hEvent() throws MqttException, InterruptedException {
    testPublish(2);
  }

  private void testPublish(int QoS) throws MqttException, InterruptedException {
    MqttClient client = new MqttClient("tcp://localhost:2001", getClientId(UUID.randomUUID().toString(), getVersion()), new MemoryPersistence());
    MqttConnectionOptions options = new MqttConnectionOptions();
    MqttMessage mqttMessage = new MqttMessage();
    mqttMessage.setPayload("this is my will msg".getBytes());
    mqttMessage.setQos(QoS);
    mqttMessage.setRetained(true);

    options.setWill("/topic/will", mqttMessage);
    options.setUserName("user1");
    options.setPassword("password1".getBytes());
    client.connect(options);
    Assertions.assertTrue(client.isConnected());
    for (int x = 0; x < 10; x++) {
      MqttMessage message = new MqttMessage("this is a test msg,1,2,3,4,5,6,7,8,9,10".getBytes());
      message.setQos(QoS);
      message.setId(x);
      message.setRetained(false);
      client.publish("/topic/test", message);
    }
    MqttMessage finish = new MqttMessage("End".getBytes());
    finish.setQos(1);
    client.publish("/topic/test", finish);
    client.disconnect();
    Assertions.assertFalse(client.isConnected());
    client.close();
  }
}
