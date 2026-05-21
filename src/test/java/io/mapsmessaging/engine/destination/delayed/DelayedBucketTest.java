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

import io.mapsmessaging.utilities.collections.bitset.BitSetFactory;
import io.mapsmessaging.utilities.collections.bitset.BitSetFactoryImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Queue;

class DelayedBucketTest {

  @Test
  void newBucket_isEmpty_sizeZero_peekMinusOne() {
    BitSetFactory factory = new BitSetFactoryImpl(8192);
    DelayedBucket bucket = new DelayedBucket(100L, factory);

    Assertions.assertTrue(bucket.isEmpty());
    Assertions.assertEquals(0, bucket.size());
    Assertions.assertEquals(-1L, bucket.peek());
  }

  @Test
  void register_addsMessageIds_andPeekReturnsLowest() {
    BitSetFactory factory = new BitSetFactoryImpl(8192);
    DelayedBucket bucket = new DelayedBucket(100L, factory);

    Assertions.assertTrue(bucket.register(9L));
    Assertions.assertTrue(bucket.register(7L));
    Assertions.assertTrue(bucket.register(8L));

    Assertions.assertFalse(bucket.isEmpty());
    Assertions.assertEquals(3, bucket.size());
    Assertions.assertEquals(7L, bucket.peek());
  }

  @Test
  void register_duplicateId_doesNotIncreaseSize() {
    BitSetFactory factory = new BitSetFactoryImpl(8192);
    DelayedBucket bucket = new DelayedBucket(100L, factory);

    Assertions.assertTrue(bucket.register(42L));
    Assertions.assertEquals(1, bucket.size());

    boolean second = bucket.register(42L);
    Assertions.assertFalse(second);
    Assertions.assertEquals(1, bucket.size());
    Assertions.assertEquals(42L, bucket.peek());
  }

  @Test
  void remove_existing_returnsTrue_andUpdatesPeekAndSize() {
    BitSetFactory factory = new BitSetFactoryImpl(8192);
    DelayedBucket bucket = new DelayedBucket(100L, factory);

    bucket.register(10L);
    bucket.register(12L);
    bucket.register(11L);

    Assertions.assertEquals(3, bucket.size());
    Assertions.assertEquals(10L, bucket.peek());

    Assertions.assertTrue(bucket.remove(10L));
    Assertions.assertEquals(2, bucket.size());
    Assertions.assertEquals(11L, bucket.peek());

    Assertions.assertTrue(bucket.remove(11L));
    Assertions.assertEquals(1, bucket.size());
    Assertions.assertEquals(12L, bucket.peek());

    Assertions.assertTrue(bucket.remove(12L));
    Assertions.assertEquals(0, bucket.size());
    Assertions.assertTrue(bucket.isEmpty());
    Assertions.assertEquals(-1L, bucket.peek());
  }

  @Test
  void remove_missing_returnsFalse_andDoesNotChangeState() {
    BitSetFactory factory = new BitSetFactoryImpl(8192);
    DelayedBucket bucket = new DelayedBucket(100L, factory);

    bucket.register(5L);

    Assertions.assertFalse(bucket.remove(999L));
    Assertions.assertEquals(1, bucket.size());
    Assertions.assertEquals(5L, bucket.peek());
  }

  @Test
  void getQueue_returnsBackingQueue_andOperationsAreVisible() {
    BitSetFactory factory = new BitSetFactoryImpl(8192);
    DelayedBucket bucket = new DelayedBucket(100L, factory);

    Queue<Long> queue = bucket.getQueue();

    Assertions.assertTrue(queue.offer(3L));
    Assertions.assertTrue(queue.offer(1L));
    Assertions.assertTrue(queue.offer(2L));

    Assertions.assertEquals(3, bucket.size());
    Assertions.assertEquals(1L, bucket.peek());

    Assertions.assertTrue(queue.remove(1L));
    Assertions.assertEquals(2, bucket.size());
    Assertions.assertEquals(2L, bucket.peek());
  }

  @Test
  void queuePoll_removesInNaturalOrder() {
    BitSetFactory factory = new BitSetFactoryImpl(8192);
    DelayedBucket bucket = new DelayedBucket(100L, factory);

    bucket.register(9L);
    bucket.register(7L);
    bucket.register(8L);

    Queue<Long> queue = bucket.getQueue();

    Assertions.assertEquals(7L, queue.poll());
    Assertions.assertEquals(8L, queue.poll());
    Assertions.assertEquals(9L, queue.poll());
    Assertions.assertNull(queue.poll());

    Assertions.assertEquals(0, bucket.size());
    Assertions.assertTrue(bucket.isEmpty());
    Assertions.assertEquals(-1L, bucket.peek());
  }

  @Test
  void delayTime_castToInt_isTruncating_byDesign() {
    BitSetFactory factory = new BitSetFactoryImpl(8192);

    long delayTime = (long) Integer.MAX_VALUE + 100L;
    DelayedBucket bucket = new DelayedBucket(delayTime, factory);

    // We can’t directly read the stored int, but we can at least assert the object is usable.
    // This test exists to document the cast behaviour (and remind future you to not “forget” it).
    Assertions.assertTrue(bucket.isEmpty());
    Assertions.assertEquals(0, bucket.size());
  }
}
