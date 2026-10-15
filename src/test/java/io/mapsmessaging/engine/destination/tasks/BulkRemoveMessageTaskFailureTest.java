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

package io.mapsmessaging.engine.destination.tasks;

import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.DestinationStats;
import io.mapsmessaging.engine.tasks.LongResponse;
import io.mapsmessaging.engine.tasks.Response;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BulkRemoveMessageTaskFailureTest {

  @Test
  void first_failed_id_is_held_while_healthy_ids_are_removed() throws Exception {
    Fixture fixture = new Fixture();
    doThrow(new IOException("Corrupt record")).when(fixture.destination).removeMessage(2L);

    assertDoesNotThrow(() -> fixture.task(1L, 2L, 3L).call());

    verify(fixture.destination).removeMessage(1L);
    verify(fixture.destination).removeMessage(3L);
    verify(fixture.destination).holdCompletedMessage(2L);
    assertEquals(List.of(new Deferred(List.of(2L), false)), fixture.deferred);
    assertEquals(0, fixture.continuations.size());
  }

  @Test
  void second_failure_stops_the_batch_and_preserves_unprocessed_ids() throws Exception {
    Fixture fixture = new Fixture();
    doThrow(new IOException("First")).when(fixture.destination).removeMessage(2L);
    doThrow(new IllegalStateException("Second")).when(fixture.destination).removeMessage(3L);

    fixture.task(1L, 2L, 3L, 4L, 5L).call();

    verify(fixture.destination).removeMessage(1L);
    verify(fixture.destination, never()).removeMessage(4L);
    verify(fixture.destination, never()).removeMessage(5L);
    assertEquals(List.of(new Deferred(List.of(2L, 3L, 4L, 5L), true)), fixture.deferred);
    assertEquals(0, fixture.continuations.size());
  }

  @Test
  void the_first_failure_is_remembered_across_time_slices() throws Exception {
    Fixture fixture = new Fixture();
    doAnswer(invocation -> {
      fixture.clock.set(TimeUnit.SECONDS.toNanos(10));
      throw new IOException("First");
    }).when(fixture.destination).removeMessage(1L);
    doThrow(new IOException("Second")).when(fixture.destination).removeMessage(2L);

    fixture.task(1L, 2L, 3L).call();
    assertEquals(1, fixture.continuations.size());
    assertEquals(0, fixture.deferred.size());
    fixture.continuations.remove().call();

    verify(fixture.destination, never()).removeMessage(3L);
    assertEquals(List.of(new Deferred(List.of(1L, 2L, 3L), true)), fixture.deferred);
    assertEquals(0, fixture.continuations.size());
  }

  @Test
  void rejected_continuation_preserves_only_unremoved_ids() throws Exception {
    Fixture fixture = new Fixture();
    doAnswer(invocation -> {
      fixture.clock.set(TimeUnit.SECONDS.toNanos(10));
      return null;
    }).when(fixture.destination).removeMessage(1L);
    when(fixture.destination.submit(any(Callable.class))).thenThrow(new RejectedExecutionException("Rejected continuation"));

    assertThrows(RejectedExecutionException.class, () -> fixture.task(1L, 2L, 3L).call());

    assertEquals(List.of(new Deferred(List.of(2L, 3L), true)), fixture.deferred);
    verify(fixture.destination, never()).removeMessage(2L);
  }

  @Test
  void an_already_queued_task_respects_a_new_batch_pause() throws Exception {
    Fixture fixture = new Fixture();
    when(fixture.destination.prepareCompletedRemoval(any())).thenReturn(false);

    fixture.task(1L, 2L).call();

    verify(fixture.destination, never()).removeMessage(1L);
    verify(fixture.destination, never()).removeMessage(2L);
    assertEquals(List.of(new Deferred(List.of(1L, 2L), true)), fixture.deferred);
  }

  @Test
  void retained_messages_are_never_removed_or_retried() throws Exception {
    Fixture fixture = new Fixture();
    when(fixture.destination.getRetainedIdentifier()).thenReturn(2L);

    fixture.task(1L, 2L, 3L).call();

    verify(fixture.destination, never()).removeMessage(2L);
    assertEquals(0, fixture.deferred.size());
  }

  @Test
  void close_prevents_removal_and_retry_submission() throws Exception {
    Fixture fixture = new Fixture();
    when(fixture.destination.isClosed()).thenReturn(true);

    fixture.task(1L).call();

    verify(fixture.destination, never()).removeMessage(1L);
    assertEquals(0, fixture.deferred.size());
    assertEquals(0, fixture.continuations.size());
  }

  private record Deferred(List<Long> ids, boolean batchFailure) {
  }

  private static final class Fixture {
    private final DestinationImpl destination = mock(DestinationImpl.class);
    private final AtomicLong clock = new AtomicLong();
    private final List<Deferred> deferred = new ArrayList<>();
    private final Queue<Callable<Response>> continuations = new ArrayDeque<>();

    private Fixture() {
      when(destination.getFullyQualifiedNamespace()).thenReturn("/reaper-test");
      when(destination.getRetainedIdentifier()).thenReturn(-1L);
      when(destination.getStats()).thenReturn(mock(DestinationStats.class));
      when(destination.prepareCompletedRemoval(any())).thenReturn(true);
      doAnswer(invocation -> {
        Queue<Long> ids = invocation.getArgument(0);
        boolean batchFailure = invocation.getArgument(1);
        deferred.add(new Deferred(List.copyOf(ids), batchFailure));
        return null;
      }).when(destination).deferCompletedMessages(any(), anyBoolean());
      when(destination.submit(any(Callable.class))).thenAnswer(invocation -> {
        continuations.add(invocation.getArgument(0));
        return CompletableFuture.completedFuture(new LongResponse(0));
      });
    }

    private BulkRemoveMessageTask task(Long... ids) {
      return new BulkRemoveMessageTask(destination, new ArrayDeque<>(List.of(ids)), clock::get);
    }
  }
}
