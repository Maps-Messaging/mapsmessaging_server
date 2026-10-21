package io.mapsmessaging.network.protocol.impl.semtech.handlers;

import io.mapsmessaging.api.MessageEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MessageStateContextBranchCoverageTest {

  @Test
  void completingUnknownTokenIsSafe() {
    MessageStateContext context = new MessageStateContext();

    assertDoesNotThrow(() -> context.complete(999));
  }

  @Test
  void pushedMessageCompletionRunsExactlyOnceAndRemovesInflightEntry() {
    MessageStateContext context = new MessageStateContext();
    MessageEvent event = mock(MessageEvent.class);
    Runnable completion = mock(Runnable.class);
    when(event.getCompletionTask()).thenReturn(completion);

    context.push(42, event);
    context.complete(42);
    context.complete(42);

    verify(completion, times(1)).run();
  }

  @Test
  void replacingTokenCompletesOnlyLatestMessage() {
    MessageStateContext context = new MessageStateContext();
    MessageEvent first = mock(MessageEvent.class);
    MessageEvent second = mock(MessageEvent.class);
    Runnable firstTask = mock(Runnable.class);
    Runnable secondTask = mock(Runnable.class);
    when(first.getCompletionTask()).thenReturn(firstTask);
    when(second.getCompletionTask()).thenReturn(secondTask);

    context.push(7, first);
    context.push(7, second);
    context.complete(7);

    verify(firstTask, never()).run();
    verify(secondTask).run();
  }
}