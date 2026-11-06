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

package io.mapsmessaging.api;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.mapsmessaging.api.features.DestinationMode;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.session.ClientConnection;
import io.mapsmessaging.engine.session.MessageCallback;
import io.mapsmessaging.engine.session.SessionImpl;
import io.mapsmessaging.engine.session.security.SecurityContext;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SessionFlowControlTest {

  @Test
  void releasesUnusedReservationAfterProtocolReturns() {
    Fixture fixture = new Fixture();

    fixture.callback.sendMessage(fixture.destination, fixture.subscription, fixture.message, fixture.completionTask);

    verify(fixture.listener).sendMessage(any(MessageEvent.class));
    verify(fixture.clientConnection).releaseUnusedSendSlot(fixture.subscription);
  }

  @Test
  void releasesUnusedReservationWhenProtocolThrows() {
    Fixture fixture = new Fixture();
    doThrow(new IllegalStateException("send failed"))
        .when(fixture.listener)
        .sendMessage(any(MessageEvent.class));

    assertThrows(
        IllegalStateException.class,
        () -> fixture.callback.sendMessage(
            fixture.destination,
            fixture.subscription,
            fixture.message,
            fixture.completionTask));

    verify(fixture.clientConnection).releaseUnusedSendSlot(fixture.subscription);
  }

  private static final class Fixture {
    final SessionImpl sessionImpl = mock(SessionImpl.class);
    final MessageListener listener = mock(MessageListener.class);
    final ClientConnection clientConnection = mock(ClientConnection.class);
    final SecurityContext securityContext = mock(SecurityContext.class);
    final DestinationImpl destination = mock(DestinationImpl.class);
    final SubscribedEventManager subscription = mock(SubscribedEventManager.class);
    final SubscriptionContext context = mock(SubscriptionContext.class);
    final Message message = mock(Message.class);
    final Runnable completionTask = mock(Runnable.class);
    final MessageCallback callback;

    Fixture() {
      when(sessionImpl.getClientConnection()).thenReturn(clientConnection);
      when(sessionImpl.getSecurityContext()).thenReturn(securityContext);
      when(destination.getFullyQualifiedNamespace()).thenReturn("/test/topic");
      when(destination.getResourceType()).thenReturn(DestinationType.TOPIC);
      when(sessionImpl.absoluteToNormalised(any(Destination.class))).thenReturn("/test/topic");
      when(subscription.getContext()).thenReturn(context);
      when(context.getDestinationMode()).thenReturn(DestinationMode.NORMAL);

      ArgumentCaptor<MessageCallback> callbackCaptor = ArgumentCaptor.forClass(MessageCallback.class);
      new Session(sessionImpl, listener);
      verify(sessionImpl).setMessageCallback(callbackCaptor.capture());
      callback = callbackCaptor.getValue();
    }
  }
}
