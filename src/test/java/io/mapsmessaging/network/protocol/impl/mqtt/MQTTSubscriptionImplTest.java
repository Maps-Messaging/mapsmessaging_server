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

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import io.mapsmessaging.test.BaseTestConfig;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

class MQTTSubscriptionImplTest extends BaseTestConfig implements MqttCallback {

  private final AtomicInteger eventCounter = new AtomicInteger(0);

  @Test
  @DisplayName("Test QoS:0 subscription")
  void testSubscribeQOS0hEvent() throws MqttException, InterruptedException, IOException {
    testSubscription(0);
  }

  @Test
  @DisplayName("Test QoS:1 subscription")
  void testSubscribeQOS1hEvent() throws MqttException, InterruptedException, IOException {
    testSubscription(1);
  }

  @Test
  @DisplayName("Test QoS:2 subscription")
  void testSubscribeQOS2hEvent() throws MqttException, InterruptedException, IOException {
    testSubscription(2);
  }

  private void testSubscription(int QoS) throws MqttException, InterruptedException, IOException {
    MqttClient client = new MqttClient("tcp://localhost:2001", UUID.randomUUID().toString(), new MemoryPersistence());
    MqttConnectOptions options = new MqttConnectOptions();
    options.setWill("/topic/will", "this is my will msg".getBytes(), QoS, true);
    options.setUserName("user1");
    options.setPassword("password1".toCharArray());
    client.connect(options);
    client.setCallback(this);
    String topicName = getTopicName();
    client.subscribe(topicName);
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
    long timeout = System.currentTimeMillis() + 10000;
    while(eventCounter.get() < 11 && timeout > System.currentTimeMillis()){
      delay(10);
    }
    Assertions.assertFalse(timeout < System.currentTimeMillis());
    client.unsubscribe(topicName);
    client.disconnect();
    Assertions.assertEquals(11, eventCounter.get());
    Assertions.assertFalse(client.isConnected());
    client.close();
  }

  @Override
  public void connectionLost(Throwable throwable) {

  }

  @Override
  public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
    eventCounter.incrementAndGet();
  }

  @Override
  public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

  }
}
