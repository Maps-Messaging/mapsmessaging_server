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

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Engine-only regression: the shared group name, not merely the destination name,
 * must determine the competing-consumer group.
 */
class SharedSubscriptionGroupIsolationRegressionTest {

  @Test
  void differentGroupsOnSameTopicHaveIndependentMessageState() throws Exception {
    DestinationImpl destination = destination("topic/sensors/temperature");

    Subscription groupA = subscribe(destination, "group-A", "A1");
    Subscription groupB = subscribe(destination, "group-B", "B1");

    assertNotSame(shared(groupA), shared(groupB),
        "Each named group must hold its own message state and receive its own copy of every publication");
  }

  @Test
  void twoMembersOfSameGroupShareOneCompetingConsumerState() throws Exception {
    DestinationImpl destination = destination("topic/sensors/humidity");

    Subscription first = subscribe(destination, "group-A", "A1");
    Subscription second = subscribe(destination, "group-A", "A2");

    assertSame(shared(first), shared(second),
        "Consumers belonging to the same named group must compete for one shared message state");
  }

  @Test
  void differentGroupsRemainDistinctRegardlessOfSubscriptionOrder() throws Exception {
    DestinationImpl destination = destination("topic/sensors/pressure");

    Subscription firstB = subscribe(destination, "group-B", "B1");
    Subscription firstA = subscribe(destination, "group-A", "A1");
    Subscription secondB = subscribe(destination, "group-B", "B2");

    assertNotSame(shared(firstA), shared(firstB));
    assertSame(shared(firstB), shared(secondB));
  }

  private static DestinationImpl destination(String name) {
    DestinationImpl destination = mock(DestinationImpl.class);
    SharedSubscriptionRegister registry = new SharedSubscriptionRegister();
    when(destination.getFullyQualifiedNamespace()).thenReturn(name);
    when(destination.getTypePath()).thenReturn(List.of("type=EngineIsolationTest"));
    when(destination.getSubscriptionBitsetFactory())
        .thenReturn(new BitSetFactoryImpl(Constants.BITSET_BLOCK_SIZE));
    when(destination.findShareRegister(name)).thenAnswer(invocation -> registry.get(name));
    doAnswer(invocation -> {
      SharedSubscriptionManager manager = invocation.getArgument(0);
      registry.add(manager.getName(), manager);
      return null;
    }).when(destination).addShareRegistry(org.mockito.ArgumentMatchers.any(SharedSubscriptionManager.class));
    return destination;
  }

  private static Subscription subscribe(DestinationImpl destination, String group, String sessionId) throws Exception {
    SubscriptionContext context = new SubscriptionContextBuilder(
        destination.getFullyQualifiedNamespace(), ClientAcknowledgement.AUTO)
        .setSharedName(group)
        .setReceiveMaximum(32)
        .build();
    SessionImpl session = mock(SessionImpl.class);
    when(session.getName()).thenReturn(sessionId);

    Subscription subscription = new SharedSubscriptionBuilder(destination, context)
        .construct(session, sessionId, sessionId, 1L);
    assertTrue(subscription instanceof SessionSharedSubscription);
    return subscription;
  }

  private static SharedSubscription shared(Subscription subscription) throws Exception {
    Field field = SessionSharedSubscription.class.getDeclaredField("sharedSubscription");
    field.setAccessible(true);
    return (SharedSubscription) field.get(subscription);
  }
}
