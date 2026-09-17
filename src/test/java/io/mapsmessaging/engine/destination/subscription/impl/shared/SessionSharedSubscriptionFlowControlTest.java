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

package io.mapsmessaging.engine.destination.subscription.impl.shared;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
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

class SessionSharedSubscriptionFlowControlTest {

  @Test
  void fullConnectionWindowBlocksUntilCapacityCallback() {
    SharedSubscription sharedSubscription = mock(SharedSubscription.class);
    SessionImpl session = mock(SessionImpl.class);
    ClientConnection clientConnection = mock(ClientConnection.class);
    SubscriptionContext context = mock(SubscriptionContext.class);
    AcknowledgementController acknowledgementController = mock(AcknowledgementController.class);
    Message message = mock(Message.class);

    when(context.getQualityOfService()).thenReturn(QualityOfService.AT_LEAST_ONCE);
    when(session.getClientConnection()).thenReturn(clientConnection);
    when(acknowledgementController.canSend()).thenReturn(true);
    when(clientConnection.tryAcquireSendSlot(any(), any())).thenReturn(false);

    SessionSharedSubscription subscription = new SessionSharedSubscription(
        sharedSubscription,
        session,
        "session",
        context,
        acknowledgementController);

    assertTrue(subscription.canAttemptSend());
    assertFalse(subscription.tryAcquireProtocolSendSlot(message));
    assertFalse(subscription.canAttemptSend());

    subscription.resumeDelivery();

    assertTrue(subscription.canAttemptSend());
    verify(sharedSubscription).schedule();
  }

  @Test
  void availableConnectionWindowReservesDeliverySlot() {
    SharedSubscription sharedSubscription = mock(SharedSubscription.class);
    SessionImpl session = mock(SessionImpl.class);
    ClientConnection clientConnection = mock(ClientConnection.class);
    SubscriptionContext context = mock(SubscriptionContext.class);
    AcknowledgementController acknowledgementController = mock(AcknowledgementController.class);
    Message message = mock(Message.class);

    when(context.getQualityOfService()).thenReturn(QualityOfService.AT_LEAST_ONCE);
    when(session.getClientConnection()).thenReturn(clientConnection);
    when(acknowledgementController.canSend()).thenReturn(true);
    when(clientConnection.tryAcquireSendSlot(any(), any())).thenReturn(true);

    SessionSharedSubscription subscription = new SessionSharedSubscription(
        sharedSubscription,
        session,
        "session",
        context,
        acknowledgementController);

    assertTrue(subscription.tryAcquireProtocolSendSlot(message));
    assertTrue(subscription.canAttemptSend());
    verify(clientConnection).tryAcquireSendSlot(subscription, message);
  }

  @Test
  void sharedMessageReadFailureDoesNotReserveConnectionSlot() throws IOException {
    DestinationImpl destination = mock(DestinationImpl.class);
    SubscriptionContext sharedContext = mock(SubscriptionContext.class);
    MessageStateManager messageStateManager = mock(MessageStateManager.class);
    AcknowledgementController sharedAcknowledgement = mock(AcknowledgementController.class);
    SessionImpl session = mock(SessionImpl.class);
    ClientConnection clientConnection = mock(ClientConnection.class);
    SubscriptionContext sessionContext = mock(SubscriptionContext.class);
    AcknowledgementController sessionAcknowledgement = mock(AcknowledgementController.class);

    when(sharedContext.isSync()).thenReturn(false);
    when(messageStateManager.nextMessageId()).thenReturn(42L);
    when(destination.getMessage(42L)).thenThrow(new IOException("store read failed"));
    when(session.getClientConnection()).thenReturn(clientConnection);
    when(sessionContext.getQualityOfService()).thenReturn(QualityOfService.AT_LEAST_ONCE);
    when(sessionAcknowledgement.canSend()).thenReturn(true);

    TestSharedSubscription sharedSubscription = new TestSharedSubscription(
        destination,
        sharedContext,
        messageStateManager,
        sharedAcknowledgement);
    sharedSubscription.addSession(
        session,
        "session",
        sessionContext,
        sessionAcknowledgement);

    assertThrows(IOException.class, sharedSubscription::retrieveWithFlowControl);

    verify(clientConnection, never()).tryAcquireSendSlot(any(), any());
    verify(messageStateManager, never()).allocate(any());
  }

  @Test
  void sharedAllocationFailureReleasesReservedConnectionSlot() throws IOException {
    DestinationImpl destination = mock(DestinationImpl.class);
    SubscriptionContext sharedContext = mock(SubscriptionContext.class);
    MessageStateManager messageStateManager = mock(MessageStateManager.class);
    AcknowledgementController sharedAcknowledgement = mock(AcknowledgementController.class);
    SessionImpl session = mock(SessionImpl.class);
    ClientConnection clientConnection = mock(ClientConnection.class);
    SubscriptionContext sessionContext = mock(SubscriptionContext.class);
    AcknowledgementController sessionAcknowledgement = mock(AcknowledgementController.class);
    Message message = mock(Message.class);

    when(sharedContext.isSync()).thenReturn(false);
    when(messageStateManager.nextMessageId()).thenReturn(42L);
    when(destination.getMessage(42L)).thenReturn(message);
    when(session.getClientConnection()).thenReturn(clientConnection);
    when(sessionContext.getQualityOfService()).thenReturn(QualityOfService.AT_LEAST_ONCE);
    when(sessionAcknowledgement.canSend()).thenReturn(true);
    when(clientConnection.tryAcquireSendSlot(any(), any())).thenReturn(true);
    doThrow(new IllegalStateException("allocate failed")).when(messageStateManager).allocate(message);

    TestSharedSubscription sharedSubscription = new TestSharedSubscription(
        destination,
        sharedContext,
        messageStateManager,
        sharedAcknowledgement);
    sharedSubscription.addSession(
        session,
        "session",
        sessionContext,
        sessionAcknowledgement);

    assertThrows(IllegalStateException.class, sharedSubscription::retrieveWithFlowControl);

    verify(clientConnection).tryAcquireSendSlot(any(), same(message));
    verify(clientConnection).releaseSendSlot(any());
  }

  private static final class TestSharedSubscription extends SharedSubscription {

    TestSharedSubscription(
        DestinationImpl destination,
        SubscriptionContext context,
        MessageStateManager messageStateManager,
        AcknowledgementController acknowledgementController) {
      super(destination, context, "shared", messageStateManager, acknowledgementController, "share");
    }

    Message retrieveWithFlowControl() throws IOException {
      return retrieveNextMessageWithFlowControl();
    }
  }
}
