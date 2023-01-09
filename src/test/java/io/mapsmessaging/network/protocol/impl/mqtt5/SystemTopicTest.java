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

class SystemTopicTest extends BaseTestConfig {

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

  private void subscribe(MqttClient mqttClient, String topic, AtomicInteger counter) throws MqttException {
    MqttSubscription[] subscriptions = new MqttSubscription[1];
    IMqttMessageListener[] listeners = new IMqttMessageListener[1];
    subscriptions[0] = new MqttSubscription(topic, 0);
    listeners[0] = (s, mqttMessage) -> counter.incrementAndGet();
    mqttClient.subscribe(subscriptions, listeners);
  }


}
