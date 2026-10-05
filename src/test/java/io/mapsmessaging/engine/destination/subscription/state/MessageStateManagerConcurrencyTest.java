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

package io.mapsmessaging.engine.destination.subscription.state;

import io.mapsmessaging.engine.Constants;
import io.mapsmessaging.utilities.collections.bitset.BitSetFactoryImpl;
import org.junit.jupiter.api.Test;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The destination's message reaper reads a subscription's message state from the scheduler
 * thread while the delivery thread is changing it. Every read must take the same lock as the
 * updates, otherwise the reader can see the bitsets half-changed and throw.
 */
class MessageStateManagerConcurrencyTest {

  @Test
  void get_all_waits_for_a_concurrent_update_to_finish() throws Exception {
    assertReadWaitsForTheLock(BaseMessageStateManager::getAll);
  }

  @Test
  void get_all_at_rest_waits_for_a_concurrent_update_to_finish() throws Exception {
    assertReadWaitsForTheLock(BaseMessageStateManager::getAllAtRest);
  }

  private void assertReadWaitsForTheLock(java.util.function.Function<BaseMessageStateManager, Queue<Long>> read)
      throws Exception {
    MessageStateManagerImpl manager = new MessageStateManagerImpl(
        "concurrency-test", 1L, new BitSetFactoryImpl(Constants.BITSET_BLOCK_SIZE));
    manager.register(42L);

    CompletableFuture<Queue<Long>> result;
    synchronized (manager) {
      result = CompletableFuture.supplyAsync(() -> read.apply(manager));
      assertThrows(TimeoutException.class, () -> result.get(200, TimeUnit.MILLISECONDS));
    }

    Queue<Long> ids = result.get(5, TimeUnit.SECONDS);
    assertTrue(ids.contains(42L));
  }
}
