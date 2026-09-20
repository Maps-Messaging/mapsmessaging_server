package io.mapsmessaging.state.drone.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BatteryStateCoverageSweepTest {
  @Test
  void batteryStateRetainsPowerTemperatureAndDurationValues() {
    BatteryState state = new BatteryState(
        75.0,
        24.2,
        5.0,
        3000.0,
        32.5,
        Boolean.FALSE,
        "PT36M");

    assertEquals(75.0, state.getPercentage());
    assertEquals(24.2, state.getVoltageVolts());
    assertEquals(5.0, state.getCurrentAmps());
    assertEquals("PT36M", state.getDuration());
  }
}
