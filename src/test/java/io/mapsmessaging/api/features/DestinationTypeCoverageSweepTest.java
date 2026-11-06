package io.mapsmessaging.api.features;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DestinationTypeCoverageSweepTest {
  @Test
  void namesMapCaseInsensitivelyAndExposeQueueTopicSemantics() {
    assertEquals(DestinationType.TOPIC, DestinationType.getType("topic"));
    assertEquals(DestinationType.QUEUE, DestinationType.getType("QUEUE"));
    assertEquals(DestinationType.TEMPORARY_TOPIC, DestinationType.getType("TemporaryTopic"));
    assertEquals(DestinationType.TEMPORARY_QUEUE, DestinationType.getType("temporaryqueue"));
    assertTrue(DestinationType.TOPIC.isTopic());
    assertTrue(DestinationType.QUEUE.isQueue());
    assertThrows(RuntimeException.class, () -> DestinationType.getType("missing"));
  }
}
