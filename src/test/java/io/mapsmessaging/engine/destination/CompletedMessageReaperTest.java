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

import io.mapsmessaging.utilities.queue.EventReaperQueue;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class CompletedMessageReaperTest {

  @Test
  void completed_messages_without_interest_are_removed() {
    EventReaperQueue completionQueue = new EventReaperQueue();
    List<List<Long>> removed = new ArrayList<>();
    CompletedMessageReaper reaper = new CompletedMessageReaper(
        "/test", completionQueue, () -> queueOf(2L), completed -> removed.add(new ArrayList<>(completed)));
    completionQueue.add(1L);
    completionQueue.add(2L);
    completionQueue.add(3L);

    reaper.run();

    assertEquals(List.of(List.of(1L, 3L)), removed);
  }

  @Test
  void a_failing_interest_lookup_does_not_stop_later_passes_and_keeps_the_message_ids() {
    EventReaperQueue completionQueue = new EventReaperQueue();
    List<List<Long>> removed = new ArrayList<>();
    AtomicInteger lookups = new AtomicInteger();
    CompletedMessageReaper reaper = new CompletedMessageReaper(
        "/test",
        completionQueue,
        () -> {
          if (lookups.getAndIncrement() == 0) {
            throw new IllegalStateException("subscription state changed while being read");
          }
          return new LinkedList<>();
        },
        completed -> removed.add(new ArrayList<>(completed)));
    completionQueue.add(1L);
    completionQueue.add(2L);

    assertDoesNotThrow(reaper::run);
    assertTrue(removed.isEmpty());

    completionQueue.add(3L);
    reaper.run();

    assertEquals(List.of(List.of(1L, 2L, 3L)), removed);
  }

  @Test
  void a_failing_removal_keeps_the_message_ids_for_the_next_pass() {
    EventReaperQueue completionQueue = new EventReaperQueue();
    List<List<Long>> removed = new ArrayList<>();
    AtomicInteger removals = new AtomicInteger();
    CompletedMessageReaper reaper = new CompletedMessageReaper(
        "/test",
        completionQueue,
        LinkedList::new,
        completed -> {
          if (removals.getAndIncrement() == 0) {
            throw new IllegalStateException("task queue rejected the removal");
          }
          removed.add(new ArrayList<>(completed));
        });
    completionQueue.add(5L);

    assertDoesNotThrow(reaper::run);
    reaper.run();

    assertEquals(List.of(List.of(5L)), removed);
  }

  @Test
  void an_idle_reaper_backs_off_before_checking_again() {
    EventReaperQueue completionQueue = new EventReaperQueue();
    List<List<Long>> removed = new ArrayList<>();
    CompletedMessageReaper reaper = new CompletedMessageReaper(
        "/test", completionQueue, LinkedList::new, completed -> removed.add(new ArrayList<>(completed)));

    reaper.run();
    completionQueue.add(7L);
    for (int pass = 0; pass < 4; pass++) {
      reaper.run();
    }
    assertTrue(removed.isEmpty());

    reaper.run();
    assertEquals(List.of(List.of(7L)), removed);
  }

  private static Queue<Long> queueOf(Long... ids) {
    return new LinkedList<>(List.of(ids));
  }
}
