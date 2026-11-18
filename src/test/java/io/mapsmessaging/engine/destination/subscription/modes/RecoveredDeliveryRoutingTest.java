/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.engine.destination.subscription.modes;

import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.subscription.Subscription;
import io.mapsmessaging.storage.delivery.PacketIdStore;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RecoveredDeliveryRoutingTest {

  @Test
  void recoveredAckUsesOnlySubscriptionWithMatchingDeliveryKey() {
    NormalSubscriptionModeManager manager = new NormalSubscriptionModeManager();
    Subscription first = subscription("topic/first");
    Subscription second = subscription("topic/second");
    manager.add(first, mock(DestinationImpl.class));
    manager.add(second, mock(DestinationImpl.class));
    CompletableFuture<Void> completion = CompletableFuture.completedFuture(null);
    when(second.acknowledgeRecoveredDelivery(42L)).thenReturn(completion);

    CompletableFuture<Void> result = manager.acknowledgeRecoveredDelivery(
        new PacketIdStore.Delivery("topic/second", 42L));

    assertSame(completion, result);
    verify(second).acknowledgeRecoveredDelivery(42L);
    verify(first, never()).acknowledgeRecoveredDelivery(anyLong());
  }

  @Test
  void unmatchedRecoveredDeliveryDoesNotAckUnrelatedSubscription() {
    NormalSubscriptionModeManager manager = new NormalSubscriptionModeManager();
    Subscription first = subscription("topic/first");
    manager.add(first, mock(DestinationImpl.class));

    assertNull(manager.acknowledgeRecoveredDelivery(
        new PacketIdStore.Delivery("topic/missing", 7L)));
    verify(first, never()).acknowledgeRecoveredDelivery(anyLong());
  }

  @Test
  void emptyContextIsNotConsideredForRecoveredAcknowledgements() {
    NormalSubscriptionModeManager manager = new NormalSubscriptionModeManager();
    Subscription stale = subscription("topic/stale");
    when(stale.isContextEmpty()).thenReturn(true);
    manager.add(stale, mock(DestinationImpl.class));

    assertNull(manager.acknowledgeRecoveredDelivery(
        new PacketIdStore.Delivery("topic/stale", 8L)));
    verify(stale, never()).acknowledgeRecoveredDelivery(anyLong());
  }

  @Test
  void distinctDeliveryKeysRetainIndependentAcknowledgementOwnership() {
    NormalSubscriptionModeManager manager = new NormalSubscriptionModeManager();
    Subscription groupA = subscription("shared/group-A/topic/orders");
    Subscription groupB = subscription("shared/group-B/topic/orders");
    manager.add(groupA, mock(DestinationImpl.class));
    manager.add(groupB, mock(DestinationImpl.class));
    CompletableFuture<Void> completion = CompletableFuture.completedFuture(null);
    when(groupB.acknowledgeRecoveredDelivery(9L)).thenReturn(completion);

    assertSame(completion, manager.acknowledgeRecoveredDelivery(
        new PacketIdStore.Delivery("shared/group-B/topic/orders", 9L)));
    verify(groupB).acknowledgeRecoveredDelivery(9L);
    verify(groupA, never()).acknowledgeRecoveredDelivery(anyLong());
  }

  private static Subscription subscription(String deliveryKey) {
    Subscription subscription = mock(Subscription.class);
    when(subscription.getDeliveryKey()).thenReturn(deliveryKey);
    return subscription;
  }
}
