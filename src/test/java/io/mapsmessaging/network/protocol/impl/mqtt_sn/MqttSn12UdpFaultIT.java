/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn;

import io.mapsmessaging.test.network.UdpFaultProxy;
import io.mapsmessaging.test.network.UdpFaultProxy.Direction;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Real MQTT-SN 1.2 wire exchanges under deterministic UDP loss and corruption. */
class MqttSn12UdpFaultIT extends BaseMqttSnConfig {

  @Test
  void lostConnectIsRetriedWithoutCreatingAnInvalidSession() throws Exception {
    try (UdpFaultProxy proxy = new UdpFaultProxy(0, new InetSocketAddress("127.0.0.1", 1884));
         DatagramSocket socket = new DatagramSocket()) {
      socket.setSoTimeout(2_000);
      byte[] connect = connectPacket("sn12-loss-" + System.nanoTime());
      proxy.dropNext(Direction.CLIENT_TO_SERVER, 1);
      send(socket, proxy, connect);
      assertThrows(SocketTimeoutException.class, () -> receive(socket));
      assertEquals(1L, proxy.stats().dropped(), "The first CONNECT must actually be lost");
      send(socket, proxy, connect);
      byte[] reply = receive(socket);
      assertEquals(0x05, reply[1] & 0xff, "Recovered CONNECT must receive CONNACK");
      assertEquals(0x00, reply[2] & 0xff, "CONNACK must report accepted");
      assertEquals(0, proxy.stats().pendingRules());
    }
  }

  @Test
  void malformedConnectDoesNotPreventNextValidConnect() throws Exception {
    try (UdpFaultProxy proxy = new UdpFaultProxy(0, new InetSocketAddress("127.0.0.1", 1884));
         DatagramSocket socket = new DatagramSocket()) {
      socket.setSoTimeout(600);
      byte[] connect = connectPacket("sn12-corrupt-" + System.nanoTime());
      proxy.reverseRangeNext(Direction.CLIENT_TO_SERVER, 1, 0, 4);
      send(socket, proxy, connect);
      assertThrows(SocketTimeoutException.class, () -> receive(socket),
          "Malformed CONNECT must not be acknowledged");
      assertEquals(1L, proxy.stats().modified(), "The corrupted datagram must pass through the proxy");
      send(socket, proxy, connect);
      assertEquals(0x05, receive(socket)[1] & 0xff, "Valid CONNECT must still work");
      assertEquals(0, proxy.stats().pendingRules());
    }
  }

  private static byte[] connectPacket(String name) {
    byte[] identity = name.getBytes(StandardCharsets.UTF_8);
    byte[] result = new byte[6 + identity.length];
    result[0] = (byte) result.length;
    result[1] = 0x04;
    result[2] = 0x04;
    result[3] = 0x01;
    result[4] = 0x00;
    result[5] = 0x3c;
    System.arraycopy(identity, 0, result, 6, identity.length);
    return result;
  }

  private static void send(DatagramSocket socket, UdpFaultProxy proxy, byte[] payload) throws Exception {
    socket.send(new DatagramPacket(payload, payload.length, proxy.getListenAddress()));
  }

  private static byte[] receive(DatagramSocket socket) throws Exception {
    byte[] bytes = new byte[1_024];
    DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
    socket.receive(packet);
    return Arrays.copyOf(bytes, packet.getLength());
  }
}
