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

package io.mapsmessaging.network.protocol.impl.nats.jetstream;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.SubscriptionContextBuilder;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.RetainHandler;
import io.mapsmessaging.config.destination.DestinationConfig;
import io.mapsmessaging.dto.rest.config.destination.MemoryStorageConfigDTO;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.DestinationStateManagerFactory;
import io.mapsmessaging.engine.destination.NatsJetStreamStoreTask;
import io.mapsmessaging.engine.destination.subscription.impl.ClientSubscribedEventManager;
import io.mapsmessaging.engine.destination.subscription.impl.DestinationSubscription;
import io.mapsmessaging.engine.destination.subscription.transaction.FixedCreditManager;
import io.mapsmessaging.engine.destination.subscription.transaction.IndividualAcknowledgementController;
import io.mapsmessaging.engine.session.SessionImpl;
import io.mapsmessaging.engine.tasks.LongResponse;
import io.mapsmessaging.engine.tasks.MessageResponse;
import io.mapsmessaging.engine.tasks.SubscriptionResponse;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.JetStreamSubscriptionInitializer;
import io.mapsmessaging.test.BaseTestConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/** Acceptance tests for normal seeded subscriptions, independent of the browser characterisations. */
class JetStreamArchiveSeededSubscriptionTest extends BaseTestConfig {

  @TempDir
  Path storageRoot;

  @Test
  void concurrent_real_storage_and_seeding_deliver_each_event_once_and_keep_archive_interest() throws Exception {
    DestinationImpl destination = topic();
    var publisher = Executors.newSingleThreadExecutor();
    CountDownLatch start = new CountDownLatch(1);
    try {
      DestinationSubscription archive = subscribe(destination, "archive", 1);
      Set<Long> expected = new HashSet<>();
      for (int number = 1; number <= 50; number++) {
        expected.add(publish(destination, number));
      }
      DestinationSubscription consumer = subscribe(destination, "consumer", 2);
      DestinationSubscription independent = subscribe(destination, "independent", 3);
      var publications = publisher.submit(() -> {
        start.await();
        Set<Long> ids = new HashSet<>();
        for (int number = 51; number <= 200; number++) {
          ids.add(publish(destination, number));
        }
        return ids;
      });
      start.countDown();
      JetStreamSubscriptionInitializer.seedFromArchive(destination, "archive", "consumer");
      JetStreamSubscriptionInitializer.seedFromArchive(destination, "archive", "independent");
      expected.addAll(publications.get(20, TimeUnit.SECONDS));
      assertEquals(200, expected.size());
      assertEquals(200, archive.getPending());
      assertEquals(200, consumer.getPending());
      assertEquals(200, independent.getPending());
      ClientSubscribedEventManager manager = new ClientSubscribedEventManager(destination, consumer);
      Set<Long> delivered = new HashSet<>();
      for (int number = 0; number < 200; number++) {
        MessageResponse next = assertInstanceOf(MessageResponse.class,
            manager.getNext().get(5, TimeUnit.SECONDS));
        assertNotNull(next.getResponse());
        long id = next.getResponse().getMessage().getIdentifier();
        assertTrue(delivered.add(id), "Duplicate delivery for MAPS ID " + id);
        if (number == 0) {
          manager.rollbackReceived(id);
          MessageResponse retry = assertInstanceOf(MessageResponse.class,
              manager.getNext().get(5, TimeUnit.SECONDS));
          assertNotNull(retry.getResponse());
          assertEquals(id, retry.getResponse().getMessage().getIdentifier());
        }
        manager.ackReceived(id);
      }
      destination.submit(() -> new LongResponse(consumer.getDepth())).get(5, TimeUnit.SECONDS);
      assertEquals(expected, delivered);
      assertEquals(0, consumer.getDepth());
      assertEquals(200, archive.getPending());
      assertEquals(200, independent.getPending());
      assertEquals(200L, destination.getStoredMessages());
      MessageResponse empty = assertInstanceOf(MessageResponse.class,
          manager.getNext().get(5, TimeUnit.SECONDS));
      assertNull(empty.getResponse());
    } finally {
      publisher.shutdownNow();
      publisher.awaitTermination(5, TimeUnit.SECONDS);
      destination.delete();
    }
  }

  private DestinationImpl topic() throws Exception {
    DestinationConfig configuration = new DestinationConfig();
    configuration.setDirectory(storageRoot.toString());
    configuration.setNamespace("/");
    configuration.setType("Memory");
    MemoryStorageConfigDTO storage = new MemoryStorageConfigDTO();
    storage.setCapacity(-1);
    storage.setExpiredEventPoll(-1);
    configuration.setStorageConfig(storage);
    return new DestinationImpl("nats-seeded-" + UUID.randomUUID(), configuration,
        UUID.randomUUID(), DestinationType.TOPIC);
  }

  private static DestinationSubscription subscribe(DestinationImpl destination, String name, long stateId)
      throws Exception {
    SubscriptionResponse result = (SubscriptionResponse) destination.submit(() -> {
      var context = new SubscriptionContextBuilder(destination.getFullyQualifiedNamespace(),
          ClientAcknowledgement.INDIVIDUAL).setSync(true).setReceiveMaximum(200)
          .setRetainHandler(RetainHandler.DO_NOT_SEND).build();
      var subscription = new DestinationSubscription(destination, context, mock(SessionImpl.class), name,
          new IndividualAcknowledgementController(new FixedCreditManager(200)),
          DestinationStateManagerFactory.create(destination, false, name, stateId, 0));
      return new SubscriptionResponse(subscription);
    }).get(5, TimeUnit.SECONDS);
    return (DestinationSubscription) result.getSubscription();
  }

  private static long publish(DestinationImpl destination, int number) throws Exception {
    return NatsJetStreamStoreTask.store(destination, new MessageBuilder()
        .setMeta(new java.util.HashMap<>(Map.of("nats.subject", "orders.created")))
        .setOpaqueData(Integer.toString(number).getBytes(StandardCharsets.UTF_8)).storeOffline(true).build());
  }
}
