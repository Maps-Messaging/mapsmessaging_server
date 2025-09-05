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
import io.mapsmessaging.test.WaitForState;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class QueueSubscriptionTest extends MQTTBaseTest {


  @ParameterizedTest
  @MethodSource("mqttPublishTestParameters")
  @DisplayName("Test QoS wildcard subscription")
  void simpleQueueSubscription(int version, String protocol, boolean auth, int QoS) throws MqttException, IOException {
    MqttConnectOptions options = getOptions(auth, version);
    MqttClient client = new MqttClient(getUrl(protocol, auth), getClientId(UuidGenerator.getInstance().generate().toString(), version), new MemoryPersistence());
    client.connect(options);
    Assertions.assertTrue(client.isConnected());
    for(int y=0;y<10;y++) {
      AtomicLong counter = new AtomicLong(0);
      client.subscribe("queue/queue1", QoS, new IMqttMessageListener() {
        @Override
        public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
          counter.incrementAndGet();
        }
      });

      for (int x = 0; x < 10; x++) {
        MqttMessage message = new MqttMessage("this is a test msg,1,2,3,4,5,6,7,8,9,10".getBytes());
        message.setQos(QoS);
        message.setId(x);
        message.setRetained(false);
        client.publish("queue/queue1", message);
      }
      WaitForState.waitFor(5, TimeUnit.SECONDS,() -> counter.get() == 10 );

      client.unsubscribe("queue/queue1");
      Assertions.assertEquals(10, counter.get());
    }
    client.disconnect();
    client.close();
    Assertions.assertFalse(client.isConnected());
  }

}
