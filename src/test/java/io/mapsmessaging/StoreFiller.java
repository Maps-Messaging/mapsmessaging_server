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

package io.mapsmessaging;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.jupiter.api.Assertions;

public class StoreFiller {

  private static final LongAdder adder = new LongAdder();

  private static void publisher(String topicName) throws MqttException, InterruptedException {
    MqttConnectOptions options = new MqttConnectOptions();
    options.setMqttVersion(4);
    options.setCleanSession(false);
    options.setMaxInflight(10000);

    MqttClient client = new MqttClient("tcp://localhost:1883","PublishClient"+System.nanoTime(), new MemoryPersistence());
    client.connect(options);
    Assertions.assertTrue(client.isConnected());
    long time = System.currentTimeMillis();
    byte[] payload = new byte[10];
    for(int x=0;x<payload.length;x++){
      payload[x] = (byte)(x%52+32);
    }
    int qos = 1;
    for(int x=0;x<1_000_000;x++){
      client.publish(topicName, payload, qos, false);
      if( (x%10000) == 0){
        System.err.println("Published "+x+" > "+( System.currentTimeMillis() - time)+"ms");
        time = System.currentTimeMillis();
      }
    }
    client.publish(topicName, "This is a payload".getBytes(), 1, false);
    TimeUnit.SECONDS.sleep(2);
    client.disconnect();
    Assertions.assertFalse(client.isConnected());
    client.close();
  }

  private static void subscriber(String topicName, long counter) throws MqttException, InterruptedException {
    MqttConnectOptions options = new MqttConnectOptions();
    options.setMqttVersion(4);
    options.setCleanSession(true);

    MqttClient client = new MqttClient("tcp://localhost:1883","Subscriber_"+counter, new MemoryPersistence());
    client.connect(options);
    Assertions.assertTrue(client.isConnected());
    client.subscribe(topicName, 0, (s, mqttMessage) -> adder.increment());
    int count = 0;
    while(count < 1200){
      if(!client.isConnected()){
        System.err.println("Disconnected subscriber for "+topicName);
        client.disconnect();
        subscriber(topicName, counter);
        return;
      }
      TimeUnit.MINUTES.sleep(1);
      count++;
    }
    System.err.println("Subscriber finished");
    client.unsubscribe(topicName);
    client.disconnect();
    Assertions.assertFalse(client.isConnected());
    client.close();
  }

  public static void main(String[] args) throws MqttException, InterruptedException {

    String[] topicNames = new String[16];
    Thread[] subscriberThreads = new Thread[topicNames.length*10];
    AtomicLong counter= new AtomicLong(0);
    for(int x=0;x<topicNames.length;x++){
      topicNames[x] ="topic-"+x ;
      for(int y=0;y<10;y++){
        int finalX = x;
        subscriberThreads[x*y] = new Thread(() -> {
          try {
            subscriber(topicNames[finalX], counter.incrementAndGet());
          } catch (MqttException e) {
            throw new RuntimeException(e);
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
        });
        subscriberThreads[x*y].start();
      }
      TimeUnit.MILLISECONDS.sleep(500);
    }
    /*
    MqttConnectOptions options = new MqttConnectOptions();
    options.setMqttVersion(4);
    options.setCleanSession(true);
    MqttClient client = new MqttClient("tcp://localhost:1883","PausedClient", new MemoryPersistence());
    client.connect(options);
    Assertions.assertTrue(client.isConnected());
    for(String topic:topicNames) {
      client.subscribe(topic, 2);
    }
    TimeUnit.SECONDS.sleep(2);
    client.disconnect();
    Assertions.assertFalse(client.isConnected());
*/
    System.err.println("Subscriptions started");
    TimeUnit.SECONDS.sleep(20);
    System.err.println("Publishers starting");
    List<Thread> threadList = new ArrayList<>();
    for(int y=0;y<100;y++) {
      for (int x = 0; x < 10; x++) {
        int index = y+x;
        Thread t = new Thread(() -> {
          try {
            publisher(topicNames[(index%topicNames.length)]);
          } catch (MqttException e) {
            throw new RuntimeException(e);
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
        });
        t.start();
        threadList.add(t);
      }
      for (Thread t : threadList) {
        while(t.isAlive()) {
          t.join(2000);
          System.err.println("Received :: "+adder.sumThenReset());
        }
      }
      System.err.println("Completed "+y);
    }
  }

}
