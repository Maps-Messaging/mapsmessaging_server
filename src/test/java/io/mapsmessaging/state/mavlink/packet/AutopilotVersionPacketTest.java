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

package io.mapsmessaging.state.mavlink.packet;

import io.mapsmessaging.mavlink.ProcessedFrame;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AutopilotVersionPacketTest {

  @Test
  void versionFieldsAreMappedWithoutNarrowing() {
    AutopilotVersionPacket packet = new AutopilotVersionPacket(frame(Map.of(
        "capabilities", 0x1_0000_0000L,
        "flight_sw_version", 0x01020304L,
        "middleware_sw_version", 0x05060708L,
        "os_sw_version", 0x11121314L,
        "board_version", 0x21222324L,
        "vendor_id", 26,
        "product_id", 42,
        "uid", 0x1234_5678_9ABCDEFL,
        "uid2", "0011223344556677"
    ), true));

    assertEquals(MavlinkMessageIds.AUTOPILOT_VERSION, packet.getMessageId());
    assertEquals(0x1_0000_0000L, packet.getCapabilities());
    assertEquals(0x01020304L, packet.getFlightSoftwareVersion());
    assertEquals(0x05060708L, packet.getMiddlewareSoftwareVersion());
    assertEquals(0x11121314L, packet.getOsSoftwareVersion());
    assertEquals(0x21222324L, packet.getBoardVersion());
    assertEquals(26L, packet.getVendorId());
    assertEquals(42L, packet.getProductId());
    assertEquals(0x1234_5678_9ABCDEFL, packet.getUid());
    assertEquals("0011223344556677", packet.getUid2());
    assertTrue(packet.isValid());
  }

  @Test
  void absentOptionalFieldsUsePacketBaseDefaults() {
    AutopilotVersionPacket packet =
        new AutopilotVersionPacket(frame(Map.of(), false));

    assertEquals(-1L, packet.getCapabilities());
    assertEquals(-1L, packet.getVendorId());
    assertEquals(-1L, packet.getUid());
    assertNull(packet.getUid2());
    assertFalse(packet.isValid());
  }

  private static ProcessedFrame frame(Map<String, Object> fields, boolean valid) {
    return new ProcessedFrame("AUTOPILOT_VERSION", null, fields, valid, List.of(), null);
  }
}
