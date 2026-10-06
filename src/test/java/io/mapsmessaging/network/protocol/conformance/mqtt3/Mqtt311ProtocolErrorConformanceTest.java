package io.mapsmessaging.network.protocol.conformance.mqtt3;

import io.mapsmessaging.network.protocol.conformance.common.MqttWireClient;
import io.mapsmessaging.network.protocol.conformance.common.MqttWireClient.WirePacket;
import io.mapsmessaging.network.protocol.conformance.common.ProtocolRequirement;
import io.mapsmessaging.test.BaseTestConfig;
import java.net.SocketTimeoutException;
import java.util.UUID;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("conformance")
@Tag("conformance-core")
@Tag("mqtt311")
class Mqtt311ProtocolErrorConformanceTest extends BaseTestConfig {

  @Test
  @Disabled("Known conformance gap: MSG-389")
  @ProtocolRequirement(
      specification = "MQTT-3.1.1",
      value = "Section 3.1.2.2 Protocol Level [MQTT-3.1.2-2]",
      source = ProtocolRequirement.MQTT_311_SOURCE)
  void unsupportedProtocolLevelReturnsConnAckOneThenCloses() throws Exception {
    try (MqttWireClient client = new MqttWireClient("localhost", 1883)) {
      client.send(MqttWireClient.connectWithProtocolLevel("mqtt311-bad-level-" + UUID.randomUUID(), 6));
      WirePacket connAck = client.readPacket();
      assertEquals(2, connAck.type());
      assertEquals(1, connAck.body()[1] & 0xff);
      assertPeerClosed(client);
    }
  }

  @Test
  @ProtocolRequirement(
      specification = "MQTT-3.1.1",
      value = "Section 3.1 CONNECT [MQTT-3.1.0-2]",
      source = ProtocolRequirement.MQTT_311_SOURCE)
  void secondConnectOnSameNetworkConnectionIsRejected() throws Exception {
    try (MqttWireClient client = connected()) {
      client.send(MqttWireClient.connect311("mqtt311-second-" + UUID.randomUUID(), true));
      assertPeerClosed(client);
    }
  }

  @Test
  @ProtocolRequirement(
      specification = "MQTT-3.1.1",
      value = "Section 2.2.2 Fixed Header Flags [MQTT-2.2.2-2]",
      source = ProtocolRequirement.MQTT_311_SOURCE)
  void invalidPingReqFlagsCloseConnection() throws Exception {
    try (MqttWireClient client = connected()) {
      client.send(new byte[]{(byte) 0xC1, 0x00});
      assertPeerClosed(client);
    }
  }

  @Test
  @Disabled("Known conformance gap: MSG-388")
  @ProtocolRequirement(
      specification = "MQTT-3.1.1",
      value = "Section 2.3.1 Packet Identifier [MQTT-2.3.1-1]",
      source = ProtocolRequirement.MQTT_311_SOURCE)
  void zeroSubscribePacketIdentifierClosesConnection() throws Exception {
    try (MqttWireClient client = connected()) {
      client.send(MqttWireClient.subscribe311(0, "conformance/mqtt311/zero-id", 0));
      assertPeerClosed(client);
    }
  }

  @Test
  @Disabled("Known conformance gap: MSG-388")
  @ProtocolRequirement(
      specification = "MQTT-3.1.1",
      value = "Section 4.7.1.2 Multi-level wildcard [MQTT-4.7.1-2]",
      source = ProtocolRequirement.MQTT_311_SOURCE)
  void malformedTopicFilterClosesConnection() throws Exception {
    try (MqttWireClient client = connected()) {
      client.send(MqttWireClient.subscribe311(501, "sport/#/ranking", 0));
      assertPeerClosed(client);
    }
  }

  @Test
  @Disabled("Known conformance gap: MSG-390")
  @ProtocolRequirement(
      specification = "MQTT-3.1.1",
      value = "Section 1.5.3 UTF-8 encoded strings",
      source = ProtocolRequirement.MQTT_311_SOURCE)
  void malformedUtf8ClientIdentifierIsRejected() throws Exception {
    try (MqttWireClient client = new MqttWireClient("localhost", 1883)) {
      client.send(MqttWireClient.connectWithRawClientId(4, true, new byte[]{(byte) 0xC3, 0x28}));
      assertPeerClosed(client);
    }
  }


  @Test
  @Disabled("Known conformance gap: MSG-395")
  @ProtocolRequirement(
      specification = "MQTT-3.1.1",
      value = "Section 2.2.3 Remaining Length: maximum four bytes",
      source = ProtocolRequirement.MQTT_311_SOURCE)
  void malformedFiveByteRemainingLengthClosesConnection() throws Exception {
    try (MqttWireClient client = connected()) {
      client.send(new byte[]{(byte) 0xC0, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, 0x00});
      assertPeerClosed(client);
    }
  }

  @Test
  @ProtocolRequirement(
      specification = "MQTT-3.1.1",
      value = "Section 4.7.1 Topic wildcards [MQTT-4.7.1-1]",
      source = ProtocolRequirement.MQTT_311_SOURCE)
  void wildcardInPublishTopicNameClosesConnection() throws Exception {
    byte[] malformedPublish = new byte[]{
        0x32, 0x09,
        0x00, 0x05, 'b', 'a', 'd', '/', '+',
        0x00, 0x01
    };
    try (MqttWireClient client = connected()) {
      client.send(malformedPublish);
      assertPeerClosed(client);
    }
  }

  private MqttWireClient connected() throws Exception {
    MqttWireClient client = new MqttWireClient("localhost", 1883);
    client.send(MqttWireClient.connect311("mqtt311-error-" + UUID.randomUUID(), true));
    WirePacket connAck = client.readPacket();
    assertEquals(2, connAck.type());
    assertEquals(0, connAck.body()[1] & 0xff);
    return client;
  }

  private void assertPeerClosed(MqttWireClient client) throws Exception {
    client.setReadTimeoutMillis(2_000);
    try {
      assertEquals(-1, client.readRawByte(), "Peer must close the MQTT 3.1.1 network connection");
    } catch (SocketTimeoutException timeout) {
      fail("Server did not close the connection after protocol violation");
    }
  }
}
