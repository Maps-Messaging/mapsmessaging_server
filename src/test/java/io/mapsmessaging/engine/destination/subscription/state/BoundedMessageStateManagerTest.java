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

package io.mapsmessaging.engine.destination.subscription.state;

import io.mapsmessaging.api.message.Message;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;

class BoundedMessageStateManagerTest {

  @Test
  void addAndRemove_manageMembership() {
    BoundedMessageStateManager bounded = new BoundedMessageStateManager();

    MessageStateManagerImpl manager1 = Mockito.mock(MessageStateManagerImpl.class);
    MessageStateManagerImpl manager2 = Mockito.mock(MessageStateManagerImpl.class);

    Assertions.assertTrue(bounded.add(manager1));
    Assertions.assertTrue(bounded.add(manager2));

    Assertions.assertTrue(bounded.remove(manager1));
    Assertions.assertFalse(bounded.remove(manager1));
  }

  @Test
  void hasMessage_returnsTrueWhenAnyUnderlyingHasIt() {
    BoundedMessageStateManager bounded = new BoundedMessageStateManager();

    MessageStateManagerImpl manager1 = Mockito.mock(MessageStateManagerImpl.class);
    MessageStateManagerImpl manager2 = Mockito.mock(MessageStateManagerImpl.class);

    bounded.add(manager1);
    bounded.add(manager2);

    Mockito.when(manager1.hasMessage(123L)).thenReturn(false);
    Mockito.when(manager2.hasMessage(123L)).thenReturn(true);

    Assertions.assertTrue(bounded.hasMessage(123L));
  }

  @Test
  void hasMessage_returnsFalseWhenNoneHaveIt() {
    BoundedMessageStateManager bounded = new BoundedMessageStateManager();

    MessageStateManagerImpl manager1 = Mockito.mock(MessageStateManagerImpl.class);
    MessageStateManagerImpl manager2 = Mockito.mock(MessageStateManagerImpl.class);

    bounded.add(manager1);
    bounded.add(manager2);

    Mockito.when(manager1.hasMessage(123L)).thenReturn(false);
    Mockito.when(manager2.hasMessage(123L)).thenReturn(false);

    Assertions.assertFalse(bounded.hasMessage(123L));
  }

  @Test
  void expired_delegatesToAll() {
    BoundedMessageStateManager bounded = new BoundedMessageStateManager();

    MessageStateManagerImpl manager1 = Mockito.mock(MessageStateManagerImpl.class);
    MessageStateManagerImpl manager2 = Mockito.mock(MessageStateManagerImpl.class);

    bounded.add(manager1);
    bounded.add(manager2);

    bounded.expired(999L);

    Mockito.verify(manager1).expired(999L);
    Mockito.verify(manager2).expired(999L);
  }

  @Test
  void allocate_delegatesToAll() {
    BoundedMessageStateManager bounded = new BoundedMessageStateManager();

    MessageStateManagerImpl manager1 = Mockito.mock(MessageStateManagerImpl.class);
    MessageStateManagerImpl manager2 = Mockito.mock(MessageStateManagerImpl.class);

    bounded.add(manager1);
    bounded.add(manager2);

    Message message = Mockito.mock(Message.class);

    bounded.allocate(message);

    Mockito.verify(manager1).allocate(message);
    Mockito.verify(manager2).allocate(message);
  }

  @Test
  void commit_delegatesToAll() {
    BoundedMessageStateManager bounded = new BoundedMessageStateManager();

    MessageStateManagerImpl manager1 = Mockito.mock(MessageStateManagerImpl.class);
    MessageStateManagerImpl manager2 = Mockito.mock(MessageStateManagerImpl.class);

    bounded.add(manager1);
    bounded.add(manager2);

    bounded.commit(77L);

    Mockito.verify(manager1).commit(77L);
    Mockito.verify(manager2).commit(77L);
  }

  @Test
  void rollback_returnsTrueWhenAnyRollbackSucceeds_andCallsAll() {
    BoundedMessageStateManager bounded = new BoundedMessageStateManager();

    MessageStateManagerImpl manager1 = Mockito.mock(MessageStateManagerImpl.class);
    MessageStateManagerImpl manager2 = Mockito.mock(MessageStateManagerImpl.class);

    bounded.add(manager1);
    bounded.add(manager2);

    Mockito.when(manager1.rollback(42L)).thenReturn(false);
    Mockito.when(manager2.rollback(42L)).thenReturn(true);

    Assertions.assertTrue(bounded.rollback(42L));

    Mockito.verify(manager1).rollback(42L);
    Mockito.verify(manager2).rollback(42L);
  }

  @Test
  void rollback_returnsFalseWhenNoRollbackSucceeds_andCallsAll() {
    BoundedMessageStateManager bounded = new BoundedMessageStateManager();

    MessageStateManagerImpl manager1 = Mockito.mock(MessageStateManagerImpl.class);
    MessageStateManagerImpl manager2 = Mockito.mock(MessageStateManagerImpl.class);

    bounded.add(manager1);
    bounded.add(manager2);

    Mockito.when(manager1.rollback(42L)).thenReturn(false);
    Mockito.when(manager2.rollback(42L)).thenReturn(false);

    Assertions.assertFalse(bounded.rollback(42L));

    Mockito.verify(manager1).rollback(42L);
    Mockito.verify(manager2).rollback(42L);
  }

  @Test
  void rollbackInFlightMessages_delegatesToAll() {
    BoundedMessageStateManager bounded = new BoundedMessageStateManager();

    MessageStateManagerImpl manager1 = Mockito.mock(MessageStateManagerImpl.class);
    MessageStateManagerImpl manager2 = Mockito.mock(MessageStateManagerImpl.class);

    bounded.add(manager1);
    bounded.add(manager2);

    bounded.rollbackInFlightMessages();

    Mockito.verify(manager1).rollbackInFlightMessages();
    Mockito.verify(manager2).rollbackInFlightMessages();
  }

  @Test
  void delete_delegatesToAll() throws IOException {
    BoundedMessageStateManager bounded = new BoundedMessageStateManager();

    MessageStateManagerImpl manager1 = Mockito.mock(MessageStateManagerImpl.class);
    MessageStateManagerImpl manager2 = Mockito.mock(MessageStateManagerImpl.class);

    bounded.add(manager1);
    bounded.add(manager2);

    bounded.delete();

    Mockito.verify(manager1).delete();
    Mockito.verify(manager2).delete();
  }

  @Test
  void size_sumsUnderlyingSizes() {
    BoundedMessageStateManager bounded = new BoundedMessageStateManager();

    MessageStateManagerImpl manager1 = Mockito.mock(MessageStateManagerImpl.class);
    MessageStateManagerImpl manager2 = Mockito.mock(MessageStateManagerImpl.class);

    bounded.add(manager1);
    bounded.add(manager2);

    Mockito.when(manager1.size()).thenReturn(3);
    Mockito.when(manager2.size()).thenReturn(5);

    Assertions.assertEquals(8, bounded.size());
  }

  @Test
  void pending_sumsUnderlyingPending() {
    BoundedMessageStateManager bounded = new BoundedMessageStateManager();

    MessageStateManagerImpl manager1 = Mockito.mock(MessageStateManagerImpl.class);
    MessageStateManagerImpl manager2 = Mockito.mock(MessageStateManagerImpl.class);

    bounded.add(manager1);
    bounded.add(manager2);

    Mockito.when(manager1.pending()).thenReturn(2);
    Mockito.when(manager2.pending()).thenReturn(7);

    Assertions.assertEquals(9, bounded.pending());
  }

  @Test
  void hasMessagesInFlight_returnsTrueIfAnyHasMessagesInFlight() {
    BoundedMessageStateManager bounded = new BoundedMessageStateManager();

    MessageStateManagerImpl manager1 = Mockito.mock(MessageStateManagerImpl.class);
    MessageStateManagerImpl manager2 = Mockito.mock(MessageStateManagerImpl.class);

    bounded.add(manager1);
    bounded.add(manager2);

    Mockito.when(manager1.hasMessagesInFlight()).thenReturn(false);
    Mockito.when(manager2.hasMessagesInFlight()).thenReturn(true);

    Assertions.assertTrue(bounded.hasMessagesInFlight());
  }

  @Test
  void hasAtRestMessages_returnsTrueIfAnyHasAtRestMessages() {
    BoundedMessageStateManager bounded = new BoundedMessageStateManager();

    MessageStateManagerImpl manager1 = Mockito.mock(MessageStateManagerImpl.class);
    MessageStateManagerImpl manager2 = Mockito.mock(MessageStateManagerImpl.class);

    bounded.add(manager1);
    bounded.add(manager2);

    Mockito.when(manager1.hasAtRestMessages()).thenReturn(true);
    Mockito.when(manager2.hasAtRestMessages()).thenReturn(false);

    Assertions.assertTrue(bounded.hasAtRestMessages());
  }

  @Test
  void close_delegatesToAll_andClearsMembership() throws IOException {
    BoundedMessageStateManager bounded = new BoundedMessageStateManager();

    MessageStateManagerImpl manager1 = Mockito.mock(MessageStateManagerImpl.class);
    MessageStateManagerImpl manager2 = Mockito.mock(MessageStateManagerImpl.class);

    bounded.add(manager1);
    bounded.add(manager2);

    Mockito.when(manager1.size()).thenReturn(1);
    Mockito.when(manager2.size()).thenReturn(2);
    Assertions.assertEquals(3, bounded.size());

    bounded.close();

    Mockito.verify(manager1).close();
    Mockito.verify(manager2).close();

    Assertions.assertEquals(0, bounded.size());
    Assertions.assertFalse(bounded.hasAtRestMessages());
    Assertions.assertFalse(bounded.hasMessagesInFlight());
  }
}
