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

package io.mapsmessaging.network.protocol.impl.proxy;

import io.mapsmessaging.network.io.Packet;
import org.junit.jupiter.api.Test;

import java.nio.BufferUnderflowException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class ProxyProtocolV1Test {

  @Test
  void matches_returnsTrueForProxyPrefix() {
    ProxyProtocolV1 v1 = new ProxyProtocolV1();
    Packet packet = TestPacketFactory.packetOf("PROXY TCP4 1.2.3.4 5.6.7.8 12 34\r\nX".getBytes(StandardCharsets.US_ASCII));

    assertTrue(v1.matches(packet));
  }

  @Test
  void matches_returnsFalseForNonProxyPrefix() {
    ProxyProtocolV1 v1 = new ProxyProtocolV1();
    Packet packet = TestPacketFactory.packetOf("NOTPRO something\r\n".getBytes(StandardCharsets.US_ASCII));

    assertFalse(v1.matches(packet));
  }

  @Test
  void parse_happyPath_parsesAddressesAndConsumesHeader() throws Exception {
    ProxyProtocolV1 v1 = new ProxyProtocolV1();

    String header = "PROXY TCP4 192.0.2.10 198.51.100.20 12345 443\r\n";
    byte[] payload = "PAYLOAD".getBytes(StandardCharsets.US_ASCII);

    byte[] bytes = concat(header.getBytes(StandardCharsets.US_ASCII), payload);
    Packet packet = TestPacketFactory.packetOf(bytes);

    int startPos = packet.position();

    ProxyProtocolInfo info = v1.parse(packet);

    assertEquals("v1", info.getProtocolVersion());

    assertEquals("192.0.2.10", info.getSource().getHostString());
    assertEquals(12345, info.getSource().getPort());

    assertEquals("198.51.100.20", info.getDestination().getHostString());
    assertEquals(443, info.getDestination().getPort());

    int expectedPos = startPos + header.getBytes(StandardCharsets.US_ASCII).length;
    assertEquals(expectedPos, packet.position(), "Packet position should be after the PROXY v1 header");
  }

  @Test
  void parse_invalidHeader_throwsIllegalArgumentException() {
    ProxyProtocolV1 v1 = new ProxyProtocolV1();

    // Missing fields (needs at least 6 tokens)
    Packet packet = TestPacketFactory.packetOf("PROXY TCP4 1.2.3.4\r\n".getBytes(StandardCharsets.US_ASCII));

    ProxyProtocolParseException ex = assertThrows(ProxyProtocolParseException.class, () -> v1.parse(packet));
    assertTrue(ex.getMessage().contains("Invalid PROXY v1 header"));
  }

  @Test
  void parse_headerWithoutCrlf_throwsBufferUnderflowException() {
    ProxyProtocolV1 v1 = new ProxyProtocolV1();

    Packet packet = TestPacketFactory.packetOf(
        "PROXY TCP4 1.2.3.4 5.6.7.8 123 456".getBytes(StandardCharsets.US_ASCII)
    );

    assertThrows(BufferUnderflowException.class, () -> v1.parse(packet));
  }

  @Test
  void parse_headerExceedsMaxWithoutCrlf_throwsProxyProtocolParseException() {
    ProxyProtocolV1 v1 = new ProxyProtocolV1();

    byte[] bytes = new byte[108];
    for (int i = 0; i < bytes.length; i++) {
      bytes[i] = 'A';
    }
    // Starts with PROXY so parse is attempted, but no CRLF and we hit max length
    bytes[0] = 'P';
    bytes[1] = 'R';
    bytes[2] = 'O';
    bytes[3] = 'X';
    bytes[4] = 'Y';
    bytes[5] = ' ';

    Packet packet = TestPacketFactory.packetOf(bytes);

    assertThrows(ProxyProtocolParseException.class, () -> v1.parse(packet));
  }


  private static byte[] concat(byte[] a, byte[] b) {
    byte[] out = new byte[a.length + b.length];
    System.arraycopy(a, 0, out, 0, a.length);
    System.arraycopy(b, 0, out, a.length, b.length);
    return out;
  }
}
