package io.mapsmessaging.api.transformers;

import io.mapsmessaging.api.message.Message;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ParsedMessageCoverageSweepTest {
  @Test
  void parsedMessageSupportsConstructionAndMutation() {
    Message first = mock(Message.class);
    Message second = mock(Message.class);
    ParsedMessage parsed = new ParsedMessage("/a", first);

    assertEquals("/a", parsed.getDestinationName());
    assertSame(first, parsed.getMessage());

    parsed.setDestinationName("/b");
    parsed.setMessage(second);
    assertEquals("/b", parsed.getDestinationName());
    assertSame(second, parsed.getMessage());
  }
}
