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
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class SimpleOverlapTest extends MQTTBaseTest {

  @ParameterizedTest
  @MethodSource("mqttPublishTestParameters")
  @DisplayName("Test QoS wildcard subscription")
  void testWildcardAndSystemTopics(int version, String protocol, boolean auth, int QoS) throws MqttException, IOException {
    MqttConnectOptions options = getOptions(auth, version);
    MqttClient client = new MqttClient(getUrl(protocol, auth), getClientId(UuidGenerator.getInstance().generate().toString(), version), new MemoryPersistence());
    AtomicInteger counter = new AtomicInteger(0);
    client.setCallback(new MqttCallback() {
      @Override
      public void connectionLost(Throwable throwable) {

      }

      @Override
      public void messageArrived(String s, MqttMessage mqttMessage) {
        counter.incrementAndGet();
      }

      @Override
      public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

      }
    });
    options.setCleanSession(true);
    options.setConnectionTimeout(10);
    client.connect(options);
    client.subscribe("#", QoS); // ALL topics
    Assertions.assertTrue(waitForError(counter, 0));
    client.disconnect();
    client.close();
  }

  @ParameterizedTest
  @MethodSource("mqttPublishTestParameters")
  @DisplayName("Test QoS wildcard subscription")
  void testSubscriptionThenWildcard(int version, String protocol, boolean auth, int QoS) throws MqttException, IOException {
    MqttConnectOptions options = getOptions(auth, version);
    options.setCleanSession(true);
    MqttClient client = new MqttClient(getUrl(protocol, auth), getClientId(UuidGenerator.getInstance().generate().toString(), version), new MemoryPersistence());
    AtomicInteger counter = new AtomicInteger(0);
    client.setCallback(new MqttCallback() {
      @Override
      public void connectionLost(Throwable throwable) {

      }

      @Override
      public void messageArrived(String s, MqttMessage mqttMessage) {
        counter.incrementAndGet();
      }

      @Override
      public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

      }
    });
    String[] topics = {"overlap/topic1","overlap/topic2","overlap/topic3"};
    client.connect(options);
    //
    // Create 3 topics
    client.subscribe("overlap/topic1", QoS);
    publish(client, topics, (byte)0);
    Assertions.assertTrue(waitFor(counter, 1));

    client.subscribe("overlap/topic2", QoS);
    publish(client, topics, (byte)0);
    Assertions.assertTrue(waitFor(counter, 2));

    client.subscribe("overlap/topic3", QoS);
    publish(client, topics, (byte)0);
    Assertions.assertTrue(waitFor(counter, 3));

    //
    // Create the wildcard subscription
    client.subscribe("overlap/#", QoS);
    publish(client, topics, (byte)0);
    Assertions.assertTrue(waitFor(counter, 3));

    //
    // Unsubscribe from 2
    client.unsubscribe("overlap/topic2");
    publish(client, topics, (byte)0);
    Assertions.assertTrue(waitFor(counter, 3));

    client.unsubscribe("overlap/topic3");
    publish(client, topics, (byte)0);
    Assertions.assertTrue(waitFor(counter, 3));


    //
    // Unsubscribe from the first topic
    client.unsubscribe("overlap/topic1");
    publish(client, topics, (byte)0);
    Assertions.assertTrue(waitFor(counter, 3));

    //
    // Unsubscribe from the wildcard
    client.unsubscribe("overlap/#");
    publish(client, topics, (byte)0);
    Assertions.assertTrue(waitFor(counter, 0));

    //
    // Now subscribe to the wildcard
    client.subscribe("overlap/#", QoS);
    publish(client, topics, (byte)0);
    Assertions.assertTrue(waitFor(counter, 3));

    client.subscribe("overlap/topic1", QoS);
    publish(client, topics, (byte)0);
    Assertions.assertTrue(waitFor(counter, 3));

    client.unsubscribe("overlap/#");
    publish(client, topics, (byte)0);
    Assertions.assertTrue(waitFor(counter, 1));

    client.unsubscribe("overlap/topic1");
    publish(client, topics, (byte)0);
    Assertions.assertTrue(waitFor(counter, 0));
    client.disconnect();
    client.close();
  }



  private boolean waitFor(AtomicInteger counter, int expected) throws IOException {
    WaitForState.waitFor(10, TimeUnit.SECONDS, () -> counter.get() == expected);
    // Lets see if there are any more updates coming
    WaitForState.waitFor(50, TimeUnit.MILLISECONDS, () -> counter.get() != expected);
    boolean response = counter.get() == expected;
    counter.set(0);
    return response;
  }

  private boolean waitForError(AtomicInteger counter, int expected) throws IOException {
    WaitForState.waitFor(20, TimeUnit.SECONDS, () -> counter.get() == expected);
    // Let's see if there are any more updates coming
    WaitForState.waitFor(10, TimeUnit.SECONDS, () -> counter.get() != expected);
    boolean response = counter.get() == expected;
    counter.set(0);
    return response;
  }

  private void publish(MqttClient client, String[] topics, byte QoS) throws MqttException {
    for(String topic:topics){
      MqttMessage message = new MqttMessage("this is a test msg,1,2,3,4,5,6,7,8,9,10".getBytes());
      message.setQos(QoS);
      message.setRetained(false);
      client.publish(topic, message);
    }
  }

}
