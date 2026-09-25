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

package io.mapsmessaging.state.metrics;

import io.mapsmessaging.state.drone.core.TwinLifecycleStatus;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.core.TwinType;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.tak.MtiStatusRegistry;
import io.mapsmessaging.state.drone.tak.MtiStatusSnapshot;
import io.mapsmessaging.utilities.admin.JMXManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TwinStatusJMXTest {

  private final Map<String, MtiStatusSnapshot> mtiStatuses = new HashMap<>();
  private boolean originalJmxEnabled;
  private TwinManager twinManager;

  @BeforeEach
  void setUp() {
    originalJmxEnabled = JMXManager.isEnableJMX();
    JMXManager.setEnableJMX(false);
    twinManager = new TwinManager();
    MtiStatusRegistry.setSnapshotSource(mtiStatuses::get);
  }

  @AfterEach
  void tearDown() {
    MtiStatusRegistry.setSnapshotSource(null);
    JMXManager.setEnableJMX(originalJmxEnabled);
  }

  @Test
  void activeDroneCounts_breakDownByMtiStateAndCumulativeAge() {
    Instant now = Instant.now();
    addDrone("go-fresh", TwinLifecycleStatus.ACTIVE, "go", now.minusSeconds(10));
    addDrone("hold-90s", TwinLifecycleStatus.ACTIVE, "hold", now.minusSeconds(90));
    addDrone("unknown-400s", TwinLifecycleStatus.ACTIVE, "UNKNOWN", now.minusSeconds(400));
    addDrone("no-mti", TwinLifecycleStatus.ACTIVE, null, null);
    addDrone("odd-state", TwinLifecycleStatus.ACTIVE, "degraded", now.minusSeconds(5));
    addDrone("stale-mitigate", TwinLifecycleStatus.STALE, "mitigate", now.minusSeconds(10));

    TwinStatusJMX active = new TwinStatusJMX(twinManager, TwinType.DRONE, TwinLifecycleStatus.ACTIVE);

    assertEquals(5, active.getTwinCount());
    assertEquals(1, active.getMtiNoneCount());
    assertEquals(1, active.getMtiGoCount());
    assertEquals(0, active.getMtiMitigateCount(), "the mitigate twin is STALE, not ACTIVE");
    assertEquals(1, active.getMtiHoldCount());
    assertEquals(1, active.getMtiUnknownCount(), "state matching must be case-insensitive");
    assertEquals(1, active.getMtiOtherCount());
    assertEquals(2, active.getMtiAgeLe30sCount());
    assertEquals(2, active.getMtiAgeLe60sCount());
    assertEquals(3, active.getMtiAgeLe120sCount());
    assertEquals(3, active.getMtiAgeLe300sCount(), "400s-old status must fall outside every bucket");
  }

  @Test
  void beansOnlyCountTheirOwnTypeAndLifecycle() {
    Instant now = Instant.now();
    addDrone("active", TwinLifecycleStatus.ACTIVE, "go", now);
    addDrone("stale", TwinLifecycleStatus.STALE, "mitigate", now);

    TwinStatusJMX stale = new TwinStatusJMX(twinManager, TwinType.DRONE, TwinLifecycleStatus.STALE);
    TwinStatusJMX disconnected = new TwinStatusJMX(twinManager, TwinType.DRONE, TwinLifecycleStatus.DISCONNECTED);
    TwinStatusJMX activeContacts = new TwinStatusJMX(twinManager, TwinType.CONTACT, TwinLifecycleStatus.ACTIVE);

    assertEquals(1, stale.getTwinCount());
    assertEquals(1, stale.getMtiMitigateCount());
    assertEquals(0, disconnected.getTwinCount());
    assertEquals(0, activeContacts.getTwinCount());
  }

  @Test
  void withoutAnMtiAdapter_everyTwinCountsAsHavingNoMtiStatus() {
    MtiStatusRegistry.setSnapshotSource(null);
    addDrone("a", TwinLifecycleStatus.ACTIVE, "go", Instant.now());
    addDrone("b", TwinLifecycleStatus.ACTIVE, "hold", Instant.now());

    TwinStatusJMX active = new TwinStatusJMX(twinManager, TwinType.DRONE, TwinLifecycleStatus.ACTIVE);

    assertEquals(2, active.getTwinCount());
    assertEquals(2, active.getMtiNoneCount());
    assertEquals(0, active.getMtiGoCount());
    assertEquals(0, active.getMtiAgeLe300sCount());
  }

  private void addDrone(String twinId, TwinLifecycleStatus status, String mtiState, Instant observedAt) {
    DroneTwin twin = new DroneTwin(twinId);
    twinManager.registerTwin(twin, new TwinUpdateContext());
    twin.setLifecycleStatus(status);
    if (observedAt != null) {
      mtiStatuses.put(twinId, new MtiStatusSnapshot(mtiState, observedAt, observedAt.plusSeconds(3600)));
    }
  }
}
