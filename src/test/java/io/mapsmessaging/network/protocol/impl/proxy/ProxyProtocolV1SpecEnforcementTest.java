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

import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.BufferUnderflowException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class ProxyProtocolV1SpecEnforcementTest {

  @Test
  void proxyEnabled_shortPacket_underflowDoesNotConsumeBytes_v1Probe() throws Exception {
    ProxyProtocolMode proxyProtocol = ProxyProtocolMode.ENABLED;
    ProxyProtocolV1 v1 = new ProxyProtocolV1();
    Packet packet = TestPacketFactory.packetOf(new byte[]{'P', 'R', 'O'}); // < 6 bytes
    int startPos = packet.position();

    ProxyProtocolInfo proxyProtocolInfo = null;
    try {
      proxyProtocolInfo = detectProxy(packet, v1);
    } catch (BufferUnderflowException e) {
      if (proxyProtocol == ProxyProtocolMode.REQUIRED) {
        throw e;
      }
    } catch (ProxyProtocolParseException e) {
      throw new IOException("Failed to parse proxy protocol", e);
    }

    assertNull(proxyProtocolInfo);
    assertEquals(startPos, packet.position(), "Proxy detection must not advance buffer on underflow in ENABLED mode");
  }

  private ProxyProtocolInfo detectProxy(Packet packet, ProxyProtocol proxyProtocol) throws BufferUnderflowException, UnknownHostException, ProxyProtocolParseException {
    if (proxyProtocol.matches(packet)) {
      return proxyProtocol.parse(packet);
    }
    return null;
  }

  @Test
  void parse_acceptsProxyUnknown_consumesHeader_andReturnsNullEndpoints() throws Exception {
    ProxyProtocolV1 v1 = new ProxyProtocolV1();

    String header = "PROXY UNKNOWN\r\n";
    byte[] payload = "PAYLOAD".getBytes(StandardCharsets.US_ASCII);
    Packet packet = TestPacketFactory.packetOf(concat(header.getBytes(StandardCharsets.US_ASCII), payload));

    int startPos = packet.position();

    ProxyProtocolInfo info = v1.parse(packet);

    assertEquals("v1", info.getProtocolVersion());
    assertNull(info.getSource(), "PROXY UNKNOWN must not provide a source address");
    assertNull(info.getDestination(), "PROXY UNKNOWN must not provide a destination address");

    int expectedPos = startPos + header.getBytes(StandardCharsets.US_ASCII).length;
    assertEquals(expectedPos, packet.position(), "Packet position should advance past PROXY UNKNOWN header");
  }

  @Test
  void parse_rejectsInvalidProtoToken() {
    ProxyProtocolV1 v1 = new ProxyProtocolV1();

    Packet packet = TestPacketFactory.packetOf(
        "PROXY POTATO 192.0.2.10 198.51.100.20 12345 443\r\n"
            .getBytes(StandardCharsets.US_ASCII)
    );

    assertThrows(ProxyProtocolParseException.class, () -> v1.parse(packet));
  }

  @Test
  void parse_rejectsIfCrlfNotFoundWithinMaxLength() {
    ProxyProtocolV1 v1 = new ProxyProtocolV1();

    // Spec max line length is 107 bytes incl CRLF.
    // This header is intentionally long and has no CRLF so parser must reject.
    String tooLongNoCrlf = "PROXY TCP4 192.0.2.10 198.51.100.20 12345 443 "
        + "EXTRA EXTRA EXTRA EXTRA EXTRA EXTRA EXTRA EXTRA EXTRA EXTRA EXTRA";

    Packet packet = TestPacketFactory.packetOf(tooLongNoCrlf.getBytes(StandardCharsets.US_ASCII));

    assertThrows(ProxyProtocolParseException.class, () -> v1.parse(packet));
  }

  private static byte[] concat(byte[] a, byte[] b) {
    byte[] out = new byte[a.length + b.length];
    System.arraycopy(a, 0, out, 0, a.length);
    System.arraycopy(b, 0, out, a.length, b.length);
    return out;
  }
}
