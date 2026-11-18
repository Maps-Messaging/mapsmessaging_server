/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.engine.destination.subscription;

import io.mapsmessaging.api.SubscriptionContextBuilder;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.api.features.DestinationMode;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.subscription.builders.QueueSubscriptionBuilder;
import io.mapsmessaging.engine.destination.subscription.builders.SchemaSubscriptionBuilder;
import io.mapsmessaging.engine.destination.subscription.builders.SharedSubscriptionBuilder;
import io.mapsmessaging.engine.destination.subscription.builders.StandardSubscriptionBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Verifies generic engine subscription-mode dispatch without any protocol connection. */
class SubscriptionFactoryRoutingContractTest {

  @Test
  void normalTopicUsesIndependentSubscriberBuilder() throws Exception {
    DestinationImpl destination = destination(false);
    SubscriptionContext context = context("topic/weather");

    assertInstanceOf(StandardSubscriptionBuilder.class,
        SubscriptionFactory.getInstance().getBuilder(destination, context, false));
  }

  @Test
  void queueUsesCompetingConsumerBuilder() throws Exception {
    DestinationImpl destination = destination(true);
    SubscriptionContext context = context("queue/orders");

    assertInstanceOf(QueueSubscriptionBuilder.class,
        SubscriptionFactory.getInstance().getBuilder(destination, context, false));
  }

  @Test
  void sharedTopicUsesSharedBuilderNotOrdinaryTopicBuilder() throws Exception {
    DestinationImpl destination = destination(false);
    SubscriptionContext context = new SubscriptionContextBuilder(
        "topic/weather", ClientAcknowledgement.AUTO).setSharedName("workers").build();

    assertInstanceOf(SharedSubscriptionBuilder.class,
        SubscriptionFactory.getInstance().getBuilder(destination, context, false));
  }

  @Test
  void schemaModeTakesPrecedenceOverSharedAndQueueRouting() throws Exception {
    DestinationImpl destination = destination(true);
    SubscriptionContext context = new SubscriptionContextBuilder(
        "$schema/queue/orders", ClientAcknowledgement.AUTO).setSharedName("workers").build();
    context.setDestinationMode(DestinationMode.SCHEMA);

    assertInstanceOf(SchemaSubscriptionBuilder.class,
        SubscriptionFactory.getInstance().getBuilder(destination, context, false));
  }

  private static SubscriptionContext context(String name) {
    return new SubscriptionContextBuilder(name, ClientAcknowledgement.AUTO).build();
  }

  private static DestinationImpl destination(boolean queue) {
    DestinationImpl destination = mock(DestinationImpl.class, RETURNS_DEEP_STUBS);
    when(destination.getResourceType().isQueue()).thenReturn(queue);
    return destination;
  }
}
