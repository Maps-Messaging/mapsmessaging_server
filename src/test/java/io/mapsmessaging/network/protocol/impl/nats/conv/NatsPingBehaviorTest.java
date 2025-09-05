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

package io.mapsmessaging.network.protocol.impl.nats.conv;

import io.mapsmessaging.test.BaseTestConfig;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NatsPingBehaviorTest extends BaseTestConfig {

  private static final int PING_TEST_PORT = 5222;
  private static final Pattern pingRe = Pattern.compile("PING");
  private static final Pattern errRe = Pattern.compile("-ERR");

  private NatsTestHelpers helper;

  @BeforeEach
  void setup() throws Exception {
    helper = new NatsTestHelpers("127.0.0.1", PING_TEST_PORT);
  }

  @AfterEach
  void teardown() throws Exception {
    helper.close();
  }

  @Test
  @Order(1)
  void testPingIntervalAndDisconnect() throws Exception {
    for (int i = 0; i < 2; i++) {
      helper.expect(pingRe, 6000);
    }

    helper.expect(errRe, 6000); // should error after no PONGs

// Wait until server closes connection
    boolean closed = false;
    try {
      int result = helper.getReader().read(); // should return -1
      closed = (result == -1);
    } catch (IOException e) {
      closed = true; // socket forcibly closed (RST)
    }
    assertTrue(closed, "Expected server to close the connection");
  }

  @Test
  @Order(2)
  void testUnpromptedPongIgnored() throws Exception {
    // Send many unsolicited PONGs
    for (int i = 0; i < 100; i++) {
      helper.send("PONG\r\n");
    }

    // Still expect PINGs from server as if no PONGs were received
    for (int i = 0; i < 2; i++) {
      helper.expect(pingRe, 6000);
    }

    helper.expect(errRe, 6000);

    boolean closed = false;
    try {
      int result = helper.getReader().read(); // should return -1
      closed = (result == -1);
    } catch (IOException e) {
      closed = true; // socket forcibly closed (RST)
    }
    assertTrue(closed, "Expected server to close the connection");
  }
}
