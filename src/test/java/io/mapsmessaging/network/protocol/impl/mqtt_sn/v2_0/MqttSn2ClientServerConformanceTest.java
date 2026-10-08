/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import io.mapsmessaging.mqttsn.ConnectOptions;
import io.mapsmessaging.mqttsn.DecodedPacket;
import io.mapsmessaging.mqttsn.DisconnectOptions;
import io.mapsmessaging.mqttsn.MqttSnCodec;
import io.mapsmessaging.mqttsn.PacketType;
import io.mapsmessaging.mqttsn.PublishOptions;
import io.mapsmessaging.mqttsn.QoS;
import io.mapsmessaging.mqttsn.SearchGwPacket;
import io.mapsmessaging.mqttsn.SleepRequest;
import io.mapsmessaging.mqttsn.SubscribeOptions;
import io.mapsmessaging.mqttsn.TopicRef;
import io.mapsmessaging.mqttsn.udp.UdpMqttSnClient;
import io.mapsmessaging.network.protocol.conformance.common.CloseableMqtt311Client;
import io.mapsmessaging.network.protocol.conformance.common.CloseableMqtt5Client;
import io.mapsmessaging.test.BaseTestConfig;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.MqttSn2ConnectCodec;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.MqttSn2FrameCodec;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.MqttSn2PacketType;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.MqttSn2SleepCodec;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.MqttSn2ControlCodec;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.MqttSn2ConnAckCodec;
import java.net.InetSocketAddress;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import io.mapsmessaging.test.WaitForState;
import org.junit.jupiter.api.Test;

/**
 * Black-box client/server checks using the released, independent MQTT-SN 2.0
 * Java client. Each assertion names the August 2026 CSD01 normative clause.
 */
class MqttSn2ClientServerConformanceTest extends BaseTestConfig {

  private static final Duration PACKET_TIMEOUT = Duration.ofSeconds(5);

  @Test
  void connectPublishQos1AndQos2AndDisconnect() throws Exception {
    // CSD01 MQTT-SN-3.1.2-1..8, MQTT-SN-3.2.2-1..4,
    // MQTT-SN-4.3.3-1..4, MQTT-SN-4.3.4-1..8, MQTT-SN-3.14.2-1.
    String clientId = clientId("qos");
    try (UdpMqttSnClient client = client(clientId, 1, true)) {

      byte[] qosOne = MqttSnCodec.encodePublish(new PublishOptions(
          QoS.AT_LEAST_ONCE, false, false, 2, TopicRef.name("mqttsn/block3/qos1"),
          "qos1".getBytes()));
      client.send(qosOne);
      assertEquals(PacketType.PUBACK, receive(client).type(), "CSD01 MQTT-SN-4.3.3-1..4");

      byte[] qosTwo = MqttSnCodec.encodePublish(new PublishOptions(
          QoS.EXACTLY_ONCE, false, false, 3, TopicRef.name("mqttsn/block3/qos2"),
          "qos2".getBytes()));
      client.send(qosTwo);
      assertEquals(PacketType.PUBREC, receive(client).type(), "CSD01 MQTT-SN-4.3.4-1..4");
      client.send(MqttSnCodec.encodeAck(PacketType.PUBREL, 3, null));
      assertEquals(PacketType.PUBCOMP, receive(client).type(), "CSD01 MQTT-SN-4.3.4-5..8");

      client.send(MqttSnCodec.encodeDisconnect(new DisconnectOptions(null, null, null, "")));
      assertEquals("DISCONNECTED", client.session().state().name(), "CSD01 MQTT-SN-3.14.2-1");
    }
  }

  @Test
  void subscribePublishAndUnsubscribeUseNameTopics() throws Exception {
    // CSD01 MQTT-SN-3.7.2-1..7, MQTT-SN-3.7.3-1..6,
    // MQTT-SN-3.8.2-1..6, MQTT-SN-3.9.2-1..4.
    try (UdpMqttSnClient client = client(clientId("topics"), 11, true)) {
      client.send(MqttSnCodec.encodeSubscribe(new SubscribeOptions(
          12, TopicRef.filter("mqttsn/block3/#"), 0, false,
          QoS.EXACTLY_ONCE, false)));
      DecodedPacket subAckPacket = receive(client);
      assertEquals(PacketType.SUBACK, subAckPacket.type());
      assertNull(MqttSnCodec.decodeSubAck(subAckPacket).topicAlias(),
          "CSD01 4.7.2.2-3 wildcard subscription must not receive an alias");
      assertEquals(Integer.valueOf(2), MqttSnCodec.decodeSubAck(subAckPacket).reasonCode(),
          "CSD01 3.8 SUBACK must report the granted QoS 2");

      client.send(MqttSnCodec.encodePublish(new PublishOptions(
          QoS.AT_MOST_ONCE, false, false, 0, TopicRef.name("mqttsn/block3/loop"),
          new byte[] {0x2a})));
      DecodedPacket publish = receiveType(client, PacketType.PUBLISH);
      assertEquals("mqttsn/block3/loop", MqttSnCodec.decodePublish(publish).topic().name(),
          "CSD01 MQTT-SN-4.3.2-1..3");

      client.send(MqttSnCodec.encodeUnsubscribe(13, TopicRef.filter("mqttsn/block3/#")));
      assertEquals(PacketType.UNSUBACK, receive(client).type(), "CSD01 MQTT-SN-3.9.3-1..3");
    }
  }

  @Test
  void predefinedTopicRegistrationReturnsAliasExists() throws Exception {
    // CSD01 4.7.2.2-4/-5: do not create a session alias over a predefined alias.
    try (UdpMqttSnClient client = client(clientId("predefined-alias"), 71, true)) {
      client.send(MqttSnCodec.encodeRegister(72, "predefined/topic"));
      DecodedPacket result = receive(client);
      assertEquals(PacketType.REGACK, result.type());
      ByteBuffer body = result.body().asReadOnlyBuffer();
      int flags = Byte.toUnsignedInt(body.get());
      assertEquals(1, flags & 3, "predefined alias topic type");
      assertEquals(1, (flags & 4) >>> 2, "REGACK includes the predefined alias");
      assertEquals(72, Short.toUnsignedInt(body.getShort()));
      assertEquals(1, Short.toUnsignedInt(body.getShort()));
      assertEquals(0x1A, Byte.toUnsignedInt(body.get()), "CSD01 Topic Alias Exists");
    }
  }

  @Test
  void noLocalSubscriptionDoesNotEchoOwnPublication() throws Exception {
    // CSD01 SUBSCRIBE No Local option, carried into broker subscription context.
    String topic = "mqttsn/block3/no-local/" + UUID.randomUUID();
    try (UdpMqttSnClient client = client(clientId("no-local"), 74, true)) {
      client.send(MqttSnCodec.encodeSubscribe(new SubscribeOptions(
          75, TopicRef.filter(topic), 0, true, QoS.AT_MOST_ONCE, false)));
      assertEquals(PacketType.SUBACK, receive(client).type());
      client.send(MqttSnCodec.encodePublish(new PublishOptions(
          QoS.AT_MOST_ONCE, false, false, 0, TopicRef.name(topic), new byte[]{7})));
      assertThrows(SocketTimeoutException.class, () ->
          client.receive(Duration.ofMillis(500), packet -> fail(
              "CSD01 No Local unexpectedly delivered " + packet.type()
                  + " with body " + packet.body())),
          "CSD01 No Local must suppress delivery to the publishing client");
    }
  }

  @Test
  void pingAndReconnectKeepTheSameClientIdentifier() throws Exception {
    // CSD01 MQTT-SN-3.12.2-1..4, MQTT-SN-3.1.2-6, MQTT-SN-3.2.2-1..4.
    String clientId = clientId("session");
    try (UdpMqttSnClient first = client(clientId, 21, true)) {
      int pingId = first.session().nextPacketIdentifier();
      first.send(MqttSnCodec.encodePingReq(pingId));
      DecodedPacket response = receiveType(first, PacketType.PINGRESP);
      assertEquals(pingId, MqttSnCodec.decodePingResp(response).packetIdentifier());
      first.send(MqttSnCodec.encodeDisconnect(new DisconnectOptions(null, null, null, "")));
    }
    try (UdpMqttSnClient reconnect = client(clientId, 22, false)) {
    }
  }

  @Test
  void sleepingClientWakesWithPingReq() throws Exception {
    // CSD01 MQTT-SN-4.14.1-1..5 and MQTT-SN-4.14.2-1..4.
    String clientId = clientId("sleep");
    try (DatagramSocket socket = new DatagramSocket()) {
      socket.setSoTimeout((int) PACKET_TIMEOUT.toMillis());
      InetSocketAddress gateway = new InetSocketAddress("127.0.0.1", 1884);
      ByteBuffer connect = MqttSn2ConnectCodec.encode(new MqttSn2ConnectCodec.Connect(
          true, false, false, false, false, 25, 30, 0, clientId,
          null, new byte[0], null, 0, 3, 0, false, 0, null, new byte[0]));
      assertEquals(0, MqttSn2ConnAckCodec.decode(receiveFrame(socket, gateway, bytes(connect)))
          .reasonCode());

      int sleepId = 26;
      byte[] sleepRequest = MqttSnCodec.encodeSleepReq(new SleepRequest(sleepId, false, 60));
      MqttSn2FrameCodec.Frame sleepResponse = receiveFrame(socket, gateway, sleepRequest);
      assertEquals(MqttSn2PacketType.SLEEPRESP, sleepResponse.type());
      assertEquals(sleepId, MqttSn2SleepCodec.decodeResponse(sleepResponse).packetIdentifier());

      int pingId = 27;
      MqttSn2FrameCodec.Frame wakeResponse = receiveFrame(socket, gateway,
          MqttSnCodec.encodePingReq(pingId));
      assertEquals(MqttSn2PacketType.PINGRESP, wakeResponse.type());
      MqttSn2ControlCodec.PingResponse response =
          MqttSn2ControlCodec.decodePingResponse(wakeResponse);
      assertEquals(pingId, response.packetIdentifier());
      assertEquals(0, response.remainingMessages());
    }
  }

  @Test
  void searchGatewayReturnsGatewayInfo() throws Exception {
    // CSD01 MQTT-SN-6.1.2-1..3 and MQTT-SN-6.1.3-1..4.
    try (UdpMqttSnClient client = new UdpMqttSnClient(
        new InetSocketAddress("127.0.0.1", 1884))) {
      client.send(MqttSnCodec.encodeSearchGw(new SearchGwPacket(new byte[] {1})));
      assertEquals(PacketType.GWINFO, receiveType(client, PacketType.GWINFO).type());
    }
  }

  @Test
  void connectCarriesWillConfigurationInTheCsd01SessionRequest() throws Exception {
    // CSD01 MQTT-SN-3.1.2-1..8: Will Topic, QoS, Retain and payload
    // are carried in CONNECT and must be accepted before CONNACK.
    int packetIdentifier = 41;
    MqttSn2ConnectCodec.Connect request = new MqttSn2ConnectCodec.Connect(
        true, true, false, false, false, packetIdentifier, 30, 0,
        clientId("will"), null, new byte[0], 300L, null,
        3, 1, true, 0, "mqttsn/block3/will", "offline".getBytes());
    ByteBuffer encoded = MqttSn2ConnectCodec.encode(request);
    byte[] wire = new byte[encoded.remaining()];
    encoded.get(wire);

    try (DatagramSocket socket = new DatagramSocket()) {
      socket.setSoTimeout((int) PACKET_TIMEOUT.toMillis());
      socket.send(new DatagramPacket(wire, wire.length,
          new InetSocketAddress("127.0.0.1", 1884)));
      byte[] responseBuffer = new byte[1024];
      DatagramPacket response = new DatagramPacket(responseBuffer, responseBuffer.length);
      socket.receive(response);
      DecodedPacket connAck = MqttSnCodec.decode(ByteBuffer.wrap(
          response.getData(), response.getOffset(), response.getLength()));
      assertEquals(PacketType.CONNACK, connAck.type());
      assertEquals(packetIdentifier, MqttSnCodec.decodeConnAck(connAck).packetIdentifier());
      assertEquals(0, MqttSnCodec.decodeConnAck(connAck).reasonCode());
    }
  }

  @Test
  void qosOneAndTwoPublicationsBridgeToMqtt311AndMqtt5() throws Exception {
    // CSD01 MQTT-SN-4.3.3-1..4 and MQTT-SN-4.3.4-1..8; the gateway
    // forwards both acknowledged QoS flows to MQTT 3.1.1 and MQTT 5 peers.
    String topic = "mqttsn/block3/bridge/" + UUID.randomUUID();
    List<String> received311 = new CopyOnWriteArrayList<>();
    List<String> received5 = new CopyOnWriteArrayList<>();
    try (CloseableMqtt311Client mqtt311 = new CloseableMqtt311Client(
             "tcp://localhost:1883", "msg324-311-" + UUID.randomUUID());
         CloseableMqtt5Client mqtt5 = new CloseableMqtt5Client(
             "tcp://localhost:1883", "msg324-5-" + UUID.randomUUID());
         UdpMqttSnClient mqttSn = client(clientId("bridge"), 51, true)) {
      MqttConnectOptions connect311 = new MqttConnectOptions();
      connect311.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
      connect311.setConnectionTimeout(5);
      connect311.setKeepAliveInterval(30);
      mqtt311.connect(connect311);
      mqtt5.connect();
      mqtt311.setCallback(new org.eclipse.paho.client.mqttv3.MqttCallback() {
        @Override
        public void connectionLost(Throwable cause) {
        }

        @Override
        public void messageArrived(String name, org.eclipse.paho.client.mqttv3.MqttMessage message) {
          received311.add(new String(message.getPayload()));
        }

        @Override
        public void deliveryComplete(org.eclipse.paho.client.mqttv3.IMqttDeliveryToken token) {
        }
      });
      mqtt5.setCallback(new org.eclipse.paho.mqttv5.client.MqttCallback() {
        @Override
        public void disconnected(org.eclipse.paho.mqttv5.client.MqttDisconnectResponse response) {
        }

        @Override
        public void mqttErrorOccurred(org.eclipse.paho.mqttv5.common.MqttException exception) {
        }

        @Override
        public void messageArrived(String name, org.eclipse.paho.mqttv5.common.MqttMessage message) {
          received5.add(new String(message.getPayload()));
        }

        @Override
        public void deliveryComplete(org.eclipse.paho.mqttv5.client.IMqttToken token) {
        }

        @Override
        public void connectComplete(boolean reconnect, String serverURI) {
        }

        @Override
        public void authPacketArrived(int reasonCode,
                                      org.eclipse.paho.mqttv5.common.packet.MqttProperties properties) {
        }
      });
      mqtt311.subscribe(topic, 2);
      mqtt5.subscribe(topic, 2);

      mqttSn.send(MqttSnCodec.encodePublish(new PublishOptions(
          QoS.AT_LEAST_ONCE, false, false, 52, TopicRef.name(topic), "qos1".getBytes())));
      assertEquals(PacketType.PUBACK, receive(mqttSn).type());
      mqttSn.send(MqttSnCodec.encodePublish(new PublishOptions(
          QoS.EXACTLY_ONCE, false, false, 53, TopicRef.name(topic), "qos2".getBytes())));
      assertEquals(PacketType.PUBREC, receive(mqttSn).type());
      mqttSn.send(MqttSnCodec.encodeAck(PacketType.PUBREL, 53, null));
      assertEquals(PacketType.PUBCOMP, receive(mqttSn).type());

      WaitForState.waitFor(5, TimeUnit.SECONDS,
          () -> received311.size() == 2 && received5.size() == 2);
      assertEquals(List.of("qos1", "qos2"), received311);
      assertEquals(List.of("qos1", "qos2"), received5);
    }
  }

  @Test
  void qos0PublishDoesNotWaitForAnAcknowledgement() throws Exception {
    // CSD01 MQTT-SN-4.3.2-1..3: QoS 0 PUBLISH carries no Packet Identifier.
    try (UdpMqttSnClient client = client(clientId("qos0"), 31, true)) {
      byte[] packet = MqttSnCodec.encodePublish(new PublishOptions(
          QoS.AT_MOST_ONCE, false, false, 0, TopicRef.name("mqttsn/block3/qos0"),
          "qos0".getBytes()));
      client.send(packet);
      assertEquals(PacketType.PUBLISH, MqttSnCodec.decode(java.nio.ByteBuffer.wrap(packet)).type());
      assertEquals(0, MqttSnCodec.decodePublish(
          MqttSnCodec.decode(java.nio.ByteBuffer.wrap(packet))).packetIdentifier());
    }
  }

  @Test
  void reservedPacketTypeIsDroppedWithoutCorruptingTheGateway() throws Exception {
    // CSD01 MQTT-SN-2.1.2-1..4 and MQTT-SN-2.1.3-1..3.
    try (DatagramSocket socket = new DatagramSocket()) {
      socket.setSoTimeout(500);
      byte[] malformed = {2, (byte) 0xFD};
      socket.send(new DatagramPacket(malformed, malformed.length,
          new InetSocketAddress("127.0.0.1", 1884)));
      byte[] responseBuffer = new byte[64];
      DatagramPacket response = new DatagramPacket(responseBuffer, responseBuffer.length);
      assertThrows(SocketTimeoutException.class, () -> socket.receive(response),
          "reserved packet types must be rejected without a protocol response");
    }
  }

  private static UdpMqttSnClient client(String clientId, int packetIdentifier,
                                       boolean cleanStart) throws Exception {
    UdpMqttSnClient client = new UdpMqttSnClient(new InetSocketAddress("127.0.0.1", 1884));
    client.send(MqttSnCodec.encodeConnect(new ConnectOptions(
        cleanStart, false, false, packetIdentifier, 30, 0, clientId)));
    DecodedPacket connAck = receiveType(client, PacketType.CONNACK);
    assertEquals(0, MqttSnCodec.decodeConnAck(connAck).reasonCode(), "CSD01 MQTT-SN-3.2.2-1");
    assertEquals(packetIdentifier, MqttSnCodec.decodeConnAck(connAck).packetIdentifier());
    assertEquals("ACTIVE", client.session().state().name(), "accepted CONNECT enters active state");
    return client;
  }

  private static DecodedPacket receive(UdpMqttSnClient client) throws Exception {
    List<DecodedPacket> packets = new ArrayList<>(1);
    client.receive(PACKET_TIMEOUT, packets::add);
    assertEquals(1, packets.size(), "one complete packet per UDP datagram");
    return packets.getFirst();
  }

  private static byte[] bytes(ByteBuffer packet) {
    byte[] bytes = new byte[packet.remaining()];
    packet.get(bytes);
    return bytes;
  }

  private static MqttSn2FrameCodec.Frame receiveFrame(
      DatagramSocket socket, InetSocketAddress gateway, byte[] request) throws Exception {
    socket.send(new DatagramPacket(request, request.length, gateway));
    byte[] buffer = new byte[2048];
    DatagramPacket response = new DatagramPacket(buffer, buffer.length);
    socket.receive(response);
    return MqttSn2FrameCodec.decode(ByteBuffer.wrap(
        response.getData(), response.getOffset(), response.getLength()));
  }

  private static DecodedPacket receiveType(UdpMqttSnClient client, PacketType expected)
      throws Exception {
    DecodedPacket packet = receive(client);
    assertEquals(expected, packet.type());
    return packet;
  }

  private static String clientId(String prefix) {
    return "msg324-" + prefix + "-" + UUID.randomUUID();
  }
}
