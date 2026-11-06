package io.mapsmessaging.state.mavlink.packet;

import io.mapsmessaging.mavlink.ProcessedFrame;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MissionRequestIntPacketTest {

  @Test
  void missionRequestIntMapsRequiredAndOptionalFields() {
    MissionRequestIntPacket packet = new MissionRequestIntPacket(frame(Map.of(
        "target_system", 11,
        "target_component", 22,
        "seq", 123,
        "mission_type", 1
    ), true));

    assertEquals(11, packet.getTargetSystem());
    assertEquals(22, packet.getTargetComponent());
    assertEquals(123, packet.getSequence());
    assertTrue(packet.isMissionTypePresent());
    assertEquals(1, packet.getMissionType());
    assertTrue(packet.isValid());
  }

  @Test
  void missingFieldsExposePacketSentinels() {
    MissionRequestIntPacket packet = new MissionRequestIntPacket(frame(Map.of(), false));

    assertEquals(-1, packet.getTargetSystem());
    assertEquals(-1, packet.getTargetComponent());
    assertEquals(-1, packet.getSequence());
    assertFalse(packet.isMissionTypePresent());
    assertEquals(-1, packet.getMissionType());
    assertFalse(packet.isValid());
  }

  private static ProcessedFrame frame(Map<String, Object> fields, boolean valid) {
    return new ProcessedFrame("MISSION_REQUEST_INT", null, fields, valid, List.of(), null);
  }
}
