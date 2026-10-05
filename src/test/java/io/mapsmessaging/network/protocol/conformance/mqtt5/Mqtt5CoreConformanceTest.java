/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.conformance.mqtt5;

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
@Tag("mqtt5")
class Mqtt5CoreConformanceTest extends BaseTestConfig {

  private static final int PORT = 1883;

  @Test
  @ProtocolRequirement(specification = "MQTT-5.0", value = "MQTT-3.2.2-1")
  void connectReturnsSuccessfulConnAck() throws Exception {
    try (MqttWireClient client = new MqttWireClient("localhost", PORT)) {
      client.send(MqttWireClient.connect5(clientId(), true));

      WirePacket connAck = client.readPacket();
      assertSuccessfulConnAck(connAck);

      client.send(MqttWireClient.disconnect());
    }
  }

  @Test
  @ProtocolRequirement(specification = "MQTT-5.0", value = "MQTT-1.5.3-1")
  void connectCanArriveOneByteAtATime() throws Exception {
    try (MqttWireClient client = new MqttWireClient("localhost", PORT)) {
      client.sendByteByByte(MqttWireClient.connect5(clientId(), true));

      assertSuccessfulConnAck(client.readPacket());
      client.send(MqttWireClient.disconnect());
    }
  }

  @Test
  @ProtocolRequirement(specification = "MQTT-5.0", value = "MQTT-3.12.4-1")
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
  @ProtocolRequirement(specification = "MQTT-5.0", value = "MQTT-3.8.4-1")
  void subscribeReturnsSubAckReasonCode() throws Exception {
    int packetId = 141;
    try (MqttWireClient client = connected()) {
      client.send(MqttWireClient.subscribe5(packetId, "/conformance/mqtt5/+", 1));

      WirePacket subAck = client.readPacket();
      assertEquals(9, subAck.type(), subAck::toString);
      assertEquals(packetId, MqttWireClient.unsignedShort(subAck.body(), 0));
      int propertyLength = subAck.body()[2] & 0xff;
      assertEquals(0, propertyLength, "Core conformance request expects no SUBACK properties");
      assertEquals(1, subAck.body()[3] & 0xff, "SUBACK must grant QoS 1");
    }
  }

  @Test
  @ProtocolRequirement(specification = "MQTT-5.0", value = "MQTT-3.11.4-1")
  void unsubscribeReturnsSuccessfulUnsubAck() throws Exception {
    int subscribeId = 151;
    int unsubscribeId = 152;
    String filter = "/conformance/mqtt5/unsubscribe";
    try (MqttWireClient client = connected()) {
      client.send(MqttWireClient.subscribe5(subscribeId, filter, 0));
      assertEquals(9, client.readPacket().type());

      client.send(MqttWireClient.unsubscribe5(unsubscribeId, filter));
      WirePacket unsubAck = client.readPacket();

      assertEquals(11, unsubAck.type(), unsubAck::toString);
      assertEquals(unsubscribeId, MqttWireClient.unsignedShort(unsubAck.body(), 0));
      assertTrue(unsubAck.body().length >= 4, unsubAck::toString);
      assertEquals(0, unsubAck.body()[2] & 0xff, "Core request expects no UNSUBACK properties");
      int reason = unsubAck.body()[3] & 0xff;
      assertTrue(reason == 0x00 || reason == 0x11,
          "UNSUBACK must report Success or No subscription existed, got 0x" + Integer.toHexString(reason));
    }
  }

  @Test
  @ProtocolRequirement(specification = "MQTT-5.0", value = "MQTT-3.4.4-1")
  void qos1PublishReturnsMatchingPubAck() throws Exception {
    int packetId = 161;
    try (MqttWireClient client = connected()) {
      client.send(MqttWireClient.publishQos1_5(
          packetId,
          "/conformance/mqtt5/qos1",
          "payload".getBytes(StandardCharsets.UTF_8)));

      WirePacket pubAck = client.readPacket();
      assertEquals(4, pubAck.type(), pubAck::toString);
      assertEquals(packetId, MqttWireClient.unsignedShort(pubAck.body(), 0));
      if (pubAck.body().length >= 3) {
        assertEquals(0, pubAck.body()[2] & 0xff, "PUBACK reason code must be Success when present");
      }
    }
  }

  @Test
  @ProtocolRequirement(specification = "MQTT-5.0", value = "MQTT-1.5.5-1")
  void variableByteIntegerBoundaryEncodingMatchesSpecification() {
    int[] values = {0, 1, 126, 127, 128, 16_383, 16_384, 2_097_151, 2_097_152, 268_435_455};
    int[] sizes =  {1, 1,   1,   1,   2,      2,      3,         3,         4,           4};

    for (int i = 0; i < values.length; i++) {
      assertEquals(sizes[i], MqttWireClient.encodeVariableByteInteger(values[i]).length,
          "Unexpected VBI size for " + values[i]);
      assertEquals(sizes[i], MqttWireClient.variableByteIntegerSize(values[i]));
    }
  }

  @Test
  @ProtocolRequirement(specification = "MQTT-5.0", value = "MQTT-2.2.2-1")
  void connectAndPingMayShareOneTcpWrite() throws Exception {
    byte[] connect = MqttWireClient.connect5(clientId(), true);
    byte[] ping = MqttWireClient.pingReq();
    byte[] combined = new byte[connect.length + ping.length];
    System.arraycopy(connect, 0, combined, 0, connect.length);
    System.arraycopy(ping, 0, combined, connect.length, ping.length);

    try (MqttWireClient client = new MqttWireClient("localhost", PORT)) {
      client.send(combined);
      assertSuccessfulConnAck(client.readPacket());
      assertEquals(13, client.readPacket().type());
    }
  }

  private MqttWireClient connected() throws Exception {
    MqttWireClient client = new MqttWireClient("localhost", PORT);
    client.send(MqttWireClient.connect5(clientId(), true));
    assertSuccessfulConnAck(client.readPacket());
    return client;
  }

  private void assertSuccessfulConnAck(WirePacket connAck) throws Exception {
    assertEquals(2, connAck.type(), connAck::toString);
    assertEquals(0, connAck.flags());
    assertTrue(connAck.body().length >= 3, connAck::toString);
    assertEquals(0, connAck.body()[1] & 0xff, "CONNACK reason code must be Success");
    int propertyLength = MqttWireClient.readVariableByteInteger(connAck.body(), 2);
    assertTrue(propertyLength >= 0);
  }

  private String clientId() {
    return "maps-conformance-" + UUID.randomUUID();
  }
}
