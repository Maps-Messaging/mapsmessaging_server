/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.conformance.mqtt3;

import io.mapsmessaging.network.protocol.conformance.common.MqttWireClient;
import io.mapsmessaging.network.protocol.conformance.common.MqttWireClient.WirePacket;
import io.mapsmessaging.network.protocol.conformance.common.ProtocolRequirement;
import io.mapsmessaging.test.BaseTestConfig;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("conformance")
@Tag("conformance-core")
@Tag("mqtt311")
class Mqtt311CoreConformanceTest extends BaseTestConfig {

  private static final int PORT = 1883;

  @Test
  @ProtocolRequirement(specification = "MQTT-3.1.1", value = "MQTT-3.1.4-4")
  void connectReturnsAcceptedConnAck() throws Exception {
    try (MqttWireClient client = new MqttWireClient("localhost", PORT)) {
      client.send(MqttWireClient.connect311(clientId(), true));

      WirePacket connAck = client.readPacket();
      assertEquals(2, connAck.type(), connAck::toString);
      assertEquals(0, connAck.flags());
      assertEquals(2, connAck.body().length);
      assertEquals(0, connAck.body()[1] & 0xff, "CONNACK return code must be Connection Accepted");

      client.send(MqttWireClient.disconnect());
    }
  }

  @Test
  @ProtocolRequirement(specification = "MQTT-3.1.1", value = "MQTT-1.5.3-1")
  void connectCanArriveOneByteAtATime() throws Exception {
    try (MqttWireClient client = new MqttWireClient("localhost", PORT)) {
      client.sendByteByByte(MqttWireClient.connect311(clientId(), true));

      WirePacket connAck = client.readPacket();
      assertEquals(2, connAck.type(), connAck::toString);
      assertEquals(0, connAck.body()[1] & 0xff);

      client.send(MqttWireClient.disconnect());
    }
  }

  @Test
  @ProtocolRequirement(specification = "MQTT-3.1.1", value = "MQTT-3.12.4-1")
  void pingReqReturnsPingResp() throws Exception {
    try (MqttWireClient client = connected()) {
      client.send(MqttWireClient.pingReq());

      WirePacket pingResp = client.readPacket();
      assertEquals(13, pingResp.type(), pingResp::toString);
      assertEquals(0, pingResp.flags());
      assertEquals(0, pingResp.body().length);
    }
  }

  @Test
  @ProtocolRequirement(specification = "MQTT-3.1.1", value = "MQTT-3.8.4-1")
  void subscribeReturnsSubAckWithRequestedQos() throws Exception {
    int packetId = 41;
    try (MqttWireClient client = connected()) {
      client.send(MqttWireClient.subscribe311(packetId, "/conformance/mqtt311/+", 1));

      WirePacket subAck = client.readPacket();
      assertEquals(9, subAck.type(), subAck::toString);
      assertEquals(packetId, MqttWireClient.unsignedShort(subAck.body(), 0));
      assertEquals(1, subAck.body()[2] & 0xff, "SUBACK must grant QoS 1");
    }
  }

  @Test
  @ProtocolRequirement(specification = "MQTT-3.1.1", value = "MQTT-3.10.4-4")
  void unsubscribeReturnsMatchingUnsubAck() throws Exception {
    int subscribeId = 51;
    int unsubscribeId = 52;
    String filter = "/conformance/mqtt311/unsubscribe";
    try (MqttWireClient client = connected()) {
      client.send(MqttWireClient.subscribe311(subscribeId, filter, 0));
      assertEquals(9, client.readPacket().type());

      client.send(MqttWireClient.unsubscribe311(unsubscribeId, filter));
      WirePacket unsubAck = client.readPacket();

      assertEquals(11, unsubAck.type(), unsubAck::toString);
      assertEquals(unsubscribeId, MqttWireClient.unsignedShort(unsubAck.body(), 0));
    }
  }

  @Test
  @ProtocolRequirement(specification = "MQTT-3.1.1", value = "MQTT-3.4.4-1")
  void qos1PublishReturnsMatchingPubAck() throws Exception {
    int packetId = 61;
    try (MqttWireClient client = connected()) {
      client.send(MqttWireClient.publishQos1_311(
          packetId,
          "/conformance/mqtt311/qos1",
          "payload".getBytes(StandardCharsets.UTF_8)));

      WirePacket pubAck = client.readPacket();
      assertEquals(4, pubAck.type(), pubAck::toString);
      assertEquals(packetId, MqttWireClient.unsignedShort(pubAck.body(), 0));
    }
  }

  @Test
  @ProtocolRequirement(specification = "MQTT-3.1.1", value = "MQTT-2.2.2-1")
  void connectAndPingMayShareOneTcpWrite() throws Exception {
    byte[] connect = MqttWireClient.connect311(clientId(), true);
    byte[] ping = MqttWireClient.pingReq();
    byte[] combined = new byte[connect.length + ping.length];
    System.arraycopy(connect, 0, combined, 0, connect.length);
    System.arraycopy(ping, 0, combined, connect.length, ping.length);

    try (MqttWireClient client = new MqttWireClient("localhost", PORT)) {
      client.send(combined);

      WirePacket connAck = client.readPacket();
      WirePacket pingResp = client.readPacket();
      assertEquals(2, connAck.type(), connAck::toString);
      assertEquals(0, connAck.body()[1] & 0xff);
      assertEquals(13, pingResp.type(), pingResp::toString);
    }
  }

  private MqttWireClient connected() throws Exception {
    MqttWireClient client = new MqttWireClient("localhost", PORT);
    client.send(MqttWireClient.connect311(clientId(), true));
    WirePacket connAck = client.readPacket();
    assertEquals(2, connAck.type(), connAck::toString);
    assertEquals(0, connAck.body()[1] & 0xff);
    return client;
  }

  private String clientId() {
    return "maps-conformance-" + UUID.randomUUID();
  }
}
