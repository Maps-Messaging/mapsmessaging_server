/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 */

package io.mapsmessaging.state.mavlink.packet;

import io.mapsmessaging.mavlink.ProcessedFrame;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExtendedSysStatePacketTest {

  @ParameterizedTest
  @CsvSource({
      "1,ON_GROUND",
      "2,IN_AIR",
      "3,TAKEOFF",
      "4,LANDING",
      "0,UNDEFINED",
      "99,UNDEFINED"
  })
  void resolvesLandedStateNames(int state, String expected) {
    assertEquals(expected, packet(state, 1, true).getLandedStateName());
  }

  @ParameterizedTest
  @CsvSource({
      "1,UNDEFINED",
      "2,TRANSITION_TO_FW",
      "3,TRANSITION_TO_MC",
      "4,MC",
      "5,FW",
      "0,UNDEFINED",
      "99,UNDEFINED"
  })
  void resolvesVtolStateNames(int state, String expected) {
    assertEquals(expected, packet(1, state, true).getVtolStateName());
  }

  @Test
  void missingFieldsResolveToUndefinedAndInvalidStateIsPreserved() {
    ProcessedFrame frame = mock(ProcessedFrame.class);
    when(frame.getFields()).thenReturn(Map.of());
    when(frame.isValid()).thenReturn(false);

    ExtendedSysStatePacket packet = new ExtendedSysStatePacket(frame);

    assertEquals(-1, packet.getLandedState());
    assertEquals(-1, packet.getVtolState());
    assertEquals("UNDEFINED", packet.getLandedStateName());
    assertEquals("UNDEFINED", packet.getVtolStateName());
    assertFalse(packet.isValid());
  }

  @Test
  void messageIdIsExtendedSysState() {
    assertEquals(MavlinkMessageIds.EXTENDED_SYS_STATE, packet(1, 1, true).getMessageId());
  }

  private ExtendedSysStatePacket packet(int landed, int vtol, boolean valid) {
    ProcessedFrame frame = mock(ProcessedFrame.class);
    Map<String, Object> fields = new HashMap<>();
    fields.put("landed_state", landed);
    fields.put("vtol_state", vtol);
    when(frame.getFields()).thenReturn(fields);
    when(frame.isValid()).thenReturn(valid);
    return new ExtendedSysStatePacket(frame);
  }
}
