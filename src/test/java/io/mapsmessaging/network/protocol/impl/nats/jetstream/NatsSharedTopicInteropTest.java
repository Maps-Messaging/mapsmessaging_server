/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

package io.mapsmessaging.network.protocol.impl.nats.jetstream;

import io.nats.client.Dispatcher;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * An unchanged MAPS destination is shared by NATS and MQTT.
 * No NATS stream namespace or protocol-specific engine storage is required.
 */
class NatsSharedTopicInteropTest extends JetStreamBaseTest {

  @Test
  void natsAndMqttPublishToExactlyTheSameMapsTopicInBothDirections() throws Exception {
    String suffix = UUID.randomUUID().toString().replace("-", "");
    String topic = "orders_" + suffix;
    byte[] created = "created-from-nats".getBytes(StandardCharsets.UTF_8);
    byte[] paid = "paid-from-mqtt".getBytes(StandardCharsets.UTF_8);
    CompletableFuture<byte[]> mqttReceived = new CompletableFuture<>();

    MqttClient mqtt = new MqttClient("tcp://localhost:1883", "maps_" + suffix.substring(0, 16), new MemoryPersistence());
    Dispatcher dispatcher = null;
    try {
      mqtt.setCallback(new MqttCallback() {
        @Override
        public void connectionLost(Throwable cause) {
          mqttReceived.completeExceptionally(cause);
        }

        @Override
        public void messageArrived(String receivedTopic, MqttMessage message) {
          if (topic.equals(receivedTopic)) {
            mqttReceived.complete(message.getPayload());
          }
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
          // The publisher does not need a client-side delivery callback.
        }
      });
      MqttConnectOptions options = new MqttConnectOptions();
      options.setCleanSession(true);
      mqtt.connect(options);
      mqtt.subscribe(topic, 1);

      natsConnection.publish(topic, created);
      natsConnection.flush(Duration.ofSeconds(2));
      assertArrayEquals(created, mqttReceived.get(5, TimeUnit.SECONDS),
          "NATS publication should reach the MQTT subscription without expanding the MAPS topic");

      CompletableFuture<byte[]> natsReceived = new CompletableFuture<>();
      dispatcher = natsConnection.createDispatcher(message -> natsReceived.complete(message.getData()));
      dispatcher.subscribe(topic);
      natsConnection.flush(Duration.ofSeconds(2));

      MqttMessage mqttMessage = new MqttMessage(paid);
      mqttMessage.setQos(1);
      mqtt.publish(topic, mqttMessage);
      assertArrayEquals(paid, natsReceived.get(5, TimeUnit.SECONDS),
          "MQTT publication should reach the NATS subscription through the same MAPS topic");
    } finally {
      if (dispatcher != null) {
        natsConnection.closeDispatcher(dispatcher);
      }
      if (mqtt.isConnected()) {
        mqtt.disconnect();
      }
      mqtt.close();
    }
  }
}
