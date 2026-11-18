/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.engine.destination.tasks;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.DestinationStats;
import io.mapsmessaging.engine.destination.subscription.DestinationSubscriptionManager;
import io.mapsmessaging.engine.destination.subscription.Subscribable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class NoInterestCleanupRegressionTest {

  @Test
  void unretainedMessageWithNoInterestedSubscribersIsScheduledForRemoval() {
    Fixture f = fixture();
    Message message = new MessageBuilder().setId(100L).build();

    assertEquals(0L, new Probe().processSubscription(f.destination, f.subscriptions, message));

    verify(f.destination).submit(any(RemoveMessageTask.class), eq(DestinationImpl.DELETE_PRIORITY));
    verify(f.stats).noInterest();
  }

  @Test
  void retainedMessageWithNoInterestedSubscribersIsNotScheduledForRemoval() {
    Fixture f = fixture();
    Message message = new MessageBuilder().setId(101L).setRetain(true).build();
    when(f.destination.getRetainedIdentifier()).thenReturn(101L);

    assertEquals(0L, new Probe().processSubscription(f.destination, f.subscriptions, message));

    verify(f.destination, never()).submit(any(RemoveMessageTask.class), eq(DestinationImpl.DELETE_PRIORITY));
    verify(f.stats).noInterest();
  }

  @Test
  void messageWithInterestedSubscriberIsKeptAndSubscriptionCountRecorded() {
    Fixture f = fixture();
    Subscribable interested = mock(Subscribable.class);
    Message message = new MessageBuilder().setId(102L).build();
    when(interested.register(message)).thenReturn(1);
    f.subscriptions.put("consumer", interested);

    assertEquals(1L, new Probe().processSubscription(f.destination, f.subscriptions, message));

    verify(f.destination, never()).submit(any(RemoveMessageTask.class), eq(DestinationImpl.DELETE_PRIORITY));
    verify(f.stats).messageSubscribed(1);
  }

  private static Fixture fixture() {
    DestinationImpl destination = mock(DestinationImpl.class);
    DestinationStats stats = mock(DestinationStats.class);
    when(destination.getStats()).thenReturn(stats);
    return new Fixture(destination, stats, new DestinationSubscriptionManager("topic/no-interest"));
  }

  private static final class Probe extends SubscriptionTask {
    @Override
    public io.mapsmessaging.engine.tasks.Response taskCall() {
      return new io.mapsmessaging.engine.tasks.VoidResponse();
    }
  }

  private record Fixture(DestinationImpl destination, DestinationStats stats,
      DestinationSubscriptionManager subscriptions) {
  }
}
