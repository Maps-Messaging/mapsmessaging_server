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

package io.mapsmessaging.network.protocol.impl.amqp.proton.listeners;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.network.protocol.impl.amqp.AMQPProtocol;
import io.mapsmessaging.network.protocol.impl.amqp.proton.ProtonEngine;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.messaging.Target;
import org.apache.qpid.proton.engine.Event;
import org.apache.qpid.proton.engine.Receiver;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LinkRemoteOpenEventListenerTest {

  @Test
  void dynamic_receiver_target_has_address_before_link_opens_without_waiting_for_creation() {
    AMQPProtocol protocol = mock(AMQPProtocol.class);
    Event event = mock(Event.class);
    Receiver receiver = mock(Receiver.class);
    org.apache.qpid.proton.engine.Session protonSession = mock(org.apache.qpid.proton.engine.Session.class);
    Session session = mock(Session.class);
    Target target = new Target();
    target.setDynamic(true);
    target.setCapabilities(Symbol.valueOf("temporary-queue"));
    when(event.getLink()).thenReturn(receiver);
    when(event.getSession()).thenReturn(protonSession);
    when(protonSession.getContext()).thenReturn(session);
    when(receiver.getRemoteTarget()).thenReturn(target);
    when(session.findDestination(anyString(), eq(DestinationType.TEMPORARY_QUEUE))).thenReturn(new CompletableFuture<>());
    doAnswer(invocation -> {
      assertTrue(target.getAddress().startsWith("/dynamic/temporary/queue/"));
      return null;
    }).when(receiver).open();
    LinkRemoteOpenEventListener listener = new LinkRemoteOpenEventListener(protocol, mock(ProtonEngine.class));

    assertTrue(listener.handleEvent(event));

    verify(receiver).setTarget(target);
    verify(receiver).setContext(new BaseEventListener.ReceiverTargetContext(target.getAddress()));
    verify(receiver).open();
  }

  @Test
  void fixed_receiver_target_is_set_before_link_opens() {
    AMQPProtocol protocol = mock(AMQPProtocol.class);
    Event event = mock(Event.class);
    Receiver receiver = mock(Receiver.class);
    Target target = new Target();
    target.setAddress("/dynamic/temporary/queue/test");
    when(event.getLink()).thenReturn(receiver);
    when(receiver.getRemoteTarget()).thenReturn(target);
    LinkRemoteOpenEventListener listener = new LinkRemoteOpenEventListener(protocol, mock(ProtonEngine.class));

    assertTrue(listener.handleEvent(event));

    InOrder order = inOrder(receiver);
    order.verify(receiver).setTarget(target);
    order.verify(receiver).setContext(new BaseEventListener.ReceiverTargetContext("/dynamic/temporary/queue/test"));
    order.verify(receiver).open();
  }
}
