/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 */

package io.mapsmessaging.rest.api.impl.messaging.impl;

import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.api.features.Priority;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.dto.rest.messaging.MessageDTO;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RestMessageListenerCoverageTest {

  private final int originalMax = RestMessageListener.getMaxSubscribedMessages();

  @AfterEach
  void restoreMaxSubscribedMessages() {
    RestMessageListener.setMaxSubscribedMessages(originalMax);
  }

  @Test
  void unknownSubscriptionHasZeroDepthAndNoDestinations() {
    RestMessageListener listener = new RestMessageListener();

    assertEquals(0, listener.subscriptionDepth("/missing"));
    assertTrue(listener.subscriptionDepth().isEmpty());
    assertTrue(listener.getKnownDestinations().isEmpty());
  }

  @Test
  void syncDeliveryQueuesBySubscriptionAndPhysicalDestination() {
    RestMessageListener listener = new RestMessageListener();
    listener.sendMessage(event("/sub/#", "/one", 1));
    listener.sendMessage(event("/sub/#", "/two", 2));
    listener.sendMessage(event("/sub/#", "/one", 3));

    assertEquals(3, listener.subscriptionDepth("/sub/#"));
    assertEquals(Map.of("/sub/#", 3), listener.subscriptionDepth());
    assertEquals(2, listener.getKnownDestinations().size());
    assertTrue(listener.getKnownDestinations().contains("/one"));
    assertTrue(listener.getKnownDestinations().contains("/two"));
  }

  @Test
  void syncDeliveryRetainsOnlyConfiguredMaximumPerDestination() {
    RestMessageListener listener = new RestMessageListener();
    RestMessageListener.setMaxSubscribedMessages(2);

    listener.sendMessage(event("/sub", "/one", 1));
    listener.sendMessage(event("/sub", "/one", 2));
    listener.sendMessage(event("/sub", "/one", 3));

    assertEquals(2, listener.subscriptionDepth("/sub"));
    Map<String, List<MessageDTO>> drained = listener.getMessages("/sub", 10);
    assertEquals(List.of(2L, 3L),
        drained.get("/one").stream().map(MessageDTO::getIdentifier).toList());
  }

  @ParameterizedTest
  @ValueSource(ints = {-100, -1, 0})
  void nonPositiveGetMessagesLimitDefaultsToTen(int requested) {
    RestMessageListener listener = new RestMessageListener();
    RestMessageListener.setMaxSubscribedMessages(20);
    for (int i = 1; i <= 15; i++) {
      listener.sendMessage(event("/sub", "/one", i));
    }

    Map<String, List<MessageDTO>> drained = listener.getMessages("/sub", requested);

    assertEquals(10, drained.get("/one").size());
    assertEquals(5, listener.subscriptionDepth("/sub"));
  }

  @Test
  void getMessagesLimitIsCappedAtOneThousand() {
    RestMessageListener listener = new RestMessageListener();
    RestMessageListener.setMaxSubscribedMessages(1100);
    for (int i = 1; i <= 1005; i++) {
      listener.sendMessage(event("/sub", "/one", i));
    }

    Map<String, List<MessageDTO>> drained = listener.getMessages("/sub", 5000);

    assertEquals(1000, drained.get("/one").size());
    assertEquals(5, listener.subscriptionDepth("/sub"));
  }

  @Test
  void getMessagesDrainsAcrossDestinationsUntilLimit() {
    RestMessageListener listener = new RestMessageListener();
    listener.sendMessage(event("/sub", "/one", 1));
    listener.sendMessage(event("/sub", "/two", 2));
    listener.sendMessage(event("/sub", "/one", 3));
    listener.sendMessage(event("/sub", "/two", 4));

    Map<String, List<MessageDTO>> drained = listener.getMessages("/sub", 3);

    assertEquals(3, drained.values().stream().mapToInt(List::size).sum());
    assertEquals(1, listener.subscriptionDepth("/sub"));
  }

  @Test
  void getMessagesUnknownNamespaceReturnsEmptyMap() {
    assertTrue(new RestMessageListener().getMessages("/missing", 10).isEmpty());
  }

  @Test
  void ackReceivedAcknowledgesEverySuppliedId() {
    RestMessageListener listener = new RestMessageListener();
    Session session = mock(Session.class);
    SubscribedEventManager manager = mock(SubscribedEventManager.class);
    listener.registerEventManager("/sub", session, manager);

    assertTrue(listener.ackReceived("/sub", List.of(1L, 2L, 3L)));

    verify(manager).ackReceived(1L);
    verify(manager).ackReceived(2L);
    verify(manager).ackReceived(3L);
  }

  @Test
  void nakReceivedRollsBackEverySuppliedId() {
    RestMessageListener listener = new RestMessageListener();
    Session session = mock(Session.class);
    SubscribedEventManager manager = mock(SubscribedEventManager.class);
    listener.registerEventManager("/sub", session, manager);

    assertTrue(listener.nakReceived("/sub", List.of(4L, 5L)));

    verify(manager).rollbackReceived(4L);
    verify(manager).rollbackReceived(5L);
  }

  @Test
  void ackAndNakRejectUnknownOrEmptyRequests() {
    RestMessageListener listener = new RestMessageListener();
    Session session = mock(Session.class);
    SubscribedEventManager manager = mock(SubscribedEventManager.class);
    listener.registerEventManager("/sub", session, manager);

    assertFalse(listener.ackReceived("/missing", List.of(1L)));
    assertFalse(listener.ackReceived("/sub", List.of()));
    assertFalse(listener.nakReceived("/missing", List.of(1L)));
    assertFalse(listener.nakReceived("/sub", List.of()));

    verifyNoInteractions(manager);
  }

  @Test
  void deregisterRemovesQueuedMessagesAndSubscription() {
    RestMessageListener listener = new RestMessageListener();
    Session session = mock(Session.class);
    SubscribedEventManager manager = mock(SubscribedEventManager.class);
    listener.registerEventManager("/sub", session, manager);
    listener.sendMessage(event("/sub", "/one", 1));

    listener.deregisterEventManager("/sub");

    assertEquals(0, listener.subscriptionDepth("/sub"));
    verify(session).removeSubscription("/sub");
    assertFalse(listener.ackReceived("/sub", List.of(1L)));
  }

  @Test
  void closeShouldRejectFurtherDeliveryAndCompleteEvent() {
    RestMessageListener listener = new RestMessageListener();
    listener.close();

    MessageEvent event = event("/sub", "/one", 77);
    listener.sendMessage(event);

    verify(event.getCompletionTask()).run();
    verify(event.getSubscription()).ackReceived(77L);
    assertEquals(0, listener.subscriptionDepth("/sub"));
  }

  private MessageEvent event(String subscriptionName, String destinationName, long id) {
    SubscriptionContext context = new SubscriptionContext(subscriptionName);
    SubscribedEventManager manager = mock(SubscribedEventManager.class);
    when(manager.getContext()).thenReturn(context);

    Message message = mock(Message.class);
    when(message.getIdentifier()).thenReturn(id);
    when(message.getPriority()).thenReturn(Priority.NORMAL);
    when(message.getOpaqueData()).thenReturn(("payload-" + id).getBytes());
    when(message.getCreation()).thenReturn(1_000L + id);
    when(message.getExpiry()).thenReturn(0L);
    when(message.getCorrelationData()).thenReturn(null);
    when(message.getContentType()).thenReturn("text/plain");
    when(message.getQualityOfService()).thenReturn(QualityOfService.AT_MOST_ONCE);
    when(message.getMeta()).thenReturn(Map.of());
    when(message.getDataMap()).thenReturn(Map.of());
    when(message.getSchemaId()).thenReturn(null);

    Runnable completion = mock(Runnable.class);
    return new MessageEvent(destinationName, manager, message, completion);
  }
}
