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
import io.mapsmessaging.network.protocol.impl.amqp.AMQPProtocol;
import io.mapsmessaging.network.protocol.impl.amqp.proton.AmqpTransactionCoordinator;
import io.mapsmessaging.network.protocol.impl.amqp.proton.ProtonEngine;
import org.apache.qpid.proton.amqp.Binary;
import org.apache.qpid.proton.amqp.messaging.Accepted;
import org.apache.qpid.proton.amqp.messaging.Released;
import org.apache.qpid.proton.amqp.transaction.TransactionalState;
import org.apache.qpid.proton.engine.Delivery;
import org.apache.qpid.proton.engine.Event;
import org.apache.qpid.proton.engine.Sender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeliveryEventListenerTest {

  private ProtonEngine engine;
  private DeliveryEventListener listener;

  @BeforeEach
  void set_up() {
    AMQPProtocol protocol = mock(AMQPProtocol.class);
    AmqpConfigDTO config = new AmqpConfigDTO();
    when(protocol.getAmqpConfig()).thenReturn(config);
    engine = mock(ProtonEngine.class);
    listener = new DeliveryEventListener(protocol, engine);
  }

  @Test
  void accepted_sender_outcome_acknowledges_and_settles_delivery() {
    SenderOutcomeFixture fixture = sender_outcome_fixture(42);
    when(fixture.delivery.getRemoteState()).thenReturn(Accepted.getInstance());

    listener.handleEvent(fixture.event);

    verify(fixture.manager).ackReceived(42);
    verify(fixture.delivery).settle();
  }

  @Test
  void released_sender_outcome_rolls_back_and_settles_delivery() {
    SenderOutcomeFixture fixture = sender_outcome_fixture(73);
    when(fixture.delivery.getRemoteState()).thenReturn(Released.getInstance());

    listener.handleEvent(fixture.event);

    verify(fixture.manager).rollbackReceived(73);
    verify(fixture.delivery).settle();
  }

  @Test
  void transactional_sender_outcome_with_unknown_id_rolls_back_delivery() {
    SenderOutcomeFixture fixture = sender_outcome_fixture(91);
    Binary transactionId = new Binary(new byte[]{1, 2, 3});
    TransactionalState state = new TransactionalState();
    state.setTxnId(transactionId);
    state.setOutcome(Accepted.getInstance());
    AmqpTransactionCoordinator coordinator = mock(AmqpTransactionCoordinator.class);
    when(engine.getTransactionCoordinator()).thenReturn(coordinator);
    when(coordinator.enlist(transactionId, fixture.delivery, fixture.manager, 91, Accepted.getInstance())).thenReturn(false);
    when(fixture.delivery.getRemoteState()).thenReturn(state);

    listener.handleEvent(fixture.event);

    verify(fixture.manager).rollbackReceived(91);
    verify(fixture.delivery).settle();
  }

  private SenderOutcomeFixture sender_outcome_fixture(long messageId) {
    Event event = mock(Event.class);
    Delivery delivery = mock(Delivery.class);
    Sender sender = mock(Sender.class);
    SubscribedEventManager manager = mock(SubscribedEventManager.class);
    when(event.getDelivery()).thenReturn(delivery);
    when(delivery.getLink()).thenReturn(sender);
    when(delivery.isUpdated()).thenReturn(true);
    when(delivery.getContext()).thenReturn(manager);
    when(delivery.getTag()).thenReturn(pack_long(messageId));
    return new SenderOutcomeFixture(event, delivery, manager);
  }

  private byte[] pack_long(long value) {
    byte[] data = new byte[8];
    for (int index = 0; index < data.length; index++) {
      data[index] = (byte) ((value >> (8 * index)) & 0xff);
    }
    return data;
  }

  private record SenderOutcomeFixture(Event event, Delivery delivery, SubscribedEventManager manager) {
  }
}
