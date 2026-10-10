/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0;

import static org.junit.jupiter.api.Assertions.*;

import io.mapsmessaging.mqttsn.ConnectOptions;
import io.mapsmessaging.mqttsn.PubWosPacket;
import io.mapsmessaging.mqttsn.DisconnectOptions;
import io.mapsmessaging.mqttsn.DecodedPacket;
import io.mapsmessaging.mqttsn.MqttSnCodec;
import io.mapsmessaging.mqttsn.PacketType;
import io.mapsmessaging.mqttsn.PublishOptions;
import io.mapsmessaging.mqttsn.QoS;
import io.mapsmessaging.mqttsn.SubscribeOptions;
import io.mapsmessaging.mqttsn.TopicRef;
import io.mapsmessaging.mqttsn.udp.UdpMqttSnClient;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.BaseMqttSnConfig;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Real UDP clients against the MAPS listener started by BaseTestConfig.
 * No server codecs, protocol instances or mocked broker routing are used.
 */
class MqttSn2LiveUdpClientInteropTest extends BaseMqttSnConfig {
  private static final InetSocketAddress MAPS = new InetSocketAddress("127.0.0.1", 1884);
  private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(5);

  @Test
  void referenceClientConnectsAndReceivesConnackOverRunningUdpListener() throws Exception {
    try (UdpMqttSnClient client = new UdpMqttSnClient(MAPS)) {
      client.send(MqttSnCodec.encodeConnect(new ConnectOptions(
          true, false, false, 1, 60, 0, "v2-udp-" + UUID.randomUUID())));
      assertEquals(PacketType.CONNACK, receive(client).type(),
          "Running MAPS listener must respond to the independent MQTT-SN 2 client");
    }
  }

  @Test
  void independentClientsSubscribeAndPublishThroughRunningMapsBroker() throws Exception {
    String topic = "v2/interop/" + UUID.randomUUID();
    byte[] payload = ("payload-" + UUID.randomUUID()).getBytes(StandardCharsets.UTF_8);
    try (UdpMqttSnClient subscriber = new UdpMqttSnClient(MAPS);
         UdpMqttSnClient publisher = new UdpMqttSnClient(MAPS)) {
      connect(subscriber, "sub-" + UUID.randomUUID());
      connect(publisher, "pub-" + UUID.randomUUID());

      subscriber.send(MqttSnCodec.encodeSubscribe(
          new SubscribeOptions(2, TopicRef.filter(topic), 0, false, QoS.AT_MOST_ONCE, false)));
      assertEquals(PacketType.SUBACK, receive(subscriber).type(),
          "MAPS must acknowledge a real v2 UDP subscription before publishing");

      publisher.send(MqttSnCodec.encodePublish(
          new PublishOptions(QoS.AT_MOST_ONCE, false, false, 0,
              TopicRef.name(topic), payload)));
      DecodedPacket delivered = receive(subscriber);
      assertEquals(PacketType.PUBLISH, delivered.type(), "MAPS must route to the second UDP client");
      assertArrayEquals(payload, MqttSnCodec.decodePublish(delivered).payload());
    }
  }

  @Test
  void pingAndUnsubscribeHaveCorrectResponsesOnRealUdpSession() throws Exception {
    String topic = "v2/interop/unsub/" + UUID.randomUUID();
    try (UdpMqttSnClient client = new UdpMqttSnClient(MAPS)) {
      connect(client, "lifecycle-" + UUID.randomUUID());
      client.send(MqttSnCodec.encodePingReq(3));
      assertEquals(PacketType.PINGRESP, receive(client).type());
      client.send(MqttSnCodec.encodeSubscribe(
          new SubscribeOptions(4, TopicRef.filter(topic), 0, false, QoS.AT_MOST_ONCE, false)));
      assertEquals(PacketType.SUBACK, receive(client).type());
      client.send(MqttSnCodec.encodeUnsubscribe(5, TopicRef.filter(topic)));
      assertEquals(PacketType.UNSUBACK, receive(client).type());
      client.send(MqttSnCodec.encodeDisconnect(new DisconnectOptions(null, null, null, "")));
    }
  }

  @Test
  void qosOnePublisherGetsPubackAndSubscriberGetsUnmodifiedPayload() throws Exception {
    String topic = "v2/interop/qos1/" + UUID.randomUUID();
    byte[] payload = ("qos1-" + UUID.randomUUID()).getBytes(StandardCharsets.UTF_8);
    try (UdpMqttSnClient subscriber = new UdpMqttSnClient(MAPS);
         UdpMqttSnClient publisher = new UdpMqttSnClient(MAPS)) {
      connect(subscriber, "q1-sub-" + UUID.randomUUID());
      connect(publisher, "q1-pub-" + UUID.randomUUID());
      subscriber.send(MqttSnCodec.encodeSubscribe(
          new SubscribeOptions(7, TopicRef.filter(topic), 0, false, QoS.AT_MOST_ONCE, false)));
      assertEquals(PacketType.SUBACK, receive(subscriber).type());
      publisher.send(MqttSnCodec.encodePublish(new PublishOptions(
          QoS.AT_LEAST_ONCE, false, false, 8, TopicRef.name(topic), payload)));
      assertEquals(PacketType.PUBACK, receive(publisher).type());
      var forwarded = receive(subscriber);
      assertEquals(PacketType.PUBLISH, forwarded.type());
      assertArrayEquals(payload, MqttSnCodec.decodePublish(forwarded).payload());
    }
  }

  @Test
  void malformedReferenceClientDatagramDoesNotCreateProtocolSession() throws Exception {
    try (java.net.DatagramSocket socket = new java.net.DatagramSocket()) {
      socket.setSoTimeout(350);
      // Claims a truncated extended packet length and must not produce CONNACK.
      byte[] malformed = {(byte) 0x01, 0, 10, 0x01};
      socket.send(new java.net.DatagramPacket(malformed, malformed.length, MAPS));
      byte[] response = new byte[256];
      assertThrows(SocketTimeoutException.class,
          () -> socket.receive(new java.net.DatagramPacket(response, response.length)));
    }
  }

  @Test
  void cleanStartClientReconnectsWithSameIdentifierUsingNewUdpSocket() throws Exception {
    String identity = "v2-restart-" + UUID.randomUUID();
    try (UdpMqttSnClient first = new UdpMqttSnClient(MAPS)) {
      connect(first, identity);
      first.send(MqttSnCodec.encodeDisconnect(new DisconnectOptions(null, null, null, "")));
    }
    try (UdpMqttSnClient second = new UdpMqttSnClient(MAPS)) {
      connect(second, identity);
      second.send(MqttSnCodec.encodePingReq(14));
      assertEquals(PacketType.PINGRESP, receive(second).type());
    }
  }

  @Test
  void qosTwoCompletesFourStepHandshakeAcrossActualUdpListener() throws Exception {
    String topic = "v2/interop/qos2/" + UUID.randomUUID();
    byte[] payload = ("qos2-" + UUID.randomUUID()).getBytes(StandardCharsets.UTF_8);
    try (UdpMqttSnClient subscriber = new UdpMqttSnClient(MAPS);
         UdpMqttSnClient publisher = new UdpMqttSnClient(MAPS)) {
      connect(subscriber, "q2-sub-" + UUID.randomUUID());
      connect(publisher, "q2-pub-" + UUID.randomUUID());
      subscriber.send(MqttSnCodec.encodeSubscribe(
          new SubscribeOptions(11, TopicRef.filter(topic), 0, false, QoS.AT_MOST_ONCE, false)));
      assertEquals(PacketType.SUBACK, receive(subscriber).type());
      publisher.send(MqttSnCodec.encodePublish(new PublishOptions(
          QoS.EXACTLY_ONCE, false, false, 12, TopicRef.name(topic), payload)));
      var pubrec = receive(publisher);
      assertEquals(PacketType.PUBREC, pubrec.type());
      assertEquals(12, MqttSnCodec.decodeAck(pubrec).packetIdentifier());
      publisher.send(MqttSnCodec.encodeAck(PacketType.PUBREL, 12, null));
      var pubcomp = receive(publisher);
      assertEquals(PacketType.PUBCOMP, pubcomp.type());
      assertEquals(12, MqttSnCodec.decodeAck(pubcomp).packetIdentifier());
      var delivered = receive(subscriber);
      assertEquals(PacketType.PUBLISH, delivered.type());
      assertArrayEquals(payload, MqttSnCodec.decodePublish(delivered).payload());
    }
  }

  @Test
  void sessionlessPubwosFromReferenceClientIsDeliveredThroughBroker() throws Exception {
    String topic = "v2/interop/pubwos/" + UUID.randomUUID();
    byte[] payload = ("pubwos-" + UUID.randomUUID()).getBytes(StandardCharsets.UTF_8);
    try (UdpMqttSnClient subscriber = new UdpMqttSnClient(MAPS);
         java.net.DatagramSocket sessionless = new java.net.DatagramSocket()) {
      connect(subscriber, "wos-sub-" + UUID.randomUUID());
      subscriber.send(MqttSnCodec.encodeSubscribe(
          new SubscribeOptions(9, TopicRef.filter(topic), 0, false, QoS.AT_MOST_ONCE, false)));
      assertEquals(PacketType.SUBACK, receive(subscriber).type());
      byte[] packet = MqttSnCodec.encodePubWos(
          new PubWosPacket(false, TopicRef.name(topic), payload));
      sessionless.send(new java.net.DatagramPacket(packet, packet.length, MAPS));
      var forwarded = receive(subscriber);
      assertEquals(PacketType.PUBLISH, forwarded.type());
      assertArrayEquals(payload, MqttSnCodec.decodePublish(forwarded).payload());
    }
  }

  private static void connect(UdpMqttSnClient client, String name) throws Exception {
    client.send(MqttSnCodec.encodeConnect(new ConnectOptions(
        true, false, false, 1, 60, 0, name)));
    assertEquals(PacketType.CONNACK, receive(client).type());
  }

  private static DecodedPacket receive(UdpMqttSnClient client) throws Exception {
    List<DecodedPacket> packets = new ArrayList<>();
    client.receive(RESPONSE_TIMEOUT, packets::add);
    assertEquals(1, packets.size(), "Expected one complete MQTT-SN UDP packet");
    return packets.get(0);
  }
}
