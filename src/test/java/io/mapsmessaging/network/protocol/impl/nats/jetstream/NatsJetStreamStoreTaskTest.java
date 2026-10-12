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

package io.mapsmessaging.network.protocol.impl.nats.jetstream;

import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.NatsJetStreamStoreTask;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NatsJetStreamStoreTaskTest {

  @Test
  void returnsIdentifierFromPostOverrideMessageNotOriginal() throws Exception {
    DestinationImpl destination = mock(DestinationImpl.class);
    Message original = mock(Message.class);
    Message effective = mock(Message.class);
    when(destination.getResourceType()).thenReturn(DestinationType.TOPIC);
    when(destination.applyMessageOverrides(original)).thenReturn(effective);
    when(destination.handleTask(any())).thenReturn(1);
    when(original.getIdentifier()).thenReturn(0L);
    when(effective.getIdentifier()).thenReturn(71L);

    assertEquals(71L, NatsJetStreamStoreTask.store(destination, original));
    verify(destination).applyMessageOverrides(original);
    verify(destination).handleTask(any());
  }

  @Test
  void cannotAcknowledgeWhenNoArchiveHasRegisteredInterest() throws Exception {
    DestinationImpl destination = mock(DestinationImpl.class);
    Message original = mock(Message.class);
    Message effective = mock(Message.class);
    when(destination.getResourceType()).thenReturn(DestinationType.TOPIC);
    when(destination.applyMessageOverrides(original)).thenReturn(effective);
    when(destination.handleTask(any())).thenReturn(0);
    when(effective.getIdentifier()).thenReturn(80L);

    IOException failure = assertThrows(IOException.class,
        () -> NatsJetStreamStoreTask.store(destination, original));
    assertTrue(failure.getMessage().contains("archive subscription"));
  }

  @Test
  void refusesDelayedOrAlreadyBoundMessages() {
    DestinationImpl destination = mock(DestinationImpl.class);
    Message original = mock(Message.class);
    when(destination.getResourceType()).thenReturn(DestinationType.TOPIC);
    when(original.isBound()).thenReturn(true);
    assertThrows(IOException.class, () -> NatsJetStreamStoreTask.store(destination, original));
  }
}
