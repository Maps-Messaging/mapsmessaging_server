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

package io.mapsmessaging.engine.destination.subscription.impl.shared;

import io.mapsmessaging.dto.rest.session.SubscriptionStateDTO;
import io.mapsmessaging.engine.destination.subscription.state.BoundedMessageStateManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

class SharedSubscriptionManagerTest {

  @Test
  void newInstance_isEmpty_andExposesName_andStateManager() {
    SharedSubscriptionManager manager = new SharedSubscriptionManager("group-A");

    Assertions.assertEquals("group-A", manager.getName());
    Assertions.assertTrue(manager.isEmpty());

    BoundedMessageStateManager stateManager = manager.getMessageStateManager();
    Assertions.assertNotNull(stateManager);
    Assertions.assertSame(stateManager, manager.getMessageStateManager());

    Assertions.assertNull(manager.find("missing"));
  }

  @Test
  void add_thenFind_returnsSameInstance_andIsNotEmpty() {
    SharedSubscriptionManager manager = new SharedSubscriptionManager("group-A");
    SharedSubscription subscription = Mockito.mock(SharedSubscription.class);

    manager.add("sub-1", subscription);

    Assertions.assertFalse(manager.isEmpty());
    Assertions.assertSame(subscription, manager.find("sub-1"));
    Assertions.assertNull(manager.find("sub-2"));
  }

  @Test
  void delete_removesAndReturnsSubscription() {
    SharedSubscriptionManager manager = new SharedSubscriptionManager("group-A");
    SharedSubscription subscription = Mockito.mock(SharedSubscription.class);

    manager.add("sub-1", subscription);

    SharedSubscription removed = manager.delete("sub-1");

    Assertions.assertSame(subscription, removed);
    Assertions.assertTrue(manager.isEmpty());
    Assertions.assertNull(manager.find("sub-1"));
  }

  @Test
  void delete_missingKey_returnsNull_andDoesNotChangeState() {
    SharedSubscriptionManager manager = new SharedSubscriptionManager("group-A");

    SharedSubscription removed = manager.delete("nope");

    Assertions.assertNull(removed);
    Assertions.assertTrue(manager.isEmpty());
  }

  @Test
  void close_callsCloseOnAllActiveSubscriptions() {
    SharedSubscriptionManager manager = new SharedSubscriptionManager("group-A");
    SharedSubscription subscription1 = Mockito.mock(SharedSubscription.class);
    SharedSubscription subscription2 = Mockito.mock(SharedSubscription.class);

    manager.add("sub-1", subscription1);
    manager.add("sub-2", subscription2);

    manager.close();

    Mockito.verify(subscription1).close();
    Mockito.verify(subscription2).close();
    Mockito.verifyNoMoreInteractions(subscription1, subscription2);
  }

  @Test
  void complete_callsExpiredOnAllActiveSubscriptions() {
    SharedSubscriptionManager manager = new SharedSubscriptionManager("group-A");
    SharedSubscription subscription1 = Mockito.mock(SharedSubscription.class);
    SharedSubscription subscription2 = Mockito.mock(SharedSubscription.class);

    manager.add("sub-1", subscription1);
    manager.add("sub-2", subscription2);

    manager.complete(123L);

    Mockito.verify(subscription1).expired(123L);
    Mockito.verify(subscription2).expired(123L);
    Mockito.verifyNoMoreInteractions(subscription1, subscription2);
  }

  @Test
  void getSubscriptionStates_returnsAllStates_inInsertionOrder() {
    SharedSubscriptionManager manager = new SharedSubscriptionManager("group-A");
    SharedSubscription subscription1 = Mockito.mock(SharedSubscription.class);
    SharedSubscription subscription2 = Mockito.mock(SharedSubscription.class);

    SubscriptionStateDTO state1 = new SubscriptionStateDTO();
    SubscriptionStateDTO state2 = new SubscriptionStateDTO();

    Mockito.when(subscription1.getState()).thenReturn(state1);
    Mockito.when(subscription2.getState()).thenReturn(state2);

    manager.add("sub-1", subscription1);
    manager.add("sub-2", subscription2);

    List<SubscriptionStateDTO> states = manager.getSubscriptionStates();

    Assertions.assertEquals(2, states.size());
    Assertions.assertSame(state1, states.get(0));
    Assertions.assertSame(state2, states.get(1));
  }

  @Test
  void getSubscriptionStates_includesNullStates_currentBehavior() {
    SharedSubscriptionManager manager = new SharedSubscriptionManager("group-A");
    SharedSubscription subscription1 = Mockito.mock(SharedSubscription.class);
    SharedSubscription subscription2 = Mockito.mock(SharedSubscription.class);

    SubscriptionStateDTO state2 = new SubscriptionStateDTO();

    Mockito.when(subscription1.getState()).thenReturn(null);
    Mockito.when(subscription2.getState()).thenReturn(state2);

    manager.add("sub-1", subscription1);
    manager.add("sub-2", subscription2);

    List<SubscriptionStateDTO> states = manager.getSubscriptionStates();

    Assertions.assertEquals(2, states.size());
    Assertions.assertNull(states.get(0));
    Assertions.assertSame(state2, states.get(1));
  }
}
