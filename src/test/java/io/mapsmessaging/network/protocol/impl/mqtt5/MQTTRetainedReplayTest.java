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

package io.mapsmessaging.network.protocol.impl.mqtt5;

import io.mapsmessaging.security.uuid.UuidGenerator;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.MqttSubscription;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MQTTRetainedReplayTest extends MQTTBaseTest {

  @ParameterizedTest(name = "Retain As Published = {0}")
  @ValueSource(booleans = {false, true})
  void retainedReplayAlwaysSetsRetainAndLivePublishHonoursRap(boolean retainAsPublished) throws Exception {
    String topicName = getTopicName();
    MqttClient publisher = new MqttClient(getUrl("tcp", false), UuidGenerator.getInstance().generate().toString(), new MemoryPersistence());
    MqttClient subscriber = new MqttClient(getUrl("tcp", false), UuidGenerator.getInstance().generate().toString(), new MemoryPersistence());

    try {
      publisher.connect(getOptions(false));

      MqttMessage retained = new MqttMessage("initial retained value".getBytes(StandardCharsets.UTF_8));
      retained.setQos(1);
      retained.setRetained(true);
      publisher.publish(topicName, retained);

      subscriber.connect(getOptions(false));
      AtomicReference<MqttMessage> received = new AtomicReference<>();
      MqttSubscription subscription = new MqttSubscription(topicName, 1);
      subscription.setRetainAsPublished(retainAsPublished);
      subscriber.subscribe(
          new MqttSubscription[]{subscription},
          new org.eclipse.paho.mqttv5.client.IMqttMessageListener[]{(topic, message) -> received.set(message)})
          .waitForCompletion(2000);

      MqttMessage replay = waitForMessage(received);
      Assertions.assertNotNull(replay, "Expected retained replay after subscribing");
      Assertions.assertTrue(replay.isRetained(), "Initial retained replay must set RETAIN regardless of RAP");

      received.set(null);
      MqttMessage liveRetained = new MqttMessage("live retained value".getBytes(StandardCharsets.UTF_8));
      liveRetained.setQos(1);
      liveRetained.setRetained(true);
      publisher.publish(topicName, liveRetained);

      MqttMessage liveRetainedDelivery = waitForMessage(received);
      Assertions.assertNotNull(liveRetainedDelivery, "Expected live retained publication");
      Assertions.assertEquals(retainAsPublished, liveRetainedDelivery.isRetained(), "Live retained publication must honour RAP");

      received.set(null);
      MqttMessage live = new MqttMessage("live value".getBytes(StandardCharsets.UTF_8));
      live.setQos(1);
      live.setRetained(false);
      publisher.publish(topicName, live);

      MqttMessage liveDelivery = waitForMessage(received);
      Assertions.assertNotNull(liveDelivery, "Expected live non-retained publication");
      Assertions.assertFalse(liveDelivery.isRetained(), "Live non-retained publication must not set RETAIN");
    } finally {
      if (subscriber.isConnected()) {
        subscriber.disconnect();
      }
      subscriber.close();

      if (publisher.isConnected()) {
        MqttMessage clearRetained = new MqttMessage(new byte[0]);
        clearRetained.setQos(1);
        clearRetained.setRetained(true);
        publisher.publish(topicName, clearRetained);
        publisher.disconnect();
      }
      publisher.close();
    }
  }

  private MqttMessage waitForMessage(AtomicReference<MqttMessage> received) throws InterruptedException {
    long timeout = System.currentTimeMillis() + 5000;
    while (received.get() == null && System.currentTimeMillis() < timeout) {
      Thread.sleep(10);
    }
    return received.get();
  }
}
