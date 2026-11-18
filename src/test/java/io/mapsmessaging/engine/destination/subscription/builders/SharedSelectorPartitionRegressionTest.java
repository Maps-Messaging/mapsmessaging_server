/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.engine.destination.subscription.builders;

import io.mapsmessaging.api.SubscriptionContextBuilder;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.engine.Constants;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.subscription.Subscription;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.destination.subscription.impl.shared.SessionSharedSubscription;
import io.mapsmessaging.engine.destination.subscription.impl.shared.SharedSubscription;
import io.mapsmessaging.engine.destination.subscription.impl.shared.SharedSubscriptionManager;
import io.mapsmessaging.engine.destination.subscription.impl.shared.SharedSubscriptionRegister;
import io.mapsmessaging.engine.session.SessionImpl;
import io.mapsmessaging.utilities.collections.bitset.BitSetFactoryImpl;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SharedSelectorPartitionRegressionTest {

  @Test
  void identicalSelectorAndGroupShareSameConsumerState() throws Exception {
    DestinationImpl destination = fixture("topic/telemetry");
    SharedSubscription first = subscribe(destination, "workers", "temperature > 20", "A1");
    SharedSubscription second = subscribe(destination, "workers", "temperature > 20", "A2");

    assertSame(first, second);
  }

  @Test
  void differentSelectorsRemainSeparateWithinNamedGroup() throws Exception {
    DestinationImpl destination = fixture("topic/telemetry");
    SharedSubscription hot = subscribe(destination, "workers", "temperature > 20", "A1");
    SharedSubscription cold = subscribe(destination, "workers", "temperature < 10", "A2");

    assertNotSame(hot, cold,
        "Distinct selectors must retain independent message acceptance state");
  }

  @Test
  void identicalSelectorsInDifferentGroupsRemainIndependent() throws Exception {
    DestinationImpl destination = fixture("topic/telemetry");
    SharedSubscription first = subscribe(destination, "workers-A", "temperature > 20", "A1");
    SharedSubscription second = subscribe(destination, "workers-B", "temperature > 20", "B1");

    assertNotSame(first, second,
        "Identical selectors must not merge differently named shared groups");
  }

  private static SharedSubscription subscribe(DestinationImpl destination, String group,
      String selector, String sessionId) throws Exception {
    SubscriptionContext context = new SubscriptionContextBuilder(
        destination.getFullyQualifiedNamespace(), ClientAcknowledgement.AUTO)
        .setSharedName(group).setSelector(selector).setReceiveMaximum(10).build();
    SessionImpl session = mock(SessionImpl.class);
    when(session.getName()).thenReturn(sessionId);
    Subscription subscription = new SharedSubscriptionBuilder(destination, context)
        .construct(session, sessionId, sessionId, 1L);
    assertInstanceOf(SessionSharedSubscription.class, subscription);
    Field field = SessionSharedSubscription.class.getDeclaredField("sharedSubscription");
    field.setAccessible(true);
    return (SharedSubscription) field.get(subscription);
  }

  private static DestinationImpl fixture(String topic) {
    DestinationImpl destination = mock(DestinationImpl.class);
    SharedSubscriptionRegister registry = new SharedSubscriptionRegister();
    when(destination.getFullyQualifiedNamespace()).thenReturn(topic);
    when(destination.getTypePath()).thenReturn(List.of("type=SharedSelectorPartitionTest"));
    when(destination.getSubscriptionBitsetFactory()).thenReturn(
        new BitSetFactoryImpl(Constants.BITSET_BLOCK_SIZE));
    when(destination.findShareRegister(anyString()))
        .thenAnswer(invocation -> registry.get(invocation.getArgument(0)));
    doAnswer(invocation -> {
      SharedSubscriptionManager group = invocation.getArgument(0);
      registry.add(group.getName(), group);
      return null;
    }).when(destination).addShareRegistry(any(SharedSubscriptionManager.class));
    return destination;
  }
}
