package io.mapsmessaging.network.protocol.impl.mqtt;

import io.mapsmessaging.engine.session.SessionManagerTest;
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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class SystemTopicTest extends BaseTestConfig {

  @Test
  void testSystemTopics() throws MqttException {
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
    MqttConnectOptions options = new MqttConnectOptions();
    options.setCleanSession(true);
    options.setUserName("user1");
    options.setKeepAliveInterval(30);
    options.setPassword("password1".toCharArray());
    client.connect(options);
    client.subscribe("$SYS/#"); // ALL System topics
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
    client.connect(options);
    client.subscribe("$NMEA/#"); // ALL System topics
    WaitForState.waitFor(20, TimeUnit.SECONDS, () -> counter.get() != 0);
    Assertions.assertTrue(counter.get() != 0);
    client.disconnect();
    client.close();
  }

}
