/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.engine.destination.subscription.state;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.utilities.collections.bitset.ConcurrentSharedFileBitSetFactoryImpl;
import io.mapsmessaging.utilities.collections.bitset.SharedFileBitSetFactoryImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MessageStateRestartRecoveryTest {

  private static final long SESSION = 2026L;

  @Test
  void pendingMessagesSurviveClosingAndReopeningPersistentFactory(@TempDir Path folder) throws Exception {
    String file = folder.resolve("subscription.bit").toString();

    try (SharedFileBitSetFactoryImpl factory = factory(file)) {
      MessageStateManagerImpl state = new MessageStateManagerImpl("restart-test", SESSION, factory);
      state.register(message(1001));
      state.register(message(1002));
      assertEquals(2, state.pending());
    }

    try (SharedFileBitSetFactoryImpl factory = factory(file)) {
      MessageStateManagerImpl state = new MessageStateManagerImpl("restart-test", SESSION, factory);
      assertEquals(2, state.pending());
      assertTrue(state.hasMessage(1001));
      assertTrue(state.hasMessage(1002));
    }
  }

  @Test
  void committedMessagesDoNotReappearAfterRestart(@TempDir Path folder) throws Exception {
    String file = folder.resolve("subscription.bit").toString();

    try (SharedFileBitSetFactoryImpl factory = factory(file)) {
      MessageStateManagerImpl state = new MessageStateManagerImpl("restart-test", SESSION, factory);
      Message message = message(2001);
      state.register(message);
      state.allocate(message);
      state.commit(2001);
      assertFalse(state.hasMessage(2001));
    }

    try (SharedFileBitSetFactoryImpl factory = factory(file)) {
      MessageStateManagerImpl state = new MessageStateManagerImpl("restart-test", SESSION, factory);
      assertFalse(state.hasMessage(2001));
      assertEquals(0, state.pending());
    }
  }

  @Test
  void restoredStateKeepsRetainedReplayDistinctFromOrdinaryPending(@TempDir Path folder) throws Exception {
    String file = folder.resolve("subscription.bit").toString();

    try (SharedFileBitSetFactoryImpl factory = factory(file)) {
      MessageStateManagerImpl state = new MessageStateManagerImpl("restart-test", SESSION, factory);
      state.register(message(3001));
      state.registerRetainedReplay(3002);
    }

    try (SharedFileBitSetFactoryImpl factory = factory(file)) {
      MessageStateManagerImpl state = new MessageStateManagerImpl("restart-test", SESSION, factory);
      assertTrue(state.hasMessage(3001));
      assertTrue(state.hasMessage(3002));
      assertFalse(state.isRetainedReplay(3001));
      assertTrue(state.isRetainedReplay(3002));
    }
  }

  private static SharedFileBitSetFactoryImpl factory(String file) throws Exception {
    return new ConcurrentSharedFileBitSetFactoryImpl(file, 4, 128);
  }

  private static Message message(long id) {
    return new MessageBuilder().setId(id).build();
  }
}
