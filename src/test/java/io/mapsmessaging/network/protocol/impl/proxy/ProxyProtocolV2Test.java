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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class ProxyProtocolV2Test {

  private static final byte[] SIGNATURE = {
      0x0D, 0x0A, 0x0D, 0x0A, 0x00, 0x0D, 0x0A, 0x51, 0x55, 0x49, 0x54, 0x0A
  };

  @Test
  void matches_returnsTrueForValidSignature() {
    ProxyProtocolV2 v2 = new ProxyProtocolV2();
    byte[] bytes = concat(SIGNATURE, new byte[]{0x21, 0x11, 0x00, 0x00}); // minimal header after signature
    Packet packet = TestPacketFactory.packetOf(bytes);

    assertTrue(v2.matches(packet));
  }

  @Test
  void matches_returnsFalseForShortPacket() {
    ProxyProtocolV2 v2 = new ProxyProtocolV2();
    Packet packet = TestPacketFactory.packetOf(new byte[]{0x01, 0x02, 0x03});

    assertFalse(v2.matches(packet));
  }

  @Test
  void matches_returnsFalseForWrongSignature() {
    ProxyProtocolV2 v2 = new ProxyProtocolV2();
    byte[] badSig = Arrays.copyOf(SIGNATURE, SIGNATURE.length);
    badSig[0] = 0x00;

    byte[] bytes = concat(badSig, new byte[]{0x21, 0x11, 0x00, 0x00});
    Packet packet = TestPacketFactory.packetOf(bytes);

    assertFalse(v2.matches(packet));
  }

  @Test
  void parse_happyPath_ipv4Tcp_parsesAddressesAndConsumesProxyBlock() throws Exception {
    ProxyProtocolV2 v2 = new ProxyProtocolV2();

    byte verCmd = 0x21;      // version 2 (0x2), command PROXY (0x1)
    byte protoFam = 0x11;    // transport=1 (tcp), family=1 (ipv4)
    int len = 12;            // ipv4: src(4)+dst(4)+srcPort(2)+dstPort(2)

    byte[] srcIp = {(byte) 192, 0, 2, 10};
    byte[] dstIp = {(byte) 198, 51, 100, 20};
    int srcPort = 12345;
    int dstPort = 443;

    ByteBuffer buf = ByteBuffer.allocate(12 + 1 + 1 + 2 + len + 3).order(ByteOrder.BIG_ENDIAN);
    buf.put(SIGNATURE);
    buf.put(verCmd);
    buf.put(protoFam);
    buf.putShort((short) len);

    buf.put(srcIp);
    buf.put(dstIp);
    buf.putShort((short) srcPort);
    buf.putShort((short) dstPort);

    // payload bytes after PROXY block
    buf.put(new byte[]{0x7A, 0x7B, 0x7C});

    Packet packet = TestPacketFactory.packetOf(buf.array());
    int startPos = packet.position();

    ProxyProtocolInfo info = v2.parse(packet);

    assertEquals("tcp4", info.getProtocolVersion());
    assertEquals("192.0.2.10", info.getSource().getAddress().getHostAddress());
    assertEquals(srcPort, info.getSource().getPort());
    assertEquals("198.51.100.20", info.getDestination().getAddress().getHostAddress());
    assertEquals(dstPort, info.getDestination().getPort());

    int expectedAdvance = 12 + 1 + 1 + 2 + len;
    assertEquals(startPos + expectedAdvance, packet.position(), "Packet position should be after the PROXY v2 block");
  }

  @Test
  void parse_invalidVersionNibble_throwsIllegalArgumentException() {
    ProxyProtocolV2 v2 = new ProxyProtocolV2();

    byte verCmd = 0x11;      // version nibble = 1 (not 2)
    byte protoFam = 0x11;
    int len = 12;

    ByteBuffer buf = ByteBuffer.allocate(12 + 1 + 1 + 2 + len).order(ByteOrder.BIG_ENDIAN);
    buf.put(SIGNATURE);
    buf.put(verCmd);
    buf.put(protoFam);
    buf.putShort((short) len);
    buf.put(new byte[len]);

    Packet packet = TestPacketFactory.packetOf(buf.array());

    assertThrows(ProxyProtocolParseException.class, () -> v2.parse(packet));
  }

  @Test
  void parse_unsupportedFamily_throwsProxyProtocolParseException() {
    ProxyProtocolV2 v2 = new ProxyProtocolV2();

    byte verCmd = 0x21;
    byte protoFam = 0x13; // transport=1 (tcp), family=3 (unsupported in your parser)
    int len = 0;

    ByteBuffer buf = ByteBuffer.allocate(12 + 1 + 1 + 2 + len).order(ByteOrder.BIG_ENDIAN);
    buf.put(SIGNATURE);
    buf.put(verCmd);
    buf.put(protoFam);
    buf.putShort((short) len);

    Packet packet = TestPacketFactory.packetOf(buf.array());

    assertThrows(ProxyProtocolParseException.class, () -> v2.parse(packet));
  }

  private static byte[] concat(byte[] a, byte[] b) {
    byte[] out = new byte[a.length + b.length];
    System.arraycopy(a, 0, out, 0, a.length);
    System.arraycopy(b, 0, out, a.length, b.length);
    return out;
  }
}
