package io.mapsmessaging.state.drone.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LinkStateCoverageSweepTest {
  @Test
  void allArgsConstructorCapturesConnectivityMetrics() {
    LinkState state = new LinkState(
        "CONNECTED",
        Boolean.TRUE,
        -67,
        25.4,
        42.5,
        0.01,
        0.005);

    assertEquals("CONNECTED", state.getState());
    assertTrue(state.getConnected());
    assertEquals(-67, state.getRssiDbm());
    assertEquals(25.4, state.getSnrDb());
    assertEquals(42.5, state.getLatencyMs());
  }
}
