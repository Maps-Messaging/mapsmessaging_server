/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.engine.destination.subscription;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.message.Message;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Isolated dispatch contract for topic fan-out and queue shared-consumer registration. */
class DestinationSubscriptionDispatchContractTest {

  @Test
  void topicRegistersOnePublicationForEachIndependentSubscriber() {
    DestinationSubscriptionManager manager = new DestinationSubscriptionManager("topic/sensors");
    Subscribable first = mock(Subscribable.class);
    Subscribable second = mock(Subscribable.class);
    Message message = new MessageBuilder().setId(7).build();
    when(first.register(message)).thenReturn(1);
    when(second.register(message)).thenReturn(1);

    manager.put("session-one", first);
    manager.put("session-two", second);

    assertEquals(2, manager.register(message));
    verify(first).register(message);
    verify(second).register(message);
  }

  @Test
  void queueRegistersPublicationOnceWithItsCompetingConsumerGroup() {
    DestinationSubscriptionManager manager = new DestinationSubscriptionManager("queue/orders");
    Subscribable competingGroup = mock(Subscribable.class);
    Message message = new MessageBuilder().setId(8).build();
    when(competingGroup.register(message)).thenReturn(1);

    manager.put("queue-group", competingGroup);
    manager.put("queue-group", competingGroup);

    assertEquals(1, manager.register(message));
    verify(competingGroup, times(1)).register(message);
  }

  @Test
  void removingOneTopicSubscriberDoesNotRemoveInterestOfOtherSubscribers() {
    DestinationSubscriptionManager manager = new DestinationSubscriptionManager("topic/sensors");
    Subscribable first = mock(Subscribable.class);
    Subscribable second = mock(Subscribable.class);
    Message message = new MessageBuilder().setId(9).build();
    when(second.register(message)).thenReturn(1);
    when(second.hasMessage(9L)).thenReturn(true);

    manager.put("first", first);
    manager.put("second", second);
    assertSame(first, manager.remove("first"));

    assertTrue(manager.hasSubscriptions());
    assertTrue(manager.hasInterest(9L));
    assertEquals(1, manager.register(message));
    verify(first, never()).register(message);
    verify(second).register(message);
  }

  @Test
  void completionAndExpiryAreForwardedToAllInterestedSubscriptions() {
    DestinationSubscriptionManager manager = new DestinationSubscriptionManager("topic/sensors");
    Subscribable first = mock(Subscribable.class);
    Subscribable second = mock(Subscribable.class);
    when(first.expired(10L)).thenReturn(true);
    when(second.expired(10L)).thenReturn(false);
    manager.put("first", first);
    manager.put("second", second);

    assertTrue(manager.expired(10L));
    verify(first).expired(10L);
    verify(second).expired(10L);
  }

  @Test
  void orphanScanPreservesEventsStillReferencedByAnotherSubscriber() {
    DestinationSubscriptionManager manager = new DestinationSubscriptionManager("topic/sensors");
    Subscribable remaining = mock(Subscribable.class);
    when(remaining.getAll()).thenReturn(new ArrayDeque<>(List.of(2L, 3L)));
    manager.put("remaining", remaining);

    java.util.Queue<Long> orphanCandidates = new ArrayDeque<>(List.of(1L, 2L, 3L, 4L));
    java.util.Queue<Long> removable = manager.scanForInterest(orphanCandidates);

    assertEquals(List.of(1L, 4L), List.copyOf(removable));
  }

  @Test
  void removingLastSubscriberLeavesDestinationWithoutInterest() {
    DestinationSubscriptionManager manager = new DestinationSubscriptionManager("topic/empty");
    Subscribable only = mock(Subscribable.class);
    manager.put("only", only);

    assertSame(only, manager.remove("only"));
    assertFalse(manager.hasSubscriptions());
    assertFalse(manager.hasInterest(77L));
  }
}
