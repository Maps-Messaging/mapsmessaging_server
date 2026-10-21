/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 */

package io.mapsmessaging.state.mavlink.listener;

import io.mapsmessaging.state.drone.model.autopilot.ArduPilotAutopilotState;
import io.mapsmessaging.state.drone.model.autopilot.AutopilotState;
import io.mapsmessaging.state.drone.model.autopilot.GenericAutopilotState;
import io.mapsmessaging.state.drone.model.autopilot.Px4AutopilotState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class MavlinkAutopilotSupportTest {

  @Test
  void autopilotTypeChoosesStableStateImplementationAndPreservesMatchingState() {
    Px4AutopilotState px4 = new Px4AutopilotState();
    ArduPilotAutopilotState ardupilot = new ArduPilotAutopilotState();
    GenericAutopilotState generic = new GenericAutopilotState();

    assertSame(px4, MavlinkAutopilotSupport.resolveAutopilotState(px4, 12));
    assertInstanceOf(Px4AutopilotState.class,
        MavlinkAutopilotSupport.resolveAutopilotState(ardupilot, 12));
    assertSame(ardupilot, MavlinkAutopilotSupport.resolveAutopilotState(ardupilot, 3));
    assertInstanceOf(ArduPilotAutopilotState.class,
        MavlinkAutopilotSupport.resolveAutopilotState(px4, 3));
    assertSame(generic, MavlinkAutopilotSupport.resolveAutopilotState(generic, 99));
    assertInstanceOf(GenericAutopilotState.class,
        MavlinkAutopilotSupport.resolveAutopilotState(null, 99));
  }

  @ParameterizedTest
  @CsvSource({
      "12,PX4",
      "3,ARDUPILOT",
      "0,GENERIC",
      "99,GENERIC"
  })
  void resolvesAutopilotTypeNames(int type, String expected) {
    assertEquals(expected, MavlinkAutopilotSupport.resolveAutopilotTypeName(type));
  }

  @ParameterizedTest
  @MethodSource("versionCases")
  void softwareVersionDecodesPackedMajorMinorPatch(long raw, String expected) {
    assertEquals(expected, MavlinkAutopilotSupport.decodeFlightSoftwareVersion(raw));
  }

  @ParameterizedTest
  @MethodSource("arduPlaneModes")
  void resolvesAllKnownArduPlaneModes(int mode, String expected) {
    assertEquals(expected, MavlinkAutopilotSupport.resolveArduPlaneFlightMode(mode));
  }

  @Test
  void arduPlaneModeUnknownValuePreservesNumericMode() {
    assertEquals("99", MavlinkAutopilotSupport.resolveArduPlaneFlightMode(99));
    assertEquals("-1", MavlinkAutopilotSupport.resolveArduPlaneFlightMode(-1));
  }

  @ParameterizedTest
  @CsvSource({
      "1,MANUAL",
      "2,ALTCTL",
      "3,POSCTL",
      "4,AUTO",
      "5,ACRO",
      "6,OFFBOARD",
      "7,STABILIZED",
      "8,RATTITUDE",
      "99,"
  })
  void px4HeartbeatDecodesMainModes(int mainMode, String expectedMain) {
    Px4AutopilotState state = new Px4AutopilotState();
    long customMode = ((long) mainMode << 16);

    MavlinkAutopilotSupport.populateHeartbeatFields(state, 12, 81, customMode, 4, 3);

    assertEquals(expectedMain, state.getMainMode());
    assertNull(state.getSubMode());
  }

  @ParameterizedTest
  @CsvSource({
      "1,READY",
      "2,TAKEOFF",
      "3,LOITER",
      "4,MISSION",
      "5,RTL",
      "6,LAND",
      "7,RTGS",
      "8,FOLLOW_TARGET",
      "9,PRECLAND",
      "99,"
  })
  void px4AutoHeartbeatDecodesSubModes(int subMode, String expectedSubMode) {
    Px4AutopilotState state = new Px4AutopilotState();
    long customMode = (4L << 16) | ((long) subMode << 24);

    MavlinkAutopilotSupport.populateHeartbeatFields(state, 12, 81, customMode, 4, 3);

    assertEquals("AUTO", state.getMainMode());
    assertEquals(expectedSubMode, state.getSubMode());
  }

  @Test
  void px4HeartbeatPopulatesCommonFieldsAndFlightMode() {
    Px4AutopilotState state = new Px4AutopilotState();
    long customMode = (4L << 16) | (4L << 24);

    MavlinkAutopilotSupport.populateHeartbeatFields(state, 12, 81, customMode, 4, 3);

    assertEquals("PX4", state.getAutopilotType());
    assertEquals(81, state.getBaseMode());
    assertEquals(customMode, state.getCustomMode());
    assertEquals(4, state.getSystemStatus());
    assertEquals(3, state.getMavlinkVersion());
    assertEquals("AUTO_MISSION", state.getFlightMode());
  }

  @Test
  void arduPilotHeartbeatStoresModeNumber() {
    ArduPilotAutopilotState state = new ArduPilotAutopilotState();

    MavlinkAutopilotSupport.populateHeartbeatFields(state, 3, 1, 15L, 2, 3);

    assertEquals("ARDUPILOT", state.getAutopilotType());
    assertEquals(15, state.getModeNumber());
    assertEquals("15", state.getFlightMode());
  }

  @Test
  void genericHeartbeatOnlyPopulatesCommonFields() {
    GenericAutopilotState state = new GenericAutopilotState();

    MavlinkAutopilotSupport.populateHeartbeatFields(state, 0, 7, 123L, 8, 2);

    assertEquals("GENERIC", state.getAutopilotType());
    assertEquals(7, state.getBaseMode());
    assertEquals(123L, state.getCustomMode());
    assertEquals(8, state.getSystemStatus());
    assertEquals(2, state.getMavlinkVersion());
  }

  @Test
  void autopilotVersionFieldsPopulateRawAndDecodedValues() {
    AutopilotState state = new GenericAutopilotState();

    MavlinkAutopilotSupport.populateAutopilotVersionFields(
        state, 123L, 0x01020300L, 0x04050600L, 0x07080900L, 55L);

    assertEquals(123L, state.getUid());
    assertEquals(0x01020300L, state.getFlightSoftwareVersionRaw());
    assertEquals("1.2.3", state.getFlightSoftwareVersion());
    assertEquals(0x04050600L, state.getMiddlewareSoftwareVersionRaw());
    assertEquals("4.5.6", state.getMiddlewareSoftwareVersion());
    assertEquals(0x07080900L, state.getOsSoftwareVersionRaw());
    assertEquals("7.8.9", state.getOsSoftwareVersion());
    assertEquals(55L, state.getCapabilities());
  }

  private static Stream<Arguments> versionCases() {
    return Stream.of(
        Arguments.of(0L, null),
        Arguments.of(-1L, null),
        Arguments.of(0x01020300L, "1.2.3"),
        Arguments.of(0xFF000100L, "255.0.1"),
        Arguments.of(0x00010200L, "0.1.2")
    );
  }

  private static Stream<Arguments> arduPlaneModes() {
    return Stream.of(
        Arguments.of(0, "MANUAL"),
        Arguments.of(1, "CIRCLE"),
        Arguments.of(2, "STABILIZE"),
        Arguments.of(3, "TRAINING"),
        Arguments.of(4, "ACRO"),
        Arguments.of(5, "FLY_BY_WIRE_A"),
        Arguments.of(6, "FLY_BY_WIRE_B"),
        Arguments.of(7, "CRUISE"),
        Arguments.of(8, "AUTOTUNE"),
        Arguments.of(10, "AUTO"),
        Arguments.of(11, "RTL"),
        Arguments.of(12, "LOITER"),
        Arguments.of(13, "TAKEOFF"),
        Arguments.of(14, "AVOID_ADSB"),
        Arguments.of(15, "GUIDED"),
        Arguments.of(17, "QSTABILIZE"),
        Arguments.of(18, "QHOVER"),
        Arguments.of(19, "QLOITER"),
        Arguments.of(20, "QLAND"),
        Arguments.of(21, "QRTL"),
        Arguments.of(22, "QAUTOTUNE"),
        Arguments.of(23, "QACRO"),
        Arguments.of(24, "THERMAL"),
        Arguments.of(25, "LOITER_ALT_QLAND")
    );
  }
}
