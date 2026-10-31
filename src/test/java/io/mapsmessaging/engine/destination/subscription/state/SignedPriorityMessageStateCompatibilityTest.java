/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.engine.destination.subscription.state;

import io.mapsmessaging.api.features.Priority;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.Constants;
import io.mapsmessaging.utilities.UniqueIdHelper;
import io.mapsmessaging.utilities.collections.ConcurrentNaturalOrderedLongQueue;
import io.mapsmessaging.utilities.collections.bitset.BitSetFactory;
import io.mapsmessaging.utilities.collections.bitset.BitSetFactoryImpl;
import io.mapsmessaging.utilities.collections.bitset.ConcurrentSharedFileBitSetFactoryImpl;
import io.mapsmessaging.utilities.collections.bitset.SharedFileBitSetFactoryImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.nio.file.Path;

class SignedPriorityMessageStateCompatibilityTest {

  private static final long UNIQUE_SESSION_ID = 12345L;
  private static final long EXISTING_REPLAY_NAMESPACE = 1L << 59;

  @Test
  void retainedReplayUsesOriginalPriorityBetweenEightAndTen() {
    BitSetFactory factory = new BitSetFactoryImpl(Constants.BITSET_BLOCK_SIZE);
    MessageStateManagerImpl manager = new MessageStateManagerImpl("retained-priority", UNIQUE_SESSION_ID, factory);

    manager.register(message(80L, Priority.TWO_BELOW_HIGHEST));
    manager.registerRetainedReplay(90L);
    manager.register(message(100L, Priority.HIGHEST));

    Assertions.assertEquals(Long.valueOf(100L), manager.messagesAtRest.poll());
    Assertions.assertEquals(Long.valueOf(90L), manager.messagesAtRest.poll(), "Retained replay must use priority 9");
    Assertions.assertEquals(Long.valueOf(80L), manager.messagesAtRest.poll());
    Assertions.assertNull(manager.messagesAtRest.poll());
    Assertions.assertTrue(manager.isRetainedReplay(90L));
  }

  @Test
  void signedMessageValuesRemainDistinctFromEmptyAtEveryPriority() {
    MessageStateManagerImpl manager = new MessageStateManagerImpl(
        "negative-message-values", UNIQUE_SESSION_ID,
        new BitSetFactoryImpl(Constants.BITSET_BLOCK_SIZE));
    try {
      Message minimum = message(Long.MIN_VALUE, Priority.NORMAL);
      Message minusOne = message(-1L, Priority.HIGHEST);
      manager.register(minimum);
      manager.register(minusOne);
      Assertions.assertEquals(Long.valueOf(-1L), manager.nextMessageId());
      manager.allocate(minusOne);
      Assertions.assertEquals(Long.valueOf(Long.MIN_VALUE), manager.nextMessageId());
      manager.allocate(minimum);
      Assertions.assertNull(manager.nextMessageId());
      manager.rollbackInFlightMessages();
      Assertions.assertEquals(Long.valueOf(-1L), manager.nextMessageId());
      manager.commit(-1L);
      manager.commit(Long.MIN_VALUE);
    } finally {
      manager.close();
    }
  }

  @Test
  void everyPriorityIncludingNegativeQueueIdentifiersSurvivesReload(@TempDir Path directory) throws Exception {
    String base = directory.resolve("signed-priority-state.bit").toString();

    try (SharedFileBitSetFactoryImpl factory = new ConcurrentSharedFileBitSetFactoryImpl(base, 4, 128)) {
      MessageStateManagerImpl manager = new MessageStateManagerImpl("signed-priority", UNIQUE_SESSION_ID, factory);
      for (Priority priority : Priority.values()) {
        long queueId = UniqueIdHelper.compute(UNIQUE_SESSION_ID, priority.getValue());
        Assertions.assertEquals(priority.getValue() >= 8, queueId < 0,
            "Unexpected signed queue identifier for priority " + priority);
        manager.register(message(1000L + priority.getValue(), priority));
      }
    }

    try (SharedFileBitSetFactoryImpl factory = new ConcurrentSharedFileBitSetFactoryImpl(base, 4, 128)) {
      MessageStateManagerImpl manager = new MessageStateManagerImpl("signed-priority", UNIQUE_SESSION_ID, factory);
      Assertions.assertEquals(Priority.values().length, manager.pending());
      for (int priority = Priority.HIGHEST.getValue(); priority >= Priority.LOWEST.getValue(); priority--) {
        Assertions.assertEquals(Long.valueOf(1000L + priority), manager.nextMessageId(),
            "Wrong persisted delivery order at priority " + priority);
        Assertions.assertEquals(Long.valueOf(1000L + priority), manager.messagesAtRest.poll());
      }
      Assertions.assertNull(manager.nextMessageId(), "Empty queue must be distinct from a stored negative ID");
    }
  }

  @Test
  void legacyPositiveReplayMarkerAndPrioritySevenRemainReadable(@TempDir Path directory) throws Exception {
    String base = directory.resolve("legacy-replay-state.bit").toString();
    long legacyReplayId = 451L;
    long legacyPriorityId = UniqueIdHelper.compute(UNIQUE_SESSION_ID, Priority.THREE_ABOVE_NORMAL.getValue());
    long legacyMarkerId = EXISTING_REPLAY_NAMESPACE | UNIQUE_SESSION_ID;

    try (SharedFileBitSetFactoryImpl factory = new ConcurrentSharedFileBitSetFactoryImpl(base, 4, 128)) {
      new ConcurrentNaturalOrderedLongQueue(legacyPriorityId, factory).add(legacyReplayId);
      new ConcurrentNaturalOrderedLongQueue(legacyMarkerId, factory).add(legacyReplayId);
    }

    try (SharedFileBitSetFactoryImpl factory = new ConcurrentSharedFileBitSetFactoryImpl(base, 4, 128)) {
      MessageStateManagerImpl manager = new MessageStateManagerImpl("legacy-replay", UNIQUE_SESSION_ID, factory);
      Assertions.assertTrue(manager.hasMessage(legacyReplayId));
      Assertions.assertTrue(manager.isRetainedReplay(legacyReplayId), "Existing replay marker must survive rollout");

      manager.registerRetainedReplay(452L);
      Assertions.assertTrue(UniqueIdHelper.compute(UNIQUE_SESSION_ID, Priority.ONE_BELOW_HIGHEST.getValue()) < 0);
      Assertions.assertEquals(Long.valueOf(452L), manager.nextMessageId(), "New priority-9 replay precedes old priority-7 replay");
      Assertions.assertEquals(Long.valueOf(452L), manager.messagesAtRest.poll());
      Assertions.assertEquals(Long.valueOf(legacyReplayId), manager.nextMessageId());
      Assertions.assertTrue(manager.isRetainedReplay(452L));
    }
  }

  private static Message message(long identifier, Priority priority) {
    Message message = Mockito.mock(Message.class);
    Mockito.when(message.getIdentifier()).thenReturn(identifier);
    Mockito.when(message.getPriority()).thenReturn(priority);
    return message;
  }
}
