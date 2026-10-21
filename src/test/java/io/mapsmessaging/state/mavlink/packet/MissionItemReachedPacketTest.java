package io.mapsmessaging.state.mavlink.packet;

import io.mapsmessaging.mavlink.ProcessedFrame;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MissionItemReachedPacketTest {

  @Test
  void mapsReachedSequence() {
    MissionItemReachedPacket packet =
        new MissionItemReachedPacket(frame(Map.of("seq", 17), true));

    assertEquals(MavlinkMessageIds.MISSION_ITEM_REACHED, packet.getMessageId());
    assertEquals(17, packet.getSequence());
    assertTrue(packet.isValid());
  }

  @Test
  void missingSequenceAndInvalidFrameUseSentinel() {
    MissionItemReachedPacket packet =
        new MissionItemReachedPacket(frame(Map.of(), false));

    assertEquals(-1, packet.getSequence());
    assertFalse(packet.isValid());
  }

  private static ProcessedFrame frame(Map<String, Object> fields, boolean valid) {
    return new ProcessedFrame("MISSION_ITEM_REACHED", null, fields, valid, List.of(), null);
  }
}
