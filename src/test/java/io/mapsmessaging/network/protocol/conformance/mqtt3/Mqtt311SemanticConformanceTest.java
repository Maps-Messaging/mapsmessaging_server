/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.conformance.mqtt3;

import io.mapsmessaging.network.protocol.conformance.common.MqttWireClient;
import io.mapsmessaging.network.protocol.conformance.common.ProtocolRequirement;
import io.mapsmessaging.test.BaseTestConfig;
import io.mapsmessaging.test.WaitForState;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("conformance")
@Tag("conformance-core")
@Tag("mqtt311")
class Mqtt311SemanticConformanceTest extends BaseTestConfig {

  private static final String URL = "tcp://localhost:1883";

  @Test
  @ProtocolRequirement(specification = "MQTT-3.1.1", value = "Section 3.3.1 RETAIN", source = ProtocolRequirement.MQTT_311_SOURCE)
  void retainedMessagesAreDeliveredToNewSubscription() throws Exception {
    String prefix = topic("retained");
    try (MqttClient publisher = client();
         MqttClient subscriber = client()) {
      connect(publisher, true);
      for (int qos = 0; qos <= 2; qos++) {
        MqttMessage message = new MqttMessage(("retained-" + qos).getBytes(StandardCharsets.UTF_8));
        message.setQos(qos);
        message.setRetained(true);
        publisher.publish(prefix + "/" + qos, message);
      }

      List<MqttMessage> received = new ArrayList<>();
      subscriber.setCallback(callback((topic, message) -> received.add(message)));
      connect(subscriber, true);
      subscriber.subscribe(prefix + "/#", 2);

      WaitForState.waitFor(5, TimeUnit.SECONDS, () -> received.size() == 3);
      assertEquals(3, received.size());
      assertTrue(received.stream().allMatch(MqttMessage::isRetained));

      for (int qos = 0; qos <= 2; qos++) {
        MqttMessage clear = new MqttMessage(new byte[0]);
        clear.setQos(1);
        clear.setRetained(true);
        publisher.publish(prefix + "/" + qos, clear);
      }
    }
  }

  @Test
  @ProtocolRequirement(specification = "MQTT-3.1.1", value = "Section 3.1.2.4 Clean Session", source = ProtocolRequirement.MQTT_311_SOURCE)
  void persistentSessionQueuesQosOneAndTwoWhileOffline() throws Exception {
    String topic = topic("offline");
    String clientId = "mqtt311-offline-" + UUID.randomUUID();
    AtomicInteger received = new AtomicInteger();

    try (MqttClient subscriber = new MqttClient(URL, clientId, new MemoryPersistence());
         MqttClient publisher = client()) {
      subscriber.setCallback(callback((name, message) -> received.incrementAndGet()));
      MqttConnectOptions persistent = options(false);
      subscriber.connect(persistent);
      subscriber.subscribe(topic, 2);
      subscriber.disconnect();

      connect(publisher, true);
      for (int qos = 0; qos <= 2; qos++) {
        publisher.publish(topic, ("offline-" + qos).getBytes(StandardCharsets.UTF_8), qos, false);
      }

      subscriber.connect(persistent);
      WaitForState.waitFor(5, TimeUnit.SECONDS, () -> received.get() >= 2);
      assertEquals(2, received.get(), "Only QoS 1/2 messages should be queued for an offline persistent session");
    }
  }

  @Test
  @ProtocolRequirement(specification = "MQTT-3.1.1", value = "Section 4.7.3 Topic Filter matching", source = ProtocolRequirement.MQTT_311_SOURCE)
  void overlappingSubscriptionsUsePermittedDeliverySemantics() throws Exception {
    String prefix = topic("overlap");
    String exact = prefix + "/value";
    List<Integer> qosValues = new ArrayList<>();

    try (MqttClient client = client()) {
      client.setCallback(callback((name, message) -> qosValues.add(message.getQos())));
      connect(client, true);
      client.subscribe(prefix + "/#", 2);
      client.subscribe(exact, 1);
      client.publish(exact, "overlap".getBytes(StandardCharsets.UTF_8), 2, false);

      WaitForState.waitFor(5, TimeUnit.SECONDS, () -> !qosValues.isEmpty());
      Thread.sleep(150);
      assertTrue(qosValues.size() == 1 || qosValues.size() == 2, "Expected one delivery or one per matching subscription");
      assertTrue(qosValues.contains(2), "At least one delivery must use the highest matching QoS");
    }
  }

  @Test
  @ProtocolRequirement(specification = "MQTT-3.1.1", value = "MQTT-4.7.2-1", source = ProtocolRequirement.MQTT_311_SOURCE)
  void rootWildcardDoesNotMatchDollarPrefixedTopic() throws Exception {
    AtomicInteger received = new AtomicInteger();
    String systemTopic = "$maps/conformance/" + UUID.randomUUID();

    try (MqttClient client = client()) {
      client.setCallback(callback((name, message) -> received.incrementAndGet()));
      connect(client, true);
      client.subscribe("#", 1);
      client.publish(systemTopic, "system".getBytes(StandardCharsets.UTF_8), 1, false);

      Thread.sleep(500);
      assertEquals(0, received.get());
    }
  }

  @Test
  @ProtocolRequirement(specification = "MQTT-3.1.1", value = "Section 3.10 UNSUBSCRIBE", source = ProtocolRequirement.MQTT_311_SOURCE)
  void unsubscribeStopsFurtherDelivery() throws Exception {
    String topic = topic("unsubscribe");
    AtomicInteger received = new AtomicInteger();

    try (MqttClient client = client()) {
      client.setCallback(callback((name, message) -> received.incrementAndGet()));
      connect(client, true);
      client.subscribe(topic, 1);
      client.unsubscribe(topic);
      client.publish(topic, "after-unsubscribe".getBytes(StandardCharsets.UTF_8), 1, false);

      Thread.sleep(500);
      assertEquals(0, received.get());
    }
  }

  @Test
  @ProtocolRequirement(specification = "MQTT-3.1.1", value = "Section 3.9 SUBACK", source = ProtocolRequirement.MQTT_311_SOURCE)
  void unauthorisedSubscriptionReturnsFailureCode() throws Exception {
    try (MqttWireClient client = new MqttWireClient("localhost", 1883)) {
      client.send(MqttWireClient.connect311("mqtt311-denied-" + UUID.randomUUID(), true));
      assertEquals(0, client.readPacket().body()[1] & 0xff);

      client.send(MqttWireClient.subscribe311(77, "test/nosubscribe", 2));
      MqttWireClient.WirePacket subAck = client.readPacket();

      assertEquals(9, subAck.type());
      assertEquals(77, MqttWireClient.unsignedShort(subAck.body(), 0));
      assertEquals(0x80, subAck.body()[2] & 0xff);
    }
  }

  private MqttClient client() throws Exception {
    return new MqttClient(URL, "mqtt311-" + UUID.randomUUID(), new MemoryPersistence());
  }

  private MqttConnectOptions options(boolean cleanSession) {
    MqttConnectOptions options = new MqttConnectOptions();
    options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
    options.setCleanSession(cleanSession);
    options.setConnectionTimeout(5);
    options.setKeepAliveInterval(30);
    return options;
  }

  private void connect(MqttClient client, boolean cleanSession) throws Exception {
    client.connect(options(cleanSession));
  }

  private String topic(String suffix) {
    return "conformance/mqtt311/" + suffix + "/" + UUID.randomUUID();
  }

  private MqttCallback callback(MessageHandler handler) {
    return new MqttCallback() {
      @Override
      public void connectionLost(Throwable cause) {
      }

      @Override
      public void messageArrived(String topic, MqttMessage message) throws Exception {
        handler.onMessage(topic, message);
      }

      @Override
      public void deliveryComplete(IMqttDeliveryToken token) {
      }
    };
  }

  @FunctionalInterface
  private interface MessageHandler {
    void onMessage(String topic, MqttMessage message) throws Exception;
  }
}
