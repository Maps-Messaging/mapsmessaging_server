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

package io.mapsmessaging.network.protocol.impl.amqp.proton;

import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.destination.subscription.impl.ClientSubscribedEventManager;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.amqp.messaging.TerminusDurability;
import org.apache.qpid.proton.engine.Sender;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubscriptionManagerTest {

  @Test
  void subscriptions_with_distinct_contexts_do_not_collide() {
    SubscribedEventManager firstManager = mock(SubscribedEventManager.class);
    SubscribedEventManager secondManager = mock(SubscribedEventManager.class);
    SubscriptionContext firstContext = new SubscriptionContext("orders");
    SubscriptionContext secondContext = new SubscriptionContext("orders");
    Sender firstSender = mock(Sender.class);
    Sender secondSender = mock(Sender.class);
    when(firstManager.getContext()).thenReturn(firstContext);
    when(secondManager.getContext()).thenReturn(secondContext);
    SubscriptionManager subscriptions = new SubscriptionManager();

    subscriptions.put(firstManager, firstSender);
    subscriptions.put(secondManager, secondSender);

    assertSame(firstSender, subscriptions.get(firstManager));
    assertSame(secondSender, subscriptions.get(secondManager));
  }

  @Test
  void delivery_manager_with_same_context_resolves_registered_sender() {
    SubscribedEventManager registrationManager = mock(SubscribedEventManager.class);
    SubscribedEventManager deliveryManager = mock(SubscribedEventManager.class);
    SubscriptionContext context = new SubscriptionContext("orders");
    Sender sender = mock(Sender.class);
    when(registrationManager.getContext()).thenReturn(context);
    when(deliveryManager.getContext()).thenReturn(context);
    SubscriptionManager subscriptions = new SubscriptionManager();

    subscriptions.put(registrationManager, sender);

    assertSame(sender, subscriptions.get(deliveryManager));
  }

  @Test
  void delivery_manager_replaces_pending_context_on_sender_link() {
    SubscribedEventManager deliveryManager = mock(SubscribedEventManager.class);
    SubscriptionContext context = new SubscriptionContext("orders");
    Sender sender = mock(Sender.class);
    when(deliveryManager.getContext()).thenReturn(context);
    when(sender.getContext()).thenReturn(context);
    SubscriptionManager subscriptions = new SubscriptionManager();
    subscriptions.put(context, sender);

    assertSame(sender, subscriptions.get(deliveryManager));

    verify(sender).setContext(deliveryManager);
  }

  @Test
  void close_with_durable_source_hibernates_subscription() {
    SubscribedEventManager manager = mock(SubscribedEventManager.class);
    Sender sender = mock(Sender.class);
    org.apache.qpid.proton.engine.Session protonSession = mock(org.apache.qpid.proton.engine.Session.class);
    Session session = mock(Session.class);
    SubscriptionContext context = new SubscriptionContext("orders");
    Source source = new Source();
    source.setDurable(TerminusDurability.UNSETTLED_STATE);
    when(manager.getContext()).thenReturn(context);
    when(sender.getSource()).thenReturn(source);
    when(sender.getSession()).thenReturn(protonSession);
    when(protonSession.getContext()).thenReturn(session);
    SubscriptionManager subscriptions = new SubscriptionManager();
    subscriptions.put(manager, sender);

    subscriptions.close();

    verify(session).hibernateSubscription("orders");
  }

  @Test
  void close_with_browser_releases_only_transient_subscription() {
    ClientSubscribedEventManager manager = mock(ClientSubscribedEventManager.class);
    Sender sender = mock(Sender.class);
    org.apache.qpid.proton.engine.Session protonSession = mock(org.apache.qpid.proton.engine.Session.class);
    Session session = mock(Session.class);
    SubscriptionContext context = new SubscriptionContext("orders");
    context.setBrowserFlag(true);
    when(manager.getContext()).thenReturn(context);
    when(sender.getSession()).thenReturn(protonSession);
    when(protonSession.getContext()).thenReturn(session);
    SubscriptionManager subscriptions = new SubscriptionManager();
    subscriptions.put(manager, sender);

    subscriptions.close();

    verify(manager).closeTransientSubscription();
    verify(session, never()).removeSubscription("orders");
  }
}
