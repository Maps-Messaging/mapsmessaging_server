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
import io.mapsmessaging.engine.destination.DestinationStats;
import io.mapsmessaging.engine.destination.subscription.DestinationSubscriptionManager;
import io.mapsmessaging.engine.resources.ResourceProperties;
import io.mapsmessaging.engine.tasks.LongResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JetStreamArchiveStoreTaskTest {

  @Test
  void archivedMessageIsStoredEvenWithoutConsumers() throws Exception {
    DestinationImpl destination = mock(DestinationImpl.class);
    DestinationSubscriptionManager subscriptions = mock(DestinationSubscriptionManager.class);
    ResourceProperties properties = new ResourceProperties();
    properties.setJetStreamArchive(true);
    when(destination.getResourceProperties()).thenReturn(properties);
    when(destination.getStats()).thenReturn(mock(DestinationStats.class));
    Message message = mock(Message.class);

    LongResponse result = assertInstanceOf(LongResponse.class,
        new NonDelayedStoreMessageTask(destination, subscriptions, message).taskCall());

    assertEquals(0, result.getResponse());
    verify(destination).addMessage(message);
    verify(subscriptions).hasSubscriptions();
  }

  @Test
  void ordinaryCoreNatsMessageWithoutInterestIsNotStored() throws Exception {
    DestinationImpl destination = mock(DestinationImpl.class);
    DestinationSubscriptionManager subscriptions = mock(DestinationSubscriptionManager.class);
    when(destination.getStats()).thenReturn(mock(DestinationStats.class));
    Message message = mock(Message.class);

    new NonDelayedStoreMessageTask(destination, subscriptions, message).taskCall();

    verify(destination, never()).addMessage(any());
  }
}
