/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.engine.destination.subscription.impl.shared;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.SubscriptionContextBuilder;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.Constants;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.destination.subscription.state.MessageStateManagerImpl;
import io.mapsmessaging.engine.destination.subscription.transaction.AutoAcknowledgementController;
import io.mapsmessaging.engine.destination.subscription.transaction.FixedCreditManager;
import io.mapsmessaging.engine.session.MessageCallback;
import io.mapsmessaging.engine.session.SessionImpl;
import io.mapsmessaging.utilities.collections.bitset.BitSetFactoryImpl;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies delivery independently of protocol handlers. Two separately constructed
 * shared groups each consume every published identifier, one member per group.
 */
class SharedSubscriptionDeliveryIsolationTest {

  @Test
  void twoGroupsWithTwoMembersEachReceiveEveryMessageExactlyOncePerGroup() throws Exception {
    DestinationImpl destination = mock(DestinationImpl.class);
    when(destination.getFullyQualifiedNamespace()).thenReturn("topic/test/temperature");
    when(destination.getTypePath()).thenReturn(List.of("type=EngineSharedDeliveryTest"));

    SharedSubscription groupA = shared(destination, "group-A", "group-A");
    SharedSubscription groupB = shared(destination, "group-B", "group-B");

    MessageCallback a1 = addMember(groupA, "a1");
    MessageCallback a2 = addMember(groupA, "a2");
    MessageCallback b1 = addMember(groupB, "b1");
    MessageCallback b2 = addMember(groupB, "b2");

    for (int id = 1; id <= 10; id++) {
      Message message = new MessageBuilder().setId(id).build();
      when(destination.getMessage((long) id)).thenReturn(message);

      groupA.getMessageStateManager().register(message);
      groupB.getMessageStateManager().register(message);

      Message selectedA = groupA.retrieveNextMessageWithFlowControl();
      Message selectedB = groupB.retrieveNextMessageWithFlowControl();
      assertNotNull(selectedA);
      assertNotNull(selectedB);
      assertEquals(id, selectedA.getIdentifier());
      assertEquals(id, selectedB.getIdentifier());

      groupA.sendMessage(selectedA);
      groupB.sendMessage(selectedB);

      groupA.getMessageStateManager().commit(id);
      groupB.getMessageStateManager().commit(id);
    }

    verify(a1, times(5)).sendMessage(eq(destination), any(), any(), any());
    verify(a2, times(5)).sendMessage(eq(destination), any(), any(), any());
    verify(b1, times(5)).sendMessage(eq(destination), any(), any(), any());
    verify(b2, times(5)).sendMessage(eq(destination), any(), any(), any());

    assertNull(groupA.retrieveNextMessageWithFlowControl());
    assertNull(groupB.retrieveNextMessageWithFlowControl());
  }

  @Test
  void idleSharedGroupCannotReserveMessageWithoutMembers() throws Exception {
    DestinationImpl destination = mock(DestinationImpl.class);
    when(destination.getFullyQualifiedNamespace()).thenReturn("topic/test/idle");
    when(destination.getTypePath()).thenReturn(List.of("type=EngineSharedDeliveryTest"));
    SharedSubscription group = shared(destination, "idle", "idle");
    Message message = new MessageBuilder().setId(11).build();
    when(destination.getMessage(11L)).thenReturn(message);
    group.getMessageStateManager().register(message);

    assertNull(group.retrieveNextMessageWithFlowControl());
    assertEquals(1, group.getPending());
  }

  private static SharedSubscription shared(DestinationImpl destination, String sessionId, String name) {
    SubscriptionContext context = new SubscriptionContextBuilder(
        destination.getFullyQualifiedNamespace(), ClientAcknowledgement.AUTO)
        .setSharedName(name).setReceiveMaximum(32).build();
    MessageStateManagerImpl state = new MessageStateManagerImpl(name, 1L,
        new BitSetFactoryImpl(Constants.BITSET_BLOCK_SIZE));
    return new SharedSubscription(destination, context, sessionId, state,
        new AutoAcknowledgementController(new FixedCreditManager(32)), name);
  }

  private static MessageCallback addMember(SharedSubscription shared, String id) {
    SessionImpl session = mock(SessionImpl.class);
    MessageCallback callback = mock(MessageCallback.class);
    when(session.getName()).thenReturn(id);
    when(session.getMessageCallback()).thenReturn(callback);
    SubscriptionContext context = new SubscriptionContextBuilder(
        shared.getDestinationImpl().getFullyQualifiedNamespace(), ClientAcknowledgement.AUTO)
        .setSharedName(shared.getName()).setReceiveMaximum(32).build();
    shared.addSession(session, id, context,
        new AutoAcknowledgementController(new FixedCreditManager(32)));
    return callback;
  }
}
