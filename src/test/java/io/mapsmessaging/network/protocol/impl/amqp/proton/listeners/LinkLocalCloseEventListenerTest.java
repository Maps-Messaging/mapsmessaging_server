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
import io.mapsmessaging.dto.rest.config.protocol.impl.AmqpConfigDTO;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.destination.subscription.impl.ClientSubscribedEventManager;
import io.mapsmessaging.network.protocol.impl.amqp.AMQPProtocol;
import io.mapsmessaging.network.protocol.impl.amqp.proton.ProtonEngine;
import io.mapsmessaging.network.protocol.impl.amqp.proton.SubscriptionManager;
import org.apache.qpid.proton.engine.Event;
import org.apache.qpid.proton.engine.Sender;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LinkLocalCloseEventListenerTest {

  @Test
  void browser_close_releases_only_transient_subscription() {
    AMQPProtocol protocol = mock(AMQPProtocol.class);
    ProtonEngine engine = mock(ProtonEngine.class);
    SubscriptionManager subscriptions = mock(SubscriptionManager.class);
    Event event = mock(Event.class);
    Sender sender = mock(Sender.class);
    org.apache.qpid.proton.engine.Session protonSession = mock(org.apache.qpid.proton.engine.Session.class);
    Session session = mock(Session.class);
    ClientSubscribedEventManager manager = mock(ClientSubscribedEventManager.class);
    SubscriptionContext context = new SubscriptionContext("orders");
    context.setBrowserFlag(true);
    when(protocol.getAmqpConfig()).thenReturn(new AmqpConfigDTO());
    when(engine.getSubscriptions()).thenReturn(subscriptions);
    when(event.getSession()).thenReturn(protonSession);
    when(protonSession.getContext()).thenReturn(session);
    when(event.getLink()).thenReturn(sender);
    when(sender.getContext()).thenReturn(manager);
    when(manager.getContext()).thenReturn(context);
    when(manager.getContexts()).thenReturn(List.of(context));
    LinkLocalCloseEventListener listener = new LinkLocalCloseEventListener(protocol, engine);

    assertTrue(listener.handleEvent(event));

    verify(manager).closeTransientSubscription();
    verify(session, never()).removeSubscription("orders");
    verify(subscriptions).remove(manager);
    verify(sender).setContext(null);
  }
}
