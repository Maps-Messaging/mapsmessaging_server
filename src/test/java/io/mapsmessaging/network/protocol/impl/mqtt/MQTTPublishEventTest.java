/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
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

package io.mapsmessaging.network.protocol.impl.mqtt;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.UUID;

class MQTTPublishEventTest extends MQTTBaseTest {


  @ParameterizedTest
  @MethodSource("mqttPublishTestParameters")
  @DisplayName("Test QoS publishing")
  void testPublish(int version, String protocol, boolean auth, int QoS) throws MqttException, IOException {
    MqttConnectOptions options = getOptions(auth, version);
    MqttClient client = new MqttClient(getUrl(protocol, auth), getClientId(UUID.randomUUID().toString(), version), new MemoryPersistence());
    client.connect(options);
    String topicName = getTopicName();
    Assertions.assertTrue(client.isConnected());
    for(int x=0;x<10;x++) {
      MqttMessage message = new MqttMessage("this is a test msg,1,2,3,4,5,6,7,8,9,10".getBytes());
      message.setQos(QoS);
      message.setId(x);
      message.setRetained(false);
      client.publish(topicName, message);
    }
    MqttMessage finish =  new MqttMessage("End".getBytes());
    finish.setQos(1);
    client.publish(topicName, finish);
    client.disconnect();
    Assertions.assertFalse(client.isConnected());
    client.close();
  }
}
