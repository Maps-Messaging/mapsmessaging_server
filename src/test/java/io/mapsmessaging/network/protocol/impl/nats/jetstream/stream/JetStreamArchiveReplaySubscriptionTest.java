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
import io.mapsmessaging.api.SubscriptionContextBuilder;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.RetainHandler;
import io.mapsmessaging.config.destination.DestinationConfig;
import io.mapsmessaging.dto.rest.config.destination.MemoryStorageConfigDTO;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.DestinationStateManagerFactory;
import io.mapsmessaging.engine.destination.subscription.impl.DestinationSubscription;
import io.mapsmessaging.engine.destination.subscription.tasks.SubscriptionTransactionTask;
import io.mapsmessaging.engine.destination.subscription.transaction.FixedCreditManager;
import io.mapsmessaging.engine.destination.subscription.transaction.IndividualAcknowledgementController;
import io.mapsmessaging.engine.session.SessionImpl;
import io.mapsmessaging.engine.tasks.MessageResponse;
import io.mapsmessaging.engine.tasks.SubscriptionResponse;
import io.mapsmessaging.test.BaseTestConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/** Acceptance tests for NATS archive seeding through real MAPS subscription queues. */
class JetStreamArchiveReplaySubscriptionTest extends BaseTestConfig {

  @TempDir
  Path storageRoot;

  @Test
  void seeded_consumer_receives_history_and_later_publications_without_consuming_archive() throws Exception {
    DestinationImpl destination = topic();
    try {
      DestinationSubscription archive = subscribe(destination, "archive", 1);
      long historical = publish(destination);
      DestinationSubscription consumer = subscribe(destination, "consumer", 2);
      DestinationSubscription independent = subscribe(destination, "independent", 3);
      assertEquals(1, JetStreamSubscriptionInitializer.seedFromArchive(destination, "archive", "consumer"));
      assertEquals(1, JetStreamSubscriptionInitializer.seedFromArchive(destination, "archive", "independent"));
      long live = publish(destination);

      assertEquals(List.of(historical, live), consume(destination, consumer, 2),
          "A seeded normal subscription must deliver history followed by live publications");
      assertEquals(2, archive.getPending(), "Consumer ACK must leave archive interest intact");
      assertEquals(2, independent.getPending(), "Consumers must have independent delivery state");
      assertEquals(List.of(historical, live), consume(destination, independent, 2));
      assertEquals(2, archive.getPending());
      assertEquals(2L, destination.getStoredMessages());
    } finally {
      destination.delete();
    }
  }

  @Test
  void publication_during_seeding_has_no_gaps_or_duplicate_deliveries() throws Exception {
    DestinationImpl destination = topic();
    var publisher = Executors.newSingleThreadExecutor();
    var start = new CountDownLatch(1);
    try {
      DestinationSubscription archive = subscribe(destination, "archive", 1);
      List<Long> expected = new ArrayList<>();
      for (int event = 0; event < 20; event++) {
        expected.add(publish(destination));
      }
      DestinationSubscription consumer = subscribe(destination, "consumer", 2);
      var publication = publisher.submit(() -> {
        start.await();
        List<Long> published = new ArrayList<>();
        for (int event = 0; event < 100; event++) {
          published.add(publish(destination));
        }
        return published;
      });
      start.countDown();
      JetStreamSubscriptionInitializer.seedFromArchive(destination, "archive", "consumer");
      expected.addAll(publication.get(15, TimeUnit.SECONDS));
      // Also prove registration remains active after initialisation has completed.
      expected.add(publish(destination));
      assertEquals(expected.size(), consumer.getPending());
      assertEquals(expected, consume(destination, consumer, expected.size()));
      assertEquals(expected.size(), archive.getPending());
      assertEquals(expected.size(), destination.getStoredMessages());
    } finally {
      start.countDown();
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
    return new DestinationImpl("jetstream-replay-" + UUID.randomUUID(), configuration,
        UUID.randomUUID(), DestinationType.TOPIC);
  }

  private static DestinationSubscription subscribe(DestinationImpl destination, String name, long stateId)
      throws Exception {
    SubscriptionResponse response = (SubscriptionResponse) destination.submit(() -> {
      var context = new SubscriptionContextBuilder(destination.getFullyQualifiedNamespace(),
          ClientAcknowledgement.INDIVIDUAL).setSync(true).setReceiveMaximum(1)
          .setRetainHandler(RetainHandler.DO_NOT_SEND).build();
      return new SubscriptionResponse(new DestinationSubscription(destination, context, mock(SessionImpl.class), name,
          new IndividualAcknowledgementController(new FixedCreditManager(1)),
          DestinationStateManagerFactory.create(destination, false, name, stateId, 0)));
    }).get(5, TimeUnit.SECONDS);
    return (DestinationSubscription) response.getSubscription();
  }

  private static long publish(DestinationImpl destination) throws Exception {
    var message = new MessageBuilder().setOpaqueData(new byte[]{1}).build();
    destination.storeMessage(message);
    return message.getIdentifier();
  }

  private static List<Long> consume(DestinationImpl destination, DestinationSubscription consumer, int count)
      throws Exception {
    List<Long> delivered = new ArrayList<>();
    for (int event = 0; event < count; event++) {
      MessageResponse response = assertInstanceOf(MessageResponse.class,
          consumer.getNext().get(5, TimeUnit.SECONDS));
      assertNotNull(response.getResponse(), "Every historical and live event must be deliverable");
      long id = response.getResponse().getMessage().getIdentifier();
      delivered.add(id);
      destination.submit(new SubscriptionTransactionTask(consumer, id, true)).get(5, TimeUnit.SECONDS);
    }
    MessageResponse exhausted = assertInstanceOf(MessageResponse.class,
        consumer.getNext().get(5, TimeUnit.SECONDS));
    assertNull(exhausted.getResponse(), "Overlapping archive/live registration must not duplicate delivery");
    assertEquals(0, consumer.getPending());
    return delivered;
  }
}
