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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Regression tests for shared group membership changes independent of any network protocol. */
class SharedSubscriptionMembershipRegressionTest {

  @Test
  void replacingMemberWithSameSessionIdRoutesToReplacementNotStaleSession() throws Exception {
    Fixture fixture = fixture("topic/membership/replacement");
    MessageCallback oldCallback = mock(MessageCallback.class);
    MessageCallback replacementCallback = mock(MessageCallback.class);

    add(fixture, session("member-1", oldCallback), "member-1");
    add(fixture, session("member-1", replacementCallback), "member-1");

    Message message = new MessageBuilder().setId(1L).build();
    when(fixture.destination.getMessage(1L)).thenReturn(message);
    fixture.state.register(message);
    Message selected = fixture.shared.retrieveNextMessageWithFlowControl();
    assertNotNull(selected);
    fixture.shared.sendMessage(selected);

    verify(replacementCallback).sendMessage(eq(fixture.destination), any(), eq(message), any());
    verify(oldCallback, never()).sendMessage(eq(fixture.destination), any(), eq(message), any());
  }

  @Test
  void removingReplacedSessionDoesNotRemoveCurrentMember() throws Exception {
    Fixture fixture = fixture("topic/membership/reconnect");
    SessionImpl original = session("same-display", mock(MessageCallback.class));
    MessageCallback currentCallback = mock(MessageCallback.class);
    SessionImpl current = session("same-display", currentCallback);

    add(fixture, original, "member-1");
    add(fixture, current, "member-1");
    fixture.shared.removeSession(original);

    Message message = new MessageBuilder().setId(3L).build();
    when(fixture.destination.getMessage(3L)).thenReturn(message);
    fixture.state.register(message);
    Message selected = fixture.shared.retrieveNextMessageWithFlowControl();
    assertNotNull(selected);
    fixture.shared.sendMessage(selected);

    verify(currentCallback).sendMessage(eq(fixture.destination), any(), eq(message), any());
  }

  @Test
  void removingMemberUsesItsRegisteredSessionIdNotTheSessionDisplayName() throws Exception {
    Fixture fixture = fixture("topic/membership/removal");
    MessageCallback removedCallback = mock(MessageCallback.class);
    MessageCallback remainingCallback = mock(MessageCallback.class);
    SessionImpl removed = session("display-name", removedCallback);
    SessionImpl remaining = session("remaining", remainingCallback);

    add(fixture, removed, "unique-session-id");
    add(fixture, remaining, "remaining");
    fixture.shared.removeSession(removed);

    Message message = new MessageBuilder().setId(2L).build();
    when(fixture.destination.getMessage(2L)).thenReturn(message);
    fixture.state.register(message);
    Message selected = fixture.shared.retrieveNextMessageWithFlowControl();
    assertNotNull(selected);
    fixture.shared.sendMessage(selected);

    verify(removedCallback, never()).sendMessage(eq(fixture.destination), any(), eq(message), any());
    verify(remainingCallback).sendMessage(eq(fixture.destination), any(), eq(message), any());
  }

  @Test
  void removedMemberCannotReceiveMessageReservedBeforeRemoval() throws Exception {
    Fixture fixture = fixture("topic/membership/reserved-removal");
    MessageCallback removedCallback = mock(MessageCallback.class);
    SessionImpl removed = session("removed", removedCallback);
    add(fixture, removed, "removed");
    add(fixture, session("remaining", mock(MessageCallback.class)), "remaining");

    Message message = new MessageBuilder().setId(40L).build();
    when(fixture.destination.getMessage(40L)).thenReturn(message);
    fixture.state.register(message);
    assertNotNull(fixture.shared.retrieveNextMessageWithFlowControl());

    fixture.shared.removeSession(removed);
    fixture.shared.sendMessage(message);

    verify(removedCallback, never()).sendMessage(eq(fixture.destination), any(), eq(message), any());
  }

  @Test
  void replacedMemberCannotReceiveMessageReservedBeforeReplacement() throws Exception {
    Fixture fixture = fixture("topic/membership/reserved-replacement");
    MessageCallback oldCallback = mock(MessageCallback.class);
    add(fixture, session("member", oldCallback), "member-1");

    Message message = new MessageBuilder().setId(41L).build();
    when(fixture.destination.getMessage(41L)).thenReturn(message);
    fixture.state.register(message);
    assertNotNull(fixture.shared.retrieveNextMessageWithFlowControl());

    add(fixture, session("member", mock(MessageCallback.class)), "member-1");
    fixture.shared.sendMessage(message);

    verify(oldCallback, never()).sendMessage(eq(fixture.destination), any(), eq(message), any());
  }

  @Test
  void removingFinalMemberLeavesNoEligibleConsumer() throws Exception {
    Fixture fixture = fixture("topic/membership/last");
    SessionImpl only = session("only", mock(MessageCallback.class));
    add(fixture, only, "only");
    fixture.shared.removeSession(only);

    verify(fixture.destination).removeSubscription("shared-session");
    org.junit.jupiter.api.Assertions.assertTrue(
        fixture.destination.findShareRegister(fixture.destination.getFullyQualifiedNamespace()).isEmpty(),
        "Removing the final member must remove the shared group registration");
  }

  private static Fixture fixture(String name) {
    DestinationImpl destination = mock(DestinationImpl.class);
    when(destination.getFullyQualifiedNamespace()).thenReturn(name);
    when(destination.getTypePath()).thenReturn(List.of("type=EngineMembershipTest"));
    SharedSubscriptionManager registry = new SharedSubscriptionManager(name);
    when(destination.findShareRegister(name)).thenReturn(registry);
    SubscriptionContext context = context(name);
    MessageStateManagerImpl state = new MessageStateManagerImpl(name, 1L,
        new BitSetFactoryImpl(Constants.BITSET_BLOCK_SIZE));
    SharedSubscription shared = new SharedSubscription(destination, context, "shared-session",
        state, new AutoAcknowledgementController(new FixedCreditManager(32)), "shared-normal");
    registry.add("shared-normal", shared);
    return new Fixture(destination, shared, state);
  }

  private static SubscriptionContext context(String topic) {
    return new SubscriptionContextBuilder(topic, ClientAcknowledgement.AUTO)
        .setSharedName("group").setReceiveMaximum(32).build();
  }

  private static SessionImpl session(String name, MessageCallback callback) {
    SessionImpl session = mock(SessionImpl.class);
    when(session.getName()).thenReturn(name);
    when(session.getMessageCallback()).thenReturn(callback);
    return session;
  }

  private static void add(Fixture fixture, SessionImpl session, String sessionId) {
    fixture.shared.addSession(session, sessionId,
        context(fixture.destination.getFullyQualifiedNamespace()),
        new AutoAcknowledgementController(new FixedCreditManager(32)));
  }

  private record Fixture(DestinationImpl destination, SharedSubscription shared, MessageStateManagerImpl state) {
  }
}
