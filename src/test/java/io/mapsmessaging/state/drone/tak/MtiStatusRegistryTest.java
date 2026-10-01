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
package io.mapsmessaging.state.drone.tak;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class MtiStatusRegistryTest {

  @AfterEach
  void clear_registry() {
    MtiStatusRegistry.setDelegate(null);
    MtiStatusRegistry.setSnapshotSource(null);
    MtiStatusRegistry.setStatusListener(null);
  }

  @Test
  void lookup_observes_replacement_and_deregistration_across_threads() {
    MtiLookupResult initial = new MtiLookupResult("u", null, null, null, null);
    MtiLookupResult replacement = new MtiLookupResult("f", null, null, null, null);
    MtiStatusRegistry.setDelegate(twinId -> initial);
    assertSame(initial, MtiStatusRegistry.lookup("vehicle"));
    CompletableFuture.runAsync(() -> MtiStatusRegistry.setDelegate(twinId -> replacement)).join();
    assertSame(replacement, MtiStatusRegistry.lookup("vehicle"));
    assertNull(MtiStatusRegistry.lookup(null));
    CompletableFuture.runAsync(() -> MtiStatusRegistry.setDelegate(null)).join();
    assertNull(MtiStatusRegistry.lookup("vehicle"));
  }

  @Test
  void snapshot_observes_replacement_and_deregistration_across_threads() {
    MtiStatusSnapshot initial = new MtiStatusSnapshot("healthy", Instant.EPOCH, Instant.MAX);
    MtiStatusSnapshot replacement = new MtiStatusSnapshot("degraded", Instant.EPOCH, Instant.MAX);
    MtiStatusRegistry.setSnapshotSource(twinId -> initial);
    assertSame(initial, MtiStatusRegistry.snapshot("vehicle"));
    CompletableFuture.runAsync(() -> MtiStatusRegistry.setSnapshotSource(twinId -> replacement)).join();
    assertSame(replacement, MtiStatusRegistry.snapshot("vehicle"));
    assertNull(MtiStatusRegistry.snapshot(null));
    CompletableFuture.runAsync(() -> MtiStatusRegistry.setSnapshotSource(null)).join();
    assertNull(MtiStatusRegistry.snapshot("vehicle"));
  }

  @Test
  void listener_receives_events_and_stops_after_deregistration() {
    AtomicReference<Instant> acceptedAt = new AtomicReference<>();
    AtomicReference<String> clearedTwin = new AtomicReference<>();
    MtiStatusRegistry.setStatusListener(new MtiStatusRegistry.StatusListener() {
      public void onStatusAccepted(String twinId, Instant receivedAt) {
        acceptedAt.set(receivedAt);
      }

      public void onStatusCleared(String twinId) {
        clearedTwin.set(twinId);
      }
    });
    Instant receivedAt = Instant.now();
    String twinId = "vehicle";
    CompletableFuture.runAsync(() -> {
      MtiStatusRegistry.statusAccepted(twinId, receivedAt);
      MtiStatusRegistry.statusCleared(twinId);
    }).join();
    assertSame(receivedAt, acceptedAt.get());
    assertSame(twinId, clearedTwin.get());
    acceptedAt.set(null);
    clearedTwin.set(null);
    MtiStatusRegistry.statusAccepted(null, receivedAt);
    MtiStatusRegistry.statusCleared(null);
    MtiStatusRegistry.setStatusListener(null);
    MtiStatusRegistry.statusAccepted(twinId, receivedAt);
    MtiStatusRegistry.statusCleared(twinId);
    assertNull(acceptedAt.get());
    assertNull(clearedTwin.get());
  }
}
