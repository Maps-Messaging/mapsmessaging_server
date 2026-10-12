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
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.Constants;
import io.mapsmessaging.engine.destination.subscription.state.IteratorStateManagerImpl;
import io.mapsmessaging.engine.destination.subscription.state.MessageStateManagerImpl;
import io.mapsmessaging.engine.destination.subscription.state.MessageStateManagerListener;
import io.mapsmessaging.utilities.collections.bitset.BitSetFactoryImpl;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Characterizes MAPS subscription-state seeding without adding replay APIs.
 * The existing browser is a snapshot. History plus live delivery is verified
 * separately by JetStreamArchiveReplaySubscriptionTest using normal subscriptions.
 */
class JetStreamArchiveReplayFeasibilityTest {

  @Test
  void browserIteratorSeedsExistingArchiveHistory() throws Exception {
    MessageStateManagerImpl archive = createManager(1);
    try {
      archive.register(message(1));
      archive.register(message(2));
      archive.register(message(3));
      IteratorStateManagerImpl browser = new IteratorStateManagerImpl(
          "jetstream-browser-" + UUID.randomUUID(), 2L, archive, true);
      try {
        assertEquals(List.of(1L, 2L, 3L), new ArrayList<>(browser.getAllAtRest()),
            "Browser snapshot should copy every archived message identifier");
      } finally {
        browser.close();
      }
    } finally {
      archive.close();
    }
  }

  @Test
  void browser_iterator_remains_a_snapshot_after_subsequent_publication() throws Exception {
    MessageStateManagerImpl archive = createManager(3);
    try {
      archive.register(message(1));
      IteratorStateManagerImpl browser = new IteratorStateManagerImpl(
          "jetstream-browser-" + UUID.randomUUID(), 4L, archive, true);
      try {
        archive.register(message(2));
        assertEquals(List.of(1L, 2L), new ArrayList<>(archive.getAllAtRest()),
            "The archive must retain the subsequent publication");
        assertEquals(List.of(1L), new ArrayList<>(browser.getAllAtRest()),
            "A browser must retain its seeded snapshot without following subsequent publications");
      } finally {
        browser.close();
      }
    } finally {
      archive.close();
    }
  }

  @Test
  void subscribeBeforeSnapshotCanDeduplicateConcurrentPublications() throws Exception {
    MessageStateManagerImpl archive = createManager(5);
    int totalMessages = 300;
    for (int sequence = 1; sequence <= 50; sequence++) {
      archive.register(message(sequence));
    }

    // Only test-side glue. This is not a production replay implementation.
    ConcurrentSkipListSet<Long> consumerIds = new ConcurrentSkipListSet<>();
    MessageStateManagerListener listener = new MessageStateManagerListener() {
      @Override
      public void add(long id, int priority) {
        consumerIds.add(id);
      }

      @Override
      public void addAll(Collection<Long> ids) {
        consumerIds.addAll(ids);
      }

      @Override
      public void remove(long id) {
        consumerIds.remove(id);
      }
    };

    ExecutorService publisher = Executors.newSingleThreadExecutor();
    archive.add(listener);
    try {
      Future<?> publishing = publisher.submit(() -> {
        for (int sequence = 51; sequence <= totalMessages; sequence++) {
          archive.register(message(sequence));
        }
      });

      // Listener registered first, so any records added during the copy
      // are captured by it as well. The set deduplicates overlap.
      consumerIds.addAll(archive.getAll());
      publishing.get(10, TimeUnit.SECONDS);

      assertEquals(totalMessages, consumerIds.size(),
          "A concurrent publication must not be lost or duplicated");
      assertEquals(LongStream.rangeClosed(1, totalMessages).boxed().toList(),
          new ArrayList<>(consumerIds));
    } finally {
      archive.remove(listener);
      publisher.shutdownNow();
      archive.close();
    }
  }

  private static MessageStateManagerImpl createManager(long stateId) {
    return new MessageStateManagerImpl("jetstream-archive-state-" + stateId,
        stateId, new BitSetFactoryImpl(Constants.BITSET_BLOCK_SIZE));
  }

  private static Message message(long sequence) {
    return new MessageBuilder()
        .setId(sequence)
        .setOpaqueData(("event-" + sequence).getBytes(StandardCharsets.UTF_8))
        .build();
  }
}
