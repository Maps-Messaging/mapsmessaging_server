package io.mapsmessaging.network.protocol.conformance.mqtt5;

import io.mapsmessaging.network.protocol.conformance.common.MqttWireClient;
import io.mapsmessaging.network.protocol.conformance.common.MqttWireClient.WirePacket;
import io.mapsmessaging.network.protocol.conformance.common.ProtocolRequirement;
import io.mapsmessaging.test.BaseTestConfig;
import java.io.EOFException;
import java.net.SocketTimeoutException;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("conformance")
@Tag("conformance-core")
@Tag("mqtt5")
class Mqtt5ProtocolErrorConformanceTest extends BaseTestConfig {

  @Test
  @ProtocolRequirement(
      specification = "MQTT-5.0",
      value = "Section 3.1 CONNECT [MQTT-3.1.0-2]",
      source = ProtocolRequirement.MQTT_5_SOURCE)
  void secondConnectClosesConnectionAsProtocolError() throws Exception {
    try (MqttWireClient client = connected()) {
      client.send(MqttWireClient.connect5("mqtt5-second-" + UUID.randomUUID(), true));
      assertDisconnectOrClose(client, 0x82);
    }
  }

  @Test
  @ProtocolRequirement(
      specification = "MQTT-5.0",
      value = "Section 2.1.3 Fixed Header Flags [MQTT-2.1.3-1]",
      source = ProtocolRequirement.MQTT_5_SOURCE)
  void invalidPingReqFlagsAreMalformedPacket() throws Exception {
    try (MqttWireClient client = connected()) {
      client.send(new byte[]{(byte) 0xC1, 0x00});
      assertDisconnectOrClose(client, 0x81);
    }
  }

  @Test
  @ProtocolRequirement(
      specification = "MQTT-5.0",
      value = "Section 2.2.1 Packet Identifier [MQTT-2.2.1-3]",
      source = ProtocolRequirement.MQTT_5_SOURCE)
  void zeroSubscribePacketIdentifierIsProtocolError() throws Exception {
    try (MqttWireClient client = connected()) {
      client.send(MqttWireClient.subscribe5(0, "conformance/mqtt5/zero-id", 0));
      assertDisconnectOrClose(client, 0x82);
    }
  }

  @Test
  @ProtocolRequirement(
      specification = "MQTT-5.0",
      value = "Section 3.1.2.11.3 Receive Maximum",
      source = ProtocolRequirement.MQTT_5_SOURCE)
  void receiveMaximumZeroInConnectIsProtocolError() throws Exception {
    byte[] receiveMaximumZero = new byte[]{0x21, 0x00, 0x00};
    try (MqttWireClient client = new MqttWireClient("localhost", 1883)) {
      client.send(MqttWireClient.connect5(
          "mqtt5-rx-zero-" + UUID.randomUUID(), true, 30, receiveMaximumZero));
      assertConnAckErrorOrClose(client, 0x82);
    }
  }

  @Test
  @ProtocolRequirement(
      specification = "MQTT-5.0",
      value = "Section 3.1.2.11.3 Receive Maximum: property MUST NOT appear more than once",
      source = ProtocolRequirement.MQTT_5_SOURCE)
  void duplicateReceiveMaximumInConnectIsProtocolError() throws Exception {
    byte[] duplicate = new byte[]{0x21, 0x00, 0x01, 0x21, 0x00, 0x01};
    try (MqttWireClient client = new MqttWireClient("localhost", 1883)) {
      client.send(MqttWireClient.connect5(
          "mqtt5-rx-duplicate-" + UUID.randomUUID(), true, 30, duplicate));
      assertConnAckErrorOrClose(client, 0x82);
    }
  }

  @Test
  @ProtocolRequirement(
      specification = "MQTT-5.0",
      value = "Section 3.8.2.1.2 Subscription Identifier: value 0 is a Protocol Error",
      source = ProtocolRequirement.MQTT_5_SOURCE)
  void subscriptionIdentifierZeroIsProtocolError() throws Exception {
    byte[] subIdZero = new byte[]{0x0B, 0x00};
    try (MqttWireClient client = connected()) {
      client.send(MqttWireClient.subscribe5(
          601, subIdZero, "conformance/mqtt5/sub-id-zero", 0));
      assertDisconnectOrClose(client, 0x82);
    }
  }

  @Test
  @ProtocolRequirement(
      specification = "MQTT-5.0",
      value = "Section 3.8.3.1 Subscription Options: Maximum QoS value 3 is a Protocol Error",
      source = ProtocolRequirement.MQTT_5_SOURCE)
  void subscriptionMaximumQosThreeIsProtocolError() throws Exception {
    try (MqttWireClient client = connected()) {
      client.send(MqttWireClient.subscribe5(
          602, new byte[0], "conformance/mqtt5/qos-three", 0x03));
      assertDisconnectOrClose(client, 0x82);
    }
  }

  @Test
  @ProtocolRequirement(
      specification = "MQTT-5.0",
      value = "Section 4.7.1.2 Multi-level wildcard [MQTT-4.7.1-1]",
      source = ProtocolRequirement.MQTT_5_SOURCE)
  void malformedTopicFilterIsProtocolError() throws Exception {
    try (MqttWireClient client = connected()) {
      client.send(MqttWireClient.subscribe5(603, "sport/#/ranking", 0));
      assertDisconnectOrClose(client, 0x82);
    }
  }

  @Test
  @ProtocolRequirement(
      specification = "MQTT-5.0",
      value = "Section 1.5.4 UTF-8 Encoded String [MQTT-1.5.4-1]",
      source = ProtocolRequirement.MQTT_5_SOURCE)
  void malformedUtf8ClientIdentifierIsMalformedPacket() throws Exception {
    try (MqttWireClient client = new MqttWireClient("localhost", 1883)) {
      client.send(MqttWireClient.connectWithRawClientId(5, true, new byte[]{(byte) 0xC3, 0x28}));
      assertConnAckErrorOrClose(client, 0x81);
    }
  }

  @Test
  @ProtocolRequirement(
      specification = "MQTT-5.0",
      value = "Section 3.3.1 QoS [MQTT-3.3.1-4]",
      source = ProtocolRequirement.MQTT_5_SOURCE)
  void publishWithReservedQosThreeIsMalformedPacket() throws Exception {
    try (MqttWireClient client = connected()) {
      client.send(MqttWireClient.packet(0x36, new byte[]{0x00, 0x01, 'a', 0x00, 0x01, 0x00}));
      assertDisconnectOrClose(client, 0x81);
    }
  }


  @Test
  @ProtocolRequirement(
      specification = "MQTT-5.0",
      value = "Section 1.5.5 Variable Byte Integer [MQTT-1.5.5-1]",
      source = ProtocolRequirement.MQTT_5_SOURCE)
  void nonMinimalRemainingLengthEncodingIsMalformedPacket() throws Exception {
    try (MqttWireClient client = connected()) {
      client.send(new byte[]{(byte) 0xC0, (byte) 0x80, 0x00});
      assertDisconnectOrClose(client, 0x81);
    }
  }

  @Test
  @ProtocolRequirement(
      specification = "MQTT-5.0",
      value = "Section 1.5.5 Variable Byte Integer: maximum four bytes",
      source = ProtocolRequirement.MQTT_5_SOURCE)
  void fiveByteRemainingLengthEncodingIsMalformedPacket() throws Exception {
    try (MqttWireClient client = connected()) {
      client.send(new byte[]{(byte) 0xC0, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, 0x00});
      assertDisconnectOrClose(client, 0x81);
    }
  }

  @Test
  @ProtocolRequirement(
      specification = "MQTT-5.0",
      value = "Section 4.7.1 Topic wildcards [MQTT-4.7.0-1]",
      source = ProtocolRequirement.MQTT_5_SOURCE)
  void wildcardInPublishTopicNameIsRejected() throws Exception {
    try (MqttWireClient client = connected()) {
      client.send(MqttWireClient.publishQos1_5(
          701,
          "bad/+",
          new byte[]{1}));
      assertDisconnectOrClose(client, 0x90);
    }
  }

  @Test
  @ProtocolRequirement(
      specification = "MQTT-5.0",
      value = "Section 3.8.3.1 Subscription Options [MQTT-3.8.3-4]",
      source = ProtocolRequirement.MQTT_5_SOURCE)
  void noLocalOnSharedSubscriptionIsProtocolError() throws Exception {
    try (MqttWireClient client = connected()) {
      client.send(MqttWireClient.subscribe5(
          702,
          new byte[0],
          "$share/conformance/topic",
          0x04));
      assertDisconnectOrClose(client, 0x82);
    }
  }

  @Test
  @ProtocolRequirement(
      specification = "MQTT-5.0",
      value = "Section 3.3.4 Subscription Identifier [MQTT-3.3.4-6]",
      source = ProtocolRequirement.MQTT_5_SOURCE)
  void clientPublishMustNotContainSubscriptionIdentifier() throws Exception {
    byte[] subscriptionIdentifier = new byte[]{0x0B, 0x01};
    try (MqttWireClient client = connected()) {
      client.send(MqttWireClient.publishQos1_5(
          703,
          "conformance/mqtt5/client-sub-id",
          subscriptionIdentifier,
          new byte[]{1}));
      assertDisconnectOrClose(client, 0x82);
    }
  }

  @Test
  @ProtocolRequirement(
      specification = "MQTT-5.0",
      value = "Section 4.3.3 QoS 2 and PUBREC Packet Identifier not found (0x92)",
      source = ProtocolRequirement.MQTT_5_SOURCE)
  void unknownPubRecReturnsPubRelPacketIdentifierNotFound() throws Exception {
    int packetId = 321;
    try (MqttWireClient client = connected()) {
      client.send(new byte[]{
          0x50, 0x02,
          (byte) ((packetId >>> 8) & 0xff),
          (byte) (packetId & 0xff)
      });

      WirePacket pubRel = client.readPacket();
      assertEquals(6, pubRel.type(), pubRel::toString);
      assertEquals(0x02, pubRel.flags());
      assertEquals(packetId, MqttWireClient.unsignedShort(pubRel.body(), 0));
      assertTrue(pubRel.body().length >= 3);
      assertEquals(0x92, pubRel.body()[2] & 0xff);
    }
  }

  private MqttWireClient connected() throws Exception {
    MqttWireClient client = new MqttWireClient("localhost", 1883);
    client.send(MqttWireClient.connect5("mqtt5-error-" + UUID.randomUUID(), true));
    WirePacket connAck = client.readPacket();
    assertEquals(2, connAck.type());
    assertEquals(0, connAck.body()[1] & 0xff);
    return client;
  }

  private void assertConnAckErrorOrClose(MqttWireClient client, int expectedReason) throws Exception {
    client.setReadTimeoutMillis(2_000);
    try {
      WirePacket packet = client.readPacket();
      assertEquals(2, packet.type(), packet::toString);
      assertTrue(packet.body().length >= 2);
      assertEquals(expectedReason, packet.body()[1] & 0xff);
      assertClosedAfterError(client);
    } catch (EOFException closed) {
      // MQTT 5 permits closing without CONNACK for malformed CONNECT handling.
    } catch (SocketTimeoutException timeout) {
      fail("Server neither rejected nor closed malformed CONNECT");
    }
  }

  private void assertDisconnectOrClose(MqttWireClient client, int expectedReason) throws Exception {
    client.setReadTimeoutMillis(2_000);
    try {
      WirePacket packet = client.readPacket();
      assertEquals(14, packet.type(), packet::toString);
      assertTrue(packet.body().length >= 1);
      assertEquals(expectedReason, packet.body()[0] & 0xff);
      assertClosedAfterError(client);
    } catch (EOFException closed) {
      // DISCONNECT is SHOULD for most post-CONNACK protocol errors; close is MUST.
    } catch (SocketTimeoutException timeout) {
      fail("Server did not close the connection after MQTT 5 protocol error");
    }
  }

  private void assertClosedAfterError(MqttWireClient client) throws Exception {
    client.setReadTimeoutMillis(2_000);
    try {
      assertEquals(-1, client.readRawByte(), "Connection must close after an MQTT 5 error response");
    } catch (SocketTimeoutException timeout) {
      fail("Connection remained open after MQTT 5 error response");
    }
  }
}
