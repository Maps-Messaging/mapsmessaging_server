/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 */

package io.mapsmessaging.network.io.impl.udp;

import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointServer;
import io.mapsmessaging.network.io.Packet;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UDPFacadeEndPointBufferTest {

  @Nested
  class HappyPath {

    @Test
    void sendDelegatesPacketWithoutChangingPeerIdentity() throws Exception {
      Harness harness = new Harness(12001);

      Packet packet = new Packet(ByteBuffer.wrap(new byte[]{1, 2, 3}));
      packet.setFromAddress(harness.peer);

      when(harness.physical.sendPacket(any(Packet.class))).thenAnswer(invocation -> {
        Packet delegated = invocation.getArgument(0);
        assertSame(packet, delegated);
        assertEquals(harness.peer, delegated.getFromAddress());
        int count = delegated.available();
        delegated.position(delegated.limit());
        return count;
      });

      assertEquals(3, harness.facade.sendPacket(packet));
      assertFalse(packet.hasRemaining());
    }

    @Test
    void peerAddressAndNameAreStablePerFacade() {
      Harness a = new Harness(12002);
      Harness b = new Harness(12003);

      assertNotEquals(a.facade.getRemoteSocketAddress(), b.facade.getRemoteSocketAddress());
      assertTrue(a.facade.getName().contains("12002"));
      assertTrue(b.facade.getName().contains("12003"));
    }
  }

  @Nested
  class SadPath {

    @Test
    void closedFacadeRejectsWritesWithoutClosingPhysicalEndpoint() throws Exception {
      Harness harness = new Harness(12004);

      harness.facade.close();

      Packet packet = new Packet(ByteBuffer.wrap(new byte[]{1}));
      assertThrows(java.io.IOException.class, () -> harness.facade.sendPacket(packet));
      verify(harness.physical, never()).close();
    }

    @Test
    void closedFacadeReportsEndOfStreamForReads() throws Exception {
      Harness harness = new Harness(12005);
      harness.facade.close();

      assertEquals(-1, harness.facade.readPacket(new Packet(8, false)));
      verify(harness.physical, never()).readPacket(any(Packet.class));
    }
  }

  @Nested
  class Murphy {

    @Test
    void closingOneFacadeDoesNotAffectSiblingFacadeSharingSamePhysicalEndpoint() throws Exception {
      EndPoint physical = mock(EndPoint.class);
      EndPointServer server = mock(EndPointServer.class);
      when(physical.getServer()).thenReturn(server);
      when(physical.getJMXTypePath()).thenReturn(List.of("root", "physical"));
      when(physical.getName()).thenReturn("udp_test");
      when(physical.getProtocol()).thenReturn("udp");
      when(physical.isUDP()).thenReturn(true);

      UDPFacadeEndPoint first = new UDPFacadeEndPoint(
          physical, new InetSocketAddress("127.0.0.1", 12006), server);
      UDPFacadeEndPoint second = new UDPFacadeEndPoint(
          physical, new InetSocketAddress("127.0.0.1", 12007), server);

      first.close();

      Packet packet = new Packet(ByteBuffer.wrap(new byte[]{7, 8}));
      when(physical.sendPacket(any(Packet.class))).thenAnswer(invocation -> {
        Packet delegated = invocation.getArgument(0);
        int count = delegated.available();
        delegated.position(delegated.limit());
        return count;
      });

      assertEquals(2, second.sendPacket(packet));
      verify(physical, never()).close();
    }
  }

  private static final class Harness {
    private final EndPoint physical = mock(EndPoint.class);
    private final EndPointServer server = mock(EndPointServer.class);
    private final InetSocketAddress peer;
    private final UDPFacadeEndPoint facade;

    private Harness(int port) {
      peer = new InetSocketAddress("127.0.0.1", port);
      when(physical.getServer()).thenReturn(server);
      when(physical.getJMXTypePath()).thenReturn(List.of("root", "physical"));
      when(physical.getName()).thenReturn("udp_test");
      when(physical.getProtocol()).thenReturn("udp");
      when(physical.isUDP()).thenReturn(true);
      facade = new UDPFacadeEndPoint(physical, peer, server);
    }
  }
}
