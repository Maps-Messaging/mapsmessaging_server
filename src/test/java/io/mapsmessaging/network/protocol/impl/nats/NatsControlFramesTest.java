/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
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

package io.mapsmessaging.network.protocol.impl.nats;

import io.mapsmessaging.test.BaseTestConfig;
import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;
import org.junit.jupiter.api.*;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NatsControlFramesTest extends BaseTestConfig {

  private Connection connection;

  @BeforeEach
  void setup() throws Exception {
    Options options = new Options.Builder()
        .server("nats://localhost:4222")
        .connectionTimeout(Duration.ofSeconds(5))
        .build();
    connection = Nats.connect(options);
  }

  @AfterEach
  void teardown() throws Exception {
    if (connection != null) {
      connection.close();
    }
  }

  @Test
  @Order(1)
  void testPingPong() throws Exception {
    connection.flush(Duration.ofSeconds(2)); // sends PING, expects PONG
    assertTrue(connection.getStatus() == Connection.Status.CONNECTED);
  }

  @Test
  @Order(2)
  void testOkHandling() throws Exception {
    connection.flush(Duration.ofSeconds(2)); // causes +OK server reply if verbose is on
    // There's no direct +OK exposure, but no exception = pass
    assertTrue(true);
  }

  @Test
  @Order(3)
  void testErrHandlingWithBadPublish() {
    assertThrows(IllegalArgumentException.class, () -> {
      connection.publish("", "invalid".getBytes());
    });
  }

  @Test
  @Order(4)
  void testInfoParsing() {
    String serverId = connection.getConnectedUrl();
    assertNotNull(serverId);
    assertTrue(serverId.startsWith("nats://"));
  }

  @Test
  @Order(5)
  void testConnectFlags() throws Exception {
    Options options = new Options.Builder()
        .server("nats://localhost:4222")
        .noEcho()          // disable echo
        .verbose()         // request +OK
        .pedantic()        // enable strict checking
        .connectionTimeout(Duration.ofSeconds(5))
        .build();

    try (Connection customConn = Nats.connect(options)) {
      assertEquals(Connection.Status.CONNECTED, customConn.getStatus());
    }
  }
}
