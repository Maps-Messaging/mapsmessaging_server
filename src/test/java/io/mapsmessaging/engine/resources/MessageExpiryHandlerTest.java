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

package io.mapsmessaging.engine.resources;

import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.tasks.LongResponse;
import io.mapsmessaging.engine.tasks.Response;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MessageExpiryHandlerTest {

  @Test
  void callback_owns_a_snapshot_and_invalidates_subscriptions_after_physical_removal() throws Exception {
    DestinationImpl destination = mock(DestinationImpl.class);
    when(destination.expireSubscriptions(any())).thenReturn(CompletableFuture.completedFuture(new LongResponse(2)));
    Queue<Long> callbackQueue = new ArrayDeque<>(List.of(11L, 12L));
    new MessageExpiryHandler(destination).expired(callbackQueue);
    callbackQueue.clear();
    ArgumentCaptor<Callable<Response>> task = ArgumentCaptor.forClass(Callable.class);
    verify(destination).submit(task.capture(), eq(DestinationImpl.DELETE_PRIORITY));
    task.getValue().call();
    ArgumentCaptor<Queue<Long>> ids = ArgumentCaptor.forClass(Queue.class);
    var order = inOrder(destination);
    order.verify(destination).removeMessage(11L);
    order.verify(destination).removeMessage(12L);
    order.verify(destination).expireSubscriptions(ids.capture());
    assertEquals(List.of(11L, 12L), List.copyOf(ids.getValue()));
  }

  @Test
  void empty_expiry_callback_does_not_schedule_storage_work() throws Exception {
    DestinationImpl destination = mock(DestinationImpl.class);
    new MessageExpiryHandler(destination).expired(new ArrayDeque<>());
    verifyNoInteractions(destination);
  }
}
