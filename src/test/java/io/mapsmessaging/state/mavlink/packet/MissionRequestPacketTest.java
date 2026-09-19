package io.mapsmessaging.state.mavlink.packet;

import io.mapsmessaging.mavlink.ProcessedFrame;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MissionRequestPacketTest {

  @Test
  void missionRequestMapsRequiredAndOptionalFields() {
    MissionRequestPacket packet = new MissionRequestPacket(frame(Map.of(
        "target_system", 1,
        "target_component", 2,
        "seq", 7,
        "mission_type", 3
    ), true));

    assertEquals(1, packet.getTargetSystem());
    assertEquals(2, packet.getTargetComponent());
    assertEquals(7, packet.getSequence());
    assertTrue(packet.isMissionTypePresent());
    assertEquals(3, packet.getMissionType());
    assertTrue(packet.isValid());
  }

  @Test
  void absentMissionTypeUsesSentinelAndPresenceFlag() {
    MissionRequestPacket packet = new MissionRequestPacket(frame(Map.of(
        "target_system", 1,
        "target_component", 2,
        "seq", 0
    ), false));

    assertFalse(packet.isMissionTypePresent());
    assertEquals(-1, packet.getMissionType());
    assertFalse(packet.isValid());
  }

  private static ProcessedFrame frame(Map<String, Object> fields, boolean valid) {
    return new ProcessedFrame("MISSION_REQUEST", null, fields, valid, List.of(), null);
  }
}
