/*
 *
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

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.destination.subscription.state.MessageStateManager;
import io.mapsmessaging.engine.destination.subscription.transaction.AcknowledgementController;
import io.mapsmessaging.engine.session.ClientConnection;
import io.mapsmessaging.engine.session.SessionImpl;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class DestinationSubscriptionFlowControlTest {

  @Test
  void fullConnectionWindowStopsBeforeRequestingNextEvent() throws IOException {
    Fixture fixture = new Fixture();
    when(fixture.clientConnection.tryAcquireSendSlot(any())).thenReturn(false);

    Message message = fixture.subscription.retrieveWithFlowControl();

    assertNull(message);
    verify(fixture.messageStateManager, never()).nextMessageId();
    verify(fixture.destination, never()).getMessage(any(Long.class));
  }

  @Test
  void availableConnectionSlotAllowsOneEventToBeRetrieved() throws IOException {
    Fixture fixture = new Fixture();
    Message expected = mock(Message.class);
    when(fixture.clientConnection.tryAcquireSendSlot(any())).thenReturn(true);
    when(fixture.messageStateManager.nextMessageId()).thenReturn(42L);
    when(fixture.destination.getMessage(42L)).thenReturn(expected);

    Message message = fixture.subscription.retrieveWithFlowControl();

    assertSame(expected, message);
    verify(fixture.messageStateManager).allocate(expected);
  }

  @Test
  void unusedConnectionSlotIsReleasedWhenNoEventExists() throws IOException {
    Fixture fixture = new Fixture();
    when(fixture.clientConnection.tryAcquireSendSlot(any())).thenReturn(true);
    when(fixture.messageStateManager.nextMessageId()).thenReturn(-1L);

    Message message = fixture.subscription.retrieveWithFlowControl();

    assertNull(message);
    verify(fixture.clientConnection).releaseSendSlot(any());
  }

  private static final class Fixture {
    final DestinationImpl destination = mock(DestinationImpl.class);
    final SubscriptionContext context = mock(SubscriptionContext.class);
    final SessionImpl session = mock(SessionImpl.class);
    final ClientConnection clientConnection = mock(ClientConnection.class);
    final AcknowledgementController acknowledgementController = mock(AcknowledgementController.class);
    final MessageStateManager messageStateManager = mock(MessageStateManager.class);
    final TestDestinationSubscription subscription;

    Fixture() {
      when(context.isSync()).thenReturn(false);
      when(context.getQualityOfService()).thenReturn(QualityOfService.AT_LEAST_ONCE);
      when(session.getClientConnection()).thenReturn(clientConnection);
      subscription = new TestDestinationSubscription(
          destination,
          context,
          session,
          "session",
          acknowledgementController,
          messageStateManager);
    }
  }

  private static final class TestDestinationSubscription extends DestinationSubscription {

    TestDestinationSubscription(
        DestinationImpl destinationImpl,
        SubscriptionContext context,
        SessionImpl sessionImpl,
        String sessionId,
        AcknowledgementController acknowledgementController,
        MessageStateManager messageStateManager) {
      super(
          destinationImpl,
          context,
          sessionImpl,
          sessionId,
          acknowledgementController,
          messageStateManager,
          false);
    }

    Message retrieveWithFlowControl() throws IOException {
      return retrieveNextMessageWithFlowControl();
    }
  }
}
