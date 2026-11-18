/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.engine.destination.subscription.tasks;

import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.subscription.impl.DestinationSubscription;
import io.mapsmessaging.engine.destination.subscription.impl.MessageDeliveryCompletionTask;
import io.mapsmessaging.engine.destination.subscription.impl.shared.SharedSubscription;
import io.mapsmessaging.engine.destination.subscription.transaction.AcknowledgementController;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EngineDeliveryCompletionBoundaryTest {

  @Test
  void autoCompletionCommitsOnlyReturnedIdentifier() {
    DestinationSubscription subscription = mock(DestinationSubscription.class);
    AcknowledgementController controller = mock(AcknowledgementController.class);
    when(subscription.getDestinationImpl()).thenReturn(mock(DestinationImpl.class));
    when(controller.messageSent()).thenReturn(77L);

    new MessageDeliveredListener(subscription, controller).taskCall();

    verify(subscription).completeMessage(77L);
    verify(subscription).schedule();
  }

  @Test
  void completionWithoutOutstandingIdentifierDoesNotCommit() {
    DestinationSubscription subscription = mock(DestinationSubscription.class);
    AcknowledgementController controller = mock(AcknowledgementController.class);
    when(subscription.getDestinationImpl()).thenReturn(mock(DestinationImpl.class));
    when(controller.messageSent()).thenReturn((Long) null);

    new MessageDeliveredListener(subscription, controller).taskCall();

    verify(subscription, never()).completeMessage(anyLong());
    verify(subscription).schedule();
  }

  @Test
  void completionCallbackSubmitsTaskToDestination() {
    DestinationImpl destination = mock(DestinationImpl.class);
    DestinationSubscription subscription = mock(DestinationSubscription.class);
    when(subscription.getDestinationImpl()).thenReturn(destination);

    new MessageDeliveryCompletionTask(subscription, mock(AcknowledgementController.class)).run();

    verify(destination).submit(any(MessageDeliveredListener.class));
  }

  @Test
  void sharedAcknowledgementRunsCleanupBeforeReleasingDeliveryState() {
    SharedSubscription shared = mock(SharedSubscription.class);
    AcknowledgementController consumer = mock(AcknowledgementController.class);
    Runnable cleanup = mock(Runnable.class);

    new SharedSubscriptionTask(shared, consumer, 80L, true, cleanup).taskCall();

    org.mockito.InOrder order = inOrder(cleanup, consumer, shared);
    order.verify(cleanup).run();
    order.verify(consumer).ack(80L);
    order.verify(shared).ackReceived(80L);
  }

  @Test
  void sharedRollbackOnlyReleasesConsumerClaim() {
    SharedSubscription shared = mock(SharedSubscription.class);
    AcknowledgementController consumer = mock(AcknowledgementController.class);

    new SharedSubscriptionTask(shared, consumer, 81L, false).taskCall();

    verify(consumer).rollback(81L);
    verify(shared, never()).ackReceived(anyLong());
  }
}
