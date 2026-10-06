/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.conformance.mqtt5;

import io.mapsmessaging.network.protocol.conformance.common.CloseableMqtt5Client;
import io.mapsmessaging.network.protocol.conformance.common.MqttWireClient;
import io.mapsmessaging.network.protocol.conformance.common.MqttWireClient.WirePacket;
import io.mapsmessaging.network.protocol.conformance.common.ProtocolRequirement;
import io.mapsmessaging.test.BaseTestConfig;
import io.mapsmessaging.test.WaitForState;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptionsBuilder;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.MqttSubscription;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("conformance")
@Tag("conformance-full")
@Tag("mqtt5")
class Mqtt5FullConformanceTest extends BaseTestConfig {

  private static final String URL = "tcp://localhost:1883";

  @Test
  @ProtocolRequirement(
      specification = "MQTT-5.0",
      value = "Section 3.2.2.3.14 Server Keep Alive [MQTT-3.2.2-21]",
      source = ProtocolRequirement.MQTT_5_SOURCE)
  void serverKeepAliveOverridesLargerClientValue() throws Exception {
    try (CloseableMqtt5Client client = client()) {
      MqttConnectionOptions options = new MqttConnectionOptionsBuilder()
          .cleanStart(true)
          .sessionExpiryInterval(0L)
          .keepAliveInterval(120)
          .build();

      IMqttToken token = client.connectWithResult(options);
      token.waitForCompletion();

      assertNotNull(token.getResponseProperties());
      assertEquals(60, token.getResponseProperties().getServerKeepAlive());
    }
  }

  @Test
  @ProtocolRequirement(
      specification = "MQTT-5.0",
      value = "Section 3.1.3.2.2 Will Delay Interval",
      source = ProtocolRequirement.MQTT_5_SOURCE)
  void willDelayDefersPublicationUntilDelayExpires() throws Exception {
    String willTopic = topic("will-delay");
    byte[] expected = "delayed-will".getBytes(StandardCharsets.UTF_8);
    AtomicReference<byte[]> received = new AtomicReference<>();

    try (CloseableMqtt5Client subscriber = client()) {
      subscriber.setCallback(callback((name, message) -> received.set(message.getPayload())));
      subscriber.connect(options(true, 0));
      subscriber.subscribe(willTopic, 1);

      long closedAt;
      try (MqttWireClient willClient = new MqttWireClient("localhost", 1883)) {
        willClient.send(MqttWireClient.connect5WithWill(
            "mqtt5-will-" + UUID.randomUUID(),
            30,
            5,
            2,
            willTopic,
            expected));
        WirePacket connAck = willClient.readPacket();
        assertEquals(2, connAck.type(), connAck::toString);
        assertEquals(0, connAck.body()[1] & 0xff);
        closedAt = System.nanoTime();
      }

      WaitForState.waitFor(5, TimeUnit.SECONDS, () -> received.get() != null);
      long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - closedAt);
      assertArrayEquals(expected, received.get());
      assertTrue(elapsedMillis >= 1_500, "Will was published before the configured delay: " + elapsedMillis + " ms");
      assertTrue(elapsedMillis < 5_000, "Will was not published before session expiry: " + elapsedMillis + " ms");
    }
  }

  @Test
  @ProtocolRequirement(
      specification = "MQTT-5.0",
      value = "Section 4.4 Message delivery retry",
      source = ProtocolRequirement.MQTT_5_SOURCE)
  void unacknowledgedQosOnePublishIsRedeliveredWithDupAfterReconnect() throws Exception {
    String clientId = "mqtt5-redelivery-" + UUID.randomUUID();
    String topic = topic("redelivery");
    byte[] expiry60 = sessionExpiryProperty(60);

    try (MqttWireClient subscriber = new MqttWireClient("localhost", 1883);
         CloseableMqtt5Client publisher = client()) {
      subscriber.send(MqttWireClient.connect5(clientId, true, 30, expiry60));
      WirePacket connAck = subscriber.readPacket();
      assertEquals(0, connAck.body()[1] & 0xff);

      subscriber.send(MqttWireClient.subscribe5(201, topic, 1));
      assertEquals(9, subscriber.readPacket().type());

      publisher.connect(options(true, 0));
      publisher.publish(topic, "redeliver".getBytes(StandardCharsets.UTF_8), 1, false);

      WirePacket first = subscriber.readPacket();
      assertEquals(3, first.type(), first::toString);
      assertEquals(0, first.fixedHeader() & 0x08, "Initial delivery must not set DUP");
      // Intentionally close without PUBACK.
    }

    try (MqttWireClient reconnected = new MqttWireClient("localhost", 1883)) {
      reconnected.send(MqttWireClient.connect5(clientId, false, 30, expiry60));
      WirePacket connAck = reconnected.readPacket();
      assertEquals(0, connAck.body()[1] & 0xff);
      assertEquals(1, connAck.body()[0] & 0x01, "Session Present must be set");

      WirePacket redelivery = reconnected.readPacket();
      assertEquals(3, redelivery.type(), redelivery::toString);
      assertNotEquals(0, redelivery.fixedHeader() & 0x08, "Redelivery must set DUP");
      reconnected.send(MqttWireClient.pubAck(MqttWireClient.publishPacketIdentifier(redelivery)));
    }

    try (MqttWireClient cleanup = new MqttWireClient("localhost", 1883)) {
      cleanup.send(MqttWireClient.connect5(clientId, true, 30, sessionExpiryProperty(0)));
      assertEquals(0, cleanup.readPacket().body()[1] & 0xff);
      cleanup.send(MqttWireClient.disconnect());
    }
  }

  @Test
  @ProtocolRequirement(
      specification = "MQTT-5.0",
      value = "Section 3.3.2.3.3 Message Expiry Interval [MQTT-3.3.2-6]",
      source = ProtocolRequirement.MQTT_5_SOURCE)
  void expiredOfflinePublicationIsNotDelivered() throws Exception {
    String clientId = "mqtt5-expiry-full-" + UUID.randomUUID();
    String topic = topic("publication-expiry");
    AtomicReference<String> received = new AtomicReference<>();

    try (CloseableMqtt5Client subscriber = new CloseableMqtt5Client(URL, clientId);
         CloseableMqtt5Client publisher = client()) {
      subscriber.setCallback(callback((name, message) ->
          received.set(new String(message.getPayload(), StandardCharsets.UTF_8))));
      subscriber.connect(options(true, 60));
      subscriber.subscribe(topic, 1);
      subscriber.disconnect();

      publisher.connect(options(true, 0));

      MqttProperties shortExpiry = new MqttProperties();
      shortExpiry.setMessageExpiryInterval(1L);
      MqttMessage expired = new MqttMessage("expired".getBytes(StandardCharsets.UTF_8));
      expired.setQos(1);
      expired.setProperties(shortExpiry);
      publisher.publish(topic, expired);

      MqttProperties longExpiry = new MqttProperties();
      longExpiry.setMessageExpiryInterval(10L);
      MqttMessage live = new MqttMessage("live".getBytes(StandardCharsets.UTF_8));
      live.setQos(1);
      live.setProperties(longExpiry);
      publisher.publish(topic, live);

      Thread.sleep(2_000);
      subscriber.connect(options(false, 0));

      WaitForState.waitFor(5, TimeUnit.SECONDS, () -> received.get() != null);
      assertEquals("live", received.get());
    }
  }

  @Test
  @ProtocolRequirement(
      specification = "MQTT-5.0",
      value = "Section 3.1.2.11.4 Maximum Packet Size",
      source = ProtocolRequirement.MQTT_5_SOURCE)
  void serverDoesNotSendPacketLargerThanClientMaximum() throws Exception {
    String topic = "m5/max";
    AtomicInteger received = new AtomicInteger();

    try (CloseableMqtt5Client subscriber = client();
         CloseableMqtt5Client publisher = client()) {
      subscriber.setCallback(callback((name, message) -> received.incrementAndGet()));
      MqttConnectionOptions limited = new MqttConnectionOptionsBuilder()
          .cleanStart(true)
          .sessionExpiryInterval(0L)
          .maximumPacketSize(64L)
          .build();
      subscriber.connect(limited);
      subscriber.subscribe(topic, 1);

      publisher.connect(options(true, 0));
      publisher.publish(topic, new byte[16], 0, false);
      WaitForState.waitFor(3, TimeUnit.SECONDS, () -> received.get() == 1);
      assertEquals(1, received.get());

      publisher.publish(topic, new byte[128], 1, false);
      Thread.sleep(500);
      assertEquals(1, received.get(), "Server must not send a packet exceeding the client's Maximum Packet Size");
    }
  }

  @Test
  @ProtocolRequirement(
      specification = "MQTT-5.0",
      value = "Section 3.1.2.11.3 Receive Maximum",
      source = ProtocolRequirement.MQTT_5_SOURCE)
  void clientReceiveMaximumLimitsOutstandingServerPublishes() throws Exception {
    String topic = topic("receive-maximum");
    byte[] receiveMaximumTwo = new byte[]{0x21, 0x00, 0x02};

    try (MqttWireClient subscriber = new MqttWireClient("localhost", 1883);
         CloseableMqtt5Client publisher = client()) {
      subscriber.send(MqttWireClient.connect5(
          "mqtt5-receive-max-" + UUID.randomUUID(),
          true,
          30,
          receiveMaximumTwo));
      assertEquals(0, subscriber.readPacket().body()[1] & 0xff);
      subscriber.send(MqttWireClient.subscribe5(301, topic, 1));
      assertEquals(9, subscriber.readPacket().type());

      publisher.connect(options(true, 0));
      for (int i = 0; i < 3; i++) {
        publisher.publish(topic, ("flow-" + i).getBytes(StandardCharsets.UTF_8), 1, false);
      }

      WirePacket first = subscriber.readPacket();
      WirePacket second = subscriber.readPacket();
      assertEquals(3, first.type());
      assertEquals(3, second.type());

      subscriber.setReadTimeoutMillis(500);
      assertThrows(SocketTimeoutException.class, subscriber::readPacket,
          "Third PUBLISH must wait while two QoS 1 deliveries remain unacknowledged");

      subscriber.send(MqttWireClient.pubAck(MqttWireClient.publishPacketIdentifier(first)));
      subscriber.setReadTimeoutMillis(5_000);
      WirePacket third = subscriber.readPacket();
      assertEquals(3, third.type());

      subscriber.send(MqttWireClient.pubAck(MqttWireClient.publishPacketIdentifier(second)));
      subscriber.send(MqttWireClient.pubAck(MqttWireClient.publishPacketIdentifier(third)));
    }
  }

  @Test
  @ProtocolRequirement(
      specification = "MQTT-5.0",
      value = "Section 3.1.2.11.3 Receive Maximum and DISCONNECT 0x93",
      source = ProtocolRequirement.MQTT_5_SOURCE)
  void exceedingServerReceiveMaximumDisconnectsWithReason93() throws Exception {
    try (MqttWireClient client = new MqttWireClient("localhost", 1883)) {
      client.send(MqttWireClient.connect5("mqtt5-server-rx-" + UUID.randomUUID(), true));
      assertEquals(0, client.readPacket().body()[1] & 0xff);

      for (int packetId = 1; packetId <= 1_024; packetId++) {
        client.send(MqttWireClient.publishQos2_5(
            packetId,
            "conformance/mqtt5/server-receive-maximum",
            new byte[]{1}));
        WirePacket pubRec = client.readPacket();
        assertEquals(5, pubRec.type(), "Expected PUBREC for packet " + packetId);
      }

      client.send(MqttWireClient.publishQos2_5(
          1_025,
          "conformance/mqtt5/server-receive-maximum",
          new byte[]{1}));
      WirePacket disconnect = client.readPacket();
      assertEquals(14, disconnect.type(), disconnect::toString);
      assertTrue(disconnect.body().length >= 1);
      assertEquals(0x93, disconnect.body()[0] & 0xff, "Receive Maximum exceeded reason code");
    }
  }

  @Test
  @ProtocolRequirement(
      specification = "MQTT-5.0",
      value = "Section 3.3.2.3.4 Topic Alias [MQTT-3.3.2-8]",
      source = ProtocolRequirement.MQTT_5_SOURCE)
  void topicAliasZeroCausesTopicAliasInvalidDisconnect() throws Exception {
    byte[] aliasZero = new byte[]{0x23, 0x00, 0x00};

    try (MqttWireClient client = new MqttWireClient("localhost", 1883)) {
      client.send(MqttWireClient.connect5("mqtt5-alias-zero-" + UUID.randomUUID(), true));
      assertEquals(0, client.readPacket().body()[1] & 0xff);

      client.send(MqttWireClient.publishQos1_5(
          401,
          "conformance/mqtt5/alias-zero",
          aliasZero,
          new byte[]{1}));

      WirePacket disconnect = client.readPacket();
      assertEquals(14, disconnect.type(), disconnect::toString);
      assertTrue(disconnect.body().length >= 1);
      assertEquals(0x94, disconnect.body()[0] & 0xff, "Topic Alias invalid reason code");
    }
  }

  @Test
  @ProtocolRequirement(
      specification = "MQTT-5.0",
      value = "Section 3.8.3.1 Retain As Published",
      source = ProtocolRequirement.MQTT_5_SOURCE)
  void retainAsPublishedPreservesRetainFlagForLiveDelivery() throws Exception {
    String topic = topic("retain-as-published");
    AtomicReference<MqttMessage> received = new AtomicReference<>();

    try (CloseableMqtt5Client subscriber = client();
         CloseableMqtt5Client publisher = client()) {
      subscriber.setCallback(callback((name, message) -> received.set(message)));
      subscriber.connect(options(true, 0));
      MqttSubscription subscription = new MqttSubscription(topic, 1);
      subscription.setRetainAsPublished(true);
      subscriber.subscribe(new MqttSubscription[]{subscription});

      publisher.connect(options(true, 0));
      MqttMessage retained = new MqttMessage("retained-live".getBytes(StandardCharsets.UTF_8));
      retained.setQos(1);
      retained.setRetained(true);
      publisher.publish(topic, retained);

      WaitForState.waitFor(5, TimeUnit.SECONDS, () -> received.get() != null);
      assertTrue(received.get().isRetained());

      MqttMessage clear = new MqttMessage(new byte[0]);
      clear.setQos(1);
      clear.setRetained(true);
      publisher.publish(topic, clear);
    }
  }

  @Test
  @ProtocolRequirement(
      specification = "MQTT-5.0",
      value = "Section 3.8.3.1 Retain Handling",
      source = ProtocolRequirement.MQTT_5_SOURCE)
  void retainHandlingTwoSuppressesExistingRetainedMessage() throws Exception {
    String topic = topic("retain-handling");
    AtomicInteger received = new AtomicInteger();

    try (CloseableMqtt5Client publisher = client();
         CloseableMqtt5Client subscriber = client()) {
      publisher.connect(options(true, 0));
      MqttMessage retained = new MqttMessage("stored".getBytes(StandardCharsets.UTF_8));
      retained.setQos(1);
      retained.setRetained(true);
      publisher.publish(topic, retained);

      subscriber.setCallback(callback((name, message) -> received.incrementAndGet()));
      subscriber.connect(options(true, 0));
      MqttSubscription subscription = new MqttSubscription(topic, 1);
      subscription.setRetainHandling(2);
      subscriber.subscribe(new MqttSubscription[]{subscription});

      Thread.sleep(500);
      assertEquals(0, received.get(), "Retain Handling 2 must not send retained messages at subscribe time");

      MqttMessage clear = new MqttMessage(new byte[0]);
      clear.setQos(1);
      clear.setRetained(true);
      publisher.publish(topic, clear);
    }
  }

  @Test
  @ProtocolRequirement(
      specification = "MQTT-5.0",
      value = "Section 4.3.3 QoS 2 delivery: PUBLISH/PUBREC/PUBREL/PUBCOMP",
      source = ProtocolRequirement.MQTT_5_SOURCE)
  void outboundQosTwoCompletesFourStepExchange() throws Exception {
    String topic = topic("qos2-out");

    try (MqttWireClient subscriber = new MqttWireClient("localhost", 1883);
         CloseableMqtt5Client publisher = client()) {
      subscriber.send(MqttWireClient.connect5("mqtt5-qos2-sub-" + UUID.randomUUID(), true));
      assertEquals(0, subscriber.readPacket().body()[1] & 0xff);

      subscriber.send(MqttWireClient.subscribe5(501, topic, 2));
      WirePacket subAck = subscriber.readPacket();
      assertEquals(9, subAck.type(), subAck::toString);
      assertEquals(2, subAck.body()[3] & 0xff);

      publisher.connect(options(true, 0));
      publisher.publish(topic, "qos2".getBytes(StandardCharsets.UTF_8), 2, false);

      WirePacket publish = subscriber.readPacket();
      assertEquals(3, publish.type(), publish::toString);
      assertEquals(2, (publish.fixedHeader() >>> 1) & 0x03);
      int packetId = MqttWireClient.publishPacketIdentifier(publish);

      subscriber.send(MqttWireClient.pubRec(packetId));
      WirePacket pubRel = subscriber.readPacket();
      assertEquals(6, pubRel.type(), pubRel::toString);
      assertEquals(0x02, pubRel.flags());
      assertEquals(packetId, MqttWireClient.unsignedShort(pubRel.body(), 0));

      subscriber.send(MqttWireClient.pubComp(packetId));
    }
  }

  private CloseableMqtt5Client client() throws Exception {
    return new CloseableMqtt5Client(URL, "mqtt5-full-" + UUID.randomUUID());
  }

  private MqttConnectionOptions options(boolean cleanStart, long sessionExpiry) {
    return new MqttConnectionOptionsBuilder()
        .cleanStart(cleanStart)
        .sessionExpiryInterval(sessionExpiry)
        .connectionTimeout(5)
        .keepAliveInterval(30)
        .build();
  }

  private String topic(String suffix) {
    return "conformance/mqtt5/full/" + suffix + "/" + UUID.randomUUID();
  }

  private byte[] sessionExpiryProperty(long seconds) {
    return new byte[]{
        0x11,
        (byte) ((seconds >>> 24) & 0xff),
        (byte) ((seconds >>> 16) & 0xff),
        (byte) ((seconds >>> 8) & 0xff),
        (byte) (seconds & 0xff)
    };
  }

  private MqttCallback callback(MessageHandler handler) {
    return new MqttCallback() {
      @Override public void disconnected(MqttDisconnectResponse response) {}
      @Override public void mqttErrorOccurred(MqttException exception) {}
      @Override public void messageArrived(String topic, MqttMessage message) throws Exception {
        handler.onMessage(topic, message);
      }
      @Override public void deliveryComplete(IMqttToken token) {}
      @Override public void connectComplete(boolean reconnect, String serverURI) {}
      @Override public void authPacketArrived(int reasonCode, MqttProperties properties) {}
    };
  }

  @FunctionalInterface
  private interface MessageHandler {
    void onMessage(String topic, MqttMessage message) throws Exception;
  }
}
