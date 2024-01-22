/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
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

import io.mapsmessaging.engine.session.SessionManagerTest;
import io.mapsmessaging.utilities.UuidGenerator;
import org.eclipse.paho.mqttv5.client.*;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.MqttSubscription;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

class SystemTopicTest extends MQTTBaseTest {

  @ParameterizedTest
  @MethodSource("mqttPublishTestParameters")
  @DisplayName("Test QoS wildcard subscription")
  void testSystemTopics(int version, String protocol, boolean auth, int QoS) throws IOException, MqttException {

    MqttClient client = new MqttClient(getUrl(protocol, auth), UuidGenerator.generate().toString(), new MemoryPersistence());
    MqttConnectionOptions options = getOptions(auth);

    AtomicInteger counter = new AtomicInteger(0);
    client.setCallback(new MqttCallback() {
      @Override
      public void disconnected(MqttDisconnectResponse mqttDisconnectResponse) {

      }

      @Override
      public void mqttErrorOccurred(MqttException e) {

      }

      @Override
      public void messageArrived(String s, MqttMessage mqttMessage) {
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
    options.setCleanStart(true);
    options.setKeepAliveInterval(30);
    client.connect(options);
    subscribe(client, "$SYS/#", counter, QoS);

    long endTime = System.currentTimeMillis() + 10000;
    while (counter.get() == 0 && endTime > System.currentTimeMillis()) {
      delay(10);
    }
    Assertions.assertTrue(counter.get() != 0);
    client.disconnect();
    client.close();
    endTime = System.currentTimeMillis() + 10000;
    while (SessionManagerTest.getInstance().hasIdleSessions() && endTime > System.currentTimeMillis()) {
      delay(100);
    }
  }

  private void subscribe(MqttClient mqttClient, String topic, AtomicInteger counter, int QoS) throws MqttException {
    MqttSubscription[] subscriptions = new MqttSubscription[1];
    IMqttMessageListener[] listeners = new IMqttMessageListener[1];
    subscriptions[0] = new MqttSubscription(topic, QoS);
    listeners[0] = (s, mqttMessage) -> counter.incrementAndGet();
    mqttClient.subscribe(subscriptions, listeners);
  }


}
