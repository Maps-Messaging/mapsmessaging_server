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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.paho.mqttsn.udpclient.MqttsCallback;
import org.eclipse.paho.mqttsn.udpclient.MqttsClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import io.mapsmessaging.test.BaseTestConfig;

public class mqttSNSubscriptionTest extends BaseTestConfig {

  private final static long TIMEOUT = 5000;
  private final static int PUBLISH_COUNT = 20;

  @Test
  public void registerAndSubscribeTopic() throws InterruptedException {
    MqttsClient client = new MqttsClient("localhost",1884 );
    CountDownLatch connected = new CountDownLatch(1);
    CountDownLatch registered = new CountDownLatch(1);
    CountDownLatch subscribed = new CountDownLatch(1);
    AtomicInteger registeredTopicId = new AtomicInteger(0);

    client.registerHandler(new MqttsCallback() {
      @Override
      public int publishArrived(boolean b, int i, int i1, byte[] bytes) {
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

      }

      @Override
      public void subackReceived(int i, int i1, int i2) {
        subscribed.countDown();
      }

      @Override
      public void pubCompReceived() {

      }

      @Override
      public void pubAckReceived(int i, int i1) {

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

    client.register("test");
    Assertions.assertTrue(registered.await(TIMEOUT, TimeUnit.MILLISECONDS));

    Assertions.assertNotEquals(registeredTopicId.get(), 0);

    //
    // Test Registered Topics
    //
    client.subscribe(registeredTopicId.get(), 1);
    Assertions.assertTrue(subscribed.await(TIMEOUT, TimeUnit.MILLISECONDS));

    client.disconnect();
  }

  @Test
  public void subscribeTopic() throws InterruptedException {
    MqttsClient client = new MqttsClient("localhost",1884 );
    CountDownLatch connected = new CountDownLatch(1);
    CountDownLatch registered = new CountDownLatch(1);
    CountDownLatch subscribed = new CountDownLatch(1);
    AtomicInteger registeredTopicId = new AtomicInteger(0);

    client.registerHandler(new MqttsCallback() {
      @Override
      public int publishArrived(boolean b, int i, int i1, byte[] bytes) {
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

      }

      @Override
      public void subackReceived(int i, int i1, int i2) {
        subscribed.countDown();
      }

      @Override
      public void pubCompReceived() {

      }

      @Override
      public void pubAckReceived(int i, int i1) {

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

    //
    // Test Registered Topics
    //
    client.subscribe("test", 1, 0);
    Assertions.assertTrue(subscribed.await(TIMEOUT, TimeUnit.MILLISECONDS));

    client.disconnect();
  }


  @Test
  public void subscribeQoS0TopicAndPublish() throws InterruptedException {
    subscribeQoSnTopicAndPublish(0);
  }

  @Test
  public void subscribeQoS1TopicAndPublish() throws InterruptedException {
    subscribeQoSnTopicAndPublish(1);
  }

  @Test
  public void subscribeQoS2TopicAndPublish() throws InterruptedException {
    subscribeQoSnTopicAndPublish(2);
  }

  public void subscribeQoSnTopicAndPublish(int qos) throws InterruptedException {
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
        if(qos == 2){
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

    client.register("test");
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
      if(qos != 0) {
        long timeout = System.currentTimeMillis() + TIMEOUT;
        while (count == published.getCount()) {
          delay(1);
          Assertions.assertFalse(timeout < System.currentTimeMillis());
        }
        count = published.getCount();
      }
    }
    if(qos != 0) {
      Assertions.assertTrue(published.await(TIMEOUT, TimeUnit.MILLISECONDS));
    }
    Assertions.assertTrue(subscribed.await(TIMEOUT, TimeUnit.MILLISECONDS));

    client.unSubscribe(registeredTopicId.get());
    Assertions.assertTrue(unsubscribed.await(TIMEOUT, TimeUnit.MILLISECONDS));

    client.disconnect();
  }


  @Test
  public void subscribeWildcardQoS0TopicAndPublish() throws InterruptedException {
    subscribeWildcardQoSnTopicAndPublish(0);
  }

  @Test
  public void subscribeWildcardQoS1TopicAndPublish() throws InterruptedException {
    subscribeWildcardQoSnTopicAndPublish(1);
  }

  @Test
  public void subscribeWildcardQoS2TopicAndPublish() throws InterruptedException {
    subscribeWildcardQoSnTopicAndPublish(2);
  }

  public void subscribeWildcardQoSnTopicAndPublish(int qos) throws InterruptedException {

    MqttsClient publisher = new MqttsClient("localhost",1884 );

    int[] registeredTopics = new int[5];
    CountDownLatch published = new CountDownLatch(PUBLISH_COUNT);

    CountDownLatch pubConnected = new CountDownLatch(1);
    CountDownLatch registered = new CountDownLatch(registeredTopics.length);
    publisher.registerHandler(new MqttsCallback() {
      @Override
      public int publishArrived(boolean b, int i, int i1, byte[] bytes) {
        return 0;
      }

      @Override
      public void connected() {
        pubConnected.countDown();
      }

      @Override
      public void disconnected(int i) {

      }

      @Override
      public void unsubackReceived() {

      }

      @Override
      public void subackReceived(int i, int i1, int i2) {

      }

      @Override
      public void pubCompReceived() {
        if(qos == 2){
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
        registered.countDown();
        registeredTopics[(int)registered.getCount()] = topicId;

      }

      @Override
      public void registerReceived(int topicId, String topic) {
      }

      @Override
      public void connectSent() {

      }
    });
    publisher.connect("PublishTest", true, (short)50);
    Assertions.assertTrue(pubConnected.await(TIMEOUT, TimeUnit.MILLISECONDS));

    for(int x=0;x<registeredTopics.length;x++){
      long count = registered.getCount();
      publisher.register("/test/wild/topic"+x);
      long timeout = System.currentTimeMillis() + TIMEOUT;
      while(registered.getCount() != 0 &&
          registered.getCount() == count){
        delay(1);
        Assertions.assertFalse(timeout < System.currentTimeMillis());
      }
    }
    Assertions.assertTrue(registered.await(TIMEOUT, TimeUnit.MILLISECONDS));



    MqttsClient client = new MqttsClient("localhost",1884 );
    CountDownLatch connected = new CountDownLatch(1);
    CountDownLatch subscribed = new CountDownLatch(1);
    CountDownLatch received = new CountDownLatch(PUBLISH_COUNT);
    CountDownLatch unsubscribed = new CountDownLatch(1);
    CountDownLatch registerRequests = new CountDownLatch(registeredTopics.length);

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
      }

      @Override
      public void pubAckReceived(int i, int i1) {
      }

      @Override
      public void regAckReceived(int topicId, int messageId) {
      }

      @Override
      public void registerReceived(int topicId, String topic) {
        registerRequests.countDown();
      }

      @Override
      public void connectSent() {

      }
    });

    client.connect("simpleConnection", true, (short)50);
    Assertions.assertTrue(connected.await(TIMEOUT, TimeUnit.MILLISECONDS));


    //
    // Test Registered Topics
    //
    client.subscribe("/test/wild/+", qos, 0);
    Assertions.assertTrue(subscribed.await(TIMEOUT, TimeUnit.MILLISECONDS));
    long count = published.getCount();
    for(int x=0;x<PUBLISH_COUNT;x++){
      publisher.publish(registeredTopics[x%registeredTopics.length], "Hi There MQTT-SN test".getBytes(), qos, false);
      if(qos != 0) {
        long timeout = System.currentTimeMillis() + TIMEOUT;
        while (count == published.getCount()) {
          delay(1);
          Assertions.assertFalse(timeout < System.currentTimeMillis());
        }
        count = published.getCount();
      }
    }
    if(qos != 0) {
      Assertions.assertTrue(published.await(TIMEOUT, TimeUnit.MILLISECONDS));
    }
    Assertions.assertTrue(received.await(TIMEOUT, TimeUnit.MILLISECONDS));
    Assertions.assertTrue(registerRequests.await(TIMEOUT, TimeUnit.MILLISECONDS));

    client.unSubscribe(registeredTopicId.get());
    Assertions.assertTrue(unsubscribed.await(TIMEOUT, TimeUnit.MILLISECONDS));

    publisher.disconnect();
    client.disconnect();
  }

}
