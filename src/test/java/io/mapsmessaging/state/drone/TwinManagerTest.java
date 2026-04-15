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

package io.mapsmessaging.state.drone;

import io.mapsmessaging.state.drone.core.*;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class TwinManagerTest {

  // --------------------------------------------------
  // Happy Path
  // --------------------------------------------------

  @Test
  void registerTwin_createsAndRetrieves() {
    TwinManager manager = new TwinManager();

    DroneTwin drone = new DroneTwin("drone-1");

    manager.registerTwin(drone, new TwinUpdateContext());

    Optional<EntityTwin> result = manager.getTwin("drone-1");

    assertTrue(result.isPresent());
    assertEquals("drone-1", result.get().getTwinId());
  }

  @Test
  void updateTwin_updatesState() {
    TwinManager manager = new TwinManager();

    DroneTwin drone = new DroneTwin("drone-1");
    manager.registerTwin(drone, new TwinUpdateContext());

    manager.updateTwin("drone-1", twin -> {
      ((DroneTwin) twin).setArmed(true);
    }, new TwinUpdateContext());

    DroneTwin updated = (DroneTwin) manager.getTwin("drone-1").get();

    assertTrue(updated.getArmed());
  }

  @Test
  void observer_receives_update_event() {
    TwinManager manager = new TwinManager();

    AtomicBoolean called = new AtomicBoolean(false);

    manager.addObserver(new TwinObserver() {
      @Override
      public void onTwinUpdated(String twinId, EntityTwin previous, EntityTwin current, TwinUpdateContext context) {
        called.set(true);
      }
    });

    DroneTwin drone = new DroneTwin("drone-1");
    manager.registerTwin(drone, new TwinUpdateContext());

    manager.updateTwin("drone-1", twin -> {
      ((DroneTwin) twin).setFlightMode("AUTO");
    }, new TwinUpdateContext());

    assertTrue(called.get());
  }

  // --------------------------------------------------
  // Complex Paths
  // --------------------------------------------------

  @Test
  void multipleUpdates_accumulateState() {
    TwinManager manager = new TwinManager();

    DroneTwin drone = new DroneTwin("drone-1");
    manager.registerTwin(drone, new TwinUpdateContext());

    manager.updateTwin("drone-1", twin -> {
      ((DroneTwin) twin).setArmed(true);
    }, new TwinUpdateContext());

    manager.updateTwin("drone-1", twin -> {
      ((DroneTwin) twin).setFlightMode("AUTO");
    }, new TwinUpdateContext());

    DroneTwin updated = (DroneTwin) manager.getTwin("drone-1").get();

    assertTrue(updated.getArmed());
    assertEquals("AUTO", updated.getFlightMode());
  }

  @Test
  void relationship_upsert_replaces_existing() {
    TwinManager manager = new TwinManager();

    DroneTwin drone = new DroneTwin("drone-1");
    manager.registerTwin(drone, new TwinUpdateContext());

    TwinRelationship r1 = new TwinRelationship("drone-1", "gs-1", "CONTROLLED_BY", true, Instant.now());
    TwinRelationship r2 = new TwinRelationship("drone-1", "gs-1", "CONTROLLED_BY", false, Instant.now().plusSeconds(10));

    manager.upsertRelationship("drone-1", r1, new TwinUpdateContext());
    manager.upsertRelationship("drone-1", r2, new TwinUpdateContext());

    DroneTwin updated = (DroneTwin) manager.getTwin("drone-1").get();

    assertEquals(1, updated.getRelationships().size());
  }

  @Test
  void relationship_update_notifies_observer() {
    TwinManager manager = new TwinManager();

    AtomicBoolean called = new AtomicBoolean(false);

    manager.addObserver(new TwinObserver() {
      @Override
      public void onRelationshipUpdated(String twinId, TwinRelationship relationship, TwinUpdateContext context) {
        called.set(true);
      }
    });

    DroneTwin drone = new DroneTwin("drone-1");
    manager.registerTwin(drone, new TwinUpdateContext());

    TwinRelationship rel = new TwinRelationship("drone-1", "gs-1", "CONTROLLED_BY", true, Instant.now());

    manager.upsertRelationship("drone-1", rel, new TwinUpdateContext());

    assertTrue(called.get());
  }

  // --------------------------------------------------
  // Unhappy Paths
  // --------------------------------------------------

  @Test
  void updateTwin_missingTwin_returnsEmpty() {
    TwinManager manager = new TwinManager();

    Optional<EntityTwin> result = manager.updateTwin("missing", t -> {}, new TwinUpdateContext());

    assertTrue(result.isEmpty());
  }

  @Test
  void removeTwin_nonExisting_returnsEmpty() {
    TwinManager manager = new TwinManager();

    Optional<EntityTwin> result = manager.removeTwin("missing", new TwinUpdateContext());

    assertTrue(result.isEmpty());
  }

  @Test
  void upsertRelationship_missingTwin_returnsEmpty() {
    TwinManager manager = new TwinManager();

    TwinRelationship rel = new TwinRelationship("a", "b", "LINKED_TO", true, Instant.now());

    Optional<EntityTwin> result = manager.upsertRelationship("missing", rel, new TwinUpdateContext());

    assertTrue(result.isEmpty());
  }

  @Test
  void registerTwin_nullTwin_throws() {
    TwinManager manager = new TwinManager();

    assertThrows(NullPointerException.class, () ->
        manager.registerTwin(null, new TwinUpdateContext())
    );
  }

  @Test
  void registerTwin_missingId_throws() {
    TwinManager manager = new TwinManager();

    DroneTwin drone = new DroneTwin();

    assertThrows(NullPointerException.class, () ->
        manager.registerTwin(drone, new TwinUpdateContext())
    );
  }

  @Test
  void concurrentUpdates_doNotCorruptState() throws Exception {
    TwinManager manager = new TwinManager();

    DroneTwin drone = new DroneTwin("drone-1");
    manager.registerTwin(drone, new TwinUpdateContext());

    Runnable task1 = () -> manager.updateTwin("drone-1", t ->
        ((DroneTwin) t).setArmed(true), new TwinUpdateContext());

    Runnable task2 = () -> manager.updateTwin("drone-1", t ->
        ((DroneTwin) t).setFlightMode("AUTO"), new TwinUpdateContext());

    Thread t1 = new Thread(task1);
    Thread t2 = new Thread(task2);

    t1.start();
    t2.start();

    t1.join();
    t2.join();

    DroneTwin updated = (DroneTwin) manager.getTwin("drone-1").get();

    assertTrue(updated.getArmed());
    assertEquals("AUTO", updated.getFlightMode());
  }
}