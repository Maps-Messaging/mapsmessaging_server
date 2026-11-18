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
import io.mapsmessaging.engine.tasks.FutureResponse;
import io.mapsmessaging.engine.tasks.LongResponse;
import io.mapsmessaging.engine.tasks.Response;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Tests storage decisions in the engine without network or protocol handlers. */
class StoreMessageRoutingContractTest {

  @Test
  void nonRetainedTopicMessageWithoutSubscribersIsNotStored() throws Exception {
    Fixture fixture = fixture();
    Message message = new MessageBuilder().setId(1L).build();

    Response response = new NonDelayedStoreMessageTask(
        fixture.destination, fixture.subscriptions, message).taskCall();

    assertInstanceOf(LongResponse.class, response);
    verify(fixture.destination, never()).addMessage(message);
    verify(fixture.stats).noInterest();
  }

  @Test
  void retainedTopicMessageIsStoredEvenWithoutSubscribers() throws Exception {
    Fixture fixture = fixture();
    Message message = new MessageBuilder().setId(2L).setRetain(true).build();

    Response response = new NonDelayedStoreMessageTask(
        fixture.destination, fixture.subscriptions, message).taskCall();

    assertInstanceOf(FutureResponse.class, response);
    verify(fixture.destination).addMessage(message);
    verify(fixture.destination).submit(any(SubscriptionUpdateTask.class));
  }

  @Test
  void queueMessagesAreStoredEvenWithoutActiveConsumers() throws Exception {
    Fixture fixture = fixture();
    Message message = new MessageBuilder().setId(3L).build();

    Response response = new QueueBasedStoreMessageTask(
        fixture.destination, fixture.subscriptions, message).taskCall();

    assertInstanceOf(FutureResponse.class, response);
    verify(fixture.destination).addMessage(message);
    verify(fixture.destination).submit(any(SubscriptionUpdateTask.class));
  }

  @Test
  void replacingRetainedMessageSchedulesRemovalOfPreviousIdentifier() throws Exception {
    Fixture fixture = fixture();
    Message message = new MessageBuilder().setId(4L).setRetain(true).build();
    when(fixture.destination.getRetainedIdentifier()).thenReturn(3L);

    new NonDelayedStoreMessageTask(fixture.destination, fixture.subscriptions, message).taskCall();

    verify(fixture.destination).submit(any(RemoveMessageTask.class), eq(DestinationImpl.DELETE_PRIORITY));
    verify(fixture.destination).addMessage(message);
  }

  private static Fixture fixture() {
    DestinationImpl destination = mock(DestinationImpl.class);
    DestinationStats stats = mock(DestinationStats.class);
    when(destination.getStats()).thenReturn(stats);
    when(destination.submit(any(Callable.class))).thenReturn(
        CompletableFuture.<Response>completedFuture(new LongResponse(1)));
    return new Fixture(destination, stats, new DestinationSubscriptionManager("test"));
  }

  private record Fixture(DestinationImpl destination, DestinationStats stats,
                         DestinationSubscriptionManager subscriptions) {
  }
}
