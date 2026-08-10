/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 */

package io.mapsmessaging.network.io.impl.udp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointServer;
import io.mapsmessaging.network.io.Packet;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import org.junit.jupiter.api.Test;

class UDPFacadeEndPointTest {

  @Test
  void closeIsLogicalAndDoesNotCloseSharedPhysicalEndpoint() throws Exception {
    EndPoint physical = mock(EndPoint.class);
    EndPointServer server = mock(EndPointServer.class);
    when(physical.getJMXTypePath()).thenReturn(List.of("server", "endpoint"));
    when(physical.getName()).thenReturn("udp-server");
    when(physical.getServer()).thenReturn(server);
    UDPFacadeEndPoint facade = new UDPFacadeEndPoint(physical, new InetSocketAddress("127.0.0.1", 14550), server);

    facade.close();
    facade.close();

    verify(physical, never()).close();
    verify(server, times(1)).handleCloseEndPoint(facade);
  }

  @Test
  void closedFacadeRejectsTrafficWithoutTouchingPhysicalEndpoint() throws Exception {
    EndPoint physical = mock(EndPoint.class);
    EndPointServer server = mock(EndPointServer.class);
    Packet packet = new Packet(32, false);
    when(physical.getJMXTypePath()).thenReturn(List.of("server", "endpoint"));
    when(physical.getName()).thenReturn("udp-server");
    when(physical.getServer()).thenReturn(server);
    when(physical.sendPacket(packet)).thenReturn(7);
    UDPFacadeEndPoint facade = new UDPFacadeEndPoint(physical, new InetSocketAddress("127.0.0.1", 14550), server);

    assertEquals(7, facade.sendPacket(packet));
    facade.close();

    assertThrows(IOException.class, () -> facade.sendPacket(packet));
    assertEquals(-1, facade.readPacket(packet));
    verify(physical, times(1)).sendPacket(packet);
    verify(physical, never()).readPacket(packet);
  }
}
