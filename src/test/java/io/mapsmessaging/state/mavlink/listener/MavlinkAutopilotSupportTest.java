/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 */

package io.mapsmessaging.state.mavlink.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MavlinkAutopilotSupportTest {

  @Test
  void arduPlaneMode_decodesOperationalModes() {
    assertEquals("MANUAL", MavlinkAutopilotSupport.resolveArduPlaneFlightMode(0));
    assertEquals("AUTO", MavlinkAutopilotSupport.resolveArduPlaneFlightMode(10));
    assertEquals("RTL", MavlinkAutopilotSupport.resolveArduPlaneFlightMode(11));
    assertEquals("LOITER", MavlinkAutopilotSupport.resolveArduPlaneFlightMode(12));
    assertEquals("GUIDED", MavlinkAutopilotSupport.resolveArduPlaneFlightMode(15));
  }

  @Test
  void arduPlaneMode_unknownValue_preservesNumericMode() {
    assertEquals("99", MavlinkAutopilotSupport.resolveArduPlaneFlightMode(99));
  }
}
