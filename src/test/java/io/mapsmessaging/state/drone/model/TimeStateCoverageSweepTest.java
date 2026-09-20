package io.mapsmessaging.state.drone.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TimeStateCoverageSweepTest {
  @Test
  void timeStateStoresClockSourcesValidityAndOffset() {
    TimeState state = new TimeState();
    state.setGpsTimeEpochMillis(1000L);
    state.setSystemTimeEpochMillis(1100L);
    state.setGpsTimeValid(Boolean.TRUE);
    state.setTimeOffsetMillis(100.0);

    assertEquals(1000L, state.getGpsTimeEpochMillis());
    assertEquals(1100L, state.getSystemTimeEpochMillis());
    assertTrue(state.getGpsTimeValid());
    assertEquals(100.0, state.getTimeOffsetMillis());
  }
}
