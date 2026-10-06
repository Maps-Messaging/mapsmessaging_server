/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.conformance.mqtt5;

import io.mapsmessaging.network.protocol.conformance.common.CloseableMqtt5Client;
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
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.paho.mqttv5.client.IMqttMessageListener;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptionsBuilder;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.MqttSubscription;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.eclipse.paho.mqttv5.common.packet.UserProperty;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("conformance")
@Tag("conformance-core")
@Tag("mqtt5")
class Mqtt5SemanticConformanceTest extends BaseTestConfig {

  private static final String URL = "tcp://localhost:1883";

  @Test
  @ProtocolRequirement(specification = "MQTT-5.0", value = "Section 3.3.1 RETAIN", source = ProtocolRequirement.MQTT_5_SOURCE)
  void retainedMessagePreservesUserProperties() throws Exception {
    String topic = topic("retained");
    List<MqttMessage> received = new ArrayList<>();

    try (CloseableMqtt5Client publisher = client();
         CloseableMqtt5Client subscriber = client()) {
      connect(publisher, true, 0);
      MqttProperties props = new MqttProperties();
      props.setUserProperties(List.of(new UserProperty("a", "2"), new UserProperty("c", "3")));
      MqttMessage retained = new MqttMessage("retained".getBytes(StandardCharsets.UTF_8));
      retained.setQos(1);
      retained.setRetained(true);
      retained.setProperties(props);
      publisher.publish(topic, retained);

      subscriber.setCallback(callback((name, message) -> received.add(message)));
      connect(subscriber, true, 0);
      subscriber.subscribe(topic, 2);

      WaitForState.waitFor(5, TimeUnit.SECONDS, () -> received.size() == 1);
      assertEquals(1, received.size());
      assertTrue(received.getFirst().isRetained());
      assertEquals(props.getUserProperties(), received.getFirst().getProperties().getUserProperties());

      MqttMessage clear = new MqttMessage(new byte[0]);
      clear.setQos(1);
      clear.setRetained(true);
      publisher.publish(topic, clear);
    }
  }

  @Test
  @ProtocolRequirement(specification = "MQTT-5.0", value = "Section 3.1.2.11 Session Expiry Interval", source = ProtocolRequirement.MQTT_5_SOURCE)
  void persistentSessionQueuesQosOneAndTwoWhileOffline() throws Exception {
    String topic = topic("offline");
    String id = "mqtt5-offline-" + UUID.randomUUID();
    AtomicInteger received = new AtomicInteger();

    try (CloseableMqtt5Client subscriber = new CloseableMqtt5Client(URL, id);
         CloseableMqtt5Client publisher = client()) {
      subscriber.setCallback(callback((name, message) -> received.incrementAndGet()));
      MqttConnectionOptions persistent = options(false, 60);
      subscriber.connect(persistent);
      subscriber.subscribe(topic, 2);
      subscriber.disconnect();

      connect(publisher, true, 0);
      for (int qos = 0; qos <= 2; qos++) {
        publisher.publish(topic, ("offline-" + qos).getBytes(StandardCharsets.UTF_8), qos, false);
      }

      subscriber.connect(persistent);
      WaitForState.waitFor(5, TimeUnit.SECONDS, () -> received.get() >= 2);
      assertEquals(2, received.get(), "Only QoS 1/2 messages should survive an offline session");
    }
  }

  @Test
  @ProtocolRequirement(specification = "MQTT-5.0", value = "Section 4.7.3 Topic Filter matching", source = ProtocolRequirement.MQTT_5_SOURCE)
  void overlappingSubscriptionsUsePermittedDeliverySemantics() throws Exception {
    String prefix = topic("overlap");
    String exact = prefix + "/value";
    List<Integer> qosValues = new ArrayList<>();

    try (CloseableMqtt5Client client = client()) {
      client.setCallback(callback((name, message) -> qosValues.add(message.getQos())));
      connect(client, true, 0);
      client.subscribe(new MqttSubscription[]{new MqttSubscription(prefix + "/#", 2), new MqttSubscription(exact, 1)});
      client.publish(exact, "overlap".getBytes(StandardCharsets.UTF_8), 2, false);

      WaitForState.waitFor(5, TimeUnit.SECONDS, () -> !qosValues.isEmpty());
      Thread.sleep(150);
      assertTrue(qosValues.size() == 1 || qosValues.size() == 2);
      assertTrue(qosValues.contains(2));
    }
  }

  @Test
  @ProtocolRequirement(specification = "MQTT-5.0", value = "MQTT-4.7.2-1", source = ProtocolRequirement.MQTT_5_SOURCE)
  void rootWildcardDoesNotMatchDollarPrefixedTopic() throws Exception {
    AtomicInteger received = new AtomicInteger();
    String systemTopic = "$maps/conformance/" + UUID.randomUUID();

    try (CloseableMqtt5Client client = client()) {
      client.setCallback(callback((name, message) -> received.incrementAndGet()));
      connect(client, true, 0);
      client.subscribe("#", 1);
      client.publish(systemTopic, "system".getBytes(StandardCharsets.UTF_8), 1, false);

      Thread.sleep(500);
      assertEquals(0, received.get());
    }
  }

  @Test
  @ProtocolRequirement(specification = "MQTT-5.0", value = "Section 3.10 UNSUBSCRIBE", source = ProtocolRequirement.MQTT_5_SOURCE)
  void unsubscribeStopsFurtherDelivery() throws Exception {
    String topic = topic("unsubscribe");
    AtomicInteger received = new AtomicInteger();

    try (CloseableMqtt5Client client = client()) {
      client.setCallback(callback((name, message) -> received.incrementAndGet()));
      connect(client, true, 0);
      client.subscribe(topic, 1);
      client.unsubscribe(topic);
      client.publish(topic, "after-unsubscribe".getBytes(StandardCharsets.UTF_8), 1, false);

      Thread.sleep(500);
      assertEquals(0, received.get());
    }
  }

  @Test
  @ProtocolRequirement(specification = "MQTT-5.0", value = "Section 3.1.2.11 Session Expiry Interval", source = ProtocolRequirement.MQTT_5_SOURCE)
  void zeroSessionExpiryDoesNotRestoreSession() throws Exception {
    String id = "mqtt5-expiry-" + UUID.randomUUID();
    String topic = topic("expiry");

    try (CloseableMqtt5Client client = new CloseableMqtt5Client(URL, id)) {
      IMqttToken first = client.connectWithResult(options(true, 0));
      first.waitForCompletion();
      assertFalse(first.getSessionPresent());
      client.subscribe(topic, 1);
      client.disconnect();

      IMqttToken second = client.connectWithResult(options(false, 0));
      second.waitForCompletion();
      assertFalse(second.getSessionPresent(), "Session Expiry Interval 0 must not restore session state");
    }
  }

  @Test
  @ProtocolRequirement(specification = "MQTT-5.0", value = "Section 2.2.2.3.8 User Property", source = ProtocolRequirement.MQTT_5_SOURCE)
  void publishUserPropertiesRoundTrip() throws Exception {
    String topic = topic("user-properties");
    AtomicReference<MqttMessage> received = new AtomicReference<>();

    try (CloseableMqtt5Client client = client()) {
      client.setCallback(callback((name, message) -> received.set(message)));
      connect(client, true, 0);
      client.subscribe(topic, 2);

      MqttProperties properties = new MqttProperties();
      List<UserProperty> expected = List.of(new UserProperty("a", "2"), new UserProperty("c", "3"));
      properties.setUserProperties(expected);
      MqttMessage message = new MqttMessage("properties".getBytes(StandardCharsets.UTF_8));
      message.setQos(1);
      message.setProperties(properties);
      client.publish(topic, message);

      WaitForState.waitFor(5, TimeUnit.SECONDS, () -> received.get() != null);
      assertNotNull(received.get());
      assertEquals(expected, received.get().getProperties().getUserProperties());
    }
  }

  @Test
  @ProtocolRequirement(specification = "MQTT-5.0", value = "Payload Format Indicator and Content Type", source = ProtocolRequirement.MQTT_5_SOURCE)
  void payloadFormatAndContentTypeRoundTrip() throws Exception {
    String topic = topic("payload-format");
    AtomicReference<MqttMessage> received = new AtomicReference<>();

    try (CloseableMqtt5Client client = client()) {
      client.setCallback(callback((name, message) -> received.set(message)));
      connect(client, true, 0);
      client.subscribe(topic, 2);

      MqttProperties properties = new MqttProperties();
      properties.setPayloadFormat(true);
      properties.setContentType("application/json");
      MqttMessage message = new MqttMessage("{}".getBytes(StandardCharsets.UTF_8));
      message.setQos(1);
      message.setProperties(properties);
      client.publish(topic, message);

      WaitForState.waitFor(5, TimeUnit.SECONDS, () -> received.get() != null);
      assertTrue(received.get().getProperties().getPayloadFormat());
      assertEquals("application/json", received.get().getProperties().getContentType());
    }
  }

  @Test
  @ProtocolRequirement(specification = "MQTT-5.0", value = "Section 3.1.3.1 Client Identifier", source = ProtocolRequirement.MQTT_5_SOURCE)
  void emptyClientIdReceivesAssignedClientIdentifier() throws Exception {
    try (CloseableMqtt5Client client = new CloseableMqtt5Client(URL, "")) {
      IMqttToken token = client.connectWithResult(options(true, 0));
      token.waitForCompletion();
      MqttProperties response = token.getResponseProperties();

      assertNotNull(response);
      assertNotNull(response.getAssignedClientIdentifier());
      assertFalse(response.getAssignedClientIdentifier().isBlank());
    }
  }

  @Test
  @ProtocolRequirement(specification = "MQTT-5.0", value = "No Local subscription option", source = ProtocolRequirement.MQTT_5_SOURCE)
  void noLocalPreventsSelfDelivery() throws Exception {
    String topic = topic("no-local");
    AtomicInteger received = new AtomicInteger();

    try (CloseableMqtt5Client client = client()) {
      client.setCallback(callback((name, message) -> received.incrementAndGet()));
      connect(client, true, 0);
      MqttSubscription subscription = new MqttSubscription(topic, 1);
      subscription.setNoLocal(true);
      client.subscribe(new MqttSubscription[]{subscription});
      client.publish(topic, "self".getBytes(StandardCharsets.UTF_8), 1, false);

      Thread.sleep(500);
      assertEquals(0, received.get());
    }
  }

  @Test
  @ProtocolRequirement(specification = "MQTT-5.0", value = "Response Topic and Correlation Data", source = ProtocolRequirement.MQTT_5_SOURCE)
  void requestResponsePropertiesRoundTrip() throws Exception {
    String requestTopic = topic("request");
    String responseTopic = topic("response");
    byte[] correlation = new byte[]{3, 3, 4};
    AtomicReference<MqttMessage> request = new AtomicReference<>();

    try (CloseableMqtt5Client requester = client();
         CloseableMqtt5Client responder = client()) {
      connect(requester, true, 0);
      connect(responder, true, 0);
      responder.setCallback(callback((name, message) -> request.set(message)));
      responder.subscribe(requestTopic, 1);

      MqttProperties properties = new MqttProperties();
      properties.setResponseTopic(responseTopic);
      properties.setCorrelationData(correlation);
      MqttMessage message = new MqttMessage("request".getBytes(StandardCharsets.UTF_8));
      message.setQos(1);
      message.setProperties(properties);
      requester.publish(requestTopic, message);

      WaitForState.waitFor(5, TimeUnit.SECONDS, () -> request.get() != null);
      assertEquals(responseTopic, request.get().getProperties().getResponseTopic());
      assertArrayEquals(correlation, request.get().getProperties().getCorrelationData());
    }
  }

  @Test
  @ProtocolRequirement(specification = "MQTT-5.0", value = "Subscription Identifier", source = ProtocolRequirement.MQTT_5_SOURCE)
  void subscriptionIdentifierIsReturnedOnMatchingPublish() throws Exception {
    String topic = topic("subscription-id");
    AtomicReference<MqttMessage> received = new AtomicReference<>();

    MqttAsyncClient client = new MqttAsyncClient(URL, "mqtt5-subid-" + UUID.randomUUID(), new MemoryPersistence());
    try {
      client.setCallback(callback((name, message) -> received.set(message)));
      IMqttToken connect = client.connect(options(true, 0));
      connect.waitForCompletion(5000);

      MqttSubscription subscription = new MqttSubscription(topic, 1);
      MqttProperties subscribeProperties = new MqttProperties();
      subscribeProperties.setSubscriptionIdentifier(456789);
      IMqttMessageListener listener = (name, message) -> received.set(message);
      client.subscribe(
          new MqttSubscription[]{subscription},
          null,
          null,
          new IMqttMessageListener[]{listener},
          subscribeProperties).waitForCompletion(5000);

      client.publish(topic, "sub-id".getBytes(StandardCharsets.UTF_8), 1, false).waitForCompletion(5000);

      WaitForState.waitFor(5, TimeUnit.SECONDS, () -> received.get() != null);
      assertTrue(received.get().getProperties().getSubscriptionIdentifiers().contains(456789));
    } finally {
      if (client.isConnected()) {
        client.disconnect().waitForCompletion(5000);
      }
      client.close();
    }
  }

  @Test
  @ProtocolRequirement(specification = "MQTT-5.0", value = "Section 4.8 Shared Subscriptions", source = ProtocolRequirement.MQTT_5_SOURCE)
  void sharedSubscriptionDistributesEachMessageOnce() throws Exception {
    String publishTopic = topic("shared");
    String sharedFilter = "$share/conformance/" + publishTopic;
    AtomicInteger first = new AtomicInteger();
    AtomicInteger second = new AtomicInteger();
    int messageCount = 12;

    try (CloseableMqtt5Client client1 = client();
         CloseableMqtt5Client client2 = client();
         CloseableMqtt5Client publisher = client()) {
      client1.setCallback(callback((name, message) -> first.incrementAndGet()));
      client2.setCallback(callback((name, message) -> second.incrementAndGet()));
      connect(client1, true, 0);
      connect(client2, true, 0);
      connect(publisher, true, 0);
      client1.subscribe(sharedFilter, 1);
      client2.subscribe(sharedFilter, 1);

      for (int i = 0; i < messageCount; i++) {
        publisher.publish(publishTopic, ("shared-" + i).getBytes(StandardCharsets.UTF_8), 1, false);
      }

      WaitForState.waitFor(5, TimeUnit.SECONDS, () -> first.get() + second.get() == messageCount);
      assertEquals(messageCount, first.get() + second.get());
    }
  }

  @Test
  @ProtocolRequirement(specification = "MQTT-5.0", value = "Section 3.9 SUBACK", source = ProtocolRequirement.MQTT_5_SOURCE)
  void unauthorisedSubscriptionReturnsFailureReason() throws Exception {
    try (MqttWireClient client = new MqttWireClient("localhost", 1883)) {
      client.send(MqttWireClient.connect5("mqtt5-denied-" + UUID.randomUUID(), true));
      assertEquals(0, client.readPacket().body()[1] & 0xff);

      client.send(MqttWireClient.subscribe5(177, "test/nosubscribe", 2));
      MqttWireClient.WirePacket subAck = client.readPacket();

      assertEquals(9, subAck.type());
      assertEquals(177, MqttWireClient.unsignedShort(subAck.body(), 0));
      assertTrue(subAck.body().length >= 4);
      assertTrue((subAck.body()[3] & 0xff) >= 0x80, "SUBACK reason must indicate failure");
    }
  }

  private CloseableMqtt5Client client() throws Exception {
    return new CloseableMqtt5Client(URL, "mqtt5-" + UUID.randomUUID());
  }

  private MqttConnectionOptions options(boolean cleanStart, long sessionExpiry) {
    return new MqttConnectionOptionsBuilder()
        .cleanStart(cleanStart)
        .sessionExpiryInterval(sessionExpiry)
        .connectionTimeout(5)
        .keepAliveInterval(30)
        .build();
  }

  private void connect(MqttClient client, boolean cleanStart, long sessionExpiry) throws Exception {
    client.connect(options(cleanStart, sessionExpiry));
  }

  private String topic(String suffix) {
    return "conformance/mqtt5/" + suffix + "/" + UUID.randomUUID();
  }

  private MqttCallback callback(MessageHandler handler) {
    return new MqttCallback() {
      @Override
      public void disconnected(MqttDisconnectResponse response) {
      }

      @Override
      public void mqttErrorOccurred(MqttException exception) {
      }

      @Override
      public void messageArrived(String topic, MqttMessage message) throws Exception {
        handler.onMessage(topic, message);
      }

      @Override
      public void deliveryComplete(IMqttToken token) {
      }

      @Override
      public void connectComplete(boolean reconnect, String serverURI) {
      }

      @Override
      public void authPacketArrived(int reasonCode, MqttProperties properties) {
      }
    };
  }

  @FunctionalInterface
  private interface MessageHandler {
    void onMessage(String topic, MqttMessage message) throws Exception;
  }
}
