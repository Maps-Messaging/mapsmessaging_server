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

import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.Constants;
import io.mapsmessaging.utilities.collections.bitset.BitSetFactory;
import io.mapsmessaging.utilities.collections.bitset.BitSetFactoryImpl;
import io.mapsmessaging.utilities.collections.bitset.ConcurrentSharedFileBitSetFactoryImpl;
import io.mapsmessaging.utilities.collections.bitset.SharedFileBitSetFactoryImpl;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

class RetainedReplayMessageStateTest {

  private static final long UNIQUE_SESSION_ID = 12345L;

  @Test
  void retainedReplayMarkerSurvivesRollbackUntilCommit() {
    MessageStateManagerImpl manager = createManager();
    Message message = createMessage(100L);

    manager.registerRetainedReplay(message.getIdentifier());
    Assertions.assertTrue(manager.isRetainedReplay(message.getIdentifier()));

    manager.allocate(message);
    manager.rollback(message.getIdentifier());
    Assertions.assertTrue(manager.isRetainedReplay(message.getIdentifier()));

    manager.allocate(message);
    manager.commit(message.getIdentifier());
    Assertions.assertFalse(manager.isRetainedReplay(message.getIdentifier()));
  }

  @Test
  void expiredRetainedReplayClearsMarker() {
    MessageStateManagerImpl manager = createManager();
    Message message = createMessage(200L);

    manager.registerRetainedReplay(message.getIdentifier());
    Assertions.assertTrue(manager.isRetainedReplay(message.getIdentifier()));

    manager.expired(message.getIdentifier());
    Assertions.assertFalse(manager.isRetainedReplay(message.getIdentifier()));
  }

  @Test
  void normalLongRegistrationIsNotRetainedReplay() {
    MessageStateManagerImpl manager = createManager();
    long messageId = 250L;

    manager.register(messageId);

    Assertions.assertTrue(manager.hasMessage(messageId));
    Assertions.assertFalse(manager.isRetainedReplay(messageId));
  }

  @Test
  void retainedReplayMarkerSurvivesPersistentFactoryReload(@TempDir Path tempDir) throws Exception {
    String baseFilename = tempDir.resolve("subscription-state.bit").toString();
    long messageId = 300L;

    try (SharedFileBitSetFactoryImpl factory = new ConcurrentSharedFileBitSetFactoryImpl(baseFilename, 4, 128)) {
      MessageStateManagerImpl manager = new MessageStateManagerImpl("persistent-retained-replay", UNIQUE_SESSION_ID, factory);
      manager.registerRetainedReplay(messageId);
      Assertions.assertTrue(manager.hasMessage(messageId));
      Assertions.assertTrue(manager.isRetainedReplay(messageId));
    }

    try (SharedFileBitSetFactoryImpl factory = new ConcurrentSharedFileBitSetFactoryImpl(baseFilename, 4, 128)) {
      MessageStateManagerImpl manager = new MessageStateManagerImpl("persistent-retained-replay", UNIQUE_SESSION_ID, factory);
      Assertions.assertTrue(manager.hasMessage(messageId));
      Assertions.assertTrue(manager.isRetainedReplay(messageId));
    }
  }

  private MessageStateManagerImpl createManager() {
    BitSetFactory bitSetFactory = new BitSetFactoryImpl(Constants.BITSET_BLOCK_SIZE);
    return new MessageStateManagerImpl("retained-replay-test", UNIQUE_SESSION_ID, bitSetFactory);
  }

  private Message createMessage(long id) {
    Message message = Mockito.mock(Message.class, Mockito.RETURNS_DEEP_STUBS);
    Mockito.when(message.getIdentifier()).thenReturn(id);
    Mockito.when(message.getPriority().getValue()).thenReturn(4);
    return message;
  }
}
