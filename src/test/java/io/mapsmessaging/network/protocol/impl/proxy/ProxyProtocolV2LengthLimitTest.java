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

class ProxyProtocolV2LengthLimitTest {

  private static final byte[] SIGNATURE = {
      0x0D, 0x0A, 0x0D, 0x0A, 0x00, 0x0D, 0x0A, 0x51, 0x55, 0x49, 0x54, 0x0A
  };

  private static final int MAX_PROXY_DATA_LENGTH = 512;

  @Test
  void parse_rejectsLengthGreaterThanMax() {
    ProxyProtocolV2 v2 = new ProxyProtocolV2();

    byte verCmd = 0x21;   // ver=2, cmd=PROXY
    byte protoFam = 0x00; // transport=UNSPEC, family=UNSPEC

    int len = MAX_PROXY_DATA_LENGTH + 1;

    ByteBuffer buf = ByteBuffer.allocate(12 + 1 + 1 + 2).order(ByteOrder.BIG_ENDIAN);
    buf.put(SIGNATURE);
    buf.put(verCmd);
    buf.put(protoFam);
    buf.putShort((short) len);

    Packet packet = TestPacketFactory.packetOf(buf.array());

    ProxyProtocolParseException ex = assertThrows(ProxyProtocolParseException.class, () -> v2.parse(packet));
    assertTrue(ex.getMessage().contains("length too large"), ex.getMessage());
  }

  @Test
  void parse_acceptsLengthEqualToMax_forUnspecFamilyAndReturnsNullEndpoints() throws Exception {
    ProxyProtocolV2 v2 = new ProxyProtocolV2();

    byte verCmd = 0x21;   // ver=2, cmd=PROXY
    byte protoFam = 0x00; // transport=UNSPEC, family=UNSPEC

    int len = MAX_PROXY_DATA_LENGTH;

    ByteBuffer buf = ByteBuffer.allocate(12 + 1 + 1 + 2 + len + 3).order(ByteOrder.BIG_ENDIAN);
    buf.put(SIGNATURE);
    buf.put(verCmd);
    buf.put(protoFam);
    buf.putShort((short) len);

    // Fill the data block with deterministic bytes (treated as TLVs/opaque when family=UNSPEC)
    for (int i = 0; i < len; i++) {
      buf.put((byte) (i & 0xFF));
    }

    // Payload after PROXY block
    buf.put(new byte[]{0x01, 0x02, 0x03});

    Packet packet = TestPacketFactory.packetOf(buf.array());
    int startPos = packet.position();

    ProxyProtocolInfo info = v2.parse(packet);

    assertNull(info.getSource());
    assertNull(info.getDestination());

    int expectedAdvance = 12 + 1 + 1 + 2 + len;
    assertEquals(startPos + expectedAdvance, packet.position(), "Packet position should advance past v2 header + len");
  }
}
