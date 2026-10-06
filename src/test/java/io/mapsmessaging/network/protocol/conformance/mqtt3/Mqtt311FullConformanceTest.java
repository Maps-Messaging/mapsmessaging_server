/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.conformance.mqtt3;

import io.mapsmessaging.network.protocol.conformance.common.CloseableMqtt311Client;
import io.mapsmessaging.network.protocol.conformance.common.MqttWireClient;
import io.mapsmessaging.network.protocol.conformance.common.MqttWireClient.WirePacket;
import io.mapsmessaging.network.protocol.conformance.common.ProtocolRequirement;
import io.mapsmessaging.test.BaseTestConfig;
import io.mapsmessaging.test.WaitForState;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("conformance")
@Tag("conformance-full")
@Tag("mqtt311")
class Mqtt311FullConformanceTest extends BaseTestConfig {

  private static final String URL = "tcp://localhost:1883";

  @Test
  @ProtocolRequirement(specification = "MQTT-3.1.1", value = "Section 3.1.3.1 Client Identifier", source = ProtocolRequirement.MQTT_311_SOURCE)
  void zeroLengthClientIdentifierRequiresCleanSession() throws Exception {
    try (MqttWireClient rejected = new MqttWireClient("localhost", 1883)) {
      rejected.send(MqttWireClient.connect311("", false));
      WirePacket connAck = rejected.readPacket();
      assertEquals(2, connAck.type(), connAck::toString);
      assertEquals(2, connAck.body()[1] & 0xff, "Empty ClientId with Clean Session 0 must be rejected");
    }

    try (MqttWireClient accepted = new MqttWireClient("localhost", 1883)) {
      accepted.send(MqttWireClient.connect311("", true));
      WirePacket connAck = accepted.readPacket();
      assertEquals(2, connAck.type(), connAck::toString);
      assertEquals(0, connAck.body()[1] & 0xff, "Empty ClientId with Clean Session 1 may be accepted");
      accepted.send(MqttWireClient.disconnect());
    }
  }

  @Test
  @ProtocolRequirement(specification = "MQTT-3.1.1", value = "Section 3.1.2.10 Will Message and Section 3.1.2.10 Keep Alive", source = ProtocolRequirement.MQTT_311_SOURCE)
  void keepAliveTimeoutPublishesWill() throws Exception {
    String willTopic = "conformance/mqtt311/will/" + UUID.randomUUID();
    byte[] expected = "keepalive-expiry".getBytes(StandardCharsets.UTF_8);
    AtomicReference<byte[]> received = new AtomicReference<>();

    try (CloseableMqtt311Client subscriber = new CloseableMqtt311Client(URL, "mqtt311-will-sub-" + UUID.randomUUID());
         MqttWireClient idleClient = new MqttWireClient("localhost", 1883)) {
      subscriber.setCallback(callback((topic, message) -> received.set(message.getPayload())));
      MqttConnectOptions options = new MqttConnectOptions();
      options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
      options.setCleanSession(true);
      subscriber.connect(options);
      subscriber.subscribe(willTopic, 1);

      idleClient.send(MqttWireClient.connect311WithWill(
          "mqtt311-will-" + UUID.randomUUID(),
          2,
          willTopic,
          expected));
      WirePacket connAck = idleClient.readPacket();
      assertEquals(0, connAck.body()[1] & 0xff);

      WaitForState.waitFor(7, TimeUnit.SECONDS, () -> received.get() != null);
      assertArrayEquals(expected, received.get(), "Will must be published after keep-alive timeout");
    }
  }

  @Test
  @ProtocolRequirement(specification = "MQTT-3.1.1", value = "Section 4.4 Message delivery retry", source = ProtocolRequirement.MQTT_311_SOURCE)
  void unacknowledgedQosOnePublishIsRedeliveredWithDupAfterReconnect() throws Exception {
    String clientId = "mqtt311-redelivery-" + UUID.randomUUID();
    String topic = "conformance/mqtt311/redelivery/" + UUID.randomUUID();

    try (MqttWireClient subscriber = new MqttWireClient("localhost", 1883);
         CloseableMqtt311Client publisher = new CloseableMqtt311Client(URL, "mqtt311-pub-" + UUID.randomUUID())) {
      subscriber.send(MqttWireClient.connect311(clientId, false));
      WirePacket connAck = subscriber.readPacket();
      assertEquals(0, connAck.body()[1] & 0xff);

      subscriber.send(MqttWireClient.subscribe311(101, topic, 1));
      assertEquals(9, subscriber.readPacket().type());

      MqttConnectOptions pubOptions = new MqttConnectOptions();
      pubOptions.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
      pubOptions.setCleanSession(true);
      publisher.connect(pubOptions);
      publisher.publish(topic, "redeliver".getBytes(StandardCharsets.UTF_8), 1, false);

      WirePacket first = subscriber.readPacket();
      assertEquals(3, first.type(), first::toString);
      assertEquals(0, first.fixedHeader() & 0x08, "First delivery must not set DUP");
      // Intentionally close without PUBACK.
    }

    try (MqttWireClient reconnected = new MqttWireClient("localhost", 1883)) {
      reconnected.send(MqttWireClient.connect311(clientId, false));
      WirePacket connAck = reconnected.readPacket();
      assertEquals(0, connAck.body()[1] & 0xff);
      assertEquals(1, connAck.body()[0] & 0x01, "Persistent session must be present");

      WirePacket redelivery = reconnected.readPacket();
      assertEquals(3, redelivery.type(), redelivery::toString);
      assertNotEquals(0, redelivery.fixedHeader() & 0x08, "Redelivery must set DUP");
      reconnected.send(MqttWireClient.pubAck(MqttWireClient.publishPacketIdentifier(redelivery)));
    }

    // Clear the persistent session created by this conformance case.
    try (MqttWireClient cleanup = new MqttWireClient("localhost", 1883)) {
      cleanup.send(MqttWireClient.connect311(clientId, true));
      assertEquals(0, cleanup.readPacket().body()[1] & 0xff);
      cleanup.send(MqttWireClient.disconnect());
    }
  }

  @Test
  @ProtocolRequirement(
      specification = "MQTT-3.1.1",
      value = "Section 4.3.3 QoS 2 delivery: PUBLISH/PUBREC/PUBREL/PUBCOMP",
      source = ProtocolRequirement.MQTT_311_SOURCE)
  void outboundQosTwoCompletesFourStepExchange() throws Exception {
    String topic = "conformance/mqtt311/qos2-out/" + UUID.randomUUID();

    try (MqttWireClient subscriber = new MqttWireClient("localhost", 1883);
         CloseableMqtt311Client publisher = new CloseableMqtt311Client(URL, "mqtt311-qos2-pub-" + UUID.randomUUID())) {
      subscriber.send(MqttWireClient.connect311("mqtt311-qos2-sub-" + UUID.randomUUID(), true));
      assertEquals(0, subscriber.readPacket().body()[1] & 0xff);

      subscriber.send(MqttWireClient.subscribe311(301, topic, 2));
      WirePacket subAck = subscriber.readPacket();
      assertEquals(9, subAck.type(), subAck::toString);
      assertEquals(2, subAck.body()[2] & 0xff);

      MqttConnectOptions options = new MqttConnectOptions();
      options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
      options.setCleanSession(true);
      publisher.connect(options);
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

  private MqttCallback callback(MessageHandler handler) {
    return new MqttCallback() {
      @Override public void connectionLost(Throwable cause) {}
      @Override public void messageArrived(String topic, MqttMessage message) throws Exception {
        handler.onMessage(topic, message);
      }
      @Override public void deliveryComplete(IMqttDeliveryToken token) {}
    };
  }

  @FunctionalInterface
  private interface MessageHandler {
    void onMessage(String topic, MqttMessage message) throws Exception;
  }
}
