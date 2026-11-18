/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.engine.destination.subscription.state;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.Constants;
import io.mapsmessaging.utilities.collections.bitset.BitSetFactoryImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageStateLifecycleContractTest {

  @Test
  void registerAllocateCommitClearsAllDeliveryInterest() {
    MessageStateManagerImpl state = state();
    Message message = message(10);

    state.register(message);
    assertEquals(1, state.pending());
    assertTrue(state.hasMessage(10));

    state.allocate(message);
    assertEquals(0, state.pending());
    assertTrue(state.hasMessagesInFlight());
    assertTrue(state.hasMessage(10));

    state.commit(10);
    assertFalse(state.hasMessagesInFlight());
    assertFalse(state.hasMessage(10));
  }

  @Test
  void rollbackReturnsInflightMessageToPendingForRedelivery() {
    MessageStateManagerImpl state = state();
    Message message = message(11);

    state.register(message);
    state.allocate(message);
    assertTrue(state.rollback(11));

    assertFalse(state.hasMessagesInFlight());
    assertTrue(state.hasAtRestMessages());
    assertEquals(11L, state.nextMessageId());
    assertTrue(state.hasMessage(11));
  }

  @Test
  void rollbackAllPreservesOutstandingMessagesAcrossConsumerLoss() {
    MessageStateManagerImpl state = state();
    Message first = message(21);
    Message second = message(22);

    state.register(first);
    state.register(second);
    state.allocate(first);
    state.allocate(second);
    assertEquals(0, state.pending());

    state.rollbackInFlightMessages();

    assertFalse(state.hasMessagesInFlight());
    assertEquals(2, state.pending());
    assertTrue(state.hasMessage(21));
    assertTrue(state.hasMessage(22));
  }

  @Test
  void expiryInvalidatesPendingAndInflightState() {
    MessageStateManagerImpl state = state();
    Message pending = message(31);
    Message inFlight = message(32);
    state.register(pending);
    state.register(inFlight);
    state.allocate(inFlight);

    state.expired(31);
    state.expired(32);

    assertEquals(0, state.pending());
    assertFalse(state.hasMessagesInFlight());
    assertFalse(state.hasMessage(31));
    assertFalse(state.hasMessage(32));
  }

  @Test
  void retainedReplayMarkerIsClearedByCompletionAndExpiry() {
    MessageStateManagerImpl state = state();
    state.registerRetainedReplay(41L);
    assertTrue(state.isRetainedReplay(41L));
    assertTrue(state.hasMessage(41L));

    state.commit(41L);
    assertFalse(state.isRetainedReplay(41L));

    state.registerRetainedReplay(42L);
    state.expired(42L);
    assertFalse(state.isRetainedReplay(42L));
    assertFalse(state.hasMessage(42L));
  }

  @Test
  void duplicateRollbackIsIdempotentOnceMessageIsPending() {
    MessageStateManagerImpl state = state();
    Message message = message(51);
    state.register(message);
    state.allocate(message);

    assertTrue(state.rollback(51));
    assertFalse(state.rollback(51));
    assertEquals(1, state.pending());
  }

  private static MessageStateManagerImpl state() {
    return new MessageStateManagerImpl("engine-delivery", 1L,
        new BitSetFactoryImpl(Constants.BITSET_BLOCK_SIZE));
  }

  private static Message message(long id) {
    return new MessageBuilder().setId(id).build();
  }
}
