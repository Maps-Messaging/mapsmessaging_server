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
import io.mapsmessaging.config.destination.DestinationConfig;
import io.mapsmessaging.dto.rest.config.destination.MemoryStorageConfigDTO;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.DestinationStateManagerFactory;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.destination.subscription.impl.DestinationSubscription;
import io.mapsmessaging.engine.destination.subscription.transaction.FixedCreditManager;
import io.mapsmessaging.engine.destination.subscription.transaction.IndividualAcknowledgementController;
import io.mapsmessaging.engine.session.SessionImpl;
import io.mapsmessaging.engine.tasks.MessageResponse;
import io.mapsmessaging.test.BaseTestConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Tests whether the existing MAPS storage expiry mechanism can support
 * JetStream LimitsPolicy without changing the core destination lifecycle.
 *
 * These are engine feasibility tests, not JetStream implementation tests.
 * Any failing assertion must remain visible until the generic behaviour
 * has been reviewed. Do not add an engine workaround just to make it green.
 */
class JetStreamArchiveStorageFeasibilityTest extends BaseTestConfig {

  @TempDir
  Path storageRoot;

  private DestinationImpl destination;
  private final List<DestinationSubscription> subscriptions = new ArrayList<>();

  @AfterEach
  void releaseDestination() throws IOException {
    if (destination != null) {
      destination.delete();
    }
  }

  @Test
  void capacityEvictionMustReleaseAllSubscriberReferences() throws Exception {
    destination = createMemoryTopic(2, -1);
    DestinationSubscription archive = subscribeSynchronously("archive");
    DestinationSubscription observer = subscribeSynchronously("observer");

    long first = publish("created", 0);
    publish("paid", 0);
    publish("created", 0);

    assertEquals(2L, destination.getStoredMessages(),
        "Memory storage must enforce its configured two-message capacity");
    assertNull(destination.getMessage(first),
        "Capacity eviction must make the oldest record unavailable");

    // Physical eviction is insufficient if both subscriptions keep stale IDs.
    awaitCondition(() -> archive.getPending() == 2 && observer.getPending() == 2,
        Duration.ofSeconds(3),
        "Capacity eviction must purge expired IDs from every subscription");
  }

  @Test
  void ordinaryConsumerAckMustNotDeleteMessageHeldByArchive() throws Exception {
    destination = createMemoryTopic(-1, -1);
    DestinationSubscription archive = subscribeSynchronously("archive");
    DestinationSubscription consumer = subscribeSynchronously("consumer");

    long id = publish("paid", 0);
    assertEquals(1, archive.getPending());
    assertEquals(1, consumer.getPending());

    MessageResponse delivery = assertInstanceOf(MessageResponse.class,
        consumer.getNext().get(3, TimeUnit.SECONDS));
    assertNotNull(delivery.getResponse());
    assertEquals(id, delivery.getResponse().getMessage().getIdentifier());
    delivery.getResponse().getSubscription().ackReceived(id);

    awaitCondition(() -> consumer.getDepth() == 0, Duration.ofSeconds(3),
        "Ordinary consumer ACK should clear its own outstanding delivery");
    assertEquals(1, archive.getPending(),
        "Ordinary subscriber ACK must not remove the archive subscription's interest");

    // Run beyond the normal destination reaper interval to establish that
    // the remaining archive interest prevents physical deletion.
    Thread.sleep(5500);
    assertEquals(1L, destination.getStoredMessages(),
        "ACK by another consumer must not remove a LimitsPolicy archive message");
  }

  @Test
  void ttlExpiryMustReleaseAllSubscriberReferences() throws Exception {
    destination = createMemoryTopic(-1, 1);
    DestinationSubscription archive = subscribeSynchronously("archive");
    DestinationSubscription observer = subscribeSynchronously("observer");

    publish("created", 300);
    assertEquals(1, archive.getPending());
    assertEquals(1, observer.getPending());

    awaitCondition(() -> destination.getStoredMessages() == 0,
        Duration.ofSeconds(8), "Storage expiry polling must remove the expired record");

    awaitCondition(() -> archive.getPending() == 0 && observer.getPending() == 0,
        Duration.ofSeconds(3),
        "Storage expiry must clear the archive and observer references, even without getNext()");
  }

  private DestinationImpl createMemoryTopic(int capacity, int expiryPollSeconds) throws IOException {
    DestinationConfig configuration = new DestinationConfig();
    configuration.setDirectory(storageRoot.toString());
    configuration.setNamespace("/");
    configuration.setType("Memory");
    MemoryStorageConfigDTO storage = new MemoryStorageConfigDTO();
    storage.setCapacity(capacity);
    storage.setExpiredEventPoll(expiryPollSeconds);
    configuration.setStorageConfig(storage);
    String name = "nats-archive-feasibility-" + UUID.randomUUID();
    return new DestinationImpl(name, configuration, UUID.randomUUID(), DestinationType.TOPIC);
  }

  private DestinationSubscription subscribeSynchronously(String name) throws IOException {
    SubscriptionContext context = new SubscriptionContextBuilder(
        destination.getFullyQualifiedNamespace(), ClientAcknowledgement.INDIVIDUAL)
        .setSync(true)
        .setReceiveMaximum(100)
        .build();
    String subscriptionId = name + "-" + UUID.randomUUID();
    long stateId = subscriptions.size() + 1L;
    DestinationSubscription subscription = new DestinationSubscription(
        destination, context, mock(SessionImpl.class), subscriptionId,
        new IndividualAcknowledgementController(new FixedCreditManager(100)),
        DestinationStateManagerFactory.create(destination, false, subscriptionId, stateId, 0));
    subscriptions.add(subscription);
    return subscription;
  }

  private long publish(String eventType, long ttlMillis) throws IOException {
    MessageBuilder builder = new MessageBuilder()
        .setOpaqueData(eventType.getBytes(StandardCharsets.UTF_8));
    if (ttlMillis > 0) {
      builder.setMessageExpiryInterval(ttlMillis, TimeUnit.MILLISECONDS);
    }
    io.mapsmessaging.api.message.Message message = builder.build();
    destination.storeMessage(message);
    return message.getIdentifier();
  }

  private static void awaitCondition(CheckedCondition condition, Duration timeout, String failure)
      throws Exception {
    long deadline = System.nanoTime() + timeout.toNanos();
    do {
      if (condition.matches()) {
        return;
      }
      Thread.sleep(25);
    } while (System.nanoTime() < deadline);
    assertTrue(condition.matches(), failure);
  }

  @FunctionalInterface
  private interface CheckedCondition {
    boolean matches() throws Exception;
  }
}
