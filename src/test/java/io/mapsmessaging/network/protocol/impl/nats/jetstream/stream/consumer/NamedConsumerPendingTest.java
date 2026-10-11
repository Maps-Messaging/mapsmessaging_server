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

package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer;

import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.data.ConsumerConfig;
import io.mapsmessaging.network.protocol.impl.nats.streams.StreamSubscriptionInfo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class NamedConsumerPendingTest {
  @Test
  void aggregatesPendingDeliveryCountsAcrossStreamSubscriptions() {
    SubscribedEventManager firstManager = mock(SubscribedEventManager.class);
    SubscribedEventManager secondManager = mock(SubscribedEventManager.class);
    StreamSubscriptionInfo first = mock(StreamSubscriptionInfo.class);
    StreamSubscriptionInfo second = mock(StreamSubscriptionInfo.class);
    when(first.getSubscribedEventManager()).thenReturn(firstManager);
    when(second.getSubscribedEventManager()).thenReturn(secondManager);
    when(firstManager.getPending()).thenReturn(2);
    when(secondManager.getPending()).thenReturn(3);
    NamedConsumer consumer = new NamedConsumer("consumer", "stream", new ConsumerConfig(), List.of(first, second));

    assertEquals(5, consumer.getAckPendingCount());
  }

  @Test
  void emptyConsumerHasNoPendingAcknowledgements() {
    NamedConsumer consumer = new NamedConsumer("consumer", "stream", new ConsumerConfig(), List.of());

    assertEquals(0, consumer.getAckPendingCount());
  }

  @Test
  void timedOutEmptyPullReturnsNoMessage() throws Exception {
    SubscribedEventManager manager = mock(SubscribedEventManager.class);
    StreamSubscriptionInfo info = mock(StreamSubscriptionInfo.class);
    when(info.getSubscribedEventManager()).thenReturn(manager);
    java.util.concurrent.CompletableFuture<io.mapsmessaging.engine.tasks.Response> pending = new java.util.concurrent.CompletableFuture<>();
    when(manager.getNext()).thenReturn(pending);
    NamedConsumer consumer = new NamedConsumer("consumer", "stream", new ConsumerConfig(), List.of(info));

    org.junit.jupiter.api.Assertions.assertNull(consumer.getNextMessage());
  }


  @Test
  void noWaitPullReturnsImmediatelyWhenFutureIsUnresolved() throws Exception {
    SubscribedEventManager manager = mock(SubscribedEventManager.class);
    StreamSubscriptionInfo info = mock(StreamSubscriptionInfo.class);
    when(info.getSubscribedEventManager()).thenReturn(manager);
    java.util.concurrent.CompletableFuture<io.mapsmessaging.engine.tasks.Response> pending = new java.util.concurrent.CompletableFuture<>();
    when(manager.getNext()).thenReturn(pending);
    NamedConsumer consumer = new NamedConsumer("consumer", "stream", new ConsumerConfig(), List.of(info));

    org.junit.jupiter.api.Assertions.assertNull(consumer.getNextMessage(true));
  }

  @Test
  void emptyConsumerPollReturnsNull() throws Exception {
    NamedConsumer consumer = new NamedConsumer("consumer", "stream", new ConsumerConfig(), List.of());
    org.junit.jupiter.api.Assertions.assertNull(consumer.getNextMessage(true));
    org.junit.jupiter.api.Assertions.assertNull(consumer.getNextMessage());
  }


  @Test
  void repeatedNoWaitPullReusesPendingFuture() throws Exception {
    SubscribedEventManager manager = mock(SubscribedEventManager.class);
    StreamSubscriptionInfo info = mock(StreamSubscriptionInfo.class);
    when(info.getSubscribedEventManager()).thenReturn(manager);
    java.util.concurrent.CompletableFuture<io.mapsmessaging.engine.tasks.Response> pending = new java.util.concurrent.CompletableFuture<>();
    when(manager.getNext()).thenReturn(pending);
    NamedConsumer consumer = new NamedConsumer("consumer", "stream", new ConsumerConfig(), List.of(info));

    org.junit.jupiter.api.Assertions.assertNull(consumer.getNextMessage(true));
    org.junit.jupiter.api.Assertions.assertNull(consumer.getNextMessage(true));
    verify(manager, times(1)).getNext();
  }

}
