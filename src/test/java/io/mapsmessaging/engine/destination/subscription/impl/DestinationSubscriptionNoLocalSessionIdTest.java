/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.engine.destination.subscription.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.destination.subscription.state.MessageStateManager;
import io.mapsmessaging.engine.destination.subscription.transaction.AcknowledgementController;
import io.mapsmessaging.engine.session.SessionImpl;
import io.mapsmessaging.utilities.threads.tasks.ThreadLocalContext;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class DestinationSubscriptionNoLocalSessionIdTest {

  @Test
  void matchingPublisherSessionIdIsSuppressedAtRegistrationBoundary() {
    String sessionId = "msg324-session-A";
    SubscriptionContext context = new SubscriptionContext("msg324/no-local");
    context.setNoLocalMessages(true);
    MessageStateManager state = mock(MessageStateManager.class);
    DestinationSubscription subscription = new DestinationSubscription(
        mock(DestinationImpl.class), context, mock(SessionImpl.class), sessionId,
        mock(AcknowledgementController.class), state, false);
    Message message = new MessageBuilder()
        .setMeta(new HashMap<>(Map.of("sessionId", sessionId, "protocol", "MQTT-SN", "version", "2.0")))
        .setOpaqueData(new byte[] {7}).build();

    try (MockedStatic<ThreadLocalContext> ignored = mockStatic(ThreadLocalContext.class)) {
      assertEquals(sessionId, message.getMeta().get("sessionId"));
      assertEquals(sessionId, subscription.getSessionId());
      assertTrue(subscription.getContext().noLocalMessages());
      assertEquals(0, subscription.register(message));
      verifyNoInteractions(state);
    }
  }

  @Test
  void differentPublisherSessionIdPassesNoLocalFilter() {
    String subscriberId = "msg324-session-A";
    String publisherId = "msg324-session-B";
    SubscriptionContext context = new SubscriptionContext("msg324/no-local");
    context.setNoLocalMessages(true);
    MessageStateManager state = mock(MessageStateManager.class);
    DestinationSubscription subscription = spy(new DestinationSubscription(
        mock(DestinationImpl.class), context, mock(SessionImpl.class), subscriberId,
        mock(AcknowledgementController.class), state, false));
    doReturn(true).when(subscription).schedule();
    Message message = new MessageBuilder()
        .setMeta(new HashMap<>(Map.of("sessionId", publisherId, "protocol", "MQTT-SN", "version", "2.0")))
        .setOpaqueData(new byte[] {8}).build();

    try (MockedStatic<ThreadLocalContext> ignored = mockStatic(ThreadLocalContext.class)) {
      assertEquals(subscriberId, subscription.getSessionId());
      assertEquals(publisherId, message.getMeta().get("sessionId"));
      assertTrue(subscription.getContext().noLocalMessages());
      assertEquals(1, subscription.register(message));
      verify(state).register(message);
    }
  }
}
