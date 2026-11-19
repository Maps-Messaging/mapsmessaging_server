package io.mapsmessaging.fusion;

import io.mapsmessaging.api.MessageEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class FusionEnvelopeCoverageSweepTest {
  @Test
  void retainsInputIndexAndMessageEvent() {
    MessageEvent event = mock(MessageEvent.class);
    FusionEnvelope envelope = new FusionEnvelope(3, event);

    assertEquals(3, envelope.getInputIndex());
    assertSame(event, envelope.getEvent());
    assertThrows(NullPointerException.class, () -> new FusionEnvelope(1, null));
  }
}
