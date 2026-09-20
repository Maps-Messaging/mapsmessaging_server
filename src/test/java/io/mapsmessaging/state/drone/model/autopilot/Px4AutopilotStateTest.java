package io.mapsmessaging.state.drone.model.autopilot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Px4AutopilotStateTest {

  @Test
  void mainAndSubModeComposeCanonicalFlightMode() {
    Px4AutopilotState state = new Px4AutopilotState();
    state.setMainMode("AUTO");
    state.setSubMode("MISSION");

    assertEquals("AUTO_MISSION", state.getFlightMode());
  }

  @Test
  void mainModeAloneIsReturnedWhenSubModeMissingOrBlank() {
    Px4AutopilotState state = new Px4AutopilotState();
    state.setMainMode("POSCTL");

    assertEquals("POSCTL", state.getFlightMode());

    state.setSubMode("   ");
    assertEquals("POSCTL", state.getFlightMode());
  }

  @Test
  void missingMainModeFallsBackToGenericAutopilotContract() {
    Px4AutopilotState state = new Px4AutopilotState();
    state.setMainMode(" ");

    assertNull(state.getFlightMode());
  }
}
