/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 */

package io.mapsmessaging.network.io.impl.dtls.state;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.impl.dtls.DTLSSessionManager;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLSession;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import static javax.net.ssl.SSLEngineResult.HandshakeStatus.NEED_UNWRAP;
import static javax.net.ssl.SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING;
import static javax.net.ssl.SSLEngineResult.Status.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DtlsBufferLifecycleTest {

  @Nested
  class HappyPath {

    @Test
    void oneDatagramDecryptsIntoOneQueuedApplicationPacket() throws Exception {
      Harness harness = new Harness(4096);
      when(harness.engine.unwrap(any(ByteBuffer.class), any(ByteBuffer.class))).thenAnswer(invocation -> {
        ByteBuffer source = invocation.getArgument(0);
        ByteBuffer destination = invocation.getArgument(1);
        int consumed = source.remaining();
        source.position(source.limit());
        destination.put(new byte[]{1, 2, 3, 4});
        return result(OK, NOT_HANDSHAKING, consumed, 4);
      });

      Packet encrypted = packet(10, 11, 12);
      harness.normal.inbound(encrypted);

      Packet application = new Packet(16, false);
      assertEquals(4, harness.stateEngine.read(application));
      application.flip();
      assertArrayEquals(new byte[]{1, 2, 3, 4}, readRemaining(application));
    }

    @Test
    void applicationBufferShouldFollowSslSessionSizing() throws Exception {
      Harness harness = new Harness(4096);
      when(harness.engine.unwrap(any(ByteBuffer.class), any(ByteBuffer.class))).thenAnswer(invocation -> {
        ByteBuffer source = invocation.getArgument(0);
        ByteBuffer destination = invocation.getArgument(1);
        assertTrue(destination.capacity() >= 4096,
            "DTLS application buffer must be sized from SSLEngine session requirements");
        int consumed = source.remaining();
        source.position(source.limit());
        destination.put((byte) 1);
        return result(OK, NOT_HANDSHAKING, consumed, 1);
      });

      harness.normal.inbound(packet(1, 2, 3));
    }
  }

  @Nested
  class SadPath {

    @Test
    void bufferOverflowMustBeRecoveredWithoutDroppingDatagram() throws Exception {
      Harness harness = new Harness(4096);
      AtomicInteger calls = new AtomicInteger();

      when(harness.engine.unwrap(any(ByteBuffer.class), any(ByteBuffer.class))).thenAnswer(invocation -> {
        ByteBuffer source = invocation.getArgument(0);
        ByteBuffer destination = invocation.getArgument(1);
        if (calls.getAndIncrement() == 0) {
          return result(BUFFER_OVERFLOW, NOT_HANDSHAKING, 0, 0);
        }
        int consumed = source.remaining();
        source.position(source.limit());
        destination.put(new byte[]{5, 6, 7});
        return result(OK, NOT_HANDSHAKING, consumed, 3);
      });

      harness.normal.inbound(packet(20, 21, 22));

      Packet application = new Packet(16, false);
      assertEquals(3, harness.stateEngine.read(application),
          "BUFFER_OVERFLOW must not cause the DTLS datagram to disappear");
      application.flip();
      assertArrayEquals(new byte[]{5, 6, 7}, readRemaining(application));
      assertTrue(calls.get() >= 2);
    }

    @Test
    void handshakeApplicationBufferShouldFollowSslSessionSizing() throws Exception {
      Harness harness = new Harness(4096);
      HandShakeState handshake = new HandShakeState(harness.stateEngine);

      when(harness.engine.getHandshakeStatus()).thenReturn(NEED_UNWRAP);
      when(harness.engine.unwrap(any(ByteBuffer.class), any(ByteBuffer.class))).thenAnswer(invocation -> {
        ByteBuffer source = invocation.getArgument(0);
        ByteBuffer destination = invocation.getArgument(1);
        assertTrue(destination.capacity() >= 4096,
            "DTLS handshake application buffer must use SSLEngine session sizing");
        if (source.hasRemaining()) {
          source.get();
        }
        return result(OK, NEED_UNWRAP, 1, 0);
      });

      handshake.handshake(packet(1, 2, 3));
    }
  }

  @Nested
  class Murphy {

    @Test
    void plaintextProducedBeforeUnderflowMustStillBeDelivered() throws Exception {
      Harness harness = new Harness(4096);
      AtomicInteger calls = new AtomicInteger();

      when(harness.engine.unwrap(any(ByteBuffer.class), any(ByteBuffer.class))).thenAnswer(invocation -> {
        ByteBuffer source = invocation.getArgument(0);
        ByteBuffer destination = invocation.getArgument(1);
        if (calls.getAndIncrement() == 0) {
          source.get();
          destination.put(new byte[]{42, 43, 44});
          return result(OK, NOT_HANDSHAKING, 1, 3);
        }
        return result(BUFFER_UNDERFLOW, NOT_HANDSHAKING, 0, 0);
      });

      harness.normal.inbound(packet(30, 31, 32));

      Packet application = new Packet(16, false);
      assertEquals(3, harness.stateEngine.read(application),
          "plaintext already produced by unwrap must survive a later BUFFER_UNDERFLOW");
      application.flip();
      assertArrayEquals(new byte[]{42, 43, 44}, readRemaining(application));
    }

    @Test
    void zeroProgressOkResultMustNotSpinForever() throws Exception {
      Harness harness = new Harness(4096);
      when(harness.engine.unwrap(any(ByteBuffer.class), any(ByteBuffer.class)))
          .thenReturn(result(OK, NOT_HANDSHAKING, 0, 0));

      assertTimeoutPreemptively(
          java.time.Duration.ofSeconds(1),
          () -> harness.normal.inbound(packet(1, 2, 3)),
          "DTLS unwrap loop must stop when the engine reports no progress");
    }
  }

  private static final class Harness {
    private final SSLEngine engine = mock(SSLEngine.class);
    private final SSLSession session = mock(SSLSession.class);
    private final DTLSSessionManager manager = mock(DTLSSessionManager.class);
    private final StateEngine stateEngine;
    private final NormalState normal;

    private Harness(int applicationBufferSize) {
      when(engine.getSession()).thenReturn(session);
      when(session.getApplicationBufferSize()).thenReturn(applicationBufferSize);
      when(session.getPacketBufferSize()).thenReturn(8192);
      stateEngine = new StateEngine(new java.net.InetSocketAddress("127.0.0.1", 15000), engine, manager);
      normal = new NormalState(stateEngine);
      stateEngine.setCurrentState(normal);
    }
  }

  private static SSLEngineResult result(
      SSLEngineResult.Status status,
      SSLEngineResult.HandshakeStatus handshakeStatus,
      int consumed,
      int produced) {
    return new SSLEngineResult(status, handshakeStatus, consumed, produced);
  }

  private static Packet packet(int... values) {
    byte[] data = new byte[values.length];
    for (int i = 0; i < values.length; i++) {
      data[i] = (byte) values[i];
    }
    return new Packet(ByteBuffer.wrap(data));
  }

  private static byte[] readRemaining(Packet packet) {
    byte[] data = new byte[packet.available()];
    packet.get(data);
    return data;
  }
}
