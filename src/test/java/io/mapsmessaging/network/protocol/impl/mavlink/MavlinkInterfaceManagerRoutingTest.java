/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.network.protocol.impl.mavlink;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.mapsmessaging.config.protocol.impl.MavlinkConfig;
import io.mapsmessaging.dto.rest.config.protocol.impl.MavlinkAcceptedSourceDTO;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.impl.SelectorTask;
import io.mapsmessaging.network.io.impl.udp.UDPFacadeEndPoint;
import io.mapsmessaging.network.io.impl.udp.session.UDPSessionState;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.MockedConstruction;

class MavlinkInterfaceManagerRoutingTest {

  @ParameterizedTest
  @CsvSource({
      "0, 7, false, true, false, false",
      "42, 0, false, true, false, false",
      "43, 7, false, true, false, true",
      "43, 7, true, true, false, false",
      "42, 7, false, true, true, true",
      "42, 7, true, true, true, false",
      "42, 7, false, false, false, false"
  })
  void routes_only_valid_frames_and_suppresses_forward_loops(
      int systemId, int componentId, boolean forwardedSource, boolean hasContext,
      boolean processed, boolean forwarded) throws Exception {
    EndPoint endpoint = endpoint();
    MavlinkProtocol protocol = mock(MavlinkProtocol.class);
    UDPSessionState<MavlinkProtocol> state = new UDPSessionState<>(hasContext ? protocol : null);
    try (MockedConstruction<SelectorTask> selectors = mockConstruction(SelectorTask.class);
         MockedConstruction<MavLinkSessionManager> sessions = mockConstruction(
             MavLinkSessionManager.class, (mock, context) -> when(mock.getState(any())).thenReturn(state))) {
      MavlinkInterfaceManager manager = spy(new MavlinkInterfaceManager(endpoint));
      byte[] frame = frame(systemId, componentId);
      Packet packet = packet(frame, forwardedSource);
      int originalPosition = packet.position();
      try {
        assertTrue(manager.processPacket(packet));
        assertEquals(originalPosition, packet.position());
        verify(manager).writeTlog(frame);
        verify(protocol, times(processed ? 1 : 0)).processRawFrame(frame, packet.getFromAddress().toString());
        verify(endpoint, times(forwarded ? 1 : 0)).sendPacket(any(Packet.class));
        if (forwarded) {
          ArgumentCaptor<Packet> sent = ArgumentCaptor.forClass(Packet.class);
          verify(endpoint).sendPacket(sent.capture());
          assertEquals(new InetSocketAddress("127.0.0.1", 14551), sent.getValue().getFromAddress());
          byte[] bytes = new byte[sent.getValue().available()];
          sent.getValue().get(bytes);
          assertArrayEquals(frame, bytes);
        }
        verify(sessions.constructed().getFirst(), times(systemId == 42 && componentId != 0 ? 1 : 0))
            .getState(any());
        verify(selectors.constructed().getFirst(), times(2)).register(SelectionKey.OP_READ);
        if (processed && forwarded) {
          InOrder order = inOrder(manager, protocol, endpoint);
          order.verify(manager).writeTlog(frame);
          order.verify(protocol).processRawFrame(frame, packet.getFromAddress().toString());
          order.verify(endpoint).sendPacket(any(Packet.class));
        }
      } finally {
        manager.close();
      }
    }
  }

  @Test
  void session_creation_failure_closes_facade_without_forwarding() throws Exception {
    EndPoint endpoint = endpoint();
    try (MockedConstruction<SelectorTask> selectors = mockConstruction(SelectorTask.class);
         MockedConstruction<MavLinkSessionManager> sessions = mockConstruction(MavLinkSessionManager.class);
         MockedConstruction<UDPFacadeEndPoint> facades = mockConstruction(UDPFacadeEndPoint.class);
         MockedConstruction<MavlinkProtocol> protocols = mockConstruction(
             MavlinkProtocol.class, (mock, context) -> { throw new IOException("setup failed"); })) {
      MavlinkInterfaceManager manager = spy(new MavlinkInterfaceManager(endpoint));
      byte[] frame = frame(42, 7);
      try {
        assertTrue(manager.processPacket(packet(frame, false)));
        verify(manager).writeTlog(frame);
        verify(facades.constructed().getFirst()).close();
        verify(endpoint, never()).sendPacket(any());
        verify(sessions.constructed().getFirst(), never()).addState(any(), any());
        verify(selectors.constructed().getFirst(), times(2)).register(SelectionKey.OP_READ);
      } finally {
        manager.close();
      }
    }
  }

  @Test
  void processing_failure_prevents_forwarding_but_rearms_selector() throws Exception {
    EndPoint endpoint = endpoint();
    MavlinkProtocol protocol = mock(MavlinkProtocol.class);
    doThrow(new IOException("processing failed")).when(protocol).processRawFrame(any(), anyString());
    UDPSessionState<MavlinkProtocol> state = new UDPSessionState<>(protocol);
    try (MockedConstruction<SelectorTask> selectors = mockConstruction(SelectorTask.class);
         MockedConstruction<MavLinkSessionManager> sessions = mockConstruction(
             MavLinkSessionManager.class, (mock, context) -> when(mock.getState(any())).thenReturn(state))) {
      MavlinkInterfaceManager manager = spy(new MavlinkInterfaceManager(endpoint));
      byte[] frame = frame(42, 7);
      try {
        assertThrows(IOException.class, () -> manager.processPacket(packet(frame, false)));
        verify(manager).writeTlog(frame);
        verify(endpoint, never()).sendPacket(any());
        verify(selectors.constructed().getFirst(), times(2)).register(SelectionKey.OP_READ);
      } finally {
        manager.close();
      }
    }
  }

  private static EndPoint endpoint() {
    EndPoint endpoint = mock(EndPoint.class, RETURNS_DEEP_STUBS);
    MavlinkConfig config = mock(MavlinkConfig.class);
    MavlinkAcceptedSourceDTO source = new MavlinkAcceptedSourceDTO();
    source.setSystemId(42);
    when(config.getAcceptedSources()).thenReturn(List.of(source));
    when(config.getForwardUrls()).thenReturn("udp://127.0.0.1:14551");
    when(endpoint.getConfig().getProtocolConfig("mavlink")).thenReturn(config);
    return endpoint;
  }

  private static byte[] frame(int systemId, int componentId) {
    return new byte[]{(byte) 0xfe, 0, 1, (byte) systemId, (byte) componentId, 0, 0, 0};
  }

  private static Packet packet(byte[] frame, boolean forwardedSource) {
    Packet packet = new Packet(ByteBuffer.wrap(frame));
    packet.setFromAddress(new InetSocketAddress("127.0.0.1", forwardedSource ? 14551 : 14550));
    return packet;
  }
}
