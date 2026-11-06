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

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReaperRetryQueueTest {

  @Test
  void isolated_failure_waits_while_fresh_ids_continue() {
    AtomicLong clock = new AtomicLong();
    ReaperRetryQueue retries = new ReaperRetryQueue(clock::get);
    retries.defer(queue(2L), false);
    Queue<Long> fresh = queue(1L, 2L, 3L);

    retries.drainReadyInto(fresh);

    assertEquals(List.of(1L, 3L), List.copyOf(fresh));
    assertFalse(retries.blocksFreshWork());
    assertFalse(retries.hasReadyIds());
    clock.set(TimeUnit.SECONDS.toNanos(5));
    retries.drainReadyInto(fresh);
    assertEquals(List.of(1L, 3L, 2L), List.copyOf(fresh));
    assertFalse(retries.hasReadyIds());
  }

  @Test
  void batch_failure_blocks_fresh_work_until_the_deadline() {
    AtomicLong clock = new AtomicLong();
    ReaperRetryQueue retries = new ReaperRetryQueue(clock::get);
    retries.defer(queue(1L, 2L, 3L), true);
    assertTrue(retries.blocksFreshWork());
    clock.set(TimeUnit.SECONDS.toNanos(5) - 1);
    assertTrue(retries.blocksFreshWork());
    assertFalse(retries.hasReadyIds());
    clock.incrementAndGet();
    assertFalse(retries.blocksFreshWork());
    assertTrue(retries.hasReadyIds());
    Queue<Long> completed = queue(4L);
    retries.drainReadyInto(completed);
    assertEquals(List.of(4L, 1L, 2L, 3L), List.copyOf(completed));
  }

  @Test
  void repeated_additions_do_not_postpone_an_existing_retry() {
    AtomicLong clock = new AtomicLong();
    ReaperRetryQueue retries = new ReaperRetryQueue(clock::get);
    retries.defer(queue(1L), false);
    clock.set(TimeUnit.SECONDS.toNanos(4));
    retries.defer(queue(2L, 1L), false);
    clock.set(TimeUnit.SECONDS.toNanos(5));
    Queue<Long> completed = queue();
    retries.drainReadyInto(completed);
    assertEquals(List.of(1L), List.copyOf(completed));
    clock.set(TimeUnit.SECONDS.toNanos(9));
    retries.drainReadyInto(completed);
    assertEquals(List.of(1L, 2L), List.copyOf(completed));
  }

  @Test
  void escalation_to_a_batch_failure_starts_a_full_delay() {
    AtomicLong clock = new AtomicLong();
    ReaperRetryQueue retries = new ReaperRetryQueue(clock::get);
    retries.defer(queue(1L), false);
    clock.set(TimeUnit.SECONDS.toNanos(4));
    retries.defer(queue(2L), true);
    clock.set(TimeUnit.SECONDS.toNanos(5));
    assertTrue(retries.blocksFreshWork());
    clock.set(TimeUnit.SECONDS.toNanos(9));
    assertTrue(retries.hasReadyIds());
  }

  @Test
  void submission_failure_can_pause_fresh_work_without_draining_ids() {
    AtomicLong clock = new AtomicLong();
    ReaperRetryQueue retries = new ReaperRetryQueue(clock::get);
    retries.defer(queue(), true);
    assertTrue(retries.blocksFreshWork());
    clock.set(TimeUnit.SECONDS.toNanos(5));
    assertFalse(retries.blocksFreshWork());
    assertFalse(retries.hasReadyIds());
  }

  @Test
  void repeated_permanent_failure_requires_another_delay() {
    AtomicLong clock = new AtomicLong();
    ReaperRetryQueue retries = new ReaperRetryQueue(clock::get);
    retries.defer(queue(1L), false);
    clock.set(TimeUnit.SECONDS.toNanos(5));
    Queue<Long> completed = queue();
    retries.drainReadyInto(completed);
    retries.defer(completed, false);
    assertFalse(retries.hasReadyIds());
    clock.set(TimeUnit.SECONDS.toNanos(10));
    assertTrue(retries.hasReadyIds());
  }

  @Test
  void clear_cancels_pending_retry_state() {
    AtomicLong clock = new AtomicLong();
    ReaperRetryQueue retries = new ReaperRetryQueue(clock::get);
    retries.defer(queue(1L), true);
    retries.clear();
    assertFalse(retries.blocksFreshWork());
    clock.set(TimeUnit.SECONDS.toNanos(10));
    assertFalse(retries.hasReadyIds());
  }

  @Test
  void monotonic_deadline_survives_nanotime_wraparound() {
    AtomicLong clock = new AtomicLong(Long.MAX_VALUE - TimeUnit.SECONDS.toNanos(2));
    ReaperRetryQueue retries = new ReaperRetryQueue(clock::get);
    retries.defer(queue(1L), true);
    assertTrue(retries.blocksFreshWork());
    clock.addAndGet(TimeUnit.SECONDS.toNanos(5));
    assertTrue(retries.hasReadyIds());
    assertFalse(retries.blocksFreshWork());
  }

  @Test
  void repeated_submission_failure_restarts_an_expired_batch_delay() {
    AtomicLong clock = new AtomicLong();
    ReaperRetryQueue retries = new ReaperRetryQueue(clock::get);
    retries.defer(queue(), true);
    clock.set(TimeUnit.SECONDS.toNanos(5));
    retries.defer(queue(), true);
    assertTrue(retries.blocksFreshWork());
    clock.set(TimeUnit.SECONDS.toNanos(10));
    assertFalse(retries.blocksFreshWork());
  }

  @Test
  void held_ids_cannot_be_removed_or_retried_until_their_pass_finishes() {
    AtomicLong clock = new AtomicLong();
    ReaperRetryQueue retries = new ReaperRetryQueue(clock::get);
    retries.hold(2L);
    Queue<Long> alreadyQueued = queue(1L, 2L, 3L);
    retries.excludePending(alreadyQueued);
    assertEquals(List.of(1L, 3L), List.copyOf(alreadyQueued));
    clock.set(TimeUnit.SECONDS.toNanos(20));
    assertFalse(retries.hasReadyIds());
    retries.defer(queue(2L), false);
    assertFalse(retries.hasReadyIds());
    clock.addAndGet(TimeUnit.SECONDS.toNanos(5));
    assertTrue(retries.hasReadyIds());
  }

  private static Queue<Long> queue(Long... ids) {
    return new ArrayDeque<>(List.of(ids));
  }
}
