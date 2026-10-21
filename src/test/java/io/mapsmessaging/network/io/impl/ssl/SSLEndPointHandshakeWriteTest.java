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

package io.mapsmessaging.network.io.impl.ssl;

import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import io.mapsmessaging.dto.rest.config.network.impl.TcpConfigDTO;
import io.mapsmessaging.network.io.EndPointServerStatus;
import io.mapsmessaging.network.io.impl.Selector;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLSession;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SSLEndPointHandshakeWriteTest {

  @Test
  void partial_client_hello_registers_write_interest() throws Exception {
    Fixture fixture = new Fixture(true);
    SSLEndPoint endPoint = fixture.createEndPoint();

    List<Integer> registeredOps = fixture.snapshotRegisteredOps();
    endPoint.close();

    Assertions.assertTrue(
        registeredOps.stream().anyMatch(op -> (op & SelectionKey.OP_WRITE) != 0),
        "A partial TLS handshake write must register OP_WRITE so the remaining encrypted bytes can drain");
  }

  @Test
  void complete_client_hello_does_not_register_write_interest() throws Exception {
    Fixture fixture = new Fixture(false);
    SSLEndPoint endPoint = fixture.createEndPoint();

    List<Integer> registeredOps = fixture.snapshotRegisteredOps();
    endPoint.close();

    Assertions.assertFalse(
        registeredOps.stream().anyMatch(op -> (op & SelectionKey.OP_WRITE) != 0),
        "A fully drained TLS handshake write must not leave OP_WRITE registered");
  }

  @Test
  void write_ready_callback_resumes_partial_handshake_write() throws Exception {
    Fixture fixture = new Fixture(true);
    SSLEndPoint endPoint = fixture.createEndPoint();
    int writesBeforeReady = fixture.writeCount.get();

    endPoint.handshakeManager.selected(null, fixture.selector, SelectionKey.OP_WRITE);

    int writesAfterReady = fixture.writeCount.get();
    endPoint.close();

    Assertions.assertTrue(
        writesAfterReady > writesBeforeReady,
        "An OP_WRITE callback must retry pending encrypted TLS handshake bytes");
  }

  @Test
  void write_interest_is_removed_after_pending_handshake_bytes_drain() throws Exception {
    Fixture fixture = new Fixture(true);
    SSLEndPoint endPoint = fixture.createEndPoint();
    fixture.registeredOps.clear();

    endPoint.handshakeManager.selected(null, fixture.selector, SelectionKey.OP_WRITE);

    List<Integer> registeredOps = fixture.snapshotRegisteredOps();
    endPoint.close();

    Assertions.assertFalse(registeredOps.isEmpty(), "The handshake selector interest must be updated after a write-ready callback");
    Assertions.assertEquals(
        SelectionKey.OP_READ,
        registeredOps.getLast(),
        "OP_WRITE must be removed once all encrypted handshake bytes have drained");
  }

  @Test
  void need_wrap_uses_empty_application_buffer() throws Exception {
    Fixture fixture = new Fixture(false);
    SSLEndPoint endPoint = fixture.createEndPoint();
    fixture.wrapSourceRemaining.clear();

    AtomicInteger handshakeStatusCalls = new AtomicInteger();
    when(fixture.sslEngine.getHandshakeStatus()).thenAnswer(invocation ->
        handshakeStatusCalls.getAndIncrement() == 0
            ? SSLEngineResult.HandshakeStatus.NEED_WRAP
            : SSLEngineResult.HandshakeStatus.FINISHED);

    doAnswer(invocation -> {
      ByteBuffer source = invocation.getArgument(0);
      fixture.wrapSourceRemaining.add(source.remaining());
      return new SSLEngineResult(
          SSLEngineResult.Status.OK,
          SSLEngineResult.HandshakeStatus.FINISHED,
          0,
          0);
    }).when(fixture.sslEngine).wrap(any(ByteBuffer.class), any(ByteBuffer.class));

    endPoint.handshakeManager.handleSSLHandshakeStatus();
    endPoint.close();

    Assertions.assertEquals(List.of(0), fixture.wrapSourceRemaining,
        "TLS handshake NEED_WRAP must not expose arbitrary application bytes to SSLEngine.wrap()");
  }

  @Test
  void handshake_without_outbound_callback_does_not_take_selector_ownership() throws Exception {
    Fixture fixture = new Fixture(false);
    SSLEndPoint endPoint = fixture.createEndPoint();
    SSLHandShakeManagerImpl inboundManager = new SSLHandShakeManagerImpl(endPoint, null, 0);
    endPoint.handshakeManager = inboundManager;
    fixture.registeredOps.clear();
    fixture.registeredAttachments.clear();

    AtomicInteger writes = new AtomicInteger();
    doAnswer(invocation -> {
      ByteBuffer encrypted = invocation.getArgument(0);
      int call = writes.incrementAndGet();
      if (call == 1) {
        encrypted.position(encrypted.position() + 2);
        return 2;
      }
      return 0;
    }).when(fixture.channel).write(any(ByteBuffer.class));

    when(fixture.sslEngine.getHandshakeStatus())
        .thenReturn(
            SSLEngineResult.HandshakeStatus.NEED_WRAP,
            SSLEngineResult.HandshakeStatus.NEED_UNWRAP);

    inboundManager.handleSSLHandshakeStatus();
    endPoint.close();

    Assertions.assertFalse(
        fixture.registeredAttachments.contains(inboundManager),
        "An inbound handshake must not replace the ProtocolAcceptRunner selector attachment");
  }

  @Test
  void need_unwrap_again_invokes_unwrap() throws Exception {
    Fixture fixture = new Fixture(false);
    SSLEndPoint endPoint = fixture.createEndPoint();

    when(fixture.sslEngine.getHandshakeStatus())
        .thenReturn(
            SSLEngineResult.HandshakeStatus.NEED_UNWRAP_AGAIN,
            SSLEngineResult.HandshakeStatus.FINISHED);
    when(fixture.sslEngine.unwrap(any(ByteBuffer.class), any(ByteBuffer.class)))
        .thenReturn(
            new SSLEngineResult(
                SSLEngineResult.Status.OK,
                SSLEngineResult.HandshakeStatus.FINISHED,
                0,
                0));

    endPoint.handshakeManager.handleSSLHandshakeStatus();
    endPoint.close();

    verify(fixture.sslEngine, atLeastOnce()).unwrap(any(ByteBuffer.class), any(ByteBuffer.class));
  }

  private static final class Fixture {

    private final SocketChannel channel = mock(SocketChannel.class);
    private final Socket socket = mock(Socket.class);
    private final Selector selector = mock(Selector.class);
    private final SSLEngine sslEngine = mock(SSLEngine.class);
    private final SSLSession sslSession = mock(SSLSession.class);
    private final EndPointServerStatus serverStatus = mock(EndPointServerStatus.class);
    private final AtomicInteger writeCount = new AtomicInteger();
    private final List<Integer> registeredOps = new ArrayList<>();
    private final List<Object> registeredAttachments = new ArrayList<>();
    private final List<Integer> wrapSourceRemaining = new ArrayList<>();
    private final boolean partialInitialWrite;

    Fixture(boolean partialInitialWrite) throws Exception {
      this.partialInitialWrite = partialInitialWrite;
      EndPointServerConfigDTO serverConfig = new EndPointServerConfigDTO();
      TcpConfigDTO tcpConfig = new TcpConfigDTO();
      tcpConfig.setTimeout(60_000);
      serverConfig.setEndPointConfig(tcpConfig);

      when(serverStatus.getConfig()).thenReturn(serverConfig);
      when(channel.socket()).thenReturn(socket);
      when(channel.isConnected()).thenReturn(true);
      when(socket.getLocalAddress()).thenReturn(InetAddress.getLoopbackAddress());
      when(sslEngine.getSession()).thenReturn(sslSession);
      when(sslSession.getPacketBufferSize()).thenReturn(64);
      when(sslSession.getApplicationBufferSize()).thenReturn(64);
      when(sslEngine.getHandshakeStatus()).thenReturn(SSLEngineResult.HandshakeStatus.NEED_UNWRAP);

      doAnswer(invocation -> {
        ByteBuffer source = invocation.getArgument(0);
        ByteBuffer encrypted = invocation.getArgument(1);
        wrapSourceRemaining.add(source.remaining());
        encrypted.put(new byte[] {1, 2, 3, 4, 5, 6, 7, 8});
        return new SSLEngineResult(
            SSLEngineResult.Status.OK,
            SSLEngineResult.HandshakeStatus.NEED_UNWRAP,
            0,
            8);
      }).when(sslEngine).wrap(any(ByteBuffer.class), any(ByteBuffer.class));

      doAnswer(invocation -> {
        ByteBuffer encrypted = invocation.getArgument(0);
        int call = writeCount.incrementAndGet();
        if (partialInitialWrite && call == 1) {
          encrypted.position(encrypted.position() + 2);
          return 2;
        }
        if (partialInitialWrite && call == 2) {
          return 0;
        }
        int remaining = encrypted.remaining();
        encrypted.position(encrypted.limit());
        return remaining;
      }).when(channel).write(any(ByteBuffer.class));

      doAnswer(invocation -> {
        registeredOps.add(invocation.getArgument(1));
        registeredAttachments.add(invocation.getArgument(2));
        return null;
      }).when(selector).register(any(SocketChannel.class), anyInt(), any());
    }

    SSLEndPoint createEndPoint() throws Exception {
      return new SSLEndPoint(
          1,
          sslEngine,
          channel,
          selector,
          endPoint -> { },
          serverStatus,
          List.of());
    }

    List<Integer> snapshotRegisteredOps() {
      return List.copyOf(registeredOps);
    }
  }
}
