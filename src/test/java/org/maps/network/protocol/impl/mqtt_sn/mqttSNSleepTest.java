/*
 *  Copyright [2020] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.maps.network.protocol.impl.mqtt_sn;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.paho.mqttsn.udpclient.MqttsCallback;
import org.eclipse.paho.mqttsn.udpclient.MqttsClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.maps.test.BaseTestConfig;

public class mqttSNSleepTest extends BaseTestConfig {

  private final static long TIMEOUT = 5000;
  private final static int PUBLISH_COUNT = 5;

  private int connectionCounter = 0;

  @Test
  public void sleepingClientTest(){
    MqttsClientManager sleepy = new MqttsClientManager("localhost", 1884);
    MqttsClientManager hyper = new MqttsClientManager("localhost", 1884);

    sleepy.subscribe("test");
    sleepy.client.sleep(30); // We sleep for 30 seconds
    for(int x=0;x<PUBLISH_COUNT;x++) {
      Assertions.assertTrue(hyper.publish("test", "These should be waiting for sleepy".getBytes()));
    }
    //
    // At this point sleepy should have 0 publish events
    //
    Assertions.assertEquals(sleepy.receivedEvents.get(), 0);

    // Ok, so sleepy seems to be sleeping, lets wait a couple of seconds to be sure
    delay(2000);
    Assertions.assertEquals(sleepy.receivedEvents.get(), 0);

    // Ok, so sleepy seems to be in a coma, cool
    sleepy.client.wakeupForNewMessages();

    // We should ask the server to send any outstanding events and then go back to sleep
    long timeout = System.currentTimeMillis() + TIMEOUT;
    while(sleepy.receivedEvents.get() != PUBLISH_COUNT && timeout > System.currentTimeMillis()){
      delay(1);
    }

    Assertions.assertEquals(sleepy.receivedEvents.get(), PUBLISH_COUNT);

    //
    // So far so good, but is sleepy asleep again
    //
    sleepy.receivedEvents.set(0);
    for(int x=0;x<PUBLISH_COUNT;x++) {
      Assertions.assertTrue(hyper.publish("test", "These should be waiting for sleepy".getBytes()));
    }
    //
    // At this point sleepy should have 0 publish events
    //
    Assertions.assertEquals(sleepy.receivedEvents.get(), 0);

    // Ok, so sleepy seems to be sleeping, lets wait a couple of seconds to be sure
    delay(2000);
    Assertions.assertEquals(sleepy.receivedEvents.get(), 0);

    // Brilliant, not only did sleepy wake up, receive events but went back to sleep...
    // final task, lets wake up, and stay awake...
    sleepy.client.connect("sleepTest", false, (short)50);

    timeout = System.currentTimeMillis() + TIMEOUT;
    while(sleepy.receivedEvents.get() != PUBLISH_COUNT && timeout > System.currentTimeMillis()){
      delay(1);
    }

    Assertions.assertEquals(sleepy.receivedEvents.get(), PUBLISH_COUNT);

    //
    // Final test, if I publish then sleepy should receive directly
    //
    sleepy.receivedEvents.set(0);
    for(int x=0;x<PUBLISH_COUNT;x++) {
      Assertions.assertTrue(hyper.publish("test", "These should be waiting for sleepy".getBytes()));
    }


    timeout = System.currentTimeMillis() + TIMEOUT;
    while(sleepy.receivedEvents.get() != PUBLISH_COUNT && timeout > System.currentTimeMillis()){
      delay(1);
    }
    sleepy.close();
    hyper.close();
  }


  private final class MqttsClientManager {

    private MqttsClient client;
    private Map<String, Integer> topicMap;
    AtomicInteger topicIdResponse = new AtomicInteger(0);
    AtomicInteger receivedEvents = new AtomicInteger(0);
    AtomicBoolean connected = new AtomicBoolean(false);
    AtomicBoolean published = new AtomicBoolean(false);


    public MqttsClientManager(String address, int port){
      topicMap = new LinkedHashMap<>();
      connectClient(address, port);
    }

    public void close(){
      client.disconnect();
    }

    public boolean publish(String topic, byte[] msg){
      if(topicMap.containsKey(topic)){
        int topicId = topicMap.get(topic);
        published.set(false);
        client.publish(topicId, msg, 1, false);
        long timeout = System.currentTimeMillis() + TIMEOUT;
        while(!published.get() && timeout > System.currentTimeMillis()){
          delay(1);
        }
        return published.get();
      }
      else{
        topicIdResponse.set(0);
        client.register(topic);
        long timeout = System.currentTimeMillis() + TIMEOUT;
        while(topicIdResponse.get() == 0 && timeout > System.currentTimeMillis()){
          delay(1);
        }

        if(topicIdResponse.get() == 0 ){
          return false;
        }
        topicMap.put(topic, topicIdResponse.get());
        return publish(topic, msg);
      }
    }
    private void connectClient(String address, int port) {
      client = new MqttsClient(address, port);
      client.registerHandler(new MqttsCallback() {
        @Override
        public int publishArrived(boolean b, int i, int i1, byte[] bytes) {
          receivedEvents.incrementAndGet();
          return 0;
        }

        @Override
        public void connected() {
          connected.set(true);
        }

        @Override
        public void disconnected(int i) {
          connected.set(false);
        }

        @Override
        public void unsubackReceived() {
        }

        @Override
        public void subackReceived(int i, int i1, int i2) {
        }

        @Override
        public void pubCompReceived() {
        }

        @Override
        public void pubAckReceived(int i, int i1) {
          published.set(true);
        }

        @Override
        public void regAckReceived(int topicId, int messageId) {
          topicIdResponse.set(topicId);
        }

        @Override
        public void registerReceived(int i, String s) {
        }

        @Override
        public void connectSent() {

        }
      });
      client.connect("sleepTest"+connectionCounter++, true, (short)50);
      long timeout = System.currentTimeMillis()+TIMEOUT;
      while(!connected.get() && timeout > System.currentTimeMillis()){
        delay(1);
      }
    }

    public void subscribe(String test) {
      client.subscribe(test, 1, 0);
    }
  }
}
