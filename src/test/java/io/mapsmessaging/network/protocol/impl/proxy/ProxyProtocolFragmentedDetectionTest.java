/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commonsclause
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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class ProxyProtocolFragmentedDetectionTest {

  @Test
  void v1_required_underflowUntilComplete_enabledGivesUp() throws Exception {
    byte[] fullHeader = buildV1Header();
    assertTrue(fullHeader.length > 6);

    // REQUIRED: underflow until we have the complete header (incl CRLF), then parse ok.
    for (int i = 1; i < fullHeader.length; i++) {
      Packet packet = TestPacketFactory.packetOf(slice(fullHeader, i));
      assertThrows(BufferUnderflowException.class, () -> detect(ProxyProtocolMode.REQUIRED, packet),
          "REQUIRED: expected BufferUnderflow at fragment size " + i);
    }

    Packet complete = TestPacketFactory.packetOf(fullHeader);
    ProxyProtocolInfo info = detect(ProxyProtocolMode.REQUIRED, complete);
    assertNotNull(info);
    assertEquals("v1", info.getProtocolVersion());
    assertNotNull(info.getSource());
    assertNotNull(info.getDestination());

    // ENABLED: give up on underflow and return null (do not block protocol).
    for (int i = 1; i < fullHeader.length; i++) {
      Packet packet = TestPacketFactory.packetOf(slice(fullHeader, i));
      ProxyProtocolInfo enabledInfo = assertDoesNotThrow(() -> detect(ProxyProtocolMode.ENABLED, packet));
      assertNull(enabledInfo, "ENABLED: expected null (give up) at fragment size " + i);
      assertEquals(0, packet.position(), "ENABLED: must not consume bytes when giving up");
    }

    Packet enabledComplete = TestPacketFactory.packetOf(fullHeader);
    ProxyProtocolInfo enabledParsed = detect(ProxyProtocolMode.ENABLED, enabledComplete);
    assertNotNull(enabledParsed);
    assertEquals("v1", enabledParsed.getProtocolVersion());
  }

  @Test
  void v2_required_underflowUntilComplete_enabledGivesUp() throws Exception {
    byte[] fullHeader = buildV2HeaderTcp4();
    assertTrue(fullHeader.length >= 16);

    // REQUIRED: underflow until we have the complete header (sig+ver/cmd+fam+len+addr block), then parse ok.
    for (int i = 1; i < fullHeader.length; i++) {
      Packet packet = TestPacketFactory.packetOf(slice(fullHeader, i));
      assertThrows(BufferUnderflowException.class, () -> detect(ProxyProtocolMode.REQUIRED, packet),
          "REQUIRED: expected BufferUnderflow at fragment size " + i);
    }

    Packet complete = TestPacketFactory.packetOf(fullHeader);
    ProxyProtocolInfo info = detect(ProxyProtocolMode.REQUIRED, complete);
    assertNotNull(info);
    assertNotNull(info.getSource());
    assertNotNull(info.getDestination());

    // ENABLED: give up on underflow and return null.
    for (int i = 1; i < fullHeader.length; i++) {
      Packet packet = TestPacketFactory.packetOf(slice(fullHeader, i));
      ProxyProtocolInfo enabledInfo = assertDoesNotThrow(() -> detect(ProxyProtocolMode.ENABLED, packet));
      assertNull(enabledInfo, "ENABLED: expected null (give up) at fragment size " + i);
      assertEquals(0, packet.position(), "ENABLED: must not consume bytes when giving up");
    }

    Packet enabledComplete = TestPacketFactory.packetOf(fullHeader);
    ProxyProtocolInfo enabledParsed = detect(ProxyProtocolMode.ENABLED, enabledComplete);
    assertNotNull(enabledParsed);
    assertNotNull(enabledParsed.getSource());
    assertNotNull(enabledParsed.getDestination());
  }

  /**
   * This wrapper mirrors your mode behavior:
   * - Underflow: REQUIRED rethrows, ENABLED swallows
   * - ParseException: always becomes IOException (as per your snippet)
   */
  private ProxyProtocolInfo detect(ProxyProtocolMode proxyProtocol, Packet packet) throws IOException {
    ProxyProtocolInfo proxyProtocolInfo;
    try {
      proxyProtocolInfo = (proxyProtocol != ProxyProtocolMode.DISABLED) ? detectProxy(packet) : null;
    } catch (BufferUnderflowException e) {
      if (proxyProtocol == ProxyProtocolMode.REQUIRED) {
        throw e;
      }
      return null;
    } catch (ProxyProtocolParseException e) {
      throw new IOException("Failed to parse proxy protocol", e);
    }
    return proxyProtocolInfo;
  }

  /**
   * Minimal detectProxy implementation for the test (uses your v1/v2 classes).
   * IMPORTANT: must not consume bytes on non-match / underflow / parse failure.
   */
  private ProxyProtocolInfo detectProxy(Packet packet) throws ProxyProtocolParseException, UnknownHostException {
    ByteBuffer buffer = packet.getRawBuffer();
    buffer.mark();
    try {
      ProxyProtocol v2 = new ProxyProtocolV2();
      if (v2.matches(packet)) {
        return v2.parse(packet);
      }

      ProxyProtocol v1 = new ProxyProtocolV1();
      if (v1.matches(packet)) {
        return v1.parse(packet);
      }

      buffer.reset();
      return null;
    } catch (BufferUnderflowException e) {
      buffer.reset();
      throw e;
    } catch (ProxyProtocolParseException|RuntimeException e) {
      buffer.reset();
      throw e;
    }
  }

  private static byte[] buildV1Header() {
    // Valid v1 header (no payload needed for these tests)
    String header = "PROXY TCP4 192.0.2.10 198.51.100.20 12345 443\r\n";
    return header.getBytes(StandardCharsets.US_ASCII);
  }

  private static byte[] buildV2HeaderTcp4() {
    byte[] signature = new byte[]{
        0x0D, 0x0A, 0x0D, 0x0A, 0x00, 0x0D, 0x0A, 0x51, 0x55, 0x49, 0x54, 0x0A
    };

    byte verCmd = 0x21;       // ver=2, cmd=PROXY
    byte protoFam = 0x11;     // transport=STREAM (TCP), family=INET4
    short len = 12;           // IPv4 addr block length

    byte[] srcIp = new byte[]{(byte) 192, 0, 2, 10};
    byte[] dstIp = new byte[]{(byte) 198, 51, 100, 20};
    short srcPort = (short) 12345;
    short dstPort = (short) 443;

    ByteBuffer buf = ByteBuffer.allocate(12 + 1 + 1 + 2 + len).order(ByteOrder.BIG_ENDIAN);
    buf.put(signature);
    buf.put(verCmd);
    buf.put(protoFam);
    buf.putShort(len);
    buf.put(srcIp);
    buf.put(dstIp);
    buf.putShort(srcPort);
    buf.putShort(dstPort);
    return buf.array();
  }

  private static byte[] slice(byte[] input, int length) {
    byte[] out = new byte[length];
    System.arraycopy(input, 0, out, 0, length);
    return out;
  }
}
