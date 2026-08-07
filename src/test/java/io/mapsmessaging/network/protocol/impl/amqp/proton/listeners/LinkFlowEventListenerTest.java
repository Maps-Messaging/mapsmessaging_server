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

import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.dto.rest.config.protocol.impl.AmqpConfigDTO;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.network.protocol.impl.amqp.AMQPProtocol;
import io.mapsmessaging.network.protocol.impl.amqp.proton.ProtonEngine;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.engine.Event;
import org.apache.qpid.proton.engine.Sender;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LinkFlowEventListenerTest {

  @Test
  void drain_with_pending_messages_waits_for_last_delivery() {
    Sender sender = sender(false);
    SubscribedEventManager manager = (SubscribedEventManager) sender.getContext();
    LinkFlowEventListener listener = listener();
    Event event = mock(Event.class);
    when(event.getLink()).thenReturn(sender);

    listener.handleEvent(event);

    verify(manager).updateCredit(10);
    verify(sender, never()).drained();
  }

  @Test
  void drain_with_empty_subscription_completes_immediately() {
    Sender sender = sender(true);
    SubscribedEventManager manager = (SubscribedEventManager) sender.getContext();
    LinkFlowEventListener listener = listener();
    Event event = mock(Event.class);
    when(event.getLink()).thenReturn(sender);

    listener.handleEvent(event);

    verify(manager).updateCredit(10);
    verify(sender).drained();
  }

  @Test
  void flow_with_pending_browser_context_does_not_close_connection() {
    Sender sender = mock(Sender.class);
    when(sender.getSource()).thenReturn(new Source());
    when(sender.getContext()).thenReturn(new SubscriptionContext("orders"));
    when(sender.getDrain()).thenReturn(true);
    LinkFlowEventListener listener = listener();
    Event event = mock(Event.class);
    when(event.getLink()).thenReturn(sender);

    listener.handleEvent(event);

    verify(sender, never()).drained();
  }

  private LinkFlowEventListener listener() {
    AMQPProtocol protocol = mock(AMQPProtocol.class);
    when(protocol.getAmqpConfig()).thenReturn(new AmqpConfigDTO());
    return new LinkFlowEventListener(protocol, mock(ProtonEngine.class));
  }

  private Sender sender(boolean empty) {
    Sender sender = mock(Sender.class);
    SubscribedEventManager manager = mock(SubscribedEventManager.class);
    when(sender.getSource()).thenReturn(new Source());
    when(sender.getContext()).thenReturn(manager);
    when(sender.getCredit()).thenReturn(10);
    when(sender.getDrain()).thenReturn(true);
    when(manager.isEmpty()).thenReturn(empty);
    return sender;
  }
}
