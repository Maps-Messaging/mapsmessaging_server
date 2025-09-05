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
import io.mapsmessaging.test.WaitForState;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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

class SimpleOverlapTest extends MQTTBaseTest {

  @ParameterizedTest
  @MethodSource("mqttPublishTestParameters")
  @DisplayName("Test QoS wildcard subscription")
  void testWildcardAndSystemTopics(int version, String protocol, boolean auth, int QoS) throws MqttException, IOException {
    MqttClient client = new MqttClient(getUrl(protocol, auth), UuidGenerator.getInstance().generate().toString(), new MemoryPersistence());
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
    options.setConnectionTimeout(10);
    client.connect(options);
    MqttSubscription[] subscriptions = new MqttSubscription[1];
    IMqttMessageListener[] listeners = new IMqttMessageListener[1];
    subscriptions[0] = new MqttSubscription("#", QoS);
    listeners[0] = (s, mqttMessage) -> counter.incrementAndGet();

    client.subscribe(subscriptions, listeners);
    Assertions.assertTrue(waitForError(counter, 0));
    client.disconnect();
    client.close();
  }

  @ParameterizedTest
  @MethodSource("mqttPublishTestParameters")
  @DisplayName("Test QoS wildcard subscription")
  void testSubscriptionThenWildcard(int version, String protocol, boolean auth, int QoS) throws MqttException, IOException {
    MqttClient client = new MqttClient(getUrl(protocol, auth), UuidGenerator.getInstance().generate().toString(), new MemoryPersistence());
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
        System.err.println("Arrived for "+s);
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
    String[] topics = {"overlap/topic1", "overlap/topic2", "overlap/topic3"};
    client.connect(options);
    //
    // Create 3 topics
    subscribe(client, "overlap/topic1", counter, QoS);
    publish(client, topics, QoS);
    Assertions.assertTrue(waitFor(counter, 1));

    subscribe(client, "overlap/topic2", counter, QoS);
    publish(client, topics, QoS);
    Assertions.assertTrue(waitFor(counter, 2));

    subscribe(client, "overlap/topic3", counter, QoS);
    publish(client, topics, QoS);
    Assertions.assertTrue(waitFor(counter, 3));

    //
    // Create the wildcard subscription
    subscribe(client, "overlap/#", counter, QoS);
    publish(client, topics, QoS);
    Assertions.assertTrue(waitFor(counter, 3));

    //
    // Unsubscribe from 2
    client.unsubscribe("overlap/topic2");
    publish(client, topics, QoS);
    Assertions.assertTrue(waitFor(counter, 3));

    client.unsubscribe("overlap/topic3");
    publish(client, topics, QoS);
    Assertions.assertTrue(waitFor(counter, 3));

    //
    // Unsubscribe from the first topic
    client.unsubscribe("overlap/topic1");
    publish(client, topics, QoS);
    Assertions.assertTrue(waitFor(counter, 3));

    //
    // Unsubscribe from the wildcard
    client.unsubscribe("overlap/#");
    publish(client, topics, QoS);
    Assertions.assertTrue(waitFor(counter, 0));

    //
    // Now subscribe to the wildcard
    subscribe(client, "overlap/#", counter, QoS);
    publish(client, topics, QoS);
    Assertions.assertTrue(waitFor(counter, 3));

    subscribe(client, "overlap/topic1", counter, QoS);
    publish(client, topics, QoS);
    Assertions.assertTrue(waitFor(counter, 3));

    client.unsubscribe("overlap/#");
    publish(client, topics, QoS);
    Assertions.assertTrue(waitFor(counter, 1));

    client.unsubscribe("overlap/topic1");
    publish(client, topics, QoS);
    Assertions.assertTrue(waitFor(counter, 0));
    client.disconnect();
    client.close();
  }


  private void subscribe(MqttClient mqttClient, String topic, AtomicInteger counter, int QoS) throws MqttException {
    MqttSubscription[] subscriptions = new MqttSubscription[1];
    IMqttMessageListener[] listeners = new IMqttMessageListener[1];
    subscriptions[0] = new MqttSubscription(topic, QoS);
    listeners[0] = (s, mqttMessage) -> counter.incrementAndGet();
    mqttClient.subscribe(subscriptions, listeners);
  }


  private boolean waitFor(AtomicInteger counter, int expected) throws IOException {
    WaitForState.waitFor(10, TimeUnit.SECONDS, () -> counter.get() == expected);
    // Lets see if there are any more updates coming
    WaitForState.waitFor(50, TimeUnit.MILLISECONDS, () -> counter.get() != expected);
    boolean response = counter.get() == expected;
    System.err.println("Counter::"+counter.get());
    counter.set(0);
    return response;
  }

  private boolean waitForError(AtomicInteger counter, int expected) throws IOException {
    WaitForState.waitFor(20, TimeUnit.SECONDS, () -> counter.get() == expected);
    // Lets see if there are any more updates coming
    WaitForState.waitFor(20, TimeUnit.SECONDS, () -> counter.get() != expected);
    boolean response = counter.get() == expected;
    counter.set(0);
    return response;
  }

  private void publish(MqttClient client, String[] topics, int QoS) throws MqttException {
    for (String topic : topics) {
      MqttMessage message = new MqttMessage("this is a test msg,1,2,3,4,5,6,7,8,9,10".getBytes());
      message.setQos(QoS);
      message.setRetained(false);
      client.publish(topic, message);
    }
  }

}
