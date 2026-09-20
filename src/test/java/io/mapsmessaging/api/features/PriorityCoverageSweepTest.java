package io.mapsmessaging.api.features;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PriorityCoverageSweepTest {
  @Test
  void priorityLookupClampsOutOfRangeValuesAndMapsKnownValues() {
    assertEquals(Priority.LOWEST, Priority.getInstance(-1));
    assertEquals(Priority.NORMAL, Priority.getInstance(4));
    assertEquals(Priority.HIGHEST, Priority.getInstance(10));
    assertEquals(Priority.HIGHEST, Priority.getInstance(999));
  }
}
