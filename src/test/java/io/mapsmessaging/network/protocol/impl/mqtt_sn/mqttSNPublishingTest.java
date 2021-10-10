/*
 *    Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */

package io.mapsmessaging.network.protocol.impl.mqtt_sn;

import static io.mapsmessaging.network.protocol.impl.mqtt_sn.Configuration.PUBLISH_COUNT;
import static io.mapsmessaging.network.protocol.impl.mqtt_sn.Configuration.TIMEOUT;

import io.mapsmessaging.test.BaseTestConfig;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.paho.mqttsn.udpclient.MqttsCallback;
import org.eclipse.paho.mqttsn.udpclient.MqttsClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class mqttSNPublishingTest extends BaseTestConfig {

  @Test
  public void  subscribeTopicAndPublishQoS0() throws InterruptedException {
    subscribeTopicAndPublishQoSn(0);
  }

  @Test
  public void  subscribeTopicAndPublishQoS1() throws InterruptedException {
    subscribeTopicAndPublishQoSn(1);
  }

  @Test
  public void  subscribeTopicAndPublishQoS2() throws InterruptedException {
    subscribeTopicAndPublishQoSn(2);
  }

  void subscribeTopicAndPublishQoSn(int qos) throws InterruptedException {
    MqttsClient client = new MqttsClient("localhost",1884 );
    CountDownLatch connected = new CountDownLatch(1);
    CountDownLatch registered = new CountDownLatch(1);
    CountDownLatch subscribed = new CountDownLatch(1);
    CountDownLatch published = new CountDownLatch(PUBLISH_COUNT);
    CountDownLatch received = new CountDownLatch(PUBLISH_COUNT);
    CountDownLatch unsubscribed = new CountDownLatch(1);

    AtomicInteger registeredTopicId = new AtomicInteger(0);

    client.registerHandler(new MqttsCallback() {
      @Override
      public int publishArrived(boolean b, int i, int i1, byte[] bytes) {
        received.countDown();
        return 0;
      }

      @Override
      public void connected() {
        connected.countDown();
      }

      @Override
      public void disconnected(int i) {

      }

      @Override
      public void unsubackReceived() {
        unsubscribed.countDown();
      }

      @Override
      public void subackReceived(int i, int i1, int i2) {
        subscribed.countDown();
      }

      @Override
      public void pubCompReceived() {
        if(qos == 2) {
          published.countDown();
        }
      }

      @Override
      public void pubAckReceived(int i, int i1) {
        if(qos == 1) {
          published.countDown();
        }
      }

      @Override
      public void regAckReceived(int topicId, int messageId) {
        registeredTopicId.set(topicId);
        registered.countDown();
      }

      @Override
      public void registerReceived(int i, String s) {

      }

      @Override
      public void connectSent() {

      }
    });

    client.connect("simpleConnection", true, (short)50);
    Assertions.assertTrue(connected.await(TIMEOUT, TimeUnit.MILLISECONDS));

    client.register("/mqttsn/test");
    Assertions.assertTrue(registered.await(TIMEOUT, TimeUnit.MILLISECONDS));

    Assertions.assertNotEquals(registeredTopicId.get(), -1);

    //
    // Test Registered Topics
    //
    client.subscribe(registeredTopicId.get(), qos);
    Assertions.assertTrue(subscribed.await(TIMEOUT, TimeUnit.MILLISECONDS));
    long count = published.getCount();
    for(int x=0;x<PUBLISH_COUNT;x++){
      client.publish(registeredTopicId.get(), "Hi There MQTT-SN test".getBytes(), qos, false);
      // At QoS == 0 there is NO response from the gateway, its a blind publish
      if(qos != 0) {
        long timeout = System.currentTimeMillis() + TIMEOUT;
        while (count == published.getCount()) {
          delay(1);
          Assertions.assertFalse(timeout < System.currentTimeMillis());
        }
        count = published.getCount();
      }
    }
    Assertions.assertTrue(subscribed.await(TIMEOUT, TimeUnit.MILLISECONDS));

    client.unSubscribe(registeredTopicId.get());
    Assertions.assertTrue(unsubscribed.await(TIMEOUT, TimeUnit.MILLISECONDS));

    client.disconnect();
  }
}
