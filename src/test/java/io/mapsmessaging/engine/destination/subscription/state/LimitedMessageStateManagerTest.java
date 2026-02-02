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
import io.mapsmessaging.utilities.collections.bitset.BitSetFactoryImpl;
import io.mapsmessaging.utilities.queue.EventReaperQueue;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Queue;

class LimitedMessageStateManagerTest {

  @Test
  void register_underLimit_doesNotEnqueueReap() {
    EventReaperQueue eventReaperQueue = new EventReaperQueue();
    BitSetFactoryImpl bitSetFactory = new BitSetFactoryImpl(Constants.BITSET_BLOCK_SIZE);

    LimitedMessageStateManager manager =
        new LimitedMessageStateManager("test", 1L, bitSetFactory, 3, eventReaperQueue);

    manager.register(10L);
    manager.register(20L);
    manager.register(30L);

    Queue<Long> reaped = eventReaperQueue.getAndClear();
    Assertions.assertTrue(reaped.isEmpty());

    Assertions.assertTrue(manager.hasMessage(10L));
    Assertions.assertTrue(manager.hasMessage(20L));
    Assertions.assertTrue(manager.hasMessage(30L));
  }

  @Test
  void register_overLimit_enqueuesLastForReaping() {
    EventReaperQueue eventReaperQueue = new EventReaperQueue();
    BitSetFactoryImpl bitSetFactory = new BitSetFactoryImpl(Constants.BITSET_BLOCK_SIZE);

    LimitedMessageStateManager manager =
        new LimitedMessageStateManager("test", 1L, bitSetFactory, 2, eventReaperQueue);

    manager.register(10L);
    manager.register(20L);

    Queue<Long> reapedBefore = eventReaperQueue.getAndClear();
    Assertions.assertTrue(reapedBefore.isEmpty());

    manager.register(30L);

    Queue<Long> reapedAfter = eventReaperQueue.getAndClear();
    Assertions.assertFalse(reapedAfter.isEmpty());
    Assertions.assertTrue(reapedAfter.contains(10L));
  }

  @Test
  void register_multipleOverLimit_addsNewLastValues() {
    EventReaperQueue eventReaperQueue = new EventReaperQueue();
    BitSetFactoryImpl bitSetFactory = new BitSetFactoryImpl(Constants.BITSET_BLOCK_SIZE);

    LimitedMessageStateManager manager =
        new LimitedMessageStateManager("test", 1L, bitSetFactory, 2, eventReaperQueue);

    manager.register(1L);
    manager.register(2L);
    manager.register(3L);
    manager.register(4L);

    Queue<Long> reaped = eventReaperQueue.getAndClear();

    // We only assert that once we're over the limit, it queues "last" candidates for reaping.
    Assertions.assertTrue(reaped.contains(1L));
    Assertions.assertTrue(reaped.contains(2L));
  }

  @Test
  void register_messageUsesIdentifierAndPriority_andCanTriggerReaping() {
    EventReaperQueue eventReaperQueue = new EventReaperQueue();
    BitSetFactoryImpl bitSetFactory = new BitSetFactoryImpl(Constants.BITSET_BLOCK_SIZE);

    LimitedMessageStateManager manager =
        new LimitedMessageStateManager("test", 1L, bitSetFactory, 1, eventReaperQueue);

    Priority priority = Mockito.mock(Priority.class);


    Message message1 = Mockito.mock(Message.class, Mockito.RETURNS_DEEP_STUBS);
    Mockito.when(message1.getIdentifier()).thenReturn(100L);
    Mockito.when(message1.getPriority()).thenReturn(priority);
    Mockito.when(priority.getValue()).thenReturn(Priority.HIGHEST.getValue());

    Message message2 = Mockito.mock(Message.class);

    Mockito.when(message2.getIdentifier()).thenReturn(200L);
    Mockito.when(message2.getPriority()).thenReturn(priority);
    Mockito.when(priority.getValue()).thenReturn(Priority.HIGHEST.getValue());


    manager.register(message1);

    Queue<Long> reapedBefore = eventReaperQueue.getAndClear();
    Assertions.assertTrue(reapedBefore.isEmpty());

    manager.register(message2);

    Queue<Long> reapedAfter = eventReaperQueue.getAndClear();
    Assertions.assertFalse(reapedAfter.isEmpty());
    // "last" should be the later id (given you're just adding ids and trimming by last())
    Assertions.assertTrue(reapedAfter.contains(100L));
  }
}
