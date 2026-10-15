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

package io.mapsmessaging.network.protocol.impl.nats.state;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SubscriptionContextBuilder;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.impl.nats.NatsProtocol;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

class SessionStateSubscriptionOwnershipTest {

  @Test
  void removingOriginalAliasRetainsManagerUntilLastAliasIsRemoved() throws Exception {
    NatsProtocol protocol = mock(NatsProtocol.class);
    EndPoint endPoint = mock(EndPoint.class);
    Session session = mock(Session.class);
    SubscribedEventManager manager = mock(SubscribedEventManager.class);
    when(protocol.getEndPoint()).thenReturn(endPoint);
    when(protocol.getMaxReceiveSize()).thenReturn(1024);
    when(session.addSubscription(any(SubscriptionContext.class))).thenReturn(manager);

    SessionState state = new SessionState(protocol);
    state.setSession(session);
    SubscriptionContext first = context("subject", "sid-1");
    SubscriptionContext second = context("subject", "sid-2");
    SubscriptionContext third = context("subject", "sid-3");

    assertSame(manager, state.createSubscription(first));
    assertSame(manager, state.createSubscription(second));
    state.removeSubscription("sid-1");
    verify(session, never()).removeSubscription("sid-1");

    assertSame(manager, state.createSubscription(third));
    assertEquals(2, state.getSubscriptions().get("subject").size());
    state.removeSubscription("sid-2");
    verify(session, never()).removeSubscription("sid-1");
    state.removeSubscription("sid-3");

    verify(session, times(1)).removeSubscription("sid-1");
    verify(session, times(1)).addSubscription(first);
  }

  private static SubscriptionContext context(String destination, String alias) {
    return new SubscriptionContextBuilder(destination, ClientAcknowledgement.AUTO)
        .setAlias(alias).build();
  }
  @Test
  void duplicateNamedConsumerRegistrationCannotReplaceExistingConsumer() {
    NatsProtocol protocol = mock(NatsProtocol.class);
    when(protocol.getEndPoint()).thenReturn(mock(EndPoint.class));
    SessionState state = new SessionState(protocol);
    io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.NamedConsumer original =
        mock(io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.NamedConsumer.class);
    io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.NamedConsumer replacement =
        mock(io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.NamedConsumer.class);
    when(original.getName()).thenReturn("shared");
    when(replacement.getName()).thenReturn("shared");

    org.junit.jupiter.api.Assertions.assertTrue(state.addNamedConsumer(original));
    org.junit.jupiter.api.Assertions.assertFalse(state.addNamedConsumer(replacement));
    assertSame(original, state.getNamedConsumers().get("shared"));
  }
  @Test
  void closeWithoutEstablishedMapsSessionReleasesNamedConsumers() throws Exception {
    NatsProtocol protocol = mock(NatsProtocol.class);
    when(protocol.getEndPoint()).thenReturn(mock(EndPoint.class));
    SessionState state = new SessionState(protocol);
    io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.NamedConsumer consumer =
        mock(io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.NamedConsumer.class);
    when(consumer.getName()).thenReturn("consumer");
    state.addNamedConsumer(consumer);

    state.close();

    verify(consumer).close();
    org.junit.jupiter.api.Assertions.assertTrue(state.getNamedConsumers().isEmpty());
    org.junit.jupiter.api.Assertions.assertFalse(state.send(new io.mapsmessaging.network.protocol.impl.nats.frames.MsgFrame(32)));
  }
}
