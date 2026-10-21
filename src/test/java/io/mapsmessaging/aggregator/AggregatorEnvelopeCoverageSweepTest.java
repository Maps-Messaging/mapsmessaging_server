package io.mapsmessaging.aggregator;

import io.mapsmessaging.api.MessageEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class AggregatorEnvelopeCoverageSweepTest {
  @Test
  void retainsInputIndexAndMessageEvent() {
    MessageEvent event = mock(MessageEvent.class);
    AggregatorEnvelope envelope = new AggregatorEnvelope(3, event);

    assertEquals(3, envelope.getInputIndex());
    assertSame(event, envelope.getEvent());
    assertThrows(NullPointerException.class, () -> new AggregatorEnvelope(1, null));
  }
}
