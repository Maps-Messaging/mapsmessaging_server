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

package io.mapsmessaging.network.protocol.impl.mqtt;

import io.mapsmessaging.test.BaseTestConfig;
import io.mapsmessaging.test.WaitForState;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SimpleOverlapTest extends BaseTestConfig  {

  @Test
  void testWildcardAndSystemTopics() throws MqttException, IOException {
    MqttClient client = new MqttClient("tcp://localhost:2001", "testWildcardAndSystemTopics", new MemoryPersistence());
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
    MqttConnectOptions options = new MqttConnectOptions();
    options.setCleanSession(true);
    options.setUserName("user1");
    options.setPassword("password1".toCharArray());
    options.setConnectionTimeout(10);
    client.connect(options);
    client.subscribe("#"); // ALL topics
    Assertions.assertTrue(waitForError(counter, 0));
    client.disconnect();
    client.close();
  }


  @Test
  void testSubscriptionThenWildcard() throws MqttException, IOException {
    MqttClient client = new MqttClient("tcp://localhost:2001", UUID.randomUUID().toString(), new MemoryPersistence());
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
    MqttConnectOptions options = new MqttConnectOptions();
    options.setUserName("user1");
    options.setPassword("password1".toCharArray());
    client.connect(options);
    //
    // Create 3 topics
    client.subscribe("overlap/topic1");
    publish(client, topics, (byte)0);
    Assertions.assertTrue(waitFor(counter, 1));

    client.subscribe("overlap/topic2");
    publish(client, topics, (byte)0);
    Assertions.assertTrue(waitFor(counter, 2));

    client.subscribe("overlap/topic3");
    publish(client, topics, (byte)0);
    Assertions.assertTrue(waitFor(counter, 3));

    //
    // Create the wildcard subscription
    client.subscribe("overlap/#");
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
    client.subscribe("overlap/#");
    publish(client, topics, (byte)0);
    Assertions.assertTrue(waitFor(counter, 3));

    client.subscribe("overlap/topic1");
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
    // Lets see if there are any more updates coming
    WaitForState.waitFor(20, TimeUnit.SECONDS, () -> counter.get() != expected);
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
