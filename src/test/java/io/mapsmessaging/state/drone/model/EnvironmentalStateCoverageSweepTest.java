package io.mapsmessaging.state.drone.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EnvironmentalStateCoverageSweepTest {
  @Test
  void environmentalStateStoresWindSpeedAndDirection() {
    EnvironmentalState state = new EnvironmentalState();
    state.setWindSpeedMetersPerSecond(8.3);
    state.setWindDirectionDegrees(135.0);

    assertEquals(8.3, state.getWindSpeedMetersPerSecond());
    assertEquals(135.0, state.getWindDirectionDegrees());
    assertTrue(state.toString().contains("8.3"));
  }
}
