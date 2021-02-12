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
import java.util.concurrent.atomic.AtomicInteger;
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
import org.maps.test.BaseTestConfig;

public class MQTTWildCardSubscriptionImplTest extends BaseTestConfig implements MqttCallback {

  private AtomicInteger eventCounter = new AtomicInteger(0);

  @Test
  @DisplayName("Test QoS:0 wildcard subscription")
  public void testWildcardSubscribeQOS0hEvent() throws MqttException, InterruptedException {
    testWildcardSubscription(0);
  }

  @Test
  @DisplayName("Test QoS:1  wildcard subscription")
  public void testWildcardSubscribeQOS1hEvent() throws MqttException, InterruptedException {
    testWildcardSubscription(1);
  }

  @Test
  @DisplayName("Test QoS:2  wildcard subscription")
  public void testWildcardSubscribeQOS2hEvent() throws MqttException, InterruptedException {
    testWildcardSubscription(2);
  }

  private void testWildcardSubscription(int QoS) throws MqttException, InterruptedException{
    MqttClient client = new MqttClient("tcp://localhost:2001", UUID.randomUUID().toString(),  new MemoryPersistence());
    MqttConnectOptions options = new MqttConnectOptions();
    options.setWill("/topic/will", "this is my will msg".getBytes(), QoS, true);
    options.setUserName("user1");
    options.setPassword("password1".toCharArray());
    client.connect(options);
    client.setCallback(this);
    String topicName = getTopicName();
    client.subscribe("/wildcard/topic/#");
    Assertions.assertTrue(client.isConnected());
    for(int x=0;x<10;x++) {
      MqttMessage message = new MqttMessage("this is a test msg,1,2,3,4,5,6,7,8,9,10".getBytes());
      message.setQos(QoS);
      message.setId(x);
      message.setRetained(false);
      client.publish("/wildcard"+getTopicName(), message);
    }
    MqttMessage finish =  new MqttMessage("End".getBytes());
    finish.setQos(1);
    client.publish("/wildcard"+getTopicName(), finish);
    long timeout = System.currentTimeMillis() + 10000;
    while(eventCounter.get() < 11 && timeout > System.currentTimeMillis()){
      delay(10);
    }
    client.unsubscribe("/wildcard/topic/#");
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
