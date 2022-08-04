package io.mapsmessaging;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.jupiter.api.Assertions;

public class StoreFiller {

  private static void publisher(String topicName) throws MqttException, InterruptedException {
    MqttConnectOptions options = new MqttConnectOptions();
    options.setMqttVersion(4);
    options.setCleanSession(true);

    MqttClient client = new MqttClient("tcp://localhost:1883","PublishClient"+System.nanoTime(), new MemoryPersistence());
    client.connect(options);
    Assertions.assertTrue(client.isConnected());
    long time = System.currentTimeMillis();
    byte[] payload = new byte[10];
    for(int x=0;x<payload.length;x++){
      payload[x] = (byte)(x%52+32);
    }
    for(int x=0;x<1_000_000;x++){
      int qos = 1;
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

  public static void main(String[] args) throws MqttException, InterruptedException {

    String[] topicNames = new String[32];
    for(int x=0;x<topicNames.length;x++){
      topicNames[x] ="topic-"+x ;
    }
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

    List<Thread> threadList = new ArrayList<>();
    for(int y=0;y<40;y++) {
      for (int x = 0; x < 20; x++) {
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
        t.join();
      }
      System.err.println("Completed "+y);
    }
  }

}
