/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.engine.destination.subscription.impl.shared;

import io.mapsmessaging.api.SubscriptionContextBuilder;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.engine.Constants;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.destination.subscription.state.MessageStateManagerImpl;
import io.mapsmessaging.engine.destination.subscription.transaction.AutoAcknowledgementController;
import io.mapsmessaging.engine.destination.subscription.transaction.FixedCreditManager;
import io.mapsmessaging.utilities.collections.bitset.BitSetFactoryImpl;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A real SharedSubscription.close() removes itself from its manager.
 * Closing the manager must tolerate that removal while closing all entries.
 */
class SharedSubscriptionManagerCloseRegressionTest {

  @Test
  void closingGroupWithMultipleSubscriptionVariantsDoesNotMutateActiveIterator() {
    String name = "topic/shared/close";
    DestinationImpl destination = mock(DestinationImpl.class);
    when(destination.getFullyQualifiedNamespace()).thenReturn(name);
    when(destination.getTypePath()).thenReturn(List.of("type=SharedCloseTest"));
    SharedSubscriptionManager manager = new SharedSubscriptionManager(name);
    when(destination.findShareRegister(name)).thenReturn(manager);

    manager.add("variant-one", shared(destination, name, "first", "variant-one"));
    manager.add("variant-two", shared(destination, name, "second", "variant-two"));

    assertDoesNotThrow(manager::close,
        "Closing all shared subscriptions must not throw due to registry mutation");
    assertTrue(manager.isEmpty(), "All variants must be removed");
  }

  private static SharedSubscription shared(DestinationImpl destination, String topic,
      String sessionId, String name) {
    SubscriptionContext context = new SubscriptionContextBuilder(topic, ClientAcknowledgement.AUTO)
        .setSharedName("workers").build();
    MessageStateManagerImpl state = new MessageStateManagerImpl(name, 1L,
        new BitSetFactoryImpl(Constants.BITSET_BLOCK_SIZE));
    return new SharedSubscription(destination, context, sessionId, state,
        new AutoAcknowledgementController(new FixedCreditManager(16)), name);
  }
}
