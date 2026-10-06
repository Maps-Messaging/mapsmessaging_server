/*
 *
 *  Copyright [ 2026 ] Ralf Himmelein and Claude
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
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Inbound TLS records larger than the caller's read buffer. The engine mock answers exactly as the
 * JDK's TLSv1.3 engine does (checked against a real engine): a record that does not fit in the
 * destination returns BUFFER_OVERFLOW with nothing consumed or produced.
 */
class SSLEndPointReadOverflowTest {

  private static final int PACKET_BUFFER_SIZE = 16709;
  private static final int APPLICATION_BUFFER_SIZE = 16704;
  private static final int RECORD_ENCRYPTED_BYTES = 5038;
  private static final int RECORD_PLAINTEXT_BYTES = 5000;

  @Test
  void readBuffer_whenRecordCannotFitEmptyBuffer_closesInsteadOfHanging() throws Exception {
    SSLEndPoint endPoint = new Fixture().createEndPoint();
    try {
      ByteBuffer detectionSized = ByteBuffer.allocate(1024);

      IOException failure = assertThrows(IOException.class, () -> endPoint.readBuffer(detectionSized));

      assertTrue(failure.getMessage().contains("cannot hold a TLS record"));
      assertEquals(0, detectionSized.position());
    } finally {
      endPoint.close();
    }
  }

  @Test
  void readBuffer_whenBufferHoldsTheRecord_decodesIt() throws Exception {
    SSLEndPoint endPoint = new Fixture().createEndPoint();
    try {
      ByteBuffer applicationIn = ByteBuffer.allocate(100_000);

      endPoint.readBuffer(applicationIn);

      assertEquals(RECORD_PLAINTEXT_BYTES, applicationIn.position());
    } finally {
      endPoint.close();
    }
  }

  @Test
  void readBuffer_whenPartlyFilledBufferOverflows_returnsDecodedDataForTheCallerToDrain() throws Exception {
    SSLEndPoint endPoint = new Fixture().createEndPoint();
    try {
      ByteBuffer applicationIn = ByteBuffer.allocate(3000);
      applicationIn.put(new byte[100]);

      assertDoesNotThrow(() -> endPoint.readBuffer(applicationIn));

      assertEquals(100, applicationIn.position());
    } finally {
      endPoint.close();
    }
  }

  private static final class Fixture {

    private final SocketChannel channel = mock(SocketChannel.class);
    private final Socket socket = mock(Socket.class);
    private final Selector selector = mock(Selector.class);
    private final SSLEngine sslEngine = mock(SSLEngine.class);
    private final SSLSession sslSession = mock(SSLSession.class);
    private final EndPointServerStatus serverStatus = mock(EndPointServerStatus.class);
    private final AtomicBoolean recordDelivered = new AtomicBoolean();

    Fixture() throws Exception {
      EndPointServerConfigDTO serverConfig = new EndPointServerConfigDTO();
      TcpConfigDTO tcpConfig = new TcpConfigDTO();
      tcpConfig.setTimeout(60_000);
      serverConfig.setEndPointConfig(tcpConfig);

      when(serverStatus.getConfig()).thenReturn(serverConfig);
      when(channel.socket()).thenReturn(socket);
      when(channel.isConnected()).thenReturn(true);
      when(socket.getLocalAddress()).thenReturn(InetAddress.getLoopbackAddress());
      when(sslEngine.getSession()).thenReturn(sslSession);
      when(sslSession.getPacketBufferSize()).thenReturn(PACKET_BUFFER_SIZE);
      when(sslSession.getApplicationBufferSize()).thenReturn(APPLICATION_BUFFER_SIZE);
      when(sslEngine.getHandshakeStatus()).thenReturn(SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING);

      when(sslEngine.wrap(any(ByteBuffer.class), any(ByteBuffer.class))).thenReturn(new SSLEngineResult(
          SSLEngineResult.Status.OK, SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING, 0, 0));
      when(channel.write(any(ByteBuffer.class))).thenAnswer(invocation -> {
        ByteBuffer encrypted = invocation.getArgument(0);
        int remaining = encrypted.remaining();
        encrypted.position(encrypted.limit());
        return remaining;
      });

      // The socket holds one encrypted record, delivered once.
      when(channel.read(any(ByteBuffer.class))).thenAnswer(invocation -> {
        if (recordDelivered.getAndSet(true)) {
          return 0;
        }
        ByteBuffer encryptedIn = invocation.getArgument(0);
        encryptedIn.put(new byte[RECORD_ENCRYPTED_BYTES]);
        return RECORD_ENCRYPTED_BYTES;
      });

      when(sslEngine.unwrap(any(ByteBuffer.class), any(ByteBuffer.class))).thenAnswer(invocation -> {
        ByteBuffer encryptedIn = invocation.getArgument(0);
        ByteBuffer applicationIn = invocation.getArgument(1);
        if (encryptedIn.remaining() < RECORD_ENCRYPTED_BYTES) {
          return new SSLEngineResult(SSLEngineResult.Status.BUFFER_UNDERFLOW,
              SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING, 0, 0);
        }
        if (applicationIn.remaining() < RECORD_PLAINTEXT_BYTES) {
          return new SSLEngineResult(SSLEngineResult.Status.BUFFER_OVERFLOW,
              SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING, 0, 0);
        }
        encryptedIn.position(encryptedIn.position() + RECORD_ENCRYPTED_BYTES);
        applicationIn.put(new byte[RECORD_PLAINTEXT_BYTES]);
        return new SSLEngineResult(SSLEngineResult.Status.OK,
            SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING, RECORD_ENCRYPTED_BYTES, RECORD_PLAINTEXT_BYTES);
      });

      doAnswer(invocation -> null).when(selector).register(any(SocketChannel.class), anyInt(), any());
    }

    SSLEndPoint createEndPoint() throws Exception {
      return new SSLEndPoint(1, sslEngine, channel, selector, endPoint -> { }, serverStatus, List.of());
    }
  }
}
