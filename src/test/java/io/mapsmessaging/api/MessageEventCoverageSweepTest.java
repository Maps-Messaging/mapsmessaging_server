package io.mapsmessaging.api;

import io.mapsmessaging.api.message.Message;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class MessageEventCoverageSweepTest {
  @Test
  void retainsDeliveryContextAndCompletionTask() {
    SubscribedEventManager subscription = mock(SubscribedEventManager.class);
    Message message = mock(Message.class);
    Runnable completion = () -> {};

    MessageEvent event = new MessageEvent("/topic", subscription, message, completion);

    assertEquals("/topic", event.getDestinationName());
    assertSame(subscription, event.getSubscription());
    assertSame(message, event.getMessage());
    assertSame(completion, event.getCompletionTask());
  }
}
