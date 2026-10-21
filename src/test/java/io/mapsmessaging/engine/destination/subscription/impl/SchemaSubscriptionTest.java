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

package io.mapsmessaging.engine.destination.subscription.impl;

import io.mapsmessaging.dto.rest.session.SubscriptionStateDTO;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.session.SessionImpl;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SchemaSubscriptionTest {

  @Test
  void constructorRegistersSubscriptionAndExposesFixedQueueSemantics() throws IOException {
    DestinationImpl destination = mock(DestinationImpl.class);
    when(destination.getFullyQualifiedNamespace()).thenReturn("/schema/topic");
    SessionImpl session = mock(SessionImpl.class);
    SubscriptionContext context = mock(SubscriptionContext.class);

    SchemaSubscription subscription =
        new SchemaSubscription(session, "session-a", destination, context);

    verify(destination).addSchemaSubscription(subscription);
    assertEquals("/schema/topic", subscription.getName());
    assertEquals("session-a", subscription.getSessionId());
    assertFalse(subscription.isEmpty());
    assertEquals(1, subscription.getDepth());
    assertEquals(0, subscription.getPending());
    assertEquals(0, subscription.size());
    assertEquals(1, subscription.register(123L));
    assertFalse(subscription.hasMessage(123L));
    assertFalse(subscription.expired(123L));
    assertTrue(subscription.getAll().isEmpty());
    assertTrue(subscription.getAllAtRest().isEmpty());
    assertNull(subscription.getNext());
    assertEquals("NoOp", subscription.getAcknowledgementType());

    subscription.delete();
    verify(destination).removeSchemaSubscription(subscription);
  }

  @Test
  void stateReflectsDestinationSessionAndHibernation() {
    DestinationImpl destination = mock(DestinationImpl.class);
    when(destination.getFullyQualifiedNamespace()).thenReturn("/schema/topic");
    SessionImpl session = mock(SessionImpl.class);
    SubscriptionContext context = mock(SubscriptionContext.class);

    SchemaSubscription subscription =
        new SchemaSubscription(session, "session-a", destination, context);

    SubscriptionStateDTO active = subscription.getState();
    assertEquals("/schema/topic", active.getDestinationName());
    assertEquals("session-a", active.getSessionId());
    assertFalse(active.isHibernating());
    assertEquals(0, active.getPending());
    assertEquals(0, active.getSize());
    assertFalse(active.isHasAtRestMessages());
    assertFalse(active.isHasMessagesInFlight());

    subscription.hibernate();
    assertTrue(subscription.getState().isHibernating());

    SessionImpl replacement = mock(SessionImpl.class);
    when(replacement.getName()).thenReturn("session-b");
    subscription.wakeUp(replacement);

    assertFalse(subscription.getState().isHibernating());
    assertEquals("session-b", subscription.getSessionId());
  }

  @Test
  void acknowledgementCreditPauseResumeCancelCloseAndRunAreNoOps() throws IOException {
    SchemaSubscription subscription = new SchemaSubscription(
        null,
        "session-a",
        mock(DestinationImpl.class),
        mock(SubscriptionContext.class)
    );

    assertDoesNotThrow(() -> subscription.ackReceived(1L));
    assertDoesNotThrow(() -> subscription.rollbackReceived(1L));
    assertDoesNotThrow(() -> subscription.updateCredit(10));
    assertDoesNotThrow(subscription::pause);
    assertDoesNotThrow(subscription::resume);
    assertDoesNotThrow(subscription::cancel);
    assertDoesNotThrow(subscription::close);
    assertDoesNotThrow(subscription::run);
  }
}
