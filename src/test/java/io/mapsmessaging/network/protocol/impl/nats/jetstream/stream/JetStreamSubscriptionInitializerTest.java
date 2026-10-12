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

package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.features.Priority;
import io.mapsmessaging.engine.Constants;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.subscription.impl.DestinationSubscription;
import io.mapsmessaging.engine.destination.subscription.state.MessageStateManagerImpl;
import io.mapsmessaging.engine.tasks.Response;
import io.mapsmessaging.utilities.collections.bitset.BitSetFactoryImpl;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JetStreamSubscriptionInitializerTest {

  @Test
  void seedsIndependentConsumerWithArchivePriorityOrder() throws Exception {
    DestinationImpl destination = destinationWithInlineSubscriptionQueue();
    MessageStateManagerImpl source = manager(71);
    MessageStateManagerImpl target = manager(72);
    try {
      source.register(message(1L, Priority.LOWEST));
      source.register(message(2L, Priority.HIGHEST));
      DestinationSubscription archive = mock(DestinationSubscription.class);
      DestinationSubscription consumer = mock(DestinationSubscription.class);
      when(archive.isSync()).thenReturn(true);
      when(archive.getMessageStateManager()).thenReturn(source);
      when(consumer.getMessageStateManager()).thenReturn(target);
      when(destination.getSubscription("nats-archive")).thenReturn(archive);
      when(destination.getSubscription("nats-consumer")).thenReturn(consumer);

      assertEquals(2, JetStreamSubscriptionInitializer.seedFromArchive(
          destination, "nats-archive", "nats-consumer"));
      assertEquals(2L, target.nextMessageId());
      assertEquals(2, source.pending());
    } finally {
      source.close();
      target.close();
    }
  }

  @Test
  void rejectsMissingArchiveInsteadOfProducingEmptyHistory() {
    DestinationImpl destination = destinationWithInlineSubscriptionQueue();
    IOException failure = assertThrows(IOException.class,
        () -> JetStreamSubscriptionInitializer.seedFromArchive(
            destination, "missing-archive", "consumer"));
    assertTrue(failure.getMessage().contains("missing"));
  }

  @Test
  void requiresDifferentSessionsForArchiveAndConsumer() {
    DestinationImpl destination = mock(DestinationImpl.class);
    assertThrows(IOException.class,
        () -> JetStreamSubscriptionInitializer.seedFromArchive(destination, "same", "same"));
    verify(destination, never()).submit(any(Callable.class));
  }

  private static DestinationImpl destinationWithInlineSubscriptionQueue() {
    DestinationImpl destination = mock(DestinationImpl.class);
    when(destination.getFullyQualifiedNamespace()).thenReturn("ORDERS/orders.created");
    doAnswer(invocation -> {
      Callable<Response> task = invocation.getArgument(0);
      CompletableFuture<Response> future = new CompletableFuture<>();
      try {
        future.complete(task.call());
      } catch (Exception failure) {
        future.completeExceptionally(failure);
      }
      return future;
    }).when(destination).submit(any(Callable.class));
    return destination;
  }

  private static MessageStateManagerImpl manager(long id) {
    return new MessageStateManagerImpl("nats-init-" + id, id,
        new BitSetFactoryImpl(Constants.BITSET_BLOCK_SIZE));
  }

  private static io.mapsmessaging.api.message.Message message(long id, Priority priority) {
    return new MessageBuilder().setId(id).setPriority(priority).setOpaqueData(new byte[]{1}).build();
  }
}
