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

import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.dto.rest.config.protocol.impl.AmqpConfigDTO;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.network.protocol.impl.amqp.AMQPProtocol;
import io.mapsmessaging.network.protocol.impl.amqp.proton.ProtonEngine;
import io.mapsmessaging.network.protocol.impl.amqp.proton.SubscriptionManager;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.engine.Event;
import org.apache.qpid.proton.engine.Sender;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SenderLinkLocalOpenEventListenerTest {

  @Test
  void dynamic_queue_source_creates_temporary_destination_and_assigns_address() throws Exception {
    AMQPProtocol protocol = mock(AMQPProtocol.class);
    ProtonEngine engine = mock(ProtonEngine.class);
    SubscriptionManager subscriptions = mock(SubscriptionManager.class);
    Event event = mock(Event.class);
    Sender sender = mock(Sender.class);
    org.apache.qpid.proton.engine.Session protonSession = mock(org.apache.qpid.proton.engine.Session.class);
    Session session = mock(Session.class);
    Destination destination = mock(Destination.class);
    SubscribedEventManager manager = mock(SubscribedEventManager.class);
    Source source = new Source();
    source.setDynamic(true);
    source.setCapabilities(Symbol.valueOf("temporary-queue"));
    when(protocol.getAmqpConfig()).thenReturn(new AmqpConfigDTO());
    when(protocol.getLogger()).thenReturn(mock(Logger.class));
    when(engine.getSubscriptions()).thenReturn(subscriptions);
    when(event.getLink()).thenReturn(sender);
    when(event.getSession()).thenReturn(protonSession);
    when(protonSession.getContext()).thenReturn(session);
    when(sender.getRemoteSource()).thenReturn(source);
    when(sender.getCredit()).thenReturn(10);
    when(session.findDestination(anyString(), eq(DestinationType.TEMPORARY_QUEUE)))
        .thenReturn(CompletableFuture.completedFuture(destination));
    when(session.resume(destination)).thenReturn(manager);
    SenderLinkLocalOpenEventListener listener = new SenderLinkLocalOpenEventListener(protocol, engine);

    assertTrue(listener.handleEvent(event));

    ArgumentCaptor<String> destinationName = ArgumentCaptor.forClass(String.class);
    verify(session, atLeastOnce()).findDestination(destinationName.capture(), eq(DestinationType.TEMPORARY_QUEUE));
    assertEquals(source.getAddress(), destinationName.getValue());
    assertTrue(source.getAddress().startsWith("/dynamic/temporary/queue/"));
    verify(sender).setSource(source);
    verify(subscriptions).put(manager, sender);
  }
}
