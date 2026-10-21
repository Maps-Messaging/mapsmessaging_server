package io.mapsmessaging.api.features;

import io.mapsmessaging.engine.destination.subscription.modes.NormalSubscriptionModeManager;
import io.mapsmessaging.engine.destination.subscription.modes.SchemaSubscriptionModeManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DestinationModeCoverageSweepTest {
  @Test
  void idsResolveToExpectedModesAndManagers() {
    assertEquals(DestinationMode.NORMAL, DestinationMode.getInstance(0));
    assertEquals(DestinationMode.SCHEMA, DestinationMode.getInstance(1));
    assertInstanceOf(
        NormalSubscriptionModeManager.class,
        DestinationMode.NORMAL.getSubscriptionModeManager());
    assertInstanceOf(
        SchemaSubscriptionModeManager.class,
        DestinationMode.SCHEMA.getSubscriptionModeManager());
    assertThrows(IllegalArgumentException.class, () -> DestinationMode.getInstance(99));
  }
}
