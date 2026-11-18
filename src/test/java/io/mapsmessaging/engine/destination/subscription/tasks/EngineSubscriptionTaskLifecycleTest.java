/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.engine.destination.subscription.tasks;

import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.subscription.Subscription;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.destination.subscription.SubscriptionController;
import io.mapsmessaging.engine.destination.subscription.set.DestinationSet;
import io.mapsmessaging.engine.tasks.BooleanResponse;
import io.mapsmessaging.engine.tasks.SubscriptionResponse;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EngineSubscriptionTaskLifecycleTest {

  @Test
  void subscribingToNewDestinationCreatesSubscriptionOnce() throws Exception {
    DestinationImpl destination = mock(DestinationImpl.class);
    SubscriptionController controller = mock(SubscriptionController.class);
    SubscriptionContext context = new SubscriptionContext("topic/orders");
    Subscription subscription = mock(Subscription.class);
    when(controller.createSubscription(context, destination)).thenReturn(subscription);
    AtomicLong remaining = new AtomicLong(1);

    assertInstanceOf(SubscriptionResponse.class,
        new SubscriptionTask(controller, context, destination, remaining).taskCall());

    verify(controller).createSubscription(context, destination);
    verify(subscription, never()).addContext(context);
    assertEquals(0L, remaining.get());
  }

  @Test
  void overlappingSubscriptionAddsContextInsteadOfRecreatingDestinationSubscription() throws Exception {
    DestinationImpl destination = mock(DestinationImpl.class);
    SubscriptionController controller = mock(SubscriptionController.class);
    SubscriptionContext context = new SubscriptionContext("topic/orders/#");
    Subscription subscription = mock(Subscription.class);
    when(controller.get(destination)).thenReturn(subscription);
    AtomicLong remaining = new AtomicLong(1);

    new SubscriptionTask(controller, context, destination, remaining).taskCall();

    verify(subscription).addContext(context);
    verify(controller, never()).createSubscription(any(), any());
    assertEquals(0L, remaining.get());
  }

  @Test
  void subscribeFailureStillReleasesPendingTaskCount() throws Exception {
    DestinationImpl destination = mock(DestinationImpl.class);
    SubscriptionController controller = mock(SubscriptionController.class);
    SubscriptionContext context = new SubscriptionContext("topic/orders");
    when(controller.createSubscription(context, destination)).thenThrow(new IOException("allocation failed"));
    AtomicLong remaining = new AtomicLong(1);

    assertThrows(IOException.class,
        () -> new SubscriptionTask(controller, context, destination, remaining).taskCall());
    assertEquals(0L, remaining.get());
  }

  @Test
  void unsubscribeOnlyOneOverlappingContextPreservesUnderlyingSubscription() throws Exception {
    DestinationImpl destination = mock(DestinationImpl.class);
    SubscriptionController controller = mock(SubscriptionController.class);
    SubscriptionContext removed = new SubscriptionContext("topic/orders/+");
    SubscriptionContext remainingContext = new SubscriptionContext("topic/orders/#");
    Subscription subscription = mock(Subscription.class);
    List<SubscriptionContext> contexts = new ArrayList<>(List.of(removed, remainingContext));
    doAnswer(invocation -> { contexts.remove(invocation.<SubscriptionContext>getArgument(0)); return null; })
        .when(subscription).removeContext(removed);
    when(subscription.getContexts()).thenAnswer(invocation -> new ArrayList<>(contexts));
    when(controller.get(destination)).thenReturn(subscription);
    DestinationSet set = new DestinationSet(removed, Map.of());
    AtomicLong pending = new AtomicLong(1);

    assertInstanceOf(BooleanResponse.class,
        new UnsubscribeTask(controller, destination, set, pending).taskCall());

    verify(subscription).removeContext(removed);
    verify(controller, never()).remove(destination);
    verify(subscription, never()).delete();
    assertEquals(1, contexts.size());
    assertEquals(0L, pending.get());
  }

  @Test
  void unsubscribeLastContextDeletesUnderlyingSubscription() throws Exception {
    DestinationImpl destination = mock(DestinationImpl.class);
    SubscriptionController controller = mock(SubscriptionController.class);
    SubscriptionContext removed = new SubscriptionContext("topic/orders/#");
    Subscription subscription = mock(Subscription.class);
    when(controller.get(destination)).thenReturn(subscription);
    when(subscription.getContexts()).thenReturn(List.of());
    AtomicLong pending = new AtomicLong(1);

    new UnsubscribeTask(controller, destination,
        new DestinationSet(removed, Map.of()), pending).taskCall();

    verify(subscription).removeContext(removed);
    verify(controller).remove(destination);
    verify(subscription).delete();
    assertEquals(0L, pending.get());
  }

  @Test
  void unsubscribeFailureStillReleasesPendingTaskCount() throws Exception {
    DestinationImpl destination = mock(DestinationImpl.class);
    SubscriptionController controller = mock(SubscriptionController.class);
    SubscriptionContext removed = new SubscriptionContext("topic/orders/#");
    Subscription subscription = mock(Subscription.class);
    when(controller.get(destination)).thenReturn(subscription);
    when(subscription.getContexts()).thenReturn(List.of());
    doThrow(new IOException("delete failed")).when(subscription).delete();
    AtomicLong pending = new AtomicLong(1);

    assertThrows(IOException.class, () -> new UnsubscribeTask(controller, destination,
        new DestinationSet(removed, Map.of()), pending).taskCall());
    assertEquals(0L, pending.get());
  }
}
