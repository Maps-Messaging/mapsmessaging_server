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

import static org.junit.jupiter.api.Assertions.*;

class ProxyProtocolV2SpecEnforcementTest {

  private static final byte[] SIGNATURE = {
      0x0D, 0x0A, 0x0D, 0x0A, 0x00, 0x0D, 0x0A, 0x51, 0x55, 0x49, 0x54, 0x0A
  };

  @Test
  void parse_localCommand_returnsNullEndpoints_andConsumesProxyBlock() throws Exception {
    ProxyProtocolV2 v2 = new ProxyProtocolV2();

    // ver=2, cmd=LOCAL (0x0)
    byte verCmd = 0x20;

    // Transport/family can be UNSPEC for LOCAL; spec says ignore address info.
    byte protoFam = 0x00;

    int len = 0;

    ByteBuffer buf = ByteBuffer.allocate(12 + 1 + 1 + 2 + len + 1).order(ByteOrder.BIG_ENDIAN);
    buf.put(SIGNATURE);
    buf.put(verCmd);
    buf.put(protoFam);
    buf.putShort((short) len);
    buf.put((byte) 0x7A); // payload

    Packet packet = TestPacketFactory.packetOf(buf.array());
    int startPos = packet.position();

    ProxyProtocolInfo info = v2.parse(packet);

    assertNull(info.getSource(), "LOCAL command must not provide a proxied source");
    assertNull(info.getDestination(), "LOCAL command must not provide a proxied destination");

    int expectedAdvance = 12 + 1 + 1 + 2 + len;
    assertEquals(startPos + expectedAdvance, packet.position(), "Packet position should be after the PROXY v2 fixed header + len");
  }

  @Test
  void parse_acceptsAfUnspec_withTlvOnly_returnsNullEndpoints_andSkipsTlv() throws Exception {
    ProxyProtocolV2 v2 = new ProxyProtocolV2();

    // ver=2, cmd=PROXY (0x1)
    byte verCmd = 0x21;

    // transport=UNSPEC (0x0), family=UNSPEC (0x0)
    byte protoFam = 0x00;

    // Provide TLV only: type=0x01, length=0x0003, value=0xAA 0xBB 0xCC
    byte tlvType = 0x01;
    short tlvLen = 3;

    int len = 1 + 2 + tlvLen; // type + length + value

    ByteBuffer buf = ByteBuffer.allocate(12 + 1 + 1 + 2 + len + 2).order(ByteOrder.BIG_ENDIAN);
    buf.put(SIGNATURE);
    buf.put(verCmd);
    buf.put(protoFam);
    buf.putShort((short) len);

    buf.put(tlvType);
    buf.putShort(tlvLen);
    buf.put(new byte[]{(byte) 0xAA, (byte) 0xBB, (byte) 0xCC});

    buf.put(new byte[]{0x11, 0x22}); // payload after TLV

    Packet packet = TestPacketFactory.packetOf(buf.array());
    int startPos = packet.position();

    ProxyProtocolInfo info = v2.parse(packet);

    assertNull(info.getSource(), "AF_UNSPEC must not require address parsing");
    assertNull(info.getDestination(), "AF_UNSPEC must not require address parsing");

    int expectedAdvance = 12 + 1 + 1 + 2 + len;
    assertEquals(startPos + expectedAdvance, packet.position(), "Packet position should advance past TLVs");
  }
}
