package io.mapsmessaging.state.mavlink.packet;

import io.mapsmessaging.mavlink.ProcessedFrame;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MissionCurrentPacketTest {

  @Test
  void mapsMissionProgressAndPrefersMissionId() {
    MissionCurrentPacket packet = new MissionCurrentPacket(frame(Map.of(
        "seq", 4,
        "total", 12,
        "mission_state", 3,
        "mission_mode", 1,
        "mission_id", 99L,
        "opaque_id", 77L
    ), true));

    assertEquals(MavlinkMessageIds.MISSION_CURRENT, packet.getMessageId());
    assertEquals(4, packet.getSequence());
    assertEquals(12, packet.getTotal());
    assertEquals(3, packet.getMissionState());
    assertEquals(1, packet.getMissionMode());
    assertEquals(99L, packet.getMissionId());
    assertTrue(packet.isValid());
  }

  @Test
  void opaqueIdIsFallbackWhenMissionIdIsAbsent() {
    MissionCurrentPacket packet = new MissionCurrentPacket(frame(Map.of(
        "seq", 0,
        "opaque_id", 1234L
    ), true));

    assertEquals(1234L, packet.getMissionId());
    assertEquals(-1, packet.getTotal());
    assertEquals(-1, packet.getMissionState());
    assertEquals(-1, packet.getMissionMode());
  }

  @Test
  void absentIdsAndInvalidFrameExposeSentinels() {
    MissionCurrentPacket packet = new MissionCurrentPacket(frame(Map.of(), false));

    assertEquals(-1, packet.getSequence());
    assertEquals(-1L, packet.getMissionId());
    assertFalse(packet.isValid());
  }

  private static ProcessedFrame frame(Map<String, Object> fields, boolean valid) {
    return new ProcessedFrame("MISSION_CURRENT", null, fields, valid, List.of(), null);
  }
}
