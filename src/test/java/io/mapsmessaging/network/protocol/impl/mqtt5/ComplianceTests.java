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
import org.eclipse.paho.mqttv5.client.*;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.MqttSubscription;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import java.util.concurrent.atomic.AtomicInteger;

class ComplianceTests extends MQTTBaseTest{


  @Test
  void testPublicationExpiry() throws Exception {

    MqttConnectionOptions bOptions = new MqttConnectionOptionsBuilder()
        .cleanStart(true)
        .sessionExpiryInterval(999999999L)
        .build();

    MqttClient bclient = new MqttClient(getUrl("tcp", false), UuidGenerator.getInstance().generate().toString(), new MemoryPersistence());
    Callback callback = new Callback();
    bclient.setCallback(callback);
    bclient.connect(bOptions);

    MqttSubscription[] subscriptions = new MqttSubscription[1];
    TestListener[] listeners = new TestListener[1];
    subscriptions[0] = new MqttSubscription("TopicA", 2);
    listeners[0] = new TestListener();
    bclient.subscribe(subscriptions, listeners);

    bclient.disconnect();

    // Publisher client
    MqttClient aclient = new MqttClient(getUrl("tcp", false), UuidGenerator.getInstance().generate().toString(), new MemoryPersistence());
    aclient.connect(new MqttConnectionOptionsBuilder().cleanStart(true).build());

    // Publish expired messages
    MqttProperties expireFast = new MqttProperties();
    expireFast.setMessageExpiryInterval(1L);
    MqttMessage message1 = new MqttMessage("qos 1 - expire".getBytes(StandardCharsets.UTF_8));
    message1.setQos(1);
    message1.setRetained(false);
    message1.setProperties(expireFast);

    MqttMessage message2 = new MqttMessage("qos 2 - expire".getBytes(StandardCharsets.UTF_8));
    message2.setQos(2);
    message2.setRetained(false);
    message2.setProperties(expireFast);


    aclient.publish("TopicA", message1);
    aclient.publish("TopicA", message2);

    // Publish non-expired messages
    MqttProperties expireSlow = new MqttProperties();
    expireSlow.setMessageExpiryInterval(6L);

    MqttMessage message3 = new MqttMessage("qos 1 -  don't expire".getBytes(StandardCharsets.UTF_8));
    message3.setQos(2);
    message3.setRetained(false);
    message3.setProperties(expireSlow);

    MqttMessage message4 = new MqttMessage("qos 2 -  don't expire".getBytes(StandardCharsets.UTF_8));
    message4.setQos(2);
    message4.setRetained(false);
    message4.setProperties(expireSlow);

    aclient.publish("TopicA",message3);
    aclient.publish("TopicA", message4);

    Thread.sleep(3000);

    bOptions = new MqttConnectionOptionsBuilder()
        .cleanStart(false)
        .sessionExpiryInterval(0L)
        .build();


    bclient.connect(bOptions);
    System.err.println("Reconnected to MQTT broker");
    int count = 0;
    while(callback.counter.get() != 2 && count < 10){
      Thread.sleep(100);
      count++;
    }
    Thread.sleep(1000);

    Assertions.assertEquals(2, callback.counter.get(), "Expected 2 unexpired messages");

    aclient.disconnect();
    bclient.disconnect();
  }

  @Test
  void testPayloadFormat() throws Exception {
    MqttClient aclient = new MqttClient(getUrl("tcp", false),
        UuidGenerator.getInstance().generate().toString(),
        new MemoryPersistence());

    Callback callback = new Callback();
    aclient.setCallback(callback);

    MqttConnectionOptions options = new MqttConnectionOptionsBuilder()
        .cleanStart(true)
        .sessionExpiryInterval(60L)
        .build();

    aclient.connect(options);

    MqttSubscription[] subscriptions = new MqttSubscription[1];
    TestListener[] listeners = new TestListener[1];
    subscriptions[0] = new MqttSubscription("TopicA", 2);
    subscriptions[0].setNoLocal(false);
    listeners[0] = new TestListener();
    aclient.subscribe(subscriptions, listeners);


    MqttProperties properties = new MqttProperties();
    properties.setPayloadFormat(true);
    properties.setContentType("My name");

    for (int qos = 0; qos <= 2; qos++) {
      MqttMessage message = new MqttMessage("".getBytes(StandardCharsets.UTF_8));
      message.setQos(qos);
      message.setRetained(false);
      message.setProperties(properties);
      aclient.publish("TopicA", message);
    }

    int retries = 0;
    while (callback.counter.get() < 3 && retries++ < 30) {
      Thread.sleep(100);
    }

    aclient.disconnect();

    Assertions.assertEquals(3, callback.counter.get(), "Expected 3 messages");

    for (int i = 0; i < callback.messages.length && callback.messages[i] != null; i++) {
      MqttMessage msg = callback.messages[i];
      MqttProperties props = msg.getProperties();
      Assertions.assertEquals("My name", props.getContentType());
      Assertions.assertEquals(true, props.getPayloadFormat());
    }
  }

  @Test
  void testRedeliveryOnReconnect() throws Exception {
    // Step 1: subscriber setup
    MqttClient bclient = new MqttClient(getUrl("tcp", false),
        UuidGenerator.getInstance().generate().toString(), new MemoryPersistence());
    Callback callback = new Callback();
    bclient.setCallback(callback);

    MqttConnectionOptions options = new MqttConnectionOptionsBuilder()
        .cleanStart(false)
        .sessionExpiryInterval(99999L)
        .build();

    bclient.connect(options);
    MqttSubscription[] subscriptions = { new MqttSubscription("test/redelivery/#", 2) };
    bclient.subscribe(subscriptions);
    bclient.disconnect();

    // Step 3: publisher sends messages
    MqttClient aclient = new MqttClient(getUrl("tcp", false),
        UuidGenerator.getInstance().generate().toString(), new MemoryPersistence());
    aclient.connect(new MqttConnectionOptionsBuilder().cleanStart(true).build());
    aclient.publish("test/redelivery/one", "QoS1".getBytes(StandardCharsets.UTF_8), 1, false);
    aclient.publish("test/redelivery/two", "QoS2".getBytes(StandardCharsets.UTF_8), 2, false);
    aclient.disconnect();

    Assertions.assertEquals(0, callback.counter.get(), "Expected no messages since we are disconnected");
    // Step 4: reconnect subscriber
    bclient.setCallback(callback);
    bclient.connect(options);

    int attempts = 0;
    while (callback.counter.get() < 2 && attempts++ < 30) {
      Thread.sleep(100);
    }

    bclient.disconnect();
    Assertions.assertEquals(2, callback.counter.get(), "Expected 2 redelivered messages");
  }

  @Test
  void testOfflineMessageQueueing() throws Exception {
    MqttClient aclient = new MqttClient(getUrl("tcp", false), UuidGenerator.getInstance().generate().toString(), new MemoryPersistence());
    MqttConnectionOptions options = new MqttConnectionOptionsBuilder()
        .cleanStart(true)
        .sessionExpiryInterval(99999L)
        .build();
    Callback callback = new Callback();
    aclient.setCallback(callback);
    aclient.connect(options);


    MqttSubscription[] subscriptions = new MqttSubscription[1];
    TestListener[] listeners = new TestListener[1];
    subscriptions[0] = new MqttSubscription("#", 2);
    listeners[0] = new TestListener();

    aclient.subscribe(subscriptions, listeners);

    MqttClient bclient = new MqttClient(getUrl("tcp", false), UuidGenerator.getInstance().generate().toString(), new MemoryPersistence());
    MqttConnectionOptions boptions = new MqttConnectionOptionsBuilder()
        .cleanStart(true)
        .sessionExpiryInterval(0L)
        .build();
    aclient.disconnect();

    bclient.connect(boptions);
    bclient.publish("TopicA/B", "qos 0".getBytes(StandardCharsets.UTF_8), 0, false);
    bclient.publish("Topic/C", "qos 1".getBytes(StandardCharsets.UTF_8), 1, false);
    bclient.publish("TopicA/C", "qos 2".getBytes(StandardCharsets.UTF_8), 2, false);

    Thread.sleep(5000);
    bclient.disconnect();

    MqttConnectionOptions reconnectOptions = new MqttConnectionOptionsBuilder()
        .cleanStart(false)
        .sessionExpiryInterval(0L)
        .build();

    aclient.connect(reconnectOptions);
    Thread.sleep(2000);
    aclient.disconnect();
    int received = callback.counter.get();
    Assertions.assertTrue(received == 2 || received == 3, "Received messages: " + received);
  }

  public static class TestListener implements IMqttMessageListener {
    @Override
    public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
      System.err.println("Message arrived: " + mqttMessage);
    }
  }

  public static class Callback implements MqttCallback {
    private final AtomicInteger counter = new AtomicInteger(0);
    public final MqttMessage[] messages = new MqttMessage[10]; // up to 10 messages for inspection

    @Override
    public void disconnected(MqttDisconnectResponse mqttDisconnectResponse) {}

    @Override
    public void mqttErrorOccurred(MqttException e) {}

    @Override
    public void messageArrived(String topic, MqttMessage message) {
      messages[counter.get()] = message;
      counter.incrementAndGet();
    }

    @Override
    public void deliveryComplete(IMqttToken token) {}

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {}

    @Override
    public void authPacketArrived(int reasonCode, MqttProperties mqttProperties) {}
  }

}
