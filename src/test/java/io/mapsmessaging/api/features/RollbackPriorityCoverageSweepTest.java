package io.mapsmessaging.api.features;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RollbackPriorityCoverageSweepTest {
  @Test
  void maintainLeavesPriorityUnchangedAndIncrementCapsAtHighest() {
    assertEquals(4, RollbackPriority.MAINTAIN.incrementPriority(4));
    assertEquals(5, RollbackPriority.INCREMENT.incrementPriority(4));
    assertEquals(
        Priority.HIGHEST.getValue(),
        RollbackPriority.INCREMENT.incrementPriority(Priority.HIGHEST.getValue()));
  }
}
