/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.engine.destination.delayed;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.engine.Constants;
import io.mapsmessaging.utilities.collections.bitset.BitSetFactoryImpl;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.*;

class TransactionBucketFailureRegressionTest {

  @Test
  void abortClearsBucketCountAndIndexImmediately() {
    TransactionalMessageManager manager = manager();
    manager.register(77L, new MessageBuilder().setId(1001L).build());
    manager.register(77L, new MessageBuilder().setId(1002L).build());
    assertEquals(2L, manager.size());

    Queue<Long> aborted = manager.removeBucket(77L);

    assertEquals(2, aborted.size());
    assertEquals(0L, manager.size(), "Aborted messages must not remain counted as pending");
    assertTrue(manager.isEmpty());
    assertFalse(manager.getTransactions().contains(77L));
  }

  @Test
  void abortOfOneBucketPreservesAnotherTransaction() {
    TransactionalMessageManager manager = manager();
    manager.register(11L, new MessageBuilder().setId(1L).build());
    manager.register(22L, new MessageBuilder().setId(2L).build());
    manager.removeBucket(11L);

    assertEquals(List.of(22L), manager.getTransactions());
    assertEquals(1L, manager.size());
    assertEquals(Long.valueOf(2L), manager.getNext(22L));
  }

  @Test
  void repeatedAbortOfSameBucketIsIdempotent() {
    TransactionalMessageManager manager = manager();
    manager.register(10L, new MessageBuilder().setId(4L).build());
    assertEquals(1, manager.removeBucket(10L).size());
    assertTrue(manager.removeBucket(10L).isEmpty());
    assertEquals(0L, manager.size());
    assertTrue(manager.getTransactions().isEmpty());
  }

  @Test
  void deletingUnknownBucketLeavesCountsUnchanged() {
    TransactionalMessageManager manager = manager();
    manager.register(10L, new MessageBuilder().setId(7L).build());

    assertFalse(manager.delete(99L));
    assertEquals(1L, manager.size());
    assertEquals(List.of(10L), manager.getTransactions());
  }

  private static TransactionalMessageManager manager() {
    return new TransactionalMessageManager(new BitSetFactoryImpl(Constants.BITSET_BLOCK_SIZE));
  }
}
