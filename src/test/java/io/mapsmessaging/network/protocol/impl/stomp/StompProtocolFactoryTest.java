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

package io.mapsmessaging.network.protocol.impl.stomp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.mapsmessaging.network.io.Packet;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class StompProtocolFactoryTest {

  private final StompProtocolFactory factory = new StompProtocolFactory();

  @Test
  void detectsConnectWithLf() throws Exception {
    assertDetected("CONNECT\n");
  }

  @Test
  void detectsConnectWithCrLf() throws Exception {
    assertDetected("CONNECT\r\n");
  }

  @Test
  void detectsStompWithLf() throws Exception {
    assertDetected("STOMP\n");
  }

  @Test
  void detectsStompWithCrLf() throws Exception {
    assertDetected("STOMP\r\n");
  }

  @Test
  void detectionDoesNotConsumeInput() throws Exception {
    Packet packet = packet("STOMP\r\naccept-version:1.2\r\n");
    int start = packet.position();

    assertTrue(factory.detect(packet));

    assertEquals(start, packet.position());
  }

  @Test
  void rejectsOtherProtocolPrefixes() throws Exception {
    assertFalse(factory.detect(packet("GET / HTTP/1.1\r\n")));
  }

  private void assertDetected(String prefix) throws Exception {
    assertTrue(factory.detect(packet(prefix + "accept-version:1.2\n")));
  }

  private Packet packet(String value) {
    return new Packet(
        ByteBuffer.wrap(value.getBytes(StandardCharsets.US_ASCII)));
  }
}
