package io.mapsmessaging.state.mavlink.listener;

import io.mapsmessaging.state.drone.model.autopilot.ArduPilotAutopilotState;
import io.mapsmessaging.state.drone.model.autopilot.AutopilotState;
import io.mapsmessaging.state.drone.model.autopilot.GenericAutopilotState;
import io.mapsmessaging.state.drone.model.autopilot.Px4AutopilotState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MavlinkAutopilotSupportTest {

  @Test
  void autopilotTypeChoosesStableStateImplementationAndPreservesMatchingState() {
    Px4AutopilotState px4 = new Px4AutopilotState();
    ArduPilotAutopilotState ardupilot = new ArduPilotAutopilotState();

    assertSame(px4, MavlinkAutopilotSupport.resolveAutopilotState(px4, 12));
    assertInstanceOf(Px4AutopilotState.class,
        MavlinkAutopilotSupport.resolveAutopilotState(ardupilot, 12));
    assertSame(ardupilot,
        MavlinkAutopilotSupport.resolveAutopilotState(ardupilot, 3));
    assertInstanceOf(GenericAutopilotState.class,
        MavlinkAutopilotSupport.resolveAutopilotState(null, 99));
  }

  @Test
  void softwareVersionDecodesPackedMajorMinorPatch() {
    assertEquals("1.2.3",
        MavlinkAutopilotSupport.decodeFlightSoftwareVersion(0x01020300L));
    assertNull(MavlinkAutopilotSupport.decodeFlightSoftwareVersion(0));
  }

  @Test
  void px4HeartbeatDecodesAutoMissionMode() {
    Px4AutopilotState state = new Px4AutopilotState();
    long customMode = (4L << 16) | (4L << 24);

    MavlinkAutopilotSupport.populateHeartbeatFields(
        state, 12, 81, customMode, 4, 3);

    assertEquals("PX4", state.getAutopilotType());
    assertEquals("AUTO", state.getMainMode());
    assertEquals("MISSION", state.getSubMode());
    assertEquals("AUTO_MISSION", state.getFlightMode());
  }

  @Test
  void ardupilotPlaneModesAndUnknownModesHaveStableNames() {
    assertEquals("AUTO", MavlinkAutopilotSupport.resolveArduPlaneFlightMode(10));
    assertEquals("FLY_BY_WIRE_A",
        MavlinkAutopilotSupport.resolveArduPlaneFlightMode(5));
    assertEquals("99", MavlinkAutopilotSupport.resolveArduPlaneFlightMode(99));
  }

  @Test
  void autopilotVersionFieldsPopulateRawAndDecodedValues() {
    AutopilotState state = new GenericAutopilotState();

    MavlinkAutopilotSupport.populateAutopilotVersionFields(
        state,
        123L,
        0x01020300L,
        0x04050600L,
        0x07080900L,
        55L);

    assertEquals(123L, state.getUid());
    assertEquals("1.2.3", state.getFlightSoftwareVersion());
    assertEquals("4.5.6", state.getMiddlewareSoftwareVersion());
    assertEquals("7.8.9", state.getOsSoftwareVersion());
    assertEquals(55L, state.getCapabilities());
  }
}
