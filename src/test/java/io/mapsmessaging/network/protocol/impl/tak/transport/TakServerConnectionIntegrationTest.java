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

package io.mapsmessaging.network.protocol.impl.tak.transport;

import io.mapsmessaging.network.EndPointURL;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLServerSocketFactory;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TakServerConnectionIntegrationTest {

  @Test
  void tcpConnectionRoundTrip() throws Exception {
    try (ServerSocket serverSocket = new ServerSocket(0)) {
      int port = serverSocket.getLocalPort();
      byte[] inbound = "ping".getBytes();
      byte[] outbound = "pong".getBytes();
      ExecutorService executor = Executors.newSingleThreadExecutor();

      Future<?> server = executor.submit(() -> {
        try (Socket accepted = serverSocket.accept()) {
          byte[] buffer = accepted.getInputStream().readNBytes(inbound.length);
          assertArrayEquals(inbound, buffer);
          accepted.getOutputStream().write(outbound);
          accepted.getOutputStream().flush();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });

      TakServerConnection connection = new TakServerConnection(new EndPointURL("tcp://127.0.0.1:" + port), Duration.ofSeconds(2));
      connection.connect();
      connection.write(inbound);
      byte[] response = connection.getInputStream().readNBytes(outbound.length);
      connection.close();

      assertArrayEquals(outbound, response);
      server.get(3, TimeUnit.SECONDS);
      executor.shutdownNow();
    }
  }

  @Test
  void reconnectsAfterDisconnect() throws Exception {
    try (ServerSocket serverSocket = new ServerSocket(0)) {
      int port = serverSocket.getLocalPort();
      CountDownLatch firstAccepted = new CountDownLatch(1);
      CountDownLatch secondAccepted = new CountDownLatch(1);
      ExecutorService executor = Executors.newSingleThreadExecutor();

      Future<?> server = executor.submit(() -> {
        try {
          try (Socket first = serverSocket.accept()) {
            firstAccepted.countDown();
            byte[] firstPayload = first.getInputStream().readNBytes(3);
            assertArrayEquals("one".getBytes(), firstPayload);
          }
          try (Socket second = serverSocket.accept()) {
            secondAccepted.countDown();
            byte[] secondPayload = second.getInputStream().readNBytes(3);
            assertArrayEquals("two".getBytes(), secondPayload);
          }
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });

      TakConnectionManager manager = new TakConnectionManager(new TakServerConnection(new EndPointURL("tcp://127.0.0.1:" + port), Duration.ofSeconds(2)));
      manager.connect();
      manager.write("one".getBytes());
      assertTrue(firstAccepted.await(2, TimeUnit.SECONDS));

      manager.reconnect();
      manager.write("two".getBytes());
      assertTrue(secondAccepted.await(2, TimeUnit.SECONDS));

      manager.close();
      server.get(3, TimeUnit.SECONDS);
      executor.shutdownNow();
    }
  }

  @Test
  void tlsHandshakeFailureProvidesActionableMessage() throws Exception {
    try (ServerSocket plainServer = new ServerSocket(0)) {
      int port = plainServer.getLocalPort();
      ExecutorService executor = Executors.newSingleThreadExecutor();
      Future<?> server = executor.submit(() -> {
        try (Socket ignored = plainServer.accept()) {
          // Non-TLS endpoint used intentionally to force TLS handshake failure.
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });

      TakServerConnection connection = new TakServerConnection(new EndPointURL("ssl://127.0.0.1:" + port), Duration.ofSeconds(2));
      IOException exception = assertThrows(IOException.class, connection::connect);
      assertTrue(exception.getMessage().contains("TLS handshake failed"));
      server.get(3, TimeUnit.SECONDS);
      executor.shutdownNow();
    }
  }

  @Test
  void tlsRoundTripWithDefaultTestKeystore() throws Exception {
    try (var serverSocket = SSLServerSocketFactory.getDefault().createServerSocket(0)) {
      int port = serverSocket.getLocalPort();
      byte[] inbound = "hello".getBytes();
      byte[] outbound = "world".getBytes();
      ExecutorService executor = Executors.newSingleThreadExecutor();

      Future<?> server = executor.submit(() -> {
        try (Socket accepted = serverSocket.accept()) {
          byte[] buffer = accepted.getInputStream().readNBytes(inbound.length);
          assertArrayEquals(inbound, buffer);
          accepted.getOutputStream().write(outbound);
          accepted.getOutputStream().flush();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });

      TakServerConnection connection = new TakServerConnection(new EndPointURL("ssl://127.0.0.1:" + port), Duration.ofSeconds(2));
      connection.connect();
      connection.write(inbound);
      byte[] response = connection.getInputStream().readNBytes(outbound.length);
      connection.close();

      assertArrayEquals(outbound, response);
      server.get(3, TimeUnit.SECONDS);
      executor.shutdownNow();
    }
  }
}
