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

import io.mapsmessaging.network.protocol.impl.amqp.AMQPProtocol;
import io.mapsmessaging.network.protocol.impl.amqp.proton.ProtonEngine;
import org.apache.qpid.proton.amqp.messaging.Target;
import org.apache.qpid.proton.engine.Event;
import org.apache.qpid.proton.engine.Receiver;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LinkRemoteOpenEventListenerTest {

  @Test
  void receiver_open_defers_terminus_preparation_to_local_open() {
    AMQPProtocol protocol = mock(AMQPProtocol.class);
    Event event = mock(Event.class);
    Receiver receiver = mock(Receiver.class);
    Target target = new Target();
    target.setDynamic(true);
    when(event.getLink()).thenReturn(receiver);
    when(receiver.getRemoteTarget()).thenReturn(target);
    LinkRemoteOpenEventListener listener = new LinkRemoteOpenEventListener(protocol, mock(ProtonEngine.class));

    assertTrue(listener.handleEvent(event));

    verify(receiver).open();
    verify(receiver, never()).setTarget(target);
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
    order.verify(receiver).open();
  }
}
