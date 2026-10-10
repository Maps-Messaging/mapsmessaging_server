/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0;

import static org.junit.jupiter.api.Assertions.*;

import io.mapsmessaging.mqttsn.ConnectOptions;
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
