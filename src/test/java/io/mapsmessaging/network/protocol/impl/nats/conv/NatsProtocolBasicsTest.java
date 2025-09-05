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
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NatsProtocolBasicsTest extends BaseTestConfig {

  private static final int PORT = 4222;
  private NatsTestHelpers helper;

  private static final Pattern pongRe = Pattern.compile("PONG");
  private static final Pattern msgRe = Pattern.compile("^MSG\\s+(.+)$");

  @BeforeEach
  void connect() throws Exception {
    helper = new NatsTestHelpers("127.0.0.1", PORT);
  }

  @AfterEach
  void disconnect() throws Exception {
    helper.close();
  }

  @Test
  @Order(1)
  void testPing() throws Exception {
    helper.send("PING\r\n");
    String response = helper.expect(pongRe, 1000);
    assertEquals("PONG", response.trim());
  }

  @Test
  @Order(2)
  void testSingleMsg() throws Exception {
    helper.send("SUB foo 1\r\n");
    helper.send("PUB foo 5\r\nhello\r\n");
    List<String> msgs = helper.expectMsgs(1, 1000);
    assertTrue(msgs.get(0).contains("MSG foo 1"));
  }

  @Test
  @Order(3)
  void testMultipleMsgs() throws Exception {
    helper.send("SUB * 2\r\n");
    helper.send("PUB foo 2\r\nok\r\n");
    List<String> msgs = helper.expectAllMsgs(2, 1000);
    assertEquals(2, msgs.size());
    boolean found = false;
    for (String msg : msgs) {
      found = found || msg.contains("foo");
    }
    assertTrue(found);
  }
}
