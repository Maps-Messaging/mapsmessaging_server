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
import org.eclipse.paho.mqttv5.client.IMqttMessageListener;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.MqttSubscription;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class SubscriptionTest extends MQTTBaseTest  {

  @ParameterizedTest
  @MethodSource("mqttPublishTestParameters")
  @DisplayName("Test QoS wildcard subscription")
  void testSubscription(int version, String protocol, boolean auth, int QoS) throws MqttException, IOException {
    MqttClient client = new MqttClient(getUrl(protocol, auth), UuidGenerator.getInstance().generate().toString(), new MemoryPersistence());
    MqttConnectionOptions options = getOptions(auth);
    client.connect(options);
    Assertions.assertTrue(client.isConnected());
    MqttSubscription subscription = new MqttSubscription("/test/topic", QoS);

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
