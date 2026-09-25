/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 */

package io.mapsmessaging.network.protocol;

import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import io.mapsmessaging.dto.rest.config.network.impl.TcpConfigDTO;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointServerStatus;
import io.mapsmessaging.network.io.Packet;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProtocolAcceptBufferHandoffTest {

  @Nested
  class HappyPath {

    @Test
    void trailingBytesAfterDetectionArePreservedForProtocolFactory() throws Exception {
      Harness harness = new Harness();
      Packet buffer = new Packet(32, false);
      buffer.put(new byte[]{10, 11, 12, 1, 2, 3, 4});
      buffer.flip();
      buffer.position(3);
      harness.setPacket(buffer);

      AtomicReference<Packet> handedOff = new AtomicReference<>();
      doAnswer(invocation -> {
        handedOff.set(invocation.getArgument(1));
        return null;
      }).when(harness.factory).create(eq(harness.endPoint), any(Packet.class));

      harness.acceptProtocol(new DetectedProtocol(null, harness.factory), 4);

      Packet result = handedOff.get();
      assertNotNull(result);
      assertEquals(0, result.position());
      assertEquals(4, result.limit());
      byte[] bytes = new byte[result.available()];
      result.get(bytes);
      assertArrayEquals(new byte[]{1, 2, 3, 4}, bytes);
    }
  }

  @Nested
  class SadPath {

    @Test
    void zeroTrailingBytesHandsProtocolAnEmptyPacketRatherThanStaleDetectionBytes() throws Exception {
      Harness harness = new Harness();
      Packet buffer = new Packet(16, false);
      buffer.put(new byte[]{10, 11, 12});
      buffer.flip();
      buffer.position(3);
      harness.setPacket(buffer);

      AtomicReference<Packet> handedOff = new AtomicReference<>();
      doAnswer(invocation -> {
        handedOff.set(invocation.getArgument(1));
        return null;
      }).when(harness.factory).create(eq(harness.endPoint), any(Packet.class));

      harness.acceptProtocol(new DetectedProtocol(null, harness.factory), 0);

      assertNotNull(handedOff.get());
      assertFalse(handedOff.get().hasRemaining());
    }
  }

  @Nested
  class Murphy {

    @Test
    void handoffWorksWhenTrailingPayloadExactlyFillsRemainingDetectedBufferSlice() throws Exception {
      Harness harness = new Harness();
      byte[] payload = new byte[13];
      for (int i = 0; i < payload.length; i++) {
        payload[i] = (byte) (i + 1);
      }
      Packet buffer = new Packet(16, false);
      buffer.put(new byte[]{99, 98, 97});
      buffer.put(payload);
      buffer.flip();
      buffer.position(3);
      harness.setPacket(buffer);

      AtomicReference<Packet> handedOff = new AtomicReference<>();
      doAnswer(invocation -> {
        handedOff.set(invocation.getArgument(1));
        return null;
      }).when(harness.factory).create(eq(harness.endPoint), any(Packet.class));

      harness.acceptProtocol(new DetectedProtocol(null, harness.factory), payload.length);

      Packet result = handedOff.get();
      byte[] actual = new byte[result.available()];
      result.get(actual);
      assertArrayEquals(payload, actual);
    }
  }

  private static final class Harness {
    private final EndPoint endPoint = mock(EndPoint.class);
    private final EndPointServerStatus server = mock(EndPointServerStatus.class);
    private final ProtocolImplFactory factory = mock(ProtocolImplFactory.class);
    private final ProtocolAcceptRunner runner;

    private Harness() throws Exception {
      EndPointServerConfigDTO config = new EndPointServerConfigDTO();
      TcpConfigDTO tcp = new TcpConfigDTO();
      tcp.setConnectionTimeout(5000);
      config.setEndPointConfig(tcp);

      when(endPoint.getServer()).thenReturn(server);
      when(server.getConfig()).thenReturn(config);
      when(endPoint.getConfig()).thenReturn(config);
      when(endPoint.getRemoteSocketAddress()).thenReturn("127.0.0.1:1883");
      when(endPoint.register(anyInt(), any())).thenReturn(null);
      when(endPoint.deregister(anyInt())).thenReturn(null);
      when(endPoint.isProxyAllowed()).thenReturn(true);
      when(factory.getName()).thenReturn("test");

      runner = new ProtocolAcceptRunner(endPoint, "mqtt,stomp");
    }

    private void setPacket(Packet packet) throws Exception {
      Field field = ProtocolAcceptRunner.class.getDeclaredField("packet");
      field.setAccessible(true);
      field.set(runner, packet);
    }

    private void acceptProtocol(DetectedProtocol detected, int packetLength) throws Exception {
      Method method = ProtocolAcceptRunner.class.getDeclaredMethod(
          "acceptProtocol", DetectedProtocol.class, int.class);
      method.setAccessible(true);
      method.invoke(runner, detected, packetLength);
    }
  }
}
