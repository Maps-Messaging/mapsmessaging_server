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

import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.api.Transaction;
import org.apache.qpid.proton.amqp.Binary;
import org.apache.qpid.proton.amqp.messaging.Accepted;
import org.apache.qpid.proton.amqp.messaging.Released;
import org.apache.qpid.proton.engine.Delivery;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AmqpTransactionCoordinatorTest {

  @Test
  void discharge_with_commit_applies_accepted_retirement() throws Exception {
    Session session = mock(Session.class);
    Transaction transaction = mock(Transaction.class);
    SubscribedEventManager manager = mock(SubscribedEventManager.class);
    Delivery delivery = mock(Delivery.class);
    when(session.startTransaction(anyString())).thenReturn(transaction);
    when(transaction.getExpiryTime()).thenReturn(Long.MAX_VALUE);
    AmqpTransactionCoordinator coordinator = new AmqpTransactionCoordinator();

    Binary transactionId = coordinator.declare(session);
    assertEquals(16, transactionId.getLength());
    assertSame(transaction, coordinator.find(transactionId));
    assertTrue(coordinator.enlist(transactionId, delivery, manager, 42, Accepted.getInstance()));
    assertTrue(coordinator.discharge(transactionId, false));

    verify(transaction).commit();
    verify(transaction).close();
    verify(manager).ackReceived(42);
    verify(delivery).settle();
  }

  @Test
  void discharge_with_rollback_releases_transactional_retirement() throws Exception {
    Session session = mock(Session.class);
    Transaction transaction = mock(Transaction.class);
    SubscribedEventManager manager = mock(SubscribedEventManager.class);
    Delivery delivery = mock(Delivery.class);
    when(session.startTransaction(anyString())).thenReturn(transaction);
    when(transaction.getExpiryTime()).thenReturn(Long.MAX_VALUE);
    AmqpTransactionCoordinator coordinator = new AmqpTransactionCoordinator();

    Binary transactionId = coordinator.declare(session);
    assertTrue(coordinator.enlist(transactionId, delivery, manager, 73, Released.getInstance()));
    assertTrue(coordinator.discharge(transactionId, true));

    verify(transaction).abort();
    verify(manager).rollbackReceived(73);
    verify(delivery).settle();
  }

  @Test
  void discharge_with_unknown_transaction_returns_false() throws Exception {
    AmqpTransactionCoordinator coordinator = new AmqpTransactionCoordinator();

    assertFalse(coordinator.discharge(new Binary(new byte[]{1, 2, 3}), false));
  }

  @Test
  void find_with_expired_transaction_rolls_back_pending_delivery() throws Exception {
    Session session = mock(Session.class);
    Transaction transaction = mock(Transaction.class);
    SubscribedEventManager manager = mock(SubscribedEventManager.class);
    Delivery delivery = mock(Delivery.class);
    when(session.startTransaction(anyString())).thenReturn(transaction);
    when(transaction.getExpiryTime()).thenReturn(1L);
    AmqpTransactionCoordinator coordinator = new AmqpTransactionCoordinator();

    Binary transactionId = coordinator.declare(session);
    assertTrue(coordinator.enlist(transactionId, delivery, manager, 17, Accepted.getInstance()));

    assertEquals(0, coordinator.expireTransactions(System.currentTimeMillis()));
    assertNull(coordinator.find(transactionId));
    verify(manager).rollbackReceived(17);
    verify(delivery).settle();
    verify(transaction).close();
  }
}
