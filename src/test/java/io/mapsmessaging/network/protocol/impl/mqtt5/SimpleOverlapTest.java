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

import io.mapsmessaging.engine.session.SessionManagerTest;
import io.mapsmessaging.test.BaseTestConfig;
import io.mapsmessaging.test.WaitForState;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class SimpleOverlapTest extends BaseTestConfig {

  @Test
  void testWildcardAndSystemTopics() throws MqttException, IOException {
    MqttClient client = new MqttClient("tcp://localhost:2001", "testWildcardAndSystemTopics", new MemoryPersistence());
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
    MqttConnectionOptions options = new MqttConnectionOptions();
    options.setCleanStart(true);
    options.setUserName("user1");
    options.setPassword("password1".getBytes());
    options.setConnectionTimeout(10);
    client.connect(options);
    MqttSubscription[] subscriptions = new MqttSubscription[1];
    IMqttMessageListener[] listeners = new IMqttMessageListener[1];
    subscriptions[0] = new MqttSubscription("#", 0);
    listeners[0] = (s, mqttMessage) -> counter.incrementAndGet();

    client.subscribe(subscriptions, listeners);
    Assertions.assertTrue(waitForError(counter, 0));
    client.disconnect();
    client.close();
  }

  @Test
  void testSystemTopics() throws MqttException {
    MqttClient client = new MqttClient("tcp://localhost:2001", UUID.randomUUID().toString(), new MemoryPersistence());
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
    MqttConnectionOptions options = new MqttConnectionOptions();
    options.setCleanStart(true);
    options.setUserName("user1");
    options.setKeepAliveInterval(30);
    options.setPassword("password1".getBytes());
    client.connect(options);
    subscribe(client, "$SYS/#", counter);
    long endTime = System.currentTimeMillis() + 20000;
    while (counter.get() == 0 && endTime > System.currentTimeMillis()) {
      delay(10);
    }
    Assertions.assertTrue(counter.get() != 0);
    client.disconnect();
    client.close();
    endTime = System.currentTimeMillis() + 20000;
    while (SessionManagerTest.getInstance().hasIdleSessions() && endTime > System.currentTimeMillis()) {
      delay(100);
    }

  }

  @Test
  @Disabled
  void testOtherSystemTopics() throws MqttException, IOException {
    MqttClient client = new MqttClient("tcp://localhost:2001", UUID.randomUUID().toString(), new MemoryPersistence());
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
    MqttConnectionOptions options = new MqttConnectionOptions();
    options.setCleanStart(true);
    options.setUserName("user1");
    options.setPassword("password1".getBytes());
    client.connect(options);
    subscribe(client, "$NMEA/#", counter);
    WaitForState.waitFor(20, TimeUnit.SECONDS, () -> counter.get() != 0);
    Assertions.assertTrue(counter.get() != 0);
    client.disconnect();
    client.close();
  }

  @Test
  void testSubscriptionThenWildcard() throws MqttException, IOException {
    MqttClient client = new MqttClient("tcp://localhost:2001", UUID.randomUUID().toString(), new MemoryPersistence());
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
    String[] topics = {"overlap/topic1", "overlap/topic2", "overlap/topic3"};
    MqttConnectionOptions options = new MqttConnectionOptions();
    options.setUserName("user1");
    options.setPassword("password1".getBytes());
    client.connect(options);
    //
    // Create 3 topics
    subscribe(client, "overlap/topic1", counter);
    publish(client, topics, (byte) 0);
    Assertions.assertTrue(waitFor(counter, 1));

    subscribe(client, "overlap/topic2", counter);
    publish(client, topics, (byte) 0);
    Assertions.assertTrue(waitFor(counter, 2));

    subscribe(client, "overlap/topic3", counter);
    publish(client, topics, (byte) 0);
    Assertions.assertTrue(waitFor(counter, 3));

    //
    // Create the wildcard subscription
    subscribe(client, "overlap/#", counter);
    publish(client, topics, (byte) 0);
    Assertions.assertTrue(waitFor(counter, 3));

    //
    // Unsubscribe from 2
    client.unsubscribe("overlap/topic2");
    publish(client, topics, (byte) 0);
    Assertions.assertTrue(waitFor(counter, 3));

    client.unsubscribe("overlap/topic3");
    publish(client, topics, (byte) 0);
    Assertions.assertTrue(waitFor(counter, 3));

    //
    // Unsubscribe from the first topic
    client.unsubscribe("overlap/topic1");
    publish(client, topics, (byte) 0);
    Assertions.assertTrue(waitFor(counter, 3));

    //
    // Unsubscribe from the wildcard
    client.unsubscribe("overlap/#");
    publish(client, topics, (byte) 0);
    Assertions.assertTrue(waitFor(counter, 0));

    //
    // Now subscribe to the wildcard
    subscribe(client, "overlap/#", counter);
    publish(client, topics, (byte) 0);
    Assertions.assertTrue(waitFor(counter, 3));

    subscribe(client, "overlap/topic1", counter);
    publish(client, topics, (byte) 0);
    Assertions.assertTrue(waitFor(counter, 3));

    client.unsubscribe("overlap/#");
    publish(client, topics, (byte) 0);
    Assertions.assertTrue(waitFor(counter, 1));

    client.unsubscribe("overlap/topic1");
    publish(client, topics, (byte) 0);
    Assertions.assertTrue(waitFor(counter, 0));
    client.disconnect();
    client.close();
  }


  private void subscribe(MqttClient mqttClient, String topic, AtomicInteger counter) throws MqttException {
    MqttSubscription[] subscriptions = new MqttSubscription[1];
    IMqttMessageListener[] listeners = new IMqttMessageListener[1];
    subscriptions[0] = new MqttSubscription(topic, 0);
    listeners[0] = (s, mqttMessage) -> counter.incrementAndGet();
    mqttClient.subscribe(subscriptions, listeners);
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
    // Lets see if there are any more updates coming
    WaitForState.waitFor(20, TimeUnit.SECONDS, () -> counter.get() != expected);
    boolean response = counter.get() == expected;
    counter.set(0);
    return response;
  }

  private void publish(MqttClient client, String[] topics, byte QoS) throws MqttException {
    for (String topic : topics) {
      MqttMessage message = new MqttMessage("this is a test msg,1,2,3,4,5,6,7,8,9,10".getBytes());
      message.setQos(QoS);
      message.setRetained(false);
      client.publish(topic, message);
    }
  }

}
