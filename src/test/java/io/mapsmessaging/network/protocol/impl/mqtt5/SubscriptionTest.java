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

package io.mapsmessaging.network.protocol.impl.mqtt5;

import java.util.UUID;
import org.eclipse.paho.mqttv5.client.IMqttMessageListener;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.MqttSubscription;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SubscriptionTest extends MQTTBaseTest  {


  @Test
  void testSubscription() throws MqttException {
    MqttClient client = new MqttClient("tcp://localhost:1883", UUID.randomUUID().toString(), new MemoryPersistence());
    MqttConnectionOptions options = new MqttConnectionOptions();
    options.setUserName("user1");
    options.setPassword("password1".getBytes());
    client.connect(options);
    Assertions.assertTrue(client.isConnected());
    MqttSubscription subscription = new MqttSubscription("/test/topic", 0);

    MqttSubscription[] subscriptions = new MqttSubscription[1];
    subscriptions[0] = subscription;
    IMqttMessageListener[] listeners = new IMqttMessageListener[1];
    listeners[0] = (topic, message) -> messageArrived(topic, message);

    IMqttToken token = client.subscribe(subscriptions, listeners);
    token.waitForCompletion(2000);
    Assertions.assertTrue(token.isComplete());
    client.unsubscribe("/test/topic");
    client.disconnect();
    Assertions.assertFalse(client.isConnected());
    client.close();
  }

  public void messageArrived(String topic, MqttMessage message) throws Exception {

  }
}
