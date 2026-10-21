package io.mapsmessaging.state.drone.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DetectionEventCoverageSweepTest {
  @Test
  void detectionHelpersTrackLifecycleAndIgnoreNullAttributes() {
    DetectionEvent event =
        new DetectionEvent(UUID.randomUUID(), "target", DetectionEventType.DETECTED);

    assertTrue(event.isDetectedOrUpdated());
    assertFalse(event.isLost());

    event.addAttribute("probability", 0.9);
    event.addAttribute(null, "ignored");
    event.addAttribute("ignored", null);
    assertEquals(1, event.getAttributes().size());

    event.setEventType(DetectionEventType.UPDATED);
    assertTrue(event.isDetectedOrUpdated());

    event.setEventType(DetectionEventType.LOST);
    assertTrue(event.isLost());
    assertFalse(event.isDetectedOrUpdated());
  }
}
