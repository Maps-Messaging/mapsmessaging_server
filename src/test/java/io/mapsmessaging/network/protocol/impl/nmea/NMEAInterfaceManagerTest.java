package io.mapsmessaging.network.protocol.impl.nmea;

import io.mapsmessaging.dto.rest.config.network.EndPointConfigDTO;
import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.Selectable;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NMEAInterfaceManagerTest {

  @Test
  void packetWithoutSourceAddressIsIgnoredWithoutConsumingData() throws Exception {
    EndPoint endpoint = endpoint();
    NMEAInterfaceManager manager = new NMEAInterfaceManager(endpoint);
    Packet packet = new Packet(16, false);
    packet.put(new byte[]{1, 2, 3});
    packet.flip();

    assertTrue(manager.processPacket(packet));

    assertEquals(0, packet.position());
    verify(endpoint, times(1)).register(anyInt(), any(Selectable.class));
  }

  @Test
  void addressedPacketIsInspectedWithoutChangingCallerPositionAndReadIsRearmed() throws Exception {
    EndPoint endpoint = endpoint();
    NMEAInterfaceManager manager = new NMEAInterfaceManager(endpoint);
    Packet packet = new Packet(16, false);
    packet.put(new byte[]{4, 5, 6});
    packet.flip();
    packet.setFromAddress(new InetSocketAddress("127.0.0.1", 10110));

    assertTrue(manager.processPacket(packet));

    assertEquals(0, packet.position());
    verify(endpoint, times(2)).register(anyInt(), any(Selectable.class));
  }

  @Test
  void exposesStableProtocolMetadata() throws Exception {
    EndPoint endpoint = endpoint();
    NMEAInterfaceManager manager = new NMEAInterfaceManager(endpoint);

    assertEquals("NMEA-0183", manager.getName());
    assertEquals("1.0", manager.getVersion());
    assertEquals("", manager.getSessionId());
    assertSame(endpoint, manager.getEndPoint());
    assertDoesNotThrow(manager::close);
  }

  private static EndPoint endpoint() throws Exception {
    EndPoint endpoint = mock(EndPoint.class);
    EndPointServerConfigDTO serverConfig = mock(EndPointServerConfigDTO.class);
    EndPointConfigDTO endPointConfig = new EndPointConfigDTO("tcp");
    when(endpoint.getConfig()).thenReturn(serverConfig);
    when(serverConfig.getEndPointConfig()).thenReturn(endPointConfig);
    when(serverConfig.getProtocolConfig("NMEA-0183")).thenReturn(null);
    when(endpoint.isUDP()).thenReturn(false);
    when(endpoint.register(anyInt(), any(Selectable.class))).thenReturn(null);
    return endpoint;
  }
}
