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

package org.maps.network.protocol.impl.mqtt;

import java.util.UUID;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.maps.test.BaseTestConfig;

public class MQTTPublishEventTest extends BaseTestConfig {


  @Test
  @DisplayName("Test QoS:0 publishing")
  public void testPublishQOS0hEvent() throws MqttException, InterruptedException {
    testPublish(0);
  }

  @Test
  @DisplayName("Test QoS:1 publishing")
  public void testPublishQOS1hEvent() throws MqttException, InterruptedException {
    testPublish(1);
  }

  @Test
  @DisplayName("Test QoS:2 publishing")
  public void testPublishQOS2hEvent() throws MqttException, InterruptedException {
    testPublish(2);
  }

  private void testPublish(int QoS) throws MqttException, InterruptedException{
    MqttClient client = new MqttClient("tcp://localhost:2001", UUID.randomUUID().toString(), new MemoryPersistence());
    MqttConnectOptions options = new MqttConnectOptions();
    options.setWill("/topic/will", "this is my will msg".getBytes(), QoS, true);
    options.setUserName("user1");
    options.setPassword("password1".toCharArray());
    client.connect(options);
    Assertions.assertTrue(client.isConnected());
    for(int x=0;x<10;x++) {
      MqttMessage message = new MqttMessage("this is a test msg,1,2,3,4,5,6,7,8,9,10".getBytes());
      message.setQos(QoS);
      message.setId(x);
      message.setRetained(false);
      client.publish("/topic/test", message);
    }
    MqttMessage finish =  new MqttMessage("End".getBytes());
    finish.setQos(1);
    client.publish("/topic/test", finish);
    client.disconnect();
    Assertions.assertFalse(client.isConnected());
    client.close();
  }
}
