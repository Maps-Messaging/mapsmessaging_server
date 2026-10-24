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

package io.mapsmessaging.network.io.impl.memory;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import io.mapsmessaging.dto.rest.config.network.KeyStoreConfigDTO;
import io.mapsmessaging.dto.rest.config.network.impl.MemoryConfigDTO;
import io.mapsmessaging.dto.rest.config.network.impl.MemorySecurityConfigDTO;
import io.mapsmessaging.dto.rest.config.network.impl.ShmConfigDTO;
import io.mapsmessaging.memory.MemoryClient;
import io.mapsmessaging.memory.MemoryConnection;
import io.mapsmessaging.memory.MemoryConnectionConfig;
import io.mapsmessaging.memory.MemoryEndpointConfig;
import io.mapsmessaging.memory.MemoryServer;
import io.mapsmessaging.memory.MemoryTransport;
import io.mapsmessaging.memory.shm.SharedMemoryTransport;
import io.mapsmessaging.memory.security.ProtectedTransport;
import io.mapsmessaging.memory.security.SecureHandshake;
import io.mapsmessaging.memory.security.SecureSession;
import io.mapsmessaging.memory.security.DataProtection;
import io.mapsmessaging.network.EndPointURL;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointConnectionFactory;
import io.mapsmessaging.network.io.EndPointServer;
import io.mapsmessaging.network.io.EndPointServerFactory;
import io.mapsmessaging.network.io.EndPointServerStatus;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.ServerPacket;
import io.mapsmessaging.network.io.impl.Selector;
import io.mapsmessaging.network.io.impl.SelectorCallback;
import io.mapsmessaging.network.io.impl.SelectorLoadManager;
import io.mapsmessaging.network.io.impl.SelectorTask;
import io.mapsmessaging.security.certificates.CertificateManager;
import io.mapsmessaging.security.certificates.CertificateManagerFactory;
import io.mapsmessaging.security.certificates.Ed25519IdentityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.MockedStatic;

import javax.security.auth.x500.X500Principal;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MemoryEndPointIntegrationTest {
  private Path directory;

  @BeforeEach
  void prepare_directory() throws Exception {
    Files.createDirectories(Path.of("target/data"));
    directory = Files.createTempDirectory(Path.of("target/data"), "memory-");
    if (!System.getProperty("os.name").startsWith("Windows")) {
      Files.setPosixFilePermissions(directory, PosixFilePermissions.fromString("rwx------"));
    }
  }

  @AfterEach
  void remove_directory() throws Exception {
    try (Stream<Path> paths = Files.walk(directory)) {
      for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
        Files.deleteIfExists(path);
      }
    }
  }

  @Test
  @Tag("native-memory")
  void unsecured_native_connections_use_existing_read_and_write_tasks() throws Exception {
    exchange(null, true);
  }

  @ParameterizedTest
  @EnumSource(DataProtection.class)
  @Tag("native-memory")
  void secured_native_connections_use_existing_read_and_write_tasks(DataProtection protection) throws Exception {
    exchange(protection, true);
  }

  @Test
  void mapped_streams_use_existing_read_and_write_tasks() throws Exception {
    exchange(null, false);
  }

  @ParameterizedTest
  @EnumSource(DataProtection.class)
  void protected_mapped_streams_use_existing_read_and_write_tasks(DataProtection protection) throws Exception {
    exchange(protection, false);
  }

  @Test
  void factories_are_discoverable_for_both_transports() {
    List<String> listeners = ServiceLoader.load(EndPointServerFactory.class).stream()
        .filter(provider -> provider.type().getPackageName().equals(MemoryEndPoint.class.getPackageName()))
        .map(provider -> provider.get().getName()).sorted().toList();
    List<String> clients = ServiceLoader.load(EndPointConnectionFactory.class).stream()
        .filter(provider -> provider.type().getPackageName().equals(MemoryEndPoint.class.getPackageName()))
        .map(provider -> provider.get().getName()).sorted().toList();
    assertEquals(List.of("rdma", "shm"), listeners);
    assertEquals(List.of("rdma", "shm"), clients);
  }

  @Test
  void already_closed_peer_is_not_published_to_accept_handler() throws Exception {
    ShmConfigDTO memory = configuration();
    EndPointURL url = new EndPointURL("shm://localhost:0");
    MessageDaemon daemon = mock(MessageDaemon.class, RETURNS_DEEP_STUBS);
    BlockingQueue<EndPoint> accepted = new LinkedBlockingQueue<>();
    try (MockedStatic<MessageDaemon> singleton = mockStatic(MessageDaemon.class);
         SelectorFixture fixture = new SelectorFixture(); MappedRendezvous mapped = new MappedRendezvous()) {
      singleton.when(MessageDaemon::getInstance).thenReturn(daemon);
      mapped.closeBeforePublish = true;
      try (EndPointServer listener = new ShmEndPointServerFactory().instance(url, fixture.manager, accepted::add, serverConfig(memory), null)) {
        listener.start();
        listener.register();
        EndPointServerStatus status = mock(EndPointServerStatus.class);
        when(status.getConfig()).thenReturn(serverConfig(memory));
        try (EndPoint client = new ShmEndPointConnectionFactory().connect(url, fixture.manager, endpoint -> { }, status, List.of())) {
          assertNull(accepted.poll(200, TimeUnit.MILLISECONDS), "Do not publish a peer already closed during acceptance");
          assertEquals(0, listener.size());
        }
      }
    }
  }

  private void exchange(DataProtection protection, boolean nativeRendezvous) throws Exception {
    ShmConfigDTO serverMemory = configuration();
    ShmConfigDTO clientMemory = configuration();
    clientMemory.setLocalAddress(serverMemory.getLocalAddress());
    if (protection != null) {
      provision_identities(serverMemory, clientMemory, protection);
    }
    EndPointURL url = new EndPointURL("shm://localhost:0");
    EndPointServerConfigDTO listenerConfig = serverConfig(serverMemory);
    BlockingQueue<EndPoint> accepted = new LinkedBlockingQueue<>();
    MessageDaemon daemon = mock(MessageDaemon.class, RETURNS_DEEP_STUBS);
    try (MockedStatic<MessageDaemon> singleton = mockStatic(MessageDaemon.class);
         SelectorFixture fixture = new SelectorFixture();
         MappedRendezvous mapped = nativeRendezvous ? null : new MappedRendezvous()) {
      singleton.when(MessageDaemon::getInstance).thenReturn(daemon);
      try (EndPointServer listener = new ShmEndPointServerFactory().instance(url, fixture.manager, accepted::add, listenerConfig, null)) {
        listener.start();
        listener.register();
        EndPointServerStatus clientStatus = mock(EndPointServerStatus.class);
        when(clientStatus.getConfig()).thenReturn(serverConfig(clientMemory));
        AtomicReference<EndPoint> connected = new AtomicReference<>();
        try (EndPoint client = new ShmEndPointConnectionFactory().connect(url, fixture.manager, connected::set, clientStatus, List.of())) {
          assertSame(client, connected.get());
          EndPoint inbound = accepted.poll(5, TimeUnit.SECONDS);
          assertNotNull(inbound);
          try (inbound) {
            assertEquals(1, listener.size());
            byte[] payload = new byte[40_000];
            for (int index = 0; index < payload.length; index++) {
              payload[index] = (byte) index;
            }
            // Small rings and buffers force repeated partial I/O through the existing tasks.
            try (TransferPeer clientPeer = new TransferPeer(client, payload.length); TransferPeer serverPeer = new TransferPeer(inbound, payload.length)) {
              clientPeer.send(payload);
              serverPeer.assertReceived(payload);
              clientPeer.assertSent();
              serverPeer.send(payload);
              clientPeer.assertReceived(payload);
              serverPeer.assertSent();
            }
          }
          assertEquals(0, listener.size());
        }
        listener.deregister();
        listener.register();
        listener.close();
        listener.close();
      }
    }
  }

  private static class TransferPeer implements SelectorCallback, AutoCloseable {
    private final EndPoint endpoint;
    private final int expectedLength;
    private final SelectorTask task;
    private final CountDownLatch received = new CountDownLatch(1);
    private final CountDownLatch sent = new CountDownLatch(1);
    private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();

    private TransferPeer(EndPoint endpoint, int expectedLength) throws IOException {
      this.endpoint = endpoint;
      this.expectedLength = expectedLength;
      task = new SelectorTask(this, endpoint.getConfig().getEndPointConfig());
      task.register(SelectionKey.OP_READ);
    }

    private void send(byte[] payload) {
      task.push(new ServerPacket() {
        @Override
        public int packFrame(Packet packet) {
          packet.put(payload);
          return payload.length;
        }
        @Override
        public void complete() {
          sent.countDown();
        }
        @Override
        public SocketAddress getFromAddress() {
          return null;
        }
      });
    }

    private void assertReceived(byte[] payload) throws InterruptedException {
      assertTrue(received.await(10, TimeUnit.SECONDS), "Expected all application bytes");
      assertArrayEquals(payload, bytes.toByteArray());
    }

    private void assertSent() throws InterruptedException {
      assertTrue(sent.await(2, TimeUnit.SECONDS), "Expected packet completion after its bytes were written");
    }

    @Override
    public boolean processPacket(Packet packet) {
      byte[] chunk = new byte[packet.available()];
      packet.get(chunk);
      bytes.writeBytes(chunk);
      if (bytes.size() == expectedLength) {
        received.countDown();
      }
      return true;
    }

    @Override
    public void close() {
      task.close();
    }

    @Override
    public String getName() {
      return "memory-test";
    }

    @Override
    public String getSessionId() {
      return "memory-test";
    }

    @Override
    public String getVersion() {
      return "1";
    }

    @Override
    public EndPoint getEndPoint() {
      return endpoint;
    }
  }

  /** Replaces only managed rendezvous: rings, crypto, endpoints and selector tasks are real. */
  private static class MappedRendezvous implements AutoCloseable {
    private final MockedStatic<MemoryServer> servers = mockStatic(MemoryServer.class);
    private final MockedStatic<MemoryClient> clients = mockStatic(MemoryClient.class);
    private final MemoryServer listener = mock(MemoryServer.class);
    private final BlockingQueue<MemoryConnection> pending = new LinkedBlockingQueue<>();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final SharedMemoryTransport serverRing;
    private final SharedMemoryTransport clientRing;
    private final AtomicReference<MemoryTransport> serverTransport;
    private final AtomicReference<MemoryTransport> clientTransport;
    private final MemoryConnection serverConnection;
    private final MemoryConnection clientConnection;
    private MemoryEndpointConfig serverConfig;
    private boolean closeBeforePublish;

    private MappedRendezvous() throws Exception {
      String name = "endpoint-test-" + UUID.randomUUID();
      serverRing = new SharedMemoryTransport(name, true, 256, 4);
      clientRing = new SharedMemoryTransport(name, false, 256, 4);
      serverTransport = new AtomicReference<>(serverRing);
      clientTransport = new AtomicReference<>(clientRing);
      serverConnection = connection(serverTransport);
      clientConnection = connection(clientTransport);
      servers.when(() -> MemoryServer.listen(any(MemoryEndpointConfig.class))).thenAnswer(invocation -> {
        serverConfig = invocation.getArgument(0);
        return listener;
      });
      when(listener.isClosed()).thenAnswer(invocation -> closed.get());
      when(listener.accept()).thenAnswer(invocation -> {
        try {
          return pending.take();
        } catch (InterruptedException exception) {
          Thread.currentThread().interrupt();
          throw new IOException("Acceptance interrupted", exception);
        }
      });
      doAnswer(invocation -> { closed.set(true); return null; }).when(listener).close();
      clients.when(() -> MemoryClient.connect(any(MemoryConnectionConfig.class))).thenAnswer(invocation -> {
        MemoryConnectionConfig config = invocation.getArgument(0);
        if (config.security() != null) {
          SecureHandshake serverHandshake = SecureHandshake.begin(serverConfig.security());
          SecureHandshake clientHandshake = SecureHandshake.begin(config.security());
          SecureSession serverSession = serverHandshake.finish(clientHandshake.offer(), true);
          SecureSession clientSession = clientHandshake.finish(serverHandshake.offer(), false);
          serverSession.verifyConfirmation(clientSession.confirmation());
          clientSession.verifyConfirmation(serverSession.confirmation());
          serverTransport.set(new ProtectedTransport(serverRing, serverSession));
          clientTransport.set(new ProtectedTransport(clientRing, clientSession));
        } else {
          assertNull(serverConfig.security());
        }
        if (closeBeforePublish) {
          serverConnection.close();
        }
        pending.add(serverConnection);
        return clientConnection;
      });
    }

    private static MemoryConnection connection(AtomicReference<MemoryTransport> transport) throws IOException {
      MemoryConnection connection = mock(MemoryConnection.class);
      AtomicBoolean closed = new AtomicBoolean();
      when(connection.isClosed()).thenAnswer(invocation -> closed.get());
      when(connection.hasData()).thenAnswer(invocation -> !closed.get() && transport.get().hasData());
      when(connection.canWrite()).thenAnswer(invocation -> !closed.get() && transport.get().canWrite());
      when(connection.remoteAddress()).thenReturn("mapped-test-peer");
      when(connection.read(any())).thenAnswer(invocation -> transport.get().read(invocation.getArgument(0)));
      when(connection.write(any())).thenAnswer(invocation -> transport.get().write(invocation.getArgument(0)));
      doAnswer(invocation -> {
        if (closed.compareAndSet(false, true)) {
          transport.get().close();
        }
        return null;
      }).when(connection).close();
      return connection;
    }

    @Override
    public void close() throws Exception {
      try {
        serverConnection.close();
        clientConnection.close();
      } finally {
        servers.close();
        clients.close();
        Files.deleteIfExists(serverRing.path());
      }
    }
  }

  private ShmConfigDTO configuration() {
    ShmConfigDTO config = new ShmConfigDTO();
    config.setLocalAddress(System.getProperty("os.name").startsWith("Windows") ? "maps-" + UUID.randomUUID() : directory.resolve("control.sock").toString());
    config.setSlotSize(256);
    config.setSlotCount(4);
    config.setServerReadBufferSize(512);
    config.setServerWriteBufferSize(512);
    return config;
  }

  private static EndPointServerConfigDTO serverConfig(MemoryConfigDTO memory) {
    EndPointServerConfigDTO config = new EndPointServerConfigDTO();
    config.setName("memory-test");
    config.setUrl("shm://localhost:0");
    config.setEndPointConfig(memory);
    config.setAuthenticationRealm("test");
    return config;
  }

  private void provision_identities(ShmConfigDTO server, ShmConfigDTO client, DataProtection protection) throws Exception {
    ConfigurationProperties store = new ConfigurationProperties();
    store.put("type", "PKCS12");
    store.put("path", directory.resolve("identities.p12").toString());
    store.put("passphrase", "store-password");
    CertificateManager manager = CertificateManagerFactory.getInstance().getManager(store);
    Ed25519IdentityManager identities = new Ed25519IdentityManager(manager);
    for (String name : List.of("server-a", "server-b")) {
      identities.createIdentity(name, "entry-password".toCharArray(), new X500Principal("CN=" + name), Duration.ofDays(1));
    }
    server.setMemorySecurity(security(store, "server-a", "server-b", protection));
    client.setMemorySecurity(security(store, "server-b", "server-a", protection));
  }

  private static MemorySecurityConfigDTO security(ConfigurationProperties properties, String local, String peer, DataProtection protection) {
    KeyStoreConfigDTO store = new KeyStoreConfigDTO();
    store.setPath(properties.getProperty("path"));
    store.setPassphrase("store-password");
    store.setAlias(local);
    MemorySecurityConfigDTO security = new MemorySecurityConfigDTO();
    security.setEnabled(true);
    security.setIdentity(local);
    security.setKeyStore(store);
    security.setTrustStore(store);
    security.setKeyPassword("entry-password");
    security.setTrustedPeers(Map.of(peer, List.of(peer)));
    security.setProtection(protection);
    return security;
  }

  private static class SelectorFixture implements AutoCloseable {
    private final Selector selector = new Selector();
    private final Thread thread = Thread.ofPlatform().start(selector);
    private final SelectorLoadManager manager = mock(SelectorLoadManager.class);
    private SelectorFixture() throws IOException {
      when(manager.allocate()).thenReturn(selector);
    }
    @Override
    public void close() throws InterruptedException {
      selector.close();
      selector.wakeup();
      thread.join(2000);
      assertFalse(thread.isAlive());
    }
  }
}
