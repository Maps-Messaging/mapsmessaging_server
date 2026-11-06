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

package io.mapsmessaging.engine.destination;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.SubscriptionContextBuilder;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.RetainHandler;
import io.mapsmessaging.config.destination.DestinationConfig;
import io.mapsmessaging.dto.rest.config.destination.MemoryStorageConfigDTO;
import io.mapsmessaging.engine.destination.subscription.impl.ClientSubscribedEventManager;
import io.mapsmessaging.engine.destination.subscription.impl.DestinationSubscription;
import io.mapsmessaging.engine.destination.subscription.tasks.SubscriptionTransactionTask;
import io.mapsmessaging.engine.destination.subscription.transaction.FixedCreditManager;
import io.mapsmessaging.engine.destination.subscription.transaction.IndividualAcknowledgementController;
import io.mapsmessaging.engine.resources.MessageExpiryHandler;
import io.mapsmessaging.engine.session.SessionImpl;
import io.mapsmessaging.engine.tasks.LongResponse;
import io.mapsmessaging.engine.tasks.MessageResponse;
import io.mapsmessaging.engine.tasks.SubscriptionResponse;
import io.mapsmessaging.test.BaseTestConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class GlobalMessageExpiryInFlightTest extends BaseTestConfig {

  @TempDir
  Path storageRoot;

  @Test
  void expiry_preserves_protocol_tracking_until_a_late_ack_completes() throws Exception {
    DestinationImpl destination = topic(-1);
    try {
      DestinationSubscription consumer = subscribe(destination, "consumer", 1);
      DestinationSubscription observer = subscribe(destination, "observer", 2);
      long id = publish(destination, false);
      pull(destination, consumer, id);
      assertEquals(1, consumer.getAcknowledgementController().size());
      assertFalse(consumer.getAcknowledgementController().canSend());
      expire(destination, id);
      assertFalse(consumer.getMessageStateManager().hasMessage(id));
      assertEquals(0, observer.getPending());
      assertEquals(1, consumer.getAcknowledgementController().size(), "Keep tracking the outstanding wire exchange");
      assertFalse(consumer.getAcknowledgementController().canSend(), "Hold the send window until protocol completion");
      settle(destination, consumer, id, true);
      assertEquals(0, consumer.getAcknowledgementController().size());
      assertTrue(consumer.getAcknowledgementController().canSend());
      settle(destination, consumer, id, true); // Duplicate late ACK remains harmless.
      assertEquals(0, consumer.getAcknowledgementController().size());
      assertEquals(0L, destination.getStoredMessages());
    } finally {
      destination.delete();
    }
  }

  @Test
  void expiry_processes_rollback_normally_without_requeueing_the_removed_payload() throws Exception {
    DestinationImpl destination = topic(-1);
    try {
      DestinationSubscription consumer = subscribe(destination, "consumer", 1);
      DestinationSubscription observer = subscribe(destination, "observer", 2);
      long id = publish(destination, false);
      pull(destination, consumer, id);
      expire(destination, id);
      assertEquals(1, consumer.getAcknowledgementController().size());
      settle(destination, consumer, id, false);
      assertEquals(0, consumer.getAcknowledgementController().size());
      assertTrue(consumer.getAcknowledgementController().canSend());
      assertEquals(0, consumer.getPending());
      assertEquals(0, observer.getPending());
      consumer.getMessageStateManager().rollbackInFlightMessages();
      assertEquals(0, consumer.getPending(), "Disconnect rollback must not resurrect the event either");
      MessageResponse next = assertInstanceOf(MessageResponse.class,
          consumer.getNext().get(5, TimeUnit.SECONDS));
      assertNull(next.getResponse());
    } finally {
      destination.delete();
    }
  }

  @Test
  void capacity_eviction_notifies_all_subscriptions_and_preserves_inflight_ack_tracking() throws Exception {
    DestinationImpl destination = topic(2);
    try {
      DestinationSubscription consumer = subscribe(destination, "consumer", 1);
      DestinationSubscription observer = subscribe(destination, "observer", 2);
      DestinationSubscription archive = subscribe(destination, "archive", 3);
      long oldest = publish(destination, false);
      pull(destination, consumer, oldest);
      publish(destination, false);
      publish(destination, false);
      drain(destination);
      assertNull(destination.getMessage(oldest));
      assertEquals(2L, destination.getStoredMessages());
      assertEquals(2, consumer.getPending());
      assertEquals(2, observer.getPending());
      assertEquals(2, archive.getPending());
      assertFalse(consumer.getMessageStateManager().hasMessage(oldest));
      assertEquals(1, consumer.getAcknowledgementController().size());
      settle(destination, consumer, oldest, false);
      assertEquals(0, consumer.getAcknowledgementController().size());
      assertEquals(2, consumer.getPending());
      assertFalse(consumer.getMessageStateManager().hasMessage(oldest));
    } finally {
      destination.delete();
    }
  }

  @Test
  void ordinary_rollback_of_a_live_event_still_redelivers_it() throws Exception {
    DestinationImpl destination = topic(-1);
    try {
      DestinationSubscription consumer = subscribe(destination, "consumer", 1);
      long id = publish(destination, false);
      pull(destination, consumer, id);
      settle(destination, consumer, id, false);
      assertEquals(1, consumer.getPending());
      assertNotNull(destination.getMessage(id));
      pull(destination, consumer, id);
    } finally {
      destination.delete();
    }
  }

  @Test
  void forced_expiry_also_clears_retained_payload_and_all_delivery_references() throws Exception {
    DestinationImpl destination = topic(-1);
    try {
      DestinationSubscription first = subscribe(destination, "first", 1);
      DestinationSubscription second = subscribe(destination, "second", 2);
      long id = publish(destination, true);
      assertEquals(id, destination.getRetainedIdentifier());
      expire(destination, id);
      assertNull(destination.getRetainedIdentifier());
      assertNull(destination.getMessage(id));
      assertEquals(0, first.getPending());
      assertEquals(0, second.getPending());
    } finally {
      destination.delete();
    }
  }

  private DestinationImpl topic(int capacity) throws Exception {
    DestinationConfig configuration = new DestinationConfig();
    configuration.setDirectory(storageRoot.toString());
    configuration.setNamespace("/");
    configuration.setType("Memory");
    MemoryStorageConfigDTO storage = new MemoryStorageConfigDTO();
    storage.setCapacity(capacity);
    storage.setExpiredEventPoll(-1);
    configuration.setStorageConfig(storage);
    return new DestinationImpl("global-expiry-" + UUID.randomUUID(), configuration,
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

  private static long publish(DestinationImpl destination, boolean retain) throws Exception {
    var message = new MessageBuilder().setOpaqueData(new byte[]{1}).setRetain(retain).build();
    destination.storeMessage(message);
    return message.getIdentifier();
  }

  private static void pull(DestinationImpl destination, DestinationSubscription subscription, long expected)
      throws Exception {
    var manager = new ClientSubscribedEventManager(destination, subscription);
    MessageResponse response = assertInstanceOf(MessageResponse.class, manager.getNext().get(5, TimeUnit.SECONDS));
    assertNotNull(response.getResponse());
    assertEquals(expected, response.getResponse().getMessage().getIdentifier());
  }

  private static void settle(DestinationImpl destination, DestinationSubscription subscription, long id, boolean ack)
      throws Exception {
    // This is the same queued transaction used by the protocol-facing event manager.
    destination.submit(new SubscriptionTransactionTask(subscription, id, ack)).get(5, TimeUnit.SECONDS);
  }

  private static void expire(DestinationImpl destination, long id) throws Exception {
    new MessageExpiryHandler(destination).expired(new ArrayDeque<>(List.of(id)));
    drain(destination);
  }

  private static void drain(DestinationImpl destination) throws Exception {
    destination.submit(() -> new LongResponse(0), DestinationImpl.DELETE_PRIORITY).get(5, TimeUnit.SECONDS);
    destination.submit(() -> new LongResponse(0)).get(5, TimeUnit.SECONDS);
  }
}
