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
      when(consumer.isSync()).thenReturn(true);
      when(destination.getMessage(1L)).thenReturn(message(1L, Priority.LOWEST));
      when(destination.getMessage(2L)).thenReturn(message(2L, Priority.HIGHEST));
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

  @Test
  void concurrent_publication_and_seeding_leave_no_gaps_or_duplicate_deliveries() throws Exception {
    var queue = java.util.concurrent.Executors.newSingleThreadExecutor();
    var publisher = java.util.concurrent.Executors.newSingleThreadExecutor();
    var start = new java.util.concurrent.CountDownLatch(1);
    DestinationImpl destination = mock(DestinationImpl.class);
    MessageStateManagerImpl archiveState = manager(81);
    MessageStateManagerImpl consumerState = manager(82);
    MessageStateManagerImpl independentState = manager(83);
    try {
      DestinationSubscription archive = mock(DestinationSubscription.class);
      DestinationSubscription consumer = mock(DestinationSubscription.class);
      DestinationSubscription independent = mock(DestinationSubscription.class);
      when(archive.isSync()).thenReturn(true);
      when(consumer.isSync()).thenReturn(true);
      when(independent.isSync()).thenReturn(true);
      when(archive.getMessageStateManager()).thenReturn(archiveState);
      when(consumer.getMessageStateManager()).thenReturn(consumerState);
      when(independent.getMessageStateManager()).thenReturn(independentState);
      when(destination.getSubscription("archive")).thenReturn(archive);
      when(destination.getSubscription("consumer")).thenReturn(consumer);
      when(destination.getSubscription("independent")).thenReturn(independent);
      when(destination.getMessage(anyLong())).thenAnswer(invocation ->
          message(invocation.getArgument(0), Priority.NORMAL));
      doAnswer(invocation -> queue.submit((Callable<Response>) invocation.getArgument(0)))
          .when(destination).submit(any(Callable.class));
      for (long id = 1; id <= 100; id++) {
        archiveState.register(message(id, Priority.NORMAL));
      }
      var publication = publisher.submit(() -> {
        start.await();
        for (long id = 101; id <= 500; id++) {
          var event = message(id, Priority.NORMAL);
          destination.submit(() -> {
            archiveState.register(event);
            consumerState.register(event);
            independentState.register(event);
            return new io.mapsmessaging.engine.tasks.LongResponse(3);
          }).get(5, java.util.concurrent.TimeUnit.SECONDS);
        }
        return null;
      });
      start.countDown();
      JetStreamSubscriptionInitializer.seedFromArchive(destination, "archive", "consumer");
      JetStreamSubscriptionInitializer.seedFromArchive(destination, "archive", "independent");
      publication.get(10, java.util.concurrent.TimeUnit.SECONDS);
      assertEquals(500, archiveState.pending());
      assertEquals(500, consumerState.pending());
      assertEquals(500, independentState.pending());
      for (long id = 1; id <= 500; id++) {
        assertEquals(id, consumerState.nextMessageId());
        consumerState.allocate(message(id, Priority.NORMAL));
        assertTrue(consumerState.rollback(id));
        consumerState.allocate(message(id, Priority.NORMAL));
        consumerState.commit(id);
      }
      assertEquals(-1L, consumerState.nextMessageId());
      assertEquals(500, archiveState.pending());
      assertEquals(500, independentState.pending());
    } finally {
      publisher.shutdownNow();
      queue.shutdownNow();
      publisher.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);
      queue.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);
      archiveState.close();
      consumerState.close();
      independentState.close();
    }
  }

  @Test
  void missing_physical_messages_are_not_copied_from_stale_archive_references() throws Exception {
    DestinationImpl destination = destinationWithInlineSubscriptionQueue();
    MessageStateManagerImpl archiveState = manager(91);
    MessageStateManagerImpl consumerState = manager(92);
    try {
      archiveState.register(message(1, Priority.NORMAL));
      archiveState.register(message(2, Priority.HIGHEST));
      DestinationSubscription archive = mock(DestinationSubscription.class);
      DestinationSubscription consumer = mock(DestinationSubscription.class);
      when(archive.isSync()).thenReturn(true);
      when(consumer.isSync()).thenReturn(true);
      when(archive.getMessageStateManager()).thenReturn(archiveState);
      when(consumer.getMessageStateManager()).thenReturn(consumerState);
      when(destination.getSubscription("archive")).thenReturn(archive);
      when(destination.getSubscription("consumer")).thenReturn(consumer);
      when(destination.getMessage(2L)).thenReturn(message(2, Priority.HIGHEST));
      assertEquals(1, JetStreamSubscriptionInitializer.seedFromArchive(destination, "archive", "consumer"));
      assertFalse(consumerState.hasMessage(1));
      assertEquals(2L, consumerState.nextMessageId());
      assertEquals(2, archiveState.pending(), "The generic engine's archive expiry semantics remain unchanged");
    } finally {
      archiveState.close();
      consumerState.close();
    }
  }

  @Test
  void historical_subject_filter_excludes_other_events_without_changing_priorities() throws Exception {
    DestinationImpl destination = destinationWithInlineSubscriptionQueue();
    MessageStateManagerImpl archiveState = manager(101);
    MessageStateManagerImpl consumerState = manager(102);
    try {
      var included = new MessageBuilder().setId(1).setPriority(Priority.HIGHEST)
          .setMeta(new java.util.HashMap<>(java.util.Map.of("nats.subject", "orders.created"))).setOpaqueData(new byte[]{1}).build();
      var excluded = new MessageBuilder().setId(2).setPriority(Priority.NORMAL)
          .setMeta(new java.util.HashMap<>(java.util.Map.of("nats.subject", "orders.deleted"))).setOpaqueData(new byte[]{2}).build();
      archiveState.register(included);
      archiveState.register(excluded);
      DestinationSubscription archive = mock(DestinationSubscription.class);
      DestinationSubscription consumer = mock(DestinationSubscription.class);
      when(archive.isSync()).thenReturn(true);
      when(consumer.isSync()).thenReturn(true);
      when(archive.getMessageStateManager()).thenReturn(archiveState);
      when(consumer.getMessageStateManager()).thenReturn(consumerState);
      when(destination.getSubscription("archive")).thenReturn(archive);
      when(destination.getSubscription("consumer")).thenReturn(consumer);
      when(destination.getMessage(1L)).thenReturn(included);
      when(destination.getMessage(2L)).thenReturn(excluded);
      assertEquals(1, JetStreamSubscriptionInitializer.seedFromArchive(
          destination, "archive", "consumer", "orders.created"));
      assertEquals(1L, consumerState.nextMessageId());
      assertFalse(consumerState.hasMessage(2));
      assertEquals(2, archiveState.pending());
    } finally {
      archiveState.close();
      consumerState.close();
    }
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
