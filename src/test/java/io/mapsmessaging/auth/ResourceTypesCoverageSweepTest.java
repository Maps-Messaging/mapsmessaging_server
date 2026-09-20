package io.mapsmessaging.auth;

import io.mapsmessaging.api.features.DestinationType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResourceTypesCoverageSweepTest {
  @Test
  void resourceSetContainsServerAndEveryDestinationNameAndIsImmutable() {
    var resources = ResourceTypes.getInstance().getResources();

    assertTrue(resources.contains("Server"));
    for (DestinationType type : DestinationType.values()) {
      assertTrue(resources.contains(type.getName()));
    }
    assertThrows(UnsupportedOperationException.class, () -> resources.add("Other"));
  }
}
