/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 */

package io.mapsmessaging.network.io.impl.hmac;

import io.mapsmessaging.config.network.impl.UdpConfig;
import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import io.mapsmessaging.network.io.EndPointServer;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.impl.Selector;
import io.mapsmessaging.network.io.security.NodeSecurity;
import io.mapsmessaging.network.io.security.PacketIntegrity;
import io.mapsmessaging.network.io.security.VerificationResult;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class HmacUdpBufferLifecycleTest {

  @Nested
  class HappyPath {

    @Test
    void validDatagramReturnsPacketInStandardEndpointWriteMode() throws Exception {
      Map<String, NodeSecurity> security = new HashMap<>();
      try (Harness harness = new Harness(security);
           DatagramChannel sender = DatagramChannel.open()) {

        sender.bind(new InetSocketAddress("127.0.0.1", 0));
        InetSocketAddress senderAddress = (InetSocketAddress) sender.getLocalAddress();

        PacketIntegrity integrity = mock(PacketIntegrity.class);
        when(integrity.verify(any(Packet.class))).thenAnswer(invocation -> {
          Packet packet = invocation.getArgument(0);
          return VerificationResult.ok("test", 0, packet.limit(), packet.position(), packet.available());
        });
        security.put(senderAddress.getAddress().getHostAddress() + ":" + senderAddress.getPort(),
            new NodeSecurity(senderAddress.getAddress().getHostAddress(), senderAddress.getPort(), integrity));

        sender.send(ByteBuffer.wrap(new byte[]{1, 2, 3, 4}), harness.localAddress());

        Packet packet = new Packet(32, false);
        int read = assertTimeoutPreemptively(Duration.ofSeconds(1), () -> {
          int count;
          do {
            count = harness.endpoint.readPacket(packet);
            if (count == 0) {
              Thread.onSpinWait();
            }
          } while (count == 0);
          return count;
        });

        assertEquals(4, read);
        assertEquals(4, packet.position(),
            "endpoint read contract requires position to follow bytes written before UDPReadTask flips");
        assertEquals(packet.capacity(), packet.limit(),
            "endpoint read contract requires write mode on return");
        assertEquals(senderAddress, packet.getFromAddress());
      }
    }

    @Test
    void validOutboundPacketIsSecuredAndConsumedByUdpSend() throws Exception {
      Map<String, NodeSecurity> security = new HashMap<>();
      try (Harness harness = new Harness(security)) {
        InetSocketAddress destination = new InetSocketAddress("127.0.0.1", 19001);

        PacketIntegrity integrity = mock(PacketIntegrity.class);
        when(integrity.secure(any(Packet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        security.put(destination.getAddress().getHostAddress() + ":" + destination.getPort(),
            new NodeSecurity(destination.getAddress().getHostAddress(), destination.getPort(), integrity));

        Packet packet = new Packet(ByteBuffer.wrap(new byte[]{5, 6, 7}));
        packet.setFromAddress(destination);

        assertEquals(3, harness.endpoint.sendPacket(packet));
        assertFalse(packet.hasRemaining());
        verify(integrity).secure(packet);
      }
    }
  }

  @Nested
  class SadPath {

    @Test
    void unknownOutboundPeerDropsPacketWithoutLeavingStaleReadableBytes() throws Exception {
      try (Harness harness = new Harness(new HashMap<>())) {
        Packet packet = new Packet(ByteBuffer.wrap(new byte[]{1, 2, 3}));
        packet.setFromAddress(new InetSocketAddress("127.0.0.1", 19002));

        assertEquals(0, harness.endpoint.sendPacket(packet));
        assertEquals(0, packet.position());
        assertEquals(packet.capacity(), packet.limit());
      }
    }

    @Test
    void invalidInboundDatagramIsClearedAndReportedAsZeroBytes() throws Exception {
      Map<String, NodeSecurity> security = new HashMap<>();
      try (Harness harness = new Harness(security);
           DatagramChannel sender = DatagramChannel.open()) {

        sender.bind(new InetSocketAddress("127.0.0.1", 0));
        InetSocketAddress senderAddress = (InetSocketAddress) sender.getLocalAddress();

        PacketIntegrity integrity = mock(PacketIntegrity.class);
        when(integrity.verify(any(Packet.class)))
            .thenReturn(VerificationResult.fail(
                io.mapsmessaging.network.io.security.FailureReason.SIGNATURE_MISMATCH,
                "test", 4, 4, 0, 0));
        security.put(senderAddress.getAddress().getHostAddress() + ":" + senderAddress.getPort(),
            new NodeSecurity(senderAddress.getAddress().getHostAddress(), senderAddress.getPort(), integrity));

        sender.send(ByteBuffer.wrap(new byte[]{9, 9, 9, 9}), harness.localAddress());

        Packet packet = new Packet(32, false);
        int read = assertTimeoutPreemptively(Duration.ofSeconds(1), () -> {
          for (;;) {
            int count = harness.endpoint.readPacket(packet);
            if (packet.getFromAddress() != null) {
              return count;
            }
            Thread.onSpinWait();
          }
        });

        assertEquals(0, read);
        assertEquals(0, packet.position());
        assertEquals(packet.capacity(), packet.limit());
      }
    }
  }

  @Nested
  class Murphy {

    @Test
    void alternatingValidAndInvalidDatagramsNeverLeakPreviousPayload() throws Exception {
      Map<String, NodeSecurity> security = new HashMap<>();
      try (Harness harness = new Harness(security);
           DatagramChannel sender = DatagramChannel.open()) {

        sender.bind(new InetSocketAddress("127.0.0.1", 0));
        InetSocketAddress senderAddress = (InetSocketAddress) sender.getLocalAddress();

        PacketIntegrity integrity = mock(PacketIntegrity.class);
        when(integrity.verify(any(Packet.class)))
            .thenReturn(
                VerificationResult.ok("test", 0, 3, 0, 3),
                VerificationResult.fail(
                    io.mapsmessaging.network.io.security.FailureReason.SIGNATURE_MISMATCH,
                    "test", 0, 2, 0, 2),
                VerificationResult.ok("test", 0, 1, 0, 1));
        security.put(senderAddress.getAddress().getHostAddress() + ":" + senderAddress.getPort(),
            new NodeSecurity(senderAddress.getAddress().getHostAddress(), senderAddress.getPort(), integrity));

        Packet packet = new Packet(32, false);

        sender.send(ByteBuffer.wrap(new byte[]{1, 2, 3}), harness.localAddress());
        assertEquals(3, readDatagram(harness, packet));
        assertEquals(3, packet.position());
        packet.clear();

        sender.send(ByteBuffer.wrap(new byte[]{8, 8}), harness.localAddress());
        assertEquals(0, readDatagram(harness, packet));
        assertEquals(0, packet.position());
        assertEquals(packet.capacity(), packet.limit());

        sender.send(ByteBuffer.wrap(new byte[]{7}), harness.localAddress());
        assertEquals(1, readDatagram(harness, packet));
        assertEquals(1, packet.position());
      }
    }

    private int readDatagram(Harness harness, Packet packet) {
      return assertTimeoutPreemptively(Duration.ofSeconds(1), () -> {
        packet.setFromAddress(null);
        for (;;) {
          int count = harness.endpoint.readPacket(packet);
          if (packet.getFromAddress() != null) {
            return count;
          }
          Thread.onSpinWait();
        }
      });
    }
  }

  private static final class Harness implements AutoCloseable {
    private final HmacUDPEndPoint endpoint;

    private Harness(Map<String, NodeSecurity> security) throws Exception {
      Selector selector = mock(Selector.class);
      EndPointServer server = mock(EndPointServer.class);
      EndPointServerConfigDTO config = new EndPointServerConfigDTO();
      UdpConfig udpConfig = new UdpConfig();
      udpConfig.setHmacHostLookupCacheExpiry(60);
      config.setEndPointConfig(udpConfig);
      when(server.getConfig()).thenReturn(config);

      endpoint = new HmacUDPEndPoint(
          new InetSocketAddress("127.0.0.1", 0),
          selector,
          1L,
          server,
          null,
          null,
          security);
    }

    private InetSocketAddress localAddress() throws Exception {
      Field field = io.mapsmessaging.network.io.impl.udp.UDPEndPoint.class.getDeclaredField("datagramChannel");
      field.setAccessible(true);
      DatagramChannel channel = (DatagramChannel) field.get(endpoint);
      return (InetSocketAddress) channel.getLocalAddress();
    }

    @Override
    public void close() throws Exception {
      endpoint.close();
    }
  }
}
