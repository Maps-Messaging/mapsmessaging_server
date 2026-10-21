package io.mapsmessaging.utilities.stats;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NoStatsTest {

  @Test
  void disabledStatsAlwaysRemainZeroRegardlessOfMutations() {
    NoStats stats = new NoStats("packets", "count");

    stats.add(100);
    stats.subtract(40);
    stats.reset();

    assertEquals("packets", stats.getName());
    assertEquals("count", stats.getUnits());
    assertEquals(0, stats.getCurrent());
    assertEquals(0, stats.getTotal());
    assertEquals(0.0f, stats.getPerSecond(), 0.0f);
  }
}