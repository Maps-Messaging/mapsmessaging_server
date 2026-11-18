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

import static org.junit.jupiter.api.Assertions.*;

class BoundedSharedMessageStateRegressionTest {

  @Test
  void expiryRemovesMessageFromEverySelectorPartition() {
    MessageStateManagerImpl first = state("first", 1L);
    MessageStateManagerImpl second = state("second", 2L);
    BoundedMessageStateManager aggregate = new BoundedMessageStateManager();
    aggregate.add(first);
    aggregate.add(second);
    Message message = message(71L);
    first.register(message);
    second.register(message);

    aggregate.expired(71L);

    assertFalse(first.hasMessage(71L));
    assertFalse(second.hasMessage(71L));
    assertFalse(aggregate.hasMessage(71L));
  }

  @Test
  void rollbackRecoversOnlyPartitionsHoldingInflightDelivery() {
    MessageStateManagerImpl first = state("first", 3L);
    MessageStateManagerImpl second = state("second", 4L);
    BoundedMessageStateManager aggregate = new BoundedMessageStateManager();
    aggregate.add(first);
    aggregate.add(second);
    Message message = message(81L);
    first.register(message);
    first.allocate(message);

    assertTrue(aggregate.rollback(81L));
    assertTrue(first.hasAtRestMessages());
    assertFalse(second.hasMessage(81L));
    assertFalse(aggregate.rollback(81L));
  }

  @Test
  void completionInOnePartitionDoesNotEraseOtherIndependentGroup() {
    MessageStateManagerImpl first = state("first", 5L);
    MessageStateManagerImpl second = state("second", 6L);
    BoundedMessageStateManager aggregate = new BoundedMessageStateManager();
    aggregate.add(first);
    aggregate.add(second);
    Message message = message(91L);
    first.register(message);
    second.register(message);
    first.allocate(message);
    second.allocate(message);

    first.commit(91L);

    assertFalse(first.hasMessage(91L));
    assertTrue(second.hasMessage(91L),
        "An acknowledgement must not consume another independently registered delivery");
  }

  @Test
  void removingPartitionLeavesOtherPartitionAvailable() {
    MessageStateManagerImpl first = state("first", 7L);
    MessageStateManagerImpl second = state("second", 8L);
    BoundedMessageStateManager aggregate = new BoundedMessageStateManager();
    aggregate.add(first);
    aggregate.add(second);
    second.register(message(101L));

    assertTrue(aggregate.remove(first));
    assertTrue(aggregate.hasMessage(101L));
    assertEquals(1, aggregate.pending());
  }

  private static MessageStateManagerImpl state(String name, long sessionId) {
    return new MessageStateManagerImpl(name, sessionId,
        new BitSetFactoryImpl(Constants.BITSET_BLOCK_SIZE));
  }

  private static Message message(long identifier) {
    return new MessageBuilder().setId(identifier).build();
  }
}
