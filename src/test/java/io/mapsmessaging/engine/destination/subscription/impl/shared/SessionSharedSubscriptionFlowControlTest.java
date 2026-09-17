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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.destination.subscription.transaction.AcknowledgementController;
import io.mapsmessaging.engine.session.ClientConnection;
import io.mapsmessaging.engine.session.SessionImpl;
import org.junit.jupiter.api.Test;

class SessionSharedSubscriptionFlowControlTest {

  @Test
  void fullConnectionWindowBlocksUntilCapacityCallback() {
    SharedSubscription sharedSubscription = mock(SharedSubscription.class);
    SessionImpl session = mock(SessionImpl.class);
    ClientConnection clientConnection = mock(ClientConnection.class);
    SubscriptionContext context = mock(SubscriptionContext.class);
    AcknowledgementController acknowledgementController = mock(AcknowledgementController.class);

    when(context.getQualityOfService()).thenReturn(QualityOfService.AT_LEAST_ONCE);
    when(session.getClientConnection()).thenReturn(clientConnection);
    when(acknowledgementController.canSend()).thenReturn(true);
    when(clientConnection.tryAcquireSendSlot(any())).thenReturn(false);

    SessionSharedSubscription subscription = new SessionSharedSubscription(
        sharedSubscription,
        session,
        "session",
        context,
        acknowledgementController);

    assertTrue(subscription.canAttemptSend());
    assertFalse(subscription.tryAcquireProtocolSendSlot());
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

    when(context.getQualityOfService()).thenReturn(QualityOfService.AT_LEAST_ONCE);
    when(session.getClientConnection()).thenReturn(clientConnection);
    when(acknowledgementController.canSend()).thenReturn(true);
    when(clientConnection.tryAcquireSendSlot(any())).thenReturn(true);

    SessionSharedSubscription subscription = new SessionSharedSubscription(
        sharedSubscription,
        session,
        "session",
        context,
        acknowledgementController);

    assertTrue(subscription.tryAcquireProtocolSendSlot());
    assertTrue(subscription.canAttemptSend());
  }
}
