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

package io.mapsmessaging.network.protocol.impl.amqp.proton.listeners;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.network.protocol.impl.amqp.AMQPProtocol;
import io.mapsmessaging.network.protocol.impl.amqp.proton.ProtonEngine;
import java.util.EnumSet;
import org.apache.qpid.proton.engine.Connection;
import org.apache.qpid.proton.engine.EndpointState;
import org.apache.qpid.proton.engine.Event;
import org.apache.qpid.proton.engine.Sender;
import org.junit.jupiter.api.Test;

class AmqpLinkLifecycleRegressionTest {

  // AMQP 1.0 Part 2 sections 2.6 and 2.7: attach retains the link until detach.
  @Test
  void opened_sender_remains_registered_for_remote_detach() {
    Connection connection = Connection.Factory.create();
    var session = connection.session();
    Sender sender = session.sender("durable");
    Event event = mock(Event.class);
    when(event.getLink()).thenReturn(sender);
    var listener = new LinkRemoteOpenEventListener(protocol(), mock(ProtonEngine.class));

    assertTrue(listener.handleEvent(event));
    assertSame(sender, connection.linkHead(EnumSet.of(EndpointState.ACTIVE), null));
  }

  // AMQP 1.0 Part 2 section 2.6.5: detach must not erase the parent session.
  @Test
  void detach_hibernates_only_the_link_and_preserves_parent_session() {
    Connection connection = Connection.Factory.create();
    var protonSession = connection.session();
    Sender sender = protonSession.sender("durable");
    var mapsSession = mock(io.mapsmessaging.api.Session.class);
    protonSession.setContext(mapsSession);
    var manager = mock(SubscribedEventManager.class);
    var context = new SubscriptionContext("topic");
    context.setAlias("durable");
    when(manager.getContext()).thenReturn(context);
    sender.setContext(manager);
    Event event = mock(Event.class);
    when(event.getLink()).thenReturn(sender);
    when(event.getSession()).thenReturn(protonSession);
    var listener = new LinkRemoteDetachEventListener(protocol(), mock(ProtonEngine.class));

    assertTrue(listener.handleEvent(event));
    assertSame(mapsSession, protonSession.getContext());
    assertTrue(sender.detached());
    org.junit.jupiter.api.Assertions.assertNull(sender.getContext());
    org.mockito.Mockito.verify(mapsSession).hibernateSubscription("durable");
  }
  private static AMQPProtocol protocol() {
    AMQPProtocol protocol = mock(AMQPProtocol.class);
    when(protocol.getLogger()).thenReturn(mock(io.mapsmessaging.logging.Logger.class));
    return protocol;
  }

}
