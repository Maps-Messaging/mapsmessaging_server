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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RetainedReplayMessageStateTest {

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

  private MessageStateManagerImpl createManager() {
    BitSetFactory bitSetFactory = new BitSetFactoryImpl(Constants.BITSET_BLOCK_SIZE);
    return new MessageStateManagerImpl("retained-replay-test", 12345L, bitSetFactory);
  }

  private Message createMessage(long id) {
    Message message = Mockito.mock(Message.class, Mockito.RETURNS_DEEP_STUBS);
    Mockito.when(message.getIdentifier()).thenReturn(id);
    Mockito.when(message.getPriority().getValue()).thenReturn(4);
    return message;
  }
}
