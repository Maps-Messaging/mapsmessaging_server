/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0;

import io.mapsmessaging.mqttsn.ConnectOptions;
import io.mapsmessaging.mqttsn.DecodedPacket;
import io.mapsmessaging.mqttsn.MqttSnCodec;
import io.mapsmessaging.mqttsn.PacketType;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.BaseMqttSnConfig;
import io.mapsmessaging.test.network.UdpFaultProxy;
import io.mapsmessaging.test.network.UdpFaultProxy.Direction;
import java.net.InetSocketAddress;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Real v2 reference-client UDP exchanges through the same fault proxy as CoAP. */
class MqttSn2UdpFaultIT extends BaseMqttSnConfig {
  @Test
  void droppedConnectCanBeRetriedByIndependentClient() throws Exception {
    try (UdpFaultProxy proxy = new UdpFaultProxy(0, new InetSocketAddress("127.0.0.1", 1884));
         DatagramSocket client = new DatagramSocket()) {
      byte[] connect = MqttSnCodec.encodeConnect(new ConnectOptions(
          true, false, false, 1, 60, 0, "v2-loss-" + UUID.randomUUID()));
      proxy.dropNext(Direction.CLIENT_TO_SERVER, 1);
      send(client, proxy, connect);
      // The reference client's read timeout is the observable proof that the first request did not arrive.
      awaitDrop(proxy);
      assertEquals(1L, proxy.stats().dropped(), "First CONNECT must be dropped");
      send(client, proxy, connect);
      DecodedPacket reply = receive(client);
      assertEquals(PacketType.CONNACK, reply.type());
      assertEquals(0, MqttSnCodec.decodeConnAck(reply).reasonCode());
      assertEquals(0, proxy.stats().pendingRules());
    }
  }

  @Test
  void lostConnackCanBeRecoveredByResendingSameConnect() throws Exception {
    try (UdpFaultProxy proxy = new UdpFaultProxy(0, new InetSocketAddress("127.0.0.1", 1884));
         DatagramSocket client = new DatagramSocket()) {
      byte[] connect = MqttSnCodec.encodeConnect(new ConnectOptions(
          true, false, false, 1, 60, 0, "v2-ack-loss-" + UUID.randomUUID()));
      proxy.dropNext(Direction.SERVER_TO_CLIENT, 1);
      send(client, proxy, connect);
      awaitDrop(proxy);
      assertEquals(1L, proxy.stats().dropped(), "First CONNACK must be dropped");
      send(client, proxy, connect);
      DecodedPacket reply = receive(client);
      assertEquals(PacketType.CONNACK, reply.type());
      assertEquals(0, MqttSnCodec.decodeConnAck(reply).reasonCode());
      assertEquals(0, proxy.stats().pendingRules());
    }
  }

  private static void awaitDrop(UdpFaultProxy proxy) throws InterruptedException {
    long deadline = System.nanoTime() + Duration.ofSeconds(2).toNanos();
    while (proxy.stats().dropped() == 0 && System.nanoTime() < deadline) {
      Thread.sleep(5);
    }
    assertEquals(1L, proxy.stats().dropped(), "Fault must actually be injected before retry");
  }

  private static void send(DatagramSocket client, UdpFaultProxy proxy, byte[] packet) throws Exception {
    client.send(new DatagramPacket(packet, packet.length, proxy.getListenAddress()));
  }

  private static DecodedPacket receive(DatagramSocket client) throws Exception {
    client.setSoTimeout(5_000);
    byte[] bytes = new byte[2_048];
    DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
    client.receive(packet);
    return MqttSnCodec.decode(ByteBuffer.wrap(Arrays.copyOf(bytes, packet.getLength())));
  }
}
