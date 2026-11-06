package io.mapsmessaging.state.drone.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FixInfoCoverageSweepTest {
  @Test
  void allArgsConstructorAndMutationRetainGnssAccuracyState() {
    FixInfo info = new FixInfo("3D", 12, 0.8, 1.2, 1.5, 2.3);

    assertEquals("3D", info.getFixType());
    assertEquals(12, info.getSatelliteCount());
    assertEquals(1.5, info.getHorizontalAccuracyMeters());
    info.setFixType("RTK");
    assertEquals("RTK", info.getFixType());
  }
}
