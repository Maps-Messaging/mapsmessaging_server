/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 */
package io.mapsmessaging.engine.destination.subscription.tasks;

import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.subscription.impl.DestinationSubscription;
import io.mapsmessaging.engine.destination.subscription.transaction.AcknowledgementController;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class SignedDeliveryAcknowledgementTest {

  @Test
  void completionProcessesMinusOneAndMinimumLongButSkipsNull() {
    DestinationSubscription subscription = mock(DestinationSubscription.class);
    DestinationImpl destination = mock(DestinationImpl.class);
    AcknowledgementController acknowledgements = mock(AcknowledgementController.class);
    when(subscription.getDestinationImpl()).thenReturn(destination);
    when(destination.getFullyQualifiedNamespace()).thenReturn("/signed/ack");
    when(acknowledgements.messageSent()).thenReturn(-1L, Long.MIN_VALUE, null);
    when(acknowledgements.getType()).thenReturn("auto");

    MessageDeliveredListener listener = new MessageDeliveredListener(subscription, acknowledgements);
    listener.taskCall();
    listener.taskCall();
    listener.taskCall();

    verify(subscription).completeMessage(-1L);
    verify(subscription).completeMessage(Long.MIN_VALUE);
    verify(subscription, times(3)).schedule();
  }
}
