/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     https://commonsclause.com/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.mapsmessaging.test.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

import static io.mapsmessaging.test.network.UdpFaultProxy.Direction.CLIENT_TO_SERVER;
import static io.mapsmessaging.test.network.UdpFaultProxy.Direction.SERVER_TO_CLIENT;
import static org.junit.jupiter.api.Assertions.*;

class UdpFaultProxyTest {

  @Test
  void forwardsDatagramsAndRoutesResponsesToTheirOriginalClients() throws Exception {
    try (EchoServer server = new EchoServer();
         UdpFaultProxy proxy = new UdpFaultProxy(0, server.address());
         DatagramSocket first = client();
         DatagramSocket second = client()) {
      send(first, proxy, "client-one");
      send(second, proxy, "client-two");
      assertEquals("client-one", receive(first));
      assertEquals("client-two", receive(second));
      assertEquals(2, proxy.stats().clientToServerForwarded());
      assertEquals(2, proxy.stats().serverToClientForwarded());
      assertEquals(2, proxy.stats().activePeers());
      assertEquals(0, proxy.stats().ioFailures());
    }
  }

  @Test
  void dropsExactlyTheNextTwoClientDatagrams() throws Exception {
    try (EchoServer server = new EchoServer();
         UdpFaultProxy proxy = new UdpFaultProxy(0, server.address());
         DatagramSocket client = client()) {
      proxy.dropNext(CLIENT_TO_SERVER, 2);
      send(client, proxy, "lost-one");
      send(client, proxy, "lost-two");
      send(client, proxy, "arrived");
      assertEquals("arrived", receive(client));
      assertEquals(3, proxy.stats().clientToServerReceived());
      assertEquals(1, proxy.stats().clientToServerForwarded());
      assertEquals(2, proxy.stats().dropped());
      assertEquals(0, proxy.stats().pendingRules());
    }
  }

  @Test
  void dropsServerRepliesWithoutDroppingClientRequests() throws Exception {
    try (EchoServer server = new EchoServer();
         UdpFaultProxy proxy = new UdpFaultProxy(0, server.address());
         DatagramSocket client = client()) {
      proxy.dropNext(SERVER_TO_CLIENT, 1);
      send(client, proxy, "reply-dropped");
      send(client, proxy, "reply-delivered");
      assertEquals("reply-delivered", receive(client));
      assertEquals(2, proxy.stats().clientToServerForwarded());
      assertEquals(1, proxy.stats().serverToClientForwarded());
      assertEquals(1, proxy.stats().dropped());
    }
  }

  @Test
  void reversesTheNextThreeDatagramsInArrivalOrder() throws Exception {
    try (EchoServer server = new EchoServer();
         UdpFaultProxy proxy = new UdpFaultProxy(0, server.address());
         DatagramSocket client = client()) {
      proxy.reverseNext(CLIENT_TO_SERVER, 3, Duration.ofSeconds(2));
      send(client, proxy, "one");
      send(client, proxy, "two");
      send(client, proxy, "three");
      assertEquals("three", receive(client));
      assertEquals("two", receive(client));
      assertEquals("one", receive(client));
      assertEquals(3, proxy.stats().reversed());
      assertEquals(0, proxy.stats().partialReverseFlushes());
    }
  }

  @Test
  void canReverseRepliesWithoutAffectingClientToServerArrivalOrder() throws Exception {
    try (EchoServer server = new EchoServer();
         UdpFaultProxy proxy = new UdpFaultProxy(0, server.address());
         DatagramSocket client = client()) {
      proxy.reverseNext(SERVER_TO_CLIENT, 3, Duration.ofSeconds(2));
      send(client, proxy, "reply-one");
      send(client, proxy, "reply-two");
      send(client, proxy, "reply-three");
      assertEquals("reply-three", receive(client));
      assertEquals("reply-two", receive(client));
      assertEquals("reply-one", receive(client));
      assertEquals(3, proxy.stats().reversed());
      assertEquals(3, proxy.stats().clientToServerForwarded());
    }
  }

  @Test
  void independentProxiesCanControlDifferentUdpServerPorts() throws Exception {
    try (EchoServer coapServer = new EchoServer();
         EchoServer mavlinkServer = new EchoServer();
         UdpFaultProxy coapProxy = new UdpFaultProxy(0, coapServer.address());
         UdpFaultProxy mavlinkProxy = new UdpFaultProxy(0, mavlinkServer.address());
         DatagramSocket coapClient = client();
         DatagramSocket mavlinkClient = client()) {
      coapProxy.dropNext(CLIENT_TO_SERVER, 1);
      send(coapClient, coapProxy, "coap-dropped");
      send(mavlinkClient, mavlinkProxy, "mavlink-unaffected");
      send(coapClient, coapProxy, "coap-forwarded");
      assertEquals("mavlink-unaffected", receive(mavlinkClient));
      assertEquals("coap-forwarded", receive(coapClient));
      assertEquals(1, coapProxy.stats().dropped());
      assertEquals(0, mavlinkProxy.stats().dropped());
    }
  }

  @Test
  void partialReverseBatchFlushesInOriginalOrderAfterDeadline() throws Exception {
    try (EchoServer server = new EchoServer();
         UdpFaultProxy proxy = new UdpFaultProxy(0, server.address());
         DatagramSocket client = client()) {
      proxy.reverseNext(CLIENT_TO_SERVER, 3, Duration.ofMillis(100));
      send(client, proxy, "first");
      send(client, proxy, "second");
      assertEquals("first", receive(client));
      assertEquals("second", receive(client));
      assertEquals(1, proxy.stats().partialReverseFlushes());
      assertEquals(0, proxy.stats().reversed());
      assertEquals(0, proxy.stats().pendingRules());
    }
  }

  @Test
  void insertsBytesAtSpecifiedPositionForExactlyTwoDatagrams() throws Exception {
    try (EchoServer server = new EchoServer();
         UdpFaultProxy proxy = new UdpFaultProxy(0, server.address());
         DatagramSocket client = client()) {
      proxy.injectNext(CLIENT_TO_SERVER, 2, 2, bytes("XX"));
      send(client, proxy, "abcd");
      send(client, proxy, "1234");
      send(client, proxy, "normal");
      assertEquals("abXXcd", receive(client));
      assertEquals("12XX34", receive(client));
      assertEquals("normal", receive(client));
      assertEquals(2, proxy.stats().modified());
    }
  }

  @Test
  void reversesOnlyTheRequestedByteRange() throws Exception {
    try (EchoServer server = new EchoServer();
         UdpFaultProxy proxy = new UdpFaultProxy(0, server.address());
         DatagramSocket client = client()) {
      proxy.reverseRangeNext(CLIENT_TO_SERVER, 1, 1, 4);
      send(client, proxy, "abcdef");
      send(client, proxy, "untouched");
      assertEquals("adcbef", receive(client));
      assertEquals("untouched", receive(client));
      assertEquals(1, proxy.stats().modified());
    }
  }

  @Test
  void supportsFaultsOnServerToClientPackets() throws Exception {
    try (EchoServer server = new EchoServer();
         UdpFaultProxy proxy = new UdpFaultProxy(0, server.address());
         DatagramSocket client = client()) {
      proxy.injectNext(SERVER_TO_CLIENT, 1, 0, bytes("prefix-"));
      proxy.reverseRangeNext(SERVER_TO_CLIENT, 1, 0, 4);
      send(client, proxy, "hello");
      send(client, proxy, "abcdE");
      assertEquals("prefix-hello", receive(client));
      assertEquals("dcbaE", receive(client));
      assertEquals(2, proxy.stats().modified());
    }
  }

  @Test
  void queuesRulesAndResetFlushesBufferedPackets() throws Exception {
    try (EchoServer server = new EchoServer();
         UdpFaultProxy proxy = new UdpFaultProxy(0, server.address());
         DatagramSocket client = client()) {
      proxy.dropNext(CLIENT_TO_SERVER, 1);
      proxy.reverseNext(CLIENT_TO_SERVER, 3, Duration.ofSeconds(2));
      send(client, proxy, "dropped");
      send(client, proxy, "buffered");
      // The ingress loop is asynchronous. Wait until the buffered datagram has been accepted.
      waitFor(() -> proxy.stats().clientToServerReceived() == 2);
      assertEquals(1, proxy.stats().pendingRules());
      proxy.resetRules();
      assertEquals("buffered", receive(client));
      assertEquals(0, proxy.stats().pendingRules());
      assertEquals(1, proxy.stats().dropped());
    }
  }

  @Test
  void invalidMutationsDropTheDatagramAndIncrementErrorCounters() throws Exception {
    try (EchoServer server = new EchoServer();
         UdpFaultProxy proxy = new UdpFaultProxy(0, server.address());
         DatagramSocket client = client()) {
      proxy.reverseRangeNext(CLIENT_TO_SERVER, 1, 0, 20);
      send(client, proxy, "short");
      send(client, proxy, "valid");
      assertEquals("valid", receive(client));
      assertEquals(1, proxy.stats().invalidMutations());
      assertEquals(1, proxy.stats().dropped());
    }
  }

  @Test
  void rejectsInvalidRuleConfigurationWithoutMutatingProxy() throws Exception {
    try (EchoServer server = new EchoServer();
         UdpFaultProxy proxy = new UdpFaultProxy(0, server.address())) {
      assertThrows(IllegalArgumentException.class, () -> proxy.dropNext(CLIENT_TO_SERVER, 0));
      assertThrows(IllegalArgumentException.class, () -> proxy.dropNext(CLIENT_TO_SERVER, 1_025));
      assertThrows(IllegalArgumentException.class, () -> proxy.injectNext(CLIENT_TO_SERVER, 1, -1, bytes("x")));
      assertThrows(IllegalArgumentException.class, () -> proxy.injectNext(CLIENT_TO_SERVER, 1, 0, new byte[0]));
      assertThrows(IllegalArgumentException.class, () -> proxy.reverseRangeNext(CLIENT_TO_SERVER, 1, 4, 2));
      assertThrows(IllegalArgumentException.class, () -> proxy.reverseNext(CLIENT_TO_SERVER, 1, Duration.ZERO));
      assertEquals(0, proxy.stats().pendingRules());
    }
  }

  @Test
  void closeReleasesKnownListeningPortAndRejectsNewRules() throws Exception {
    try (EchoServer server = new EchoServer()) {
      UdpFaultProxy proxy = new UdpFaultProxy(0, server.address());
      int port = proxy.getListenPort();
      proxy.close();
      proxy.close();
      assertThrows(IllegalStateException.class, () -> proxy.dropNext(CLIENT_TO_SERVER, 1));
      try (DatagramSocket rebound = new DatagramSocket(new InetSocketAddress(InetAddress.getLoopbackAddress(), port))) {
        assertEquals(port, rebound.getLocalPort());
      }
    }
  }

  private static DatagramSocket client() throws SocketException {
    DatagramSocket socket = new DatagramSocket(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
    socket.setSoTimeout(2_000);
    return socket;
  }

  private static void send(DatagramSocket client, UdpFaultProxy proxy, String value) throws IOException {
    byte[] data = bytes(value);
    client.send(new DatagramPacket(data, data.length, proxy.getListenAddress()));
  }

  private static String receive(DatagramSocket socket) throws IOException {
    byte[] data = new byte[2_048];
    DatagramPacket packet = new DatagramPacket(data, data.length);
    socket.receive(packet);
    return new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
  }

  private static byte[] bytes(String value) {
    return value.getBytes(StandardCharsets.UTF_8);
  }

  private static void waitFor(Check check) throws InterruptedException {
    long deadline = System.nanoTime() + Duration.ofSeconds(2).toNanos();
    while (!check.matches() && System.nanoTime() < deadline) {
      Thread.sleep(5);
    }
    assertTrue(check.matches(), "Expected asynchronous packet processing to complete");
  }

  private interface Check {
    boolean matches();
  }

  private static final class EchoServer implements AutoCloseable {
    private final DatagramSocket socket;
    private final Thread thread;
    private final AtomicReference<IOException> failure = new AtomicReference<>();

    private EchoServer() throws SocketException {
      socket = new DatagramSocket(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
      socket.setSoTimeout(200);
      thread = new Thread(() -> {
        while (!socket.isClosed()) {
          byte[] bytes = new byte[65_507];
          DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
          try {
            socket.receive(packet);
            socket.send(new DatagramPacket(
                Arrays.copyOf(packet.getData(), packet.getLength()), packet.getLength(), packet.getSocketAddress()));
          } catch (SocketTimeoutException ignored) {
            // Allow orderly shutdown.
          } catch (SocketException exception) {
            if (!socket.isClosed()) {
              failure.compareAndSet(null, exception);
            }
            return;
          } catch (IOException exception) {
            failure.compareAndSet(null, exception);
            return;
          }
        }
      }, "udp-fault-proxy-test-echo");
      thread.setDaemon(true);
      thread.start();
    }

    private InetSocketAddress address() {
      return new InetSocketAddress(socket.getLocalAddress(), socket.getLocalPort());
    }

    @Override
    public void close() throws InterruptedException {
      socket.close();
      thread.join(1_000);
      assertNull(failure.get(), "Echo fixture failed");
    }
  }
}
