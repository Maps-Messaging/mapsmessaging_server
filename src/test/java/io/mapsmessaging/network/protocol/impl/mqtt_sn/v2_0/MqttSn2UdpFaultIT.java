/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0;

import io.mapsmessaging.mqttsn.ConnectOptions;
import io.mapsmessaging.mqttsn.DecodedPacket;
import io.mapsmessaging.mqttsn.MqttSnCodec;
import io.mapsmessaging.mqttsn.PacketType;
import io.mapsmessaging.mqttsn.udp.UdpMqttSnClient;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.BaseMqttSnConfig;
import io.mapsmessaging.test.network.UdpFaultProxy;
import io.mapsmessaging.test.network.UdpFaultProxy.Direction;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Real v2 reference-client UDP exchanges through the same fault proxy as CoAP. */
class MqttSn2UdpFaultIT extends BaseMqttSnConfig {
  @Test
  void droppedConnectCanBeRetriedByIndependentClient() throws Exception {
    try (UdpFaultProxy proxy = new UdpFaultProxy(0, new InetSocketAddress("127.0.0.1", 1884));
         UdpMqttSnClient client = new UdpMqttSnClient(proxy.getListenAddress())) {
      byte[] connect = MqttSnCodec.encodeConnect(new ConnectOptions(
          true, false, false, 1, 60, 0, "v2-loss-" + UUID.randomUUID()));
      proxy.dropNext(Direction.CLIENT_TO_SERVER, 1);
      client.send(connect);
      // The reference client's read timeout is the observable proof that the first request did not arrive.
      awaitDrop(proxy);
      assertEquals(1L, proxy.stats().dropped(), "First CONNECT must be dropped");
      client.send(connect);
      assertEquals(PacketType.CONNACK, receive(client).type());
      assertEquals(0, proxy.stats().pendingRules());
    }
  }

  @Test
  void lostConnackCanBeRecoveredByResendingSameConnect() throws Exception {
    try (UdpFaultProxy proxy = new UdpFaultProxy(0, new InetSocketAddress("127.0.0.1", 1884));
         UdpMqttSnClient client = new UdpMqttSnClient(proxy.getListenAddress())) {
      byte[] connect = MqttSnCodec.encodeConnect(new ConnectOptions(
          true, false, false, 1, 60, 0, "v2-ack-loss-" + UUID.randomUUID()));
      proxy.dropNext(Direction.SERVER_TO_CLIENT, 1);
      client.send(connect);
      awaitDrop(proxy);
      assertEquals(1L, proxy.stats().dropped(), "First CONNACK must be dropped");
      client.send(connect);
      assertEquals(PacketType.CONNACK, receive(client).type());
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

  private static DecodedPacket receive(UdpMqttSnClient client) throws Exception {
    List<DecodedPacket> packets = new ArrayList<>();
    client.receive(Duration.ofSeconds(5), packets::add);
    assertEquals(1, packets.size(), "Expected a complete MQTT-SN v2 datagram");
    return packets.getFirst();
  }
}
