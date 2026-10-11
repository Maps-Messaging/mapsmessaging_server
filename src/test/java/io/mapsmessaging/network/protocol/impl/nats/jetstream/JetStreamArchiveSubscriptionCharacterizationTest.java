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

import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.api.SubscriptionContextBuilder;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.engine.tasks.MessageResponse;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Characterizes existing MAPS primitives for a proposed JetStream archive.
 * These tests do not claim that JetStream retention/seeding is implemented.
 */
class JetStreamArchiveSubscriptionCharacterizationTest extends JetStreamBaseTest {

  @Test
  void synchronousSubscriptionRetainsNatsMessagesWithoutInvokingDeliveryCallback() throws Exception {
    String suffix = UUID.randomUUID().toString().replace("-", "");
    String topic = "orders_" + suffix;
    AtomicInteger callbacks = new AtomicInteger();
    Session archiveSession = createSession("nats_archive_" + suffix, 0, 0, false,
        event -> callbacks.incrementAndGet());
    try {
      Destination destination = archiveSession.findDestination(topic, DestinationType.TOPIC).get();
      assertNotNull(destination);
      SubscribedEventManager archive = archiveSession.addSubscription(
          new SubscriptionContextBuilder(topic, ClientAcknowledgement.INDIVIDUAL)
              .setSync(true)
              .setReceiveMaximum(100)
              .build());
      assertNotNull(archive);

      byte[] expected = "created".getBytes(StandardCharsets.UTF_8);
      natsConnection.publish(topic, expected);
      natsConnection.flush(Duration.ofSeconds(2));

      assertEquals(1, archive.getPending(), "Archive must retain interest without polling for delivery");
      assertEquals(1L, destination.getStoredMessages());
      assertEquals(0, callbacks.get(), "Synchronous archive must not receive automatic delivery callbacks");

      MessageResponse pulled = assertInstanceOf(MessageResponse.class,
          archive.getNext().get(3, TimeUnit.SECONDS));
      MessageEvent event = pulled.getResponse();
      assertNotNull(event);
      assertArrayEquals(expected, event.getMessage().getOpaqueData());
      assertEquals(0, callbacks.get());
    } finally {
      close(archiveSession);
    }
  }

  @Test
  void oneMapsTopicSupportsIndependentEventTypeSelectors() throws Exception {
    String suffix = UUID.randomUUID().toString().replace("-", "");
    String topic = "orders_" + suffix;
    AtomicInteger archiveCallbacks = new AtomicInteger();
    AtomicInteger filteredCallbacks = new AtomicInteger();
    Session archiveSession = createSession("nats_archive_" + suffix, 0, 0, false,
        event -> archiveCallbacks.incrementAndGet());
    Session filteredSession = createSession("nats_filter_" + suffix, 0, 0, false,
        event -> filteredCallbacks.incrementAndGet());
    try {
      Destination destination = archiveSession.findDestination(topic, DestinationType.TOPIC).get();
      SubscribedEventManager archive = archiveSession.addSubscription(
          new SubscriptionContextBuilder(topic, ClientAcknowledgement.INDIVIDUAL)
              .setSync(true).setReceiveMaximum(100).build());
      SubscribedEventManager createdOnly = filteredSession.addSubscription(
          new SubscriptionContextBuilder(topic, ClientAcknowledgement.INDIVIDUAL)
              .setSync(true)
              .setSelector("stream = 'created'")
              .setReceiveMaximum(100)
              .build());

      assertNotNull(archive);
      assertNotNull(createdOnly);
      destination.storeMessage(new MessageBuilder()
          .setMeta(Map.of("stream", "created"))
          .setOpaqueData("order-1".getBytes(StandardCharsets.UTF_8)).build());
      destination.storeMessage(new MessageBuilder()
          .setMeta(Map.of("stream", "paid"))
          .setOpaqueData("order-2".getBytes(StandardCharsets.UTF_8)).build());

      assertEquals(2, archive.getPending(), "Archive sees the whole shared topic");
      assertEquals(1, createdOnly.getPending(), "Selector sees only matching event metadata");
      assertEquals(2L, destination.getStoredMessages());
      assertEquals(0, archiveCallbacks.get());
      assertEquals(0, filteredCallbacks.get());

      MessageResponse pulled = assertInstanceOf(MessageResponse.class,
          createdOnly.getNext().get(3, TimeUnit.SECONDS));
      MessageEvent received = pulled.getResponse();
      assertNotNull(received);
      assertEquals("created", received.getMessage().get("stream"));
      assertArrayEquals("order-1".getBytes(StandardCharsets.UTF_8),
          received.getMessage().getOpaqueData());
    } finally {
      close(filteredSession);
      close(archiveSession);
    }
  }
}
