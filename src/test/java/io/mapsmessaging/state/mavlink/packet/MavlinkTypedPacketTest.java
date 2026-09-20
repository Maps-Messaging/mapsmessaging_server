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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MavlinkTypedPacketTest {

  @Test
  void missionAckMapsEveryDefinedResultName() {
    Map<Integer, String> expected = new LinkedHashMap<>();
    expected.put(0, "ACCEPTED");
    expected.put(1, "ERROR");
    expected.put(2, "UNSUPPORTED_FRAME");
    expected.put(3, "UNSUPPORTED");
    expected.put(4, "NO_SPACE");
    expected.put(5, "INVALID");
    expected.put(6, "INVALID_PARAM1");
    expected.put(7, "INVALID_PARAM2");
    expected.put(8, "INVALID_PARAM3");
    expected.put(9, "INVALID_PARAM4");
    expected.put(10, "INVALID_PARAM5_X");
    expected.put(11, "INVALID_PARAM6_Y");
    expected.put(12, "INVALID_PARAM7");
    expected.put(13, "INVALID_SEQUENCE");
    expected.put(14, "DENIED");
    expected.put(15, "OPERATION_CANCELLED");

    for (Map.Entry<Integer, String> entry : expected.entrySet()) {
      MissionAckPacket packet = new MissionAckPacket(frame(Map.of(
          "target_system", 1,
          "target_component", 2,
          "type", entry.getKey(),
          "mission_type", 0
      ), true));

      assertEquals(entry.getValue(), packet.getTypeName());
      assertEquals(entry.getKey() == 0, packet.isAccepted());
      assertEquals(entry.getKey() != 0, packet.isRejected());
      assertTrue(packet.isMissionTypePresent());
      assertTrue(packet.isValid());
    }
  }

  @Test
  void missionAckHandlesUnknownAndAbsentOptionalMissionType() {
    MissionAckPacket packet = new MissionAckPacket(frame(Map.of(
        "target_system", 1,
        "target_component", 2,
        "type", 99
    ), false));

    assertEquals("UNKNOWN", packet.getTypeName());
    assertFalse(packet.isAccepted());
    assertTrue(packet.isRejected());
    assertFalse(packet.isMissionTypePresent());
    assertEquals(-1, packet.getMissionType());
    assertFalse(packet.isValid());
  }

  @Test
  void statusTextMapsAllSeverityPredicatesAndOptionalFields() {
    for (int severity = 0; severity <= 7; severity++) {
      StatusTextPacket packet = statusPacket(severity);

      assertEquals(severity == 0, packet.isEmergency());
      assertEquals(severity == 1, packet.isAlert());
      assertEquals(severity == 2, packet.isCritical());
      assertEquals(severity == 3, packet.isError());
      assertEquals(severity == 4, packet.isWarning());
      assertEquals(severity == 5, packet.isNotice());
      assertEquals(severity == 6, packet.isInfo());
      assertEquals(severity == 7, packet.isDebug());
      assertEquals(severity <= 4, packet.isProblem());
      assertEquals(MavlinkMessageIds.STATUSTEXT, packet.getMessageId());
      assertEquals("GPS Glitch", packet.getText());
      assertTrue(packet.isIdPresent());
      assertTrue(packet.isChunkSequencePresent());
    }

    StatusTextPacket unknown = statusPacket(8);
    assertFalse(unknown.isProblem());
    assertFalse(unknown.isDebug());
  }

  @Test
  void statusTextDecodesNullTerminatedByteArrayAndAbsentExtensions() {
    StatusTextPacket packet = new StatusTextPacket(frame(Map.of(
        "severity", 6,
        "text", new byte[]{'O', 'K', 0, 'X'}
    ), true));

    assertEquals("OK", packet.getText());
    assertFalse(packet.isIdPresent());
    assertFalse(packet.isChunkSequencePresent());
    assertEquals(-1, packet.getId());
    assertEquals(-1, packet.getChunkSequence());
  }

  @Test
  void batteryStatusConvertsKnownValuesAndFiltersUnknownVoltages() {
    Map<String, Object> fields = new HashMap<>();
    fields.put("id", 2);
    fields.put("battery_function", 1);
    fields.put("type", 3);
    fields.put("temperature", 2534);
    fields.put("voltages", new int[]{12000, 65535, 11800});
    fields.put("current_battery", 1234);
    fields.put("current_consumed", 100);
    fields.put("energy_consumed", 200);
    fields.put("battery_remaining", 75);
    fields.put("time_remaining", 600);
    fields.put("charge_state", 2);
    fields.put("voltages_ext", List.of(5000, 4900));
    fields.put("mode", 1);
    fields.put("fault_bitmask", 4L);

    BatteryStatusPacket packet = new BatteryStatusPacket(frame(fields, true));

    assertEquals(MavlinkMessageIds.BATTERY_STATUS, packet.getMessageId());
    assertTrue(packet.isTemperatureKnown());
    assertEquals(25.34, packet.getTemperatureDegreesCelsius(), 0.0001);
    assertTrue(packet.isCurrentBatteryKnown());
    assertEquals(12.34, packet.getCurrentBatteryAmps(), 0.0001);
    assertTrue(packet.isBatteryRemainingKnown());
    assertArrayEquals(new int[]{12000, 11800}, packet.getKnownVoltages());
    assertTrue(packet.isTimeRemainingPresent());
    assertTrue(packet.isChargeStatePresent());
    assertTrue(packet.isVoltagesExtPresent());
    assertTrue(packet.isModePresent());
    assertTrue(packet.isFaultBitmaskPresent());
    assertTrue(packet.hasFaults());
    assertTrue(packet.isValid());
  }

  @Test
  void batteryStatusPreservesUnknownSentinelsAndAbsentExtensions() {
    Map<String, Object> fields = new HashMap<>();
    fields.put("temperature", 32767);
    fields.put("voltages", new int[]{65535});
    fields.put("current_battery", -1);
    fields.put("battery_remaining", -1);

    BatteryStatusPacket packet = new BatteryStatusPacket(frame(fields, false));

    assertFalse(packet.isTemperatureKnown());
    assertTrue(Double.isNaN(packet.getTemperatureDegreesCelsius()));
    assertFalse(packet.isCurrentBatteryKnown());
    assertTrue(Double.isNaN(packet.getCurrentBatteryAmps()));
    assertFalse(packet.isBatteryRemainingKnown());
    assertArrayEquals(new int[0], packet.getKnownVoltages());
    assertFalse(packet.isTimeRemainingPresent());
    assertFalse(packet.isChargeStatePresent());
    assertFalse(packet.isVoltagesExtPresent());
    assertFalse(packet.isModePresent());
    assertFalse(packet.isFaultBitmaskPresent());
    assertFalse(packet.hasFaults());
    assertFalse(packet.isValid());
  }

  private static StatusTextPacket statusPacket(int severity) {
    return new StatusTextPacket(frame(Map.of(
        "severity", severity,
        "text", "GPS Glitch",
        "id", 42,
        "chunk_seq", 1
    ), true));
  }

  private static ProcessedFrame frame(Map<String, Object> fields, boolean valid) {
    return new ProcessedFrame(
        "TEST",
        null,
        fields,
        valid,
        List.of(),
        null
    );
  }
}
