/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.engine.destination.subscription.builders;

import io.mapsmessaging.api.SubscriptionContextBuilder;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class SelectorCompilationContractTest {

  @Test
  void standardBuilderAcceptsNoSelector() throws IOException {
    assertDoesNotThrow(() -> new StandardSubscriptionBuilder(mock(DestinationImpl.class),
        context(null), false));
  }

  @Test
  void standardBuilderAcceptsValidBooleanSelector() throws IOException {
    assertDoesNotThrow(() -> new StandardSubscriptionBuilder(mock(DestinationImpl.class),
        context("temperature > 20"), false));
  }

  @Test
  void standardBuilderRejectsMalformedSelectorBeforeCreatingSubscription() {
    IOException error = assertThrows(IOException.class,
        () -> new StandardSubscriptionBuilder(mock(DestinationImpl.class),
            context("temperature >"), false));
    assertNotNull(error.getCause());
  }

  @Test
  void sharedBuilderAlsoRejectsMalformedSelector() {
    IOException error = assertThrows(IOException.class,
        () -> new SharedSubscriptionBuilder(mock(DestinationImpl.class),
            context("temperature >")));
    assertNotNull(error.getCause());
  }

  private static SubscriptionContext context(String selector) {
    SubscriptionContextBuilder builder =
        new SubscriptionContextBuilder("topic/telemetry", ClientAcknowledgement.AUTO);
    if (selector != null) {
      builder.setSelector(selector);
    }
    return builder.build();
  }
}
