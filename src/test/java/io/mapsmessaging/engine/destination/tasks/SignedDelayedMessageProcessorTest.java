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
package io.mapsmessaging.engine.destination.tasks;

import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.delayed.MessageManager;
import io.mapsmessaging.engine.destination.subscription.DestinationSubscriptionManager;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class SignedDelayedMessageProcessorTest {

  @Test
  void processesNegativeIdsBeforeTheNullEndOfBucket() throws Exception {
    DestinationImpl destination = mock(DestinationImpl.class);
    MessageManager manager = mock(MessageManager.class);
    DestinationSubscriptionManager subscriptions = mock(DestinationSubscriptionManager.class);
    Message first = mock(Message.class);
    Message second = mock(Message.class);
    when(manager.getNext(42L)).thenReturn(Long.MIN_VALUE, -1L, null);
    when(destination.getMessage(Long.MIN_VALUE)).thenReturn(first);
    when(destination.getMessage(-1L)).thenReturn(second);
    List<Message> processed = new ArrayList<>();

    MessageProcessor processor = new MessageProcessor(destination, subscriptions, manager, 42L) {
      @Override
      protected long processMessage(Message message) {
        processed.add(message);
        return 1;
      }
    };
    processor.taskCall();

    assertEquals(List.of(first, second), processed);
    verify(manager).remove(42L, Long.MIN_VALUE);
    verify(manager).remove(42L, -1L);
  }
}
