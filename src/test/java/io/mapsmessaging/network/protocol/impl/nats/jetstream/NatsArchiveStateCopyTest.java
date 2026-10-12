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
import io.mapsmessaging.api.features.Priority;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.Constants;
import io.mapsmessaging.engine.destination.subscription.state.MessageStateManagerImpl;
import io.mapsmessaging.engine.destination.subscription.state.NatsArchiveStateCopy;
import io.mapsmessaging.utilities.collections.bitset.BitSetFactoryImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NatsArchiveStateCopyTest {

  @Test
  void copiesPriorityBucketsWithoutSharingAcknowledgementState() throws Exception {
    MessageStateManagerImpl archive = manager(100);
    MessageStateManagerImpl consumer = manager(200);
    try {
      Message low = message(1, Priority.LOWEST);
      Message normal = message(2, Priority.NORMAL);
      Message high = message(3, Priority.HIGHEST);
      archive.register(low);
      archive.register(normal);
      archive.register(high);

      assertEquals(3, NatsArchiveStateCopy.copyAtRest(archive, consumer));
      assertEquals(3L, consumer.nextMessageId(), "Higher priority must remain first after copy");
      assertEquals(3, archive.pending());

      consumer.allocate(high);
      consumer.commit(3L);
      assertEquals(2L, consumer.nextMessageId());
      assertEquals(3, archive.pending(), "Consumer ACK must not change the archive");
      assertEquals(3L, archive.nextMessageId());
    } finally {
      consumer.close();
      archive.close();
    }
  }

  @Test
  void overlapsBetweenLiveRegistrationsAndArchiveCopyAreNotDuplicated() throws Exception {
    MessageStateManagerImpl archive = manager(301);
    MessageStateManagerImpl consumer = manager(302);
    try {
      Message first = message(11, Priority.NORMAL);
      Message second = message(12, Priority.HIGHEST);
      archive.register(first);
      archive.register(second);
      consumer.register(second); // Already arrived via the ordinary MAPS subscription update.

      assertEquals(1, NatsArchiveStateCopy.copyAtRest(archive, consumer));
      assertEquals(2, consumer.pending());
      assertEquals(0, NatsArchiveStateCopy.copyAtRest(archive, consumer));
      assertEquals(2, consumer.pending());
      assertEquals(2, archive.pending());
    } finally {
      consumer.close();
      archive.close();
    }
  }

  @Test
  void rejectsSelfCopyInsteadOfSharingMutableState() throws Exception {
    MessageStateManagerImpl archive = manager(401);
    try {
      assertThrows(IllegalArgumentException.class,
          () -> NatsArchiveStateCopy.copyAtRest(archive, archive));
    } finally {
      archive.close();
    }
  }

  private static MessageStateManagerImpl manager(long id) {
    return new MessageStateManagerImpl("nats-copy-" + id, id,
        new BitSetFactoryImpl(Constants.BITSET_BLOCK_SIZE));
  }

  private static Message message(long id, Priority priority) {
    return new MessageBuilder().setId(id).setPriority(priority).setOpaqueData(new byte[]{1}).build();
  }
}
