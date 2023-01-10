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

import io.mapsmessaging.test.BaseTestConfig;
import io.mapsmessaging.test.WaitForState;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.eclipse.paho.mqttv5.client.IMqttMessageListener;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.MqttSubscription;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class QueueSubscriptionTest extends BaseTestConfig {


  @Test
  void simpleQueueSubscriptionQoS0() throws MqttException, IOException {
    simpleQueueSubscription(0);
  }

  @Test
  void simpleQueueSubscriptionQoS1() throws MqttException, IOException {
    simpleQueueSubscription(1);
  }

  @Test
  void simpleQueueSubscriptionQoS2() throws MqttException, IOException {
    simpleQueueSubscription(2);
  }

  private void simpleQueueSubscription(int QoS) throws MqttException, IOException {
    MqttClient client = new MqttClient("tcp://localhost:1883", UUID.randomUUID().toString(), new MemoryPersistence());
    MqttConnectionOptions options = new MqttConnectionOptions();
    options.setUserName("user1");
    options.setPassword("password1".getBytes());
    client.connect(options);
    Assertions.assertTrue(client.isConnected());
    for (int y = 0; y < 5; y++) {
      AtomicLong counter = new AtomicLong(0);
      MqttSubscription[] subscriptions = new MqttSubscription[1];
      IMqttMessageListener[] listeners = new IMqttMessageListener[1];
      subscriptions[0] = new MqttSubscription("queue/queue1", QoS);
      listeners[0] = (s, mqttMessage) -> counter.incrementAndGet();
      client.setCallback(new MqttCallback() {
        @Override
        public void disconnected(MqttDisconnectResponse mqttDisconnectResponse) {

        }

        @Override
        public void mqttErrorOccurred(MqttException e) {

        }

        @Override
        public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
          counter.incrementAndGet();
        }

        @Override
        public void deliveryComplete(IMqttToken iMqttToken) {

        }

        @Override
        public void connectComplete(boolean b, String s) {

        }

        @Override
        public void authPacketArrived(int i, MqttProperties mqttProperties) {

        }
      });
      client.subscribe(subscriptions, listeners);
      for (int x = 0; x < 10; x++) {
        MqttMessage message = new MqttMessage("this is a test msg,1,2,3,4,5,6,7,8,9,10".getBytes());
        message.setQos(QoS);
        message.setId(x);
        message.setRetained(false);
        client.publish("queue/queue1", message);
      }
      WaitForState.waitFor(5, TimeUnit.SECONDS, () -> counter.get() == 10);

      client.unsubscribe("queue/queue1");
      Assertions.assertEquals(10, counter.get());
    }
    client.disconnect();
    client.close();
    Assertions.assertFalse(client.isConnected());
  }

}