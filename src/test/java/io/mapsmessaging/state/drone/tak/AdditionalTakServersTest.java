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

package io.mapsmessaging.state.drone.tak;

import io.mapsmessaging.dto.rest.config.protocol.impl.TakServerDTO;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AdditionalTakServersTest {

  private static final int READ_TIMEOUT_MS = 5000;
  private static final String COT_EVENT =
      "<event version=\"2.0\" uid=\"nquiringminds-maps-drone\" type=\"a-f-A-M-F-Q\" how=\"m-g\">"
          + "<point lat=\"51.5\" lon=\"-0.1\" hae=\"120\" ce=\"5\" le=\"5\"/>"
          + "<detail><remarks>MTI: go</remarks></detail></event>";

  @Test
  void everyServer_receivesTheIdenticalCotEvent() throws IOException {
    try (ServerSocket first = listen(); ServerSocket second = listen()) {
      AdditionalTakServers servers = new AdditionalTakServers(
          List.of(server(first.getLocalPort()), server(second.getLocalPort())), server -> null);
      try {
        servers.accept(COT_EVENT);

        assertEquals(COT_EVENT, readLine(first));
        assertEquals(COT_EVENT, readLine(second));
      } finally {
        servers.close();
      }
    }
  }

  @Test
  void unreachableServer_doesNotDelayDeliveryToTheOthers() throws IOException {
    int deadPort = unusedPort();
    try (ServerSocket live = listen()) {
      AdditionalTakServers servers = new AdditionalTakServers(
          List.of(server(deadPort), server(live.getLocalPort())), server -> null);
      try {
        servers.accept(COT_EVENT);

        assertEquals(COT_EVENT, readLine(live));
        assertFalse(servers.getConnections().get(0).isConnected());
      } finally {
        servers.close();
      }
    }
  }

  @Test
  void sslSocketFactory_isResolvedOncePerServer() {
    List<String> resolvedHosts = new ArrayList<>();
    TakServerDTO first = server(unusedPort());
    TakServerDTO second = server(unusedPort());
    second.setHostname("127.0.0.1");
    first.setHostname("localhost");

    AdditionalTakServers servers = new AdditionalTakServers(List.of(first, second), server -> {
      resolvedHosts.add(server.getHostname());
      return null;
    });
    try {
      assertEquals(List.of("localhost", "127.0.0.1"), resolvedHosts);
      assertEquals(2, servers.getConnections().size());
    } finally {
      servers.close();
    }
  }

  @Test
  void none_hasNoConnectionsAndIgnoresEvents() {
    AdditionalTakServers servers = AdditionalTakServers.none();

    assertTrue(servers.isEmpty());
    assertTrue(servers.getConnections().isEmpty());
    assertDoesNotThrow(() -> servers.accept(COT_EVENT));
    assertDoesNotThrow(servers::close);
  }

  @Test
  void nullServerList_isTreatedAsEmpty() {
    AdditionalTakServers servers = new AdditionalTakServers(null, server -> null);

    assertTrue(servers.isEmpty());
  }

  private static TakServerDTO server(int port) {
    TakServerDTO server = new TakServerDTO();
    server.setHostname("127.0.0.1");
    server.setPort(port);
    return server;
  }

  private static ServerSocket listen() throws IOException {
    ServerSocket serverSocket = new ServerSocket(0, 5, InetAddress.getLoopbackAddress());
    serverSocket.setSoTimeout(READ_TIMEOUT_MS);
    return serverSocket;
  }

  private static int unusedPort() {
    try (ServerSocket serverSocket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
      return serverSocket.getLocalPort();
    } catch (IOException exception) {
      throw new IllegalStateException(exception);
    }
  }

  private static String readLine(ServerSocket serverSocket) throws IOException {
    try (Socket client = serverSocket.accept()) {
      client.setSoTimeout(READ_TIMEOUT_MS);
      BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
      return reader.readLine();
    }
  }
}
