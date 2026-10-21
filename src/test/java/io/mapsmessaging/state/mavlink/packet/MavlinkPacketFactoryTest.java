package io.mapsmessaging.state.mavlink.packet;

import io.mapsmessaging.mavlink.ProcessedFrame;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MavlinkPacketFactoryTest {

  @Test
  void knownMessageIdsCreateExpectedTypedPacket() {
    assertFactory(MavlinkMessageIds.ALTITUDE, AltitudePacket.class);
    assertFactory(MavlinkMessageIds.ATTITUDE, AttitudePacket.class);
    assertFactory(MavlinkMessageIds.AUTOPILOT_VERSION, AutopilotVersionPacket.class);
    assertFactory(MavlinkMessageIds.BATTERY_STATUS, BatteryStatusPacket.class);
    assertFactory(MavlinkMessageIds.COMMAND_ACK, CommandAckPacket.class);
    assertFactory(MavlinkMessageIds.EXTENDED_SYS_STATE, ExtendedSysStatePacket.class);
    assertFactory(MavlinkMessageIds.GLOBAL_POSITION_INT, GlobalPositionPacket.class);
    assertFactory(MavlinkMessageIds.GPS_RAW_INT, GpsRawIntPacket.class);
    assertFactory(MavlinkMessageIds.HEARTBEAT, HeartbeatPacket.class);
    assertFactory(MavlinkMessageIds.HOME_POSITION, HomePositionPacket.class);
    assertFactory(MavlinkMessageIds.NAMED_VALUE_FLOAT, NamedValueFloatPacket.class);
    assertFactory(MavlinkMessageIds.MISSION_CURRENT, MissionCurrentPacket.class);
    assertFactory(MavlinkMessageIds.MISSION_ITEM_REACHED, MissionItemReachedPacket.class);
    assertFactory(MavlinkMessageIds.MISSION_REQUEST, MissionRequestPacket.class);
    assertFactory(MavlinkMessageIds.MISSION_REQUEST_INT, MissionRequestIntPacket.class);
    assertFactory(MavlinkMessageIds.MISSION_ACK, MissionAckPacket.class);
    assertFactory(MavlinkMessageIds.MOUNT_STATUS, MountStatusPacket.class);
    assertFactory(MavlinkMessageIds.STATUSTEXT, StatusTextPacket.class);
    assertFactory(MavlinkMessageIds.SYS_STATUS, SysStatusPacket.class);
    assertFactory(MavlinkMessageIds.SYSTEM_TIME, SystemTimePacket.class);
  }

  @Test
  void unknownMessageIdReturnsNull() {
    assertNull(MavlinkPacketFactory.create(frame(Integer.MAX_VALUE)));
  }

  private static void assertFactory(
      int messageId,
      Class<? extends MavlinkPacket> expectedType
  ) {
    assertInstanceOf(expectedType, MavlinkPacketFactory.create(frame(messageId)));
  }

  private static ProcessedFrame frame(int messageId) {
    ProcessedFrame frame = mock(ProcessedFrame.class, RETURNS_DEEP_STUBS);
    when(frame.getFrame().getMessageId()).thenReturn(messageId);
    when(frame.getFields()).thenReturn(Map.of());
    when(frame.isValid()).thenReturn(true);
    return frame;
  }
}
