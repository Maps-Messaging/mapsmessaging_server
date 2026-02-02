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

package io.mapsmessaging.engine.destination.delayed;

import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.utilities.collections.bitset.BitSetFactory;
import io.mapsmessaging.utilities.collections.bitset.BitSetFactoryImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Queue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MessageManagerTest {

  @Test
  void newManager_startsEmpty() {
    BitSetFactory factory = new BitSetFactoryImpl(8192);
    MessageManager manager = new MessageManager(factory);

    Assertions.assertEquals(0, manager.size());
    Assertions.assertTrue(manager.isEmpty());
    Assertions.assertTrue(manager.getBucketIds().isEmpty());
  }

  @Test
  void register_createsBucket_andBucketIdsAreSorted() {
    BitSetFactory factory = new BitSetFactoryImpl(8192);
    MessageManager manager = new MessageManager(factory);

    manager.register(200L, newMessage(10L));
    manager.register(100L, newMessage(11L));
    manager.register(150L, newMessage(12L));

    List<Long> bucketIds = manager.getBucketIds();
    Assertions.assertEquals(List.of(100L, 150L, 200L), bucketIds);
  }

  @Test
  void register_duplicateMessageId_inSameBucket_doesNotIncrementCounterTwice() {
    BitSetFactory factory = new BitSetFactoryImpl(8192);
    MessageManager manager = new MessageManager(factory);

    manager.register(100L, newMessage(50L));
    Assertions.assertEquals(1, manager.size());

    manager.register(100L, newMessage(50L));
    Assertions.assertEquals(1, manager.size());

    manager.register(100L, newMessage(51L));
    Assertions.assertEquals(2, manager.size());
  }

  @Test
  void getNext_whenBucketExists_returnsLowestMessageId_andDoesNotRemoveIt() {
    BitSetFactory factory = new BitSetFactoryImpl(8192);
    MessageManager manager = new MessageManager(factory);

    manager.register(100L, newMessage(9L));
    manager.register(100L, newMessage(7L));
    manager.register(100L, newMessage(8L));

    long next1 = manager.getNext(100L);
    long next2 = manager.getNext(100L);

    Assertions.assertEquals(7L, next1);
    Assertions.assertEquals(7L, next2);
    Assertions.assertEquals(3, manager.size());
  }

  @Test
  void remove_existingMessage_decrementsCounter_andReturnsTrue() {
    BitSetFactory factory = new BitSetFactoryImpl(8192);
    MessageManager manager = new MessageManager(factory);

    manager.register(100L, newMessage(7L));
    manager.register(100L, newMessage(8L));
    Assertions.assertEquals(2, manager.size());

    boolean removed = manager.remove(100L, 7L);

    Assertions.assertTrue(removed);
    Assertions.assertEquals(1, manager.size());
    Assertions.assertEquals(8L, manager.getNext(100L));
  }

  @Test
  void remove_missingMessage_returnsFalse_andDoesNotChangeCounter() {
    BitSetFactory factory = new BitSetFactoryImpl(8192);
    MessageManager manager = new MessageManager(factory);

    manager.register(100L, newMessage(7L));
    Assertions.assertEquals(1, manager.size());

    boolean removed = manager.remove(100L, 999L);

    Assertions.assertFalse(removed);
    Assertions.assertEquals(1, manager.size());
  }

  @Test
  void getNext_unknownBucket_cleansBucketListEntry_andReturnsMinusOne() {
    BitSetFactory factory = new BitSetFactoryImpl(8192);
    MessageManager manager = new MessageManager(factory);

    manager.register(100L, newMessage(1L));
    manager.register(200L, newMessage(2L));
    Assertions.assertEquals(List.of(100L, 200L), manager.getBucketIds());

    // Force the "unknown bucket" path by deleting the bucket from the tree but leaving bucketList behind.
    // This is easiest by calling delete(bucketId), which currently removes only from treeList.
    boolean deleted = manager.delete(200L);
    Assertions.assertTrue(deleted);

    long next = manager.getNext(200L);
    Assertions.assertEquals(-1L, next);

    Assertions.assertEquals(List.of(100L), manager.getBucketIds());
  }

  @Test
  void delete_bucket_removesBucket_andDecrementsCounter() {
    BitSetFactory factory = new BitSetFactoryImpl(8192);
    MessageManager manager = new MessageManager(factory);

    manager.register(100L, newMessage(10L));
    manager.register(100L, newMessage(11L));
    manager.register(200L, newMessage(20L));
    Assertions.assertEquals(3, manager.size());

    boolean deleted = manager.delete(100L);

    Assertions.assertTrue(deleted);
    Assertions.assertEquals(1, manager.size());
    Assertions.assertEquals(List.of(200L), manager.getBucketIds());
  }

  @Test
  void removeBucket_returnsQueueContainingMessages_andRemovesBucketFromTree() {
    BitSetFactory factory = new BitSetFactoryImpl(8192);
    MessageManager manager = new MessageManager(factory);

    manager.register(100L, newMessage(10L));
    manager.register(100L, newMessage(12L));
    manager.register(100L, newMessage(11L));
    Assertions.assertEquals(3, manager.size());

    Queue<Long> removed = manager.removeBucket(100L);

    Assertions.assertNotNull(removed);
    Assertions.assertEquals(10L, removed.poll());
    Assertions.assertEquals(11L, removed.poll());
    Assertions.assertEquals(12L, removed.poll());
    Assertions.assertNull(removed.poll());

    // Counter is not adjusted by removeBucket(). That’s a contract decision.
    // If you expect size() to change here, this test should enforce it (and code must change).
    Assertions.assertEquals(3, manager.size());

    Assertions.assertEquals(-1L, manager.getNext(100L));
  }

  @Test
  void close_clearsAllState_andResetsCounter() throws IOException {
    BitSetFactory factory = new BitSetFactoryImpl(8192);
    MessageManager manager = new MessageManager(factory);

    manager.register(100L, newMessage(10L));
    manager.register(200L, newMessage(20L));
    Assertions.assertEquals(2, manager.size());
    Assertions.assertFalse(manager.getBucketIds().isEmpty());

    manager.close();

    Assertions.assertEquals(0, manager.size());
    Assertions.assertTrue(manager.isEmpty());
    Assertions.assertTrue(manager.getBucketIds().isEmpty());
  }

  @Test
  void delete_clearsAllState_andResetsCounter() throws IOException {
    BitSetFactory factory = new BitSetFactoryImpl(8192);
    MessageManager manager = new MessageManager(factory);

    manager.register(100L, newMessage(10L));
    manager.register(200L, newMessage(20L));
    Assertions.assertEquals(2, manager.size());

    manager.delete();

    Assertions.assertEquals(0, manager.size());
    Assertions.assertTrue(manager.isEmpty());
    Assertions.assertTrue(manager.getBucketIds().isEmpty());
  }

  @Test
  void getNext_whenBucketBecomesEmpty_removesThatBucketId_notTheFirstBucket() {
    BitSetFactory factory = new BitSetFactoryImpl(8192);
    MessageManager manager = new MessageManager(factory);

    manager.register(100L, newMessage(1L));
    manager.register(200L, newMessage(2L));
    Assertions.assertEquals(List.of(100L, 200L), manager.getBucketIds());

    // Empty bucket 200
    boolean removed = manager.remove(200L, 2L);
    Assertions.assertTrue(removed);

    long next = manager.getNext(200L);
    Assertions.assertEquals(-1L, next);

    // Intended behaviour: bucket 200 removed, bucket 100 remains.
    Assertions.assertEquals(List.of(100L), manager.getBucketIds());
  }

  private Message newMessage(long id) {
    Message message = mock(Message.class);
    when(message.getIdentifier()).thenReturn(id);
    return message;
  }
}
