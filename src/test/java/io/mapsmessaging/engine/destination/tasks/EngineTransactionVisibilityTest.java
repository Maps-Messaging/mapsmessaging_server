/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.engine.destination.tasks;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.Constants;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.DestinationStats;
import io.mapsmessaging.engine.destination.delayed.TransactionalMessageManager;
import io.mapsmessaging.engine.destination.subscription.DestinationSubscriptionManager;
import io.mapsmessaging.engine.destination.subscription.Subscribable;
import io.mapsmessaging.engine.tasks.LongResponse;
import io.mapsmessaging.utilities.collections.bitset.BitSetFactoryImpl;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Engine-only visibility tests: an uncommitted transaction must not reach subscribers. */
class EngineTransactionVisibilityTest {

  @Test
  void commitReleasesAllMessagesInTheSelectedTransactionOnly() throws Exception {
    Fixture fixture = fixture();
    Message a = message(101);
    Message b = message(102);
    Message other = message(201);
    fixture.manager.register(7L, a);
    fixture.manager.register(7L, b);
    fixture.manager.register(8L, other);
    when(fixture.destination.getMessage(101L)).thenReturn(a);
    when(fixture.destination.getMessage(102L)).thenReturn(b);
    when(fixture.destination.getMessage(201L)).thenReturn(other);

    assertEquals(3, fixture.manager.size());
    verifyNoInteractions(fixture.subscriber);

    LongResponse result = (LongResponse) new TransactionalMessageProcessor(
        fixture.destination, fixture.subscriptions, fixture.manager, 7L).taskCall();

    assertEquals(2L, result.getResponse());
    verify(fixture.subscriber).register(a);
    verify(fixture.subscriber).register(b);
    verify(fixture.subscriber, never()).register(other);
    assertEquals(1, fixture.manager.size());
    assertEquals(List.of(8L), fixture.manager.getTransactions());
  }

  @Test
  void anUnknownCommitDoesNotDeliverAnything() throws Exception {
    Fixture fixture = fixture();

    LongResponse result = (LongResponse) new TransactionalMessageProcessor(
        fixture.destination, fixture.subscriptions, fixture.manager, 999L).taskCall();

    assertEquals(0L, result.getResponse());
    verifyNoInteractions(fixture.subscriber);
  }

  @Test
  void removingAbortedBucketDoesNotDeliverItsMessages() {
    Fixture fixture = fixture();
    fixture.manager.register(7L, message(101));
    fixture.manager.register(7L, message(102));

    java.util.Queue<Long> aborted = fixture.manager.removeBucket(7L);

    assertEquals(2, aborted.size());
    assertEquals(0L, fixture.manager.getTransactions().stream().filter(id -> id == 7L).count());
    verifyNoInteractions(fixture.subscriber);
  }

  @Test
  void commitSkipsMissingStoredMessageAndContinues() throws Exception {
    Fixture fixture = fixture();
    Message remaining = message(302);
    fixture.manager.register(9L, message(301));
    fixture.manager.register(9L, remaining);
    when(fixture.destination.getMessage(302L)).thenReturn(remaining);

    LongResponse result = (LongResponse) new TransactionalMessageProcessor(
        fixture.destination, fixture.subscriptions, fixture.manager, 9L).taskCall();

    assertEquals(1L, result.getResponse());
    verify(fixture.subscriber).register(remaining);
    assertTrue(fixture.manager.isEmpty());
  }


  @Test
  void subscriberFailureLeavesTransactionMessageAvailableForRetry() throws Exception {
    Fixture fixture = fixture();
    Message message = message(401);
    fixture.manager.register(15L, message);
    when(fixture.destination.getMessage(401L)).thenReturn(message);
    when(fixture.subscriber.register(message))
        .thenThrow(new IllegalStateException("subscriber failed"))
        .thenReturn(1);

    assertThrows(IllegalStateException.class, () -> new TransactionalMessageProcessor(
        fixture.destination, fixture.subscriptions, fixture.manager, 15L).taskCall());

    assertEquals(1L, fixture.manager.size());
    assertEquals(Long.valueOf(401L), fixture.manager.getNext(15L));

    LongResponse retried = (LongResponse) new TransactionalMessageProcessor(
        fixture.destination, fixture.subscriptions, fixture.manager, 15L).taskCall();
    assertEquals(1L, retried.getResponse());
    assertTrue(fixture.manager.isEmpty());
  }

  @Test
  void duplicateCommitAfterSuccessfulCommitDoesNotRedispatchMessage() throws Exception {
    Fixture fixture = fixture();
    Message message = message(501);
    fixture.manager.register(20L, message);
    when(fixture.destination.getMessage(501L)).thenReturn(message);

    new TransactionalMessageProcessor(
        fixture.destination, fixture.subscriptions, fixture.manager, 20L).taskCall();
    LongResponse duplicate = (LongResponse) new TransactionalMessageProcessor(
        fixture.destination, fixture.subscriptions, fixture.manager, 20L).taskCall();

    assertEquals(0L, duplicate.getResponse());
    verify(fixture.subscriber, times(1)).register(message);
  }

  private static Message message(long identifier) {
    return new MessageBuilder().setId(identifier).build();
  }

  private static Fixture fixture() {
    DestinationImpl destination = mock(DestinationImpl.class);
    DestinationStats stats = mock(DestinationStats.class);
    when(destination.getStats()).thenReturn(stats);
    TransactionalMessageManager manager = new TransactionalMessageManager(
        new BitSetFactoryImpl(Constants.BITSET_BLOCK_SIZE));
    DestinationSubscriptionManager subscriptions = new DestinationSubscriptionManager("topic/transactions");
    Subscribable subscriber = mock(Subscribable.class);
    subscriptions.put("subscriber", subscriber);
    when(subscriber.register(any(Message.class))).thenReturn(1);
    return new Fixture(destination, stats, manager, subscriptions, subscriber);
  }

  private record Fixture(DestinationImpl destination, DestinationStats stats,
      TransactionalMessageManager manager, DestinationSubscriptionManager subscriptions,
      Subscribable subscriber) {
  }
}
