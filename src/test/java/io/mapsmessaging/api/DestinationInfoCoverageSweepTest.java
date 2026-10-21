package io.mapsmessaging.api;

import io.mapsmessaging.api.features.DestinationType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DestinationInfoCoverageSweepTest {
  @Test
  void supportsValueEqualityAndMutation() {
    DestinationInfo info = new DestinationInfo("/events", DestinationType.TOPIC);
    DestinationInfo same = new DestinationInfo("/events", DestinationType.TOPIC);

    assertEquals(same, info);
    info.setType(DestinationType.QUEUE);
    assertEquals(DestinationType.QUEUE, info.getType());
    assertNotEquals(same, info);
  }
}
