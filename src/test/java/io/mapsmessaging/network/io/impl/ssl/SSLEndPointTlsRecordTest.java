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
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.impl.Selector;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.TrustManagerFactory;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Exercises the endpoint with real JDK records; only the non-blocking socket boundary is mocked. */
@Timeout(10)
class SSLEndPointTlsRecordTest {

  @ParameterizedTest
  @CsvSource({"TLSv1.2,5000", "TLSv1.3,5000", "TLSv1.2,16384", "TLSv1.3,16384"})
  void a_record_larger_than_detection_buffer_decodes_without_losing_payload(String protocol, int size) throws Exception {
    try (Fixture fixture = new Fixture(protocol)) {
      byte[] payload = payload(size, 11);
      fixture.incoming.add(ByteBuffer.wrap(fixture.encrypt(payload)));
      Packet packet = new Packet(32768, false);

      assertTrue(fixture.endPoint.readPacket(packet) > 0);

      assertArrayEquals(payload, decoded(packet));
      assertFalse(fixture.endPoint.hasBufferedReadData());
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"TLSv1.2", "TLSv1.3"})
  void a_second_buffered_record_survives_overflow_and_decodes_on_the_next_read(String protocol) throws Exception {
    try (Fixture fixture = new Fixture(protocol)) {
      byte[] first = payload(5000, 17);
      byte[] second = payload(5000, 29);
      byte[] firstRecord = fixture.encrypt(first);
      byte[] secondRecord = fixture.encrypt(second);
      ByteBuffer records = ByteBuffer.allocate(firstRecord.length + secondRecord.length);
      records.put(firstRecord).put(secondRecord).flip();
      fixture.incoming.add(records);
      Packet packet = new Packet(6000, false);

      fixture.endPoint.readPacket(packet);
      assertArrayEquals(first, decoded(packet));
      assertTrue(fixture.incoming.isEmpty(), "Both records must already have left the socket");
      assertTrue(fixture.endPoint.hasBufferedReadData());

      packet.clear();
      assertTrue(fixture.endPoint.readPacket(packet) > 0);
      assertArrayEquals(second, decoded(packet));
      assertFalse(fixture.endPoint.hasBufferedReadData());
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"TLSv1.2", "TLSv1.3"})
  void a_fragmented_record_produces_payload_only_after_its_remaining_bytes_arrive(String protocol) throws Exception {
    try (Fixture fixture = new Fixture(protocol)) {
      byte[] payload = payload(5000, 37);
      byte[] record = fixture.encrypt(payload);
      fixture.incoming.add(ByteBuffer.wrap(Arrays.copyOfRange(record, 0, 7)));
      Packet packet = new Packet(32768, false);

      assertTrue(fixture.endPoint.readPacket(packet) > 0);
      assertEquals(0, packet.position());
      assertTrue(fixture.endPoint.hasBufferedReadData());

      fixture.incoming.add(ByteBuffer.wrap(Arrays.copyOfRange(record, 7, record.length)));
      assertTrue(fixture.endPoint.readPacket(packet) > 0);
      assertArrayEquals(payload, decoded(packet));
      assertFalse(fixture.endPoint.hasBufferedReadData());
    }
  }

  private static byte[] payload(int size, int seed) {
    byte[] payload = new byte[size];
    for (int index = 0; index < size; index++) {
      payload[index] = (byte) (index * 31 + seed);
    }
    return payload;
  }

  private static byte[] decoded(Packet packet) {
    packet.flip();
    byte[] payload = new byte[packet.available()];
    packet.get(payload);
    return payload;
  }

  private static SSLContext context() throws Exception {
    char[] password = System.getProperty("javax.net.ssl.keyStorePassword", "password").toCharArray();
    KeyStore keys = KeyStore.getInstance("JKS");
    Path keyStore = Path.of(System.getProperty("javax.net.ssl.keyStore", "my-keystore.jks"));
    try (InputStream input = Files.newInputStream(keyStore)) {
      keys.load(input, password);
    }
    KeyManagerFactory keyManagers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    keyManagers.init(keys, password);
    // The fixture trusts the same test identity at both ends; no external trust or network is needed.
    TrustManagerFactory trustManagers = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    trustManagers.init(keys);
    SSLContext context = SSLContext.getInstance("TLS");
    context.init(keyManagers.getKeyManagers(), trustManagers.getTrustManagers(), null);
    return context;
  }

  private static final class Fixture implements AutoCloseable {
    private final Queue<ByteBuffer> incoming = new ArrayDeque<>();
    private final SSLEngine client;
    private final SSLEndPoint endPoint;

    private Fixture(String protocol) throws Exception {
      SSLContext context = context();
      client = context.createSSLEngine();
      client.setUseClientMode(true);
      client.setEnabledProtocols(new String[]{protocol});
      SSLEngine server = context.createSSLEngine();
      server.setEnabledProtocols(new String[]{protocol});
      SocketChannel channel = mock(SocketChannel.class);
      Socket socket = mock(Socket.class);
      Selector selector = mock(Selector.class);
      EndPointServerStatus status = mock(EndPointServerStatus.class);
      EndPointServerConfigDTO config = new EndPointServerConfigDTO();
      config.setEndPointConfig(new TcpConfigDTO());
      when(status.getConfig()).thenReturn(config);
      when(channel.socket()).thenReturn(socket);
      when(channel.isConnected()).thenReturn(true);
      when(socket.getLocalAddress()).thenReturn(InetAddress.getLoopbackAddress());
      when(channel.write(any(ByteBuffer.class))).thenAnswer(invocation -> {
        ByteBuffer bytes = invocation.getArgument(0);
        int count = bytes.remaining();
        bytes.position(bytes.limit());
        return count;
      });
      when(channel.read(any(ByteBuffer.class))).thenAnswer(invocation -> {
        if (incoming.isEmpty()) {
          return 0;
        }
        ByteBuffer target = invocation.getArgument(0);
        ByteBuffer source = incoming.element();
        int count = Math.min(target.remaining(), source.remaining());
        int limit = source.limit();
        source.limit(source.position() + count);
        target.put(source);
        source.limit(limit);
        if (!source.hasRemaining()) {
          incoming.remove();
        }
        return count;
      });
      endPoint = new SSLEndPoint(1, server, channel, selector, null, status, List.of());
      try {
        handshake(server);
        endPoint.handshakeManager = new SSLHandshakeManagerFinished(ByteBuffer.allocate(server.getSession().getApplicationBufferSize()));
      } catch (Exception failure) {
        endPoint.close();
        throw failure;
      }
    }

    private byte[] encrypt(byte[] payload) throws Exception {
      ByteBuffer encrypted = ByteBuffer.allocate(65536);
      ByteBuffer application = ByteBuffer.wrap(payload);
      while (application.hasRemaining()) {
        SSLEngineResult result = client.wrap(application, encrypted);
        assertEquals(SSLEngineResult.Status.OK, result.getStatus());
        assertTrue(result.bytesConsumed() > 0, "Wrapping application data must make progress");
      }
      encrypted.flip();
      byte[] record = new byte[encrypted.remaining()];
      encrypted.get(record);
      return record;
    }

    private void handshake(SSLEngine server) throws Exception {
      client.beginHandshake();
      ByteBuffer clientToServer = ByteBuffer.allocate(131072);
      ByteBuffer serverToClient = ByteBuffer.allocate(131072);
      ByteBuffer sink = ByteBuffer.allocate(65536);
      for (int attempt = 0; attempt < 100; attempt++) {
        handshakeStep(client, server, clientToServer, sink);
        handshakeStep(server, client, serverToClient, sink);
        if (client.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING
            && server.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING
            && clientToServer.position() == 0 && serverToClient.position() == 0) {
          return;
        }
      }
      throw new AssertionError("TLS handshake did not complete");
    }

    private static void handshakeStep(SSLEngine sender, SSLEngine receiver, ByteBuffer wire, ByteBuffer sink) throws Exception {
      runTasks(sender);
      runTasks(receiver);
      if (sender.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_WRAP) {
        sender.wrap(ByteBuffer.allocate(0), wire);
      }
      wire.flip();
      if (wire.hasRemaining()) {
        sink.clear();
        receiver.unwrap(wire, sink);
      }
      wire.compact();
      runTasks(receiver);
    }

    private static void runTasks(SSLEngine engine) {
      Runnable task;
      while ((task = engine.getDelegatedTask()) != null) {
        task.run();
      }
    }

    @Override
    public void close() {
      endPoint.close();
    }
  }
}
