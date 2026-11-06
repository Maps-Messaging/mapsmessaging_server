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

package io.mapsmessaging.rest.api.impl.messaging.impl;

import io.mapsmessaging.api.SubscribedEventManager;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RestMessageListenerAcknowledgementTest {

  private static final String DESTINATION = "/transactions";

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void batch_processes_every_id_in_order(boolean acknowledge) {
    RestMessageListener listener = new RestMessageListener();
    SubscribedEventManager manager = mock(SubscribedEventManager.class);
    listener.registerEventManager(DESTINATION, null, manager);

    assertTrue(process(listener, acknowledge, DESTINATION, List.of(101L, 102L, 103L)));

    InOrder order = inOrder(manager);
    if (acknowledge) {
      order.verify(manager).ackReceived(101L);
      order.verify(manager).ackReceived(102L);
      order.verify(manager).ackReceived(103L);
    } else {
      order.verify(manager).rollbackReceived(101L);
      order.verify(manager).rollbackReceived(102L);
      order.verify(manager).rollbackReceived(103L);
    }
    verifyNoMoreInteractions(manager);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void singleton_processes_one_id(boolean acknowledge) {
    RestMessageListener listener = new RestMessageListener();
    SubscribedEventManager manager = mock(SubscribedEventManager.class);
    listener.registerEventManager(DESTINATION, null, manager);

    assertTrue(process(listener, acknowledge, DESTINATION, List.of(101L)));
    if (acknowledge) {
      verify(manager).ackReceived(101L);
    } else {
      verify(manager).rollbackReceived(101L);
    }
    verifyNoMoreInteractions(manager);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void empty_batch_returns_false_without_processing(boolean acknowledge) {
    RestMessageListener listener = new RestMessageListener();
    SubscribedEventManager manager = mock(SubscribedEventManager.class);
    listener.registerEventManager(DESTINATION, null, manager);

    assertFalse(process(listener, acknowledge, DESTINATION, List.of()));
    verifyNoInteractions(manager);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void unknown_subscription_returns_false_without_processing(boolean acknowledge) {
    RestMessageListener listener = new RestMessageListener();
    SubscribedEventManager manager = mock(SubscribedEventManager.class);
    listener.registerEventManager(DESTINATION, null, manager);

    assertFalse(process(listener, acknowledge, "/unknown", List.of(101L, 102L)));
    verifyNoInteractions(manager);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void batch_only_uses_the_selected_subscription(boolean acknowledge) {
    RestMessageListener listener = new RestMessageListener();
    SubscribedEventManager manager = mock(SubscribedEventManager.class);
    SubscribedEventManager otherManager = mock(SubscribedEventManager.class);
    listener.registerEventManager(DESTINATION, null, manager);
    listener.registerEventManager("/other", null, otherManager);

    assertTrue(process(listener, acknowledge, DESTINATION, List.of(101L, 102L)));
    if (acknowledge) {
      verify(manager).ackReceived(101L);
      verify(manager).ackReceived(102L);
    } else {
      verify(manager).rollbackReceived(101L);
      verify(manager).rollbackReceived(102L);
    }
    verifyNoMoreInteractions(manager);
    verifyNoInteractions(otherManager);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void failure_on_later_id_propagates_and_stops_processing(boolean acknowledge) {
    RestMessageListener listener = new RestMessageListener();
    SubscribedEventManager manager = mock(SubscribedEventManager.class);
    listener.registerEventManager(DESTINATION, null, manager);
    IllegalStateException failure = new IllegalStateException("engine failure");
    if (acknowledge) {
      doThrow(failure).when(manager).ackReceived(102L);
    } else {
      doThrow(failure).when(manager).rollbackReceived(102L);
    }

    assertSame(failure, assertThrows(IllegalStateException.class,
        () -> process(listener, acknowledge, DESTINATION, List.of(101L, 102L, 103L))));
    InOrder order = inOrder(manager);
    if (acknowledge) {
      order.verify(manager).ackReceived(101L);
      order.verify(manager).ackReceived(102L);
    } else {
      order.verify(manager).rollbackReceived(101L);
      order.verify(manager).rollbackReceived(102L);
    }
    verifyNoMoreInteractions(manager);
  }

  private boolean process(RestMessageListener listener, boolean acknowledge, String destination, List<Long> ids) {
    return acknowledge ? listener.ackReceived(destination, ids) : listener.nakReceived(destination, ids);
  }
}
