package io.mapsmessaging.state.drone.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SystemStateCoverageSweepTest {
  @Test
  void allArgsConstructorCapturesSystemHealthState() {
    SystemState state = new SystemState(42.5, 65.3, Boolean.TRUE, "nominal");

    assertEquals(42.5, state.getCpuLoadPercent());
    assertEquals(65.3, state.getSystemTemperatureCelsius());
    assertTrue(state.getHealthy());
    assertEquals("nominal", state.getStatusMessage());
  }
}
