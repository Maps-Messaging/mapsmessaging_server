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

import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.dto.rest.config.protocol.impl.AmqpConfigDTO;
import io.mapsmessaging.network.protocol.impl.amqp.AMQPProtocol;
import io.mapsmessaging.network.protocol.impl.amqp.proton.ProtonEngine;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.messaging.MessageAnnotations;
import org.apache.qpid.proton.engine.Event;
import org.apache.qpid.proton.engine.EventType;
import org.apache.qpid.proton.engine.Receiver;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BaseEventListenerTest {

  @Test
  void anonymous_relay_uses_jms_destination_type_annotation() {
    AMQPProtocol protocol = mock(AMQPProtocol.class);
    when(protocol.getAmqpConfig()).thenReturn(new AmqpConfigDTO());
    TestEventListener listener = new TestEventListener(protocol, mock(ProtonEngine.class));
    Receiver receiver = mock(Receiver.class);
    Map<Symbol, Object> annotations = new LinkedHashMap<>();
    annotations.put(Symbol.valueOf("x-opt-jms-dest"), (byte) 0);
    org.apache.qpid.proton.message.Message message = org.apache.qpid.proton.message.Message.Factory.create();
    message.setMessageAnnotations(new MessageAnnotations(annotations));

    assertEquals(DestinationType.QUEUE, listener.destination_type(receiver, message));
  }

  @Test
  void fixed_receiver_uses_remote_target_address_when_local_target_is_unavailable() {
    AMQPProtocol protocol = mock(AMQPProtocol.class);
    when(protocol.getAmqpConfig()).thenReturn(new AmqpConfigDTO());
    TestEventListener listener = new TestEventListener(protocol, mock(ProtonEngine.class));
    Receiver receiver = mock(Receiver.class);
    org.apache.qpid.proton.amqp.messaging.Target remoteTarget = new org.apache.qpid.proton.amqp.messaging.Target();
    remoteTarget.setAddress("/dynamic/temporary/queue/test");
    org.apache.qpid.proton.message.Message message = org.apache.qpid.proton.message.Message.Factory.create();
    when(receiver.getRemoteTarget()).thenReturn(remoteTarget);

    assertEquals("/dynamic/temporary/queue/test", listener.destination_name(receiver, message));
  }

  @Test
  void fixed_receiver_uses_retained_target_address_when_proton_termini_are_unavailable() {
    AMQPProtocol protocol = mock(AMQPProtocol.class);
    when(protocol.getAmqpConfig()).thenReturn(new AmqpConfigDTO());
    TestEventListener listener = new TestEventListener(protocol, mock(ProtonEngine.class));
    Receiver receiver = mock(Receiver.class);
    org.apache.qpid.proton.message.Message message = org.apache.qpid.proton.message.Message.Factory.create();
    when(receiver.getContext()).thenReturn(new BaseEventListener.ReceiverTargetContext("/dynamic/temporary/queue/test"));

    assertEquals("/dynamic/temporary/queue/test", listener.destination_name(receiver, message));
  }

  private static final class TestEventListener extends BaseEventListener {

    private TestEventListener(AMQPProtocol protocol, ProtonEngine engine) {
      super(protocol, engine);
    }

    private DestinationType destination_type(Receiver receiver, org.apache.qpid.proton.message.Message message) {
      return getDestinationType(receiver, message);
    }

    private String destination_name(Receiver receiver, org.apache.qpid.proton.message.Message message) {
      return getDestinationName(receiver, message);
    }

    @Override
    public boolean handleEvent(Event event) {
      return false;
    }

    @Override
    public EventType getType() {
      return Event.Type.NON_CORE_EVENT;
    }
  }
}
