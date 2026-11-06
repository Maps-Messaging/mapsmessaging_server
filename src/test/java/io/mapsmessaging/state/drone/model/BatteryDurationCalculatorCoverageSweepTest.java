package io.mapsmessaging.state.drone.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BatteryDurationCalculatorCoverageSweepTest {
  @Test
  void durationCalculationHandlesValidZeroAndInvalidPowerInputs() {
    assertEquals("PT1H", BatteryDurationCalculator.calculateDuration(5000.0, 5.0));
    assertEquals("PT30M", BatteryDurationCalculator.calculateDurationFromAmpHours(2.5, 5.0));
    assertEquals("PT0S", BatteryDurationCalculator.calculateDuration(0.0, 5.0));

    assertNull(BatteryDurationCalculator.calculateDuration((BatteryState) null));
    assertNull(BatteryDurationCalculator.calculateDuration(null, 5.0));
    assertNull(BatteryDurationCalculator.calculateDuration(1000.0, 0.0));
    assertNull(BatteryDurationCalculator.calculateDuration(Double.NaN, 1.0));
  }
}
