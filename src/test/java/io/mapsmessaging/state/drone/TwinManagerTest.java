package io.mapsmessaging.state.drone;

import io.mapsmessaging.dto.rest.config.protocol.impl.VehicleClass;
import io.mapsmessaging.state.drone.core.EntityTwin;
import io.mapsmessaging.state.drone.core.TwinLifecycleStatus;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.core.TwinObserver;
import io.mapsmessaging.state.drone.core.TwinRelationship;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.core.TwinType;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.drone.GroundStationTwin;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class TwinManagerTest {

  @Test
  void droneTwin_constructor_initialisesCoreIdentity() {
    DroneTwin droneTwin = new DroneTwin("drone-1");

    assertEquals("drone-1", droneTwin.getTwinId());
    assertEquals(TwinType.DRONE, droneTwin.getTwinType());
    assertNotNull(droneTwin.getUuid());
    assertNotNull(droneTwin.getMmsi());
    assertNull(droneTwin.getProtocolSourceId());
  }

  @Test
  void groundStationTwin_constructor_initialisesCoreIdentity() {
    GroundStationTwin groundStationTwin = new GroundStationTwin("gcs-1");

    assertEquals("gcs-1", groundStationTwin.getTwinId());
    assertEquals(TwinType.GROUND_CONTROL, groundStationTwin.getTwinType());
    assertEquals(VehicleClass.GCS, groundStationTwin.getVehicleClass());
    assertNotNull(groundStationTwin.getUuid());
  }

  @Test
  void entityTwin_secondsHelpers_returnEpochSeconds() {
    DroneTwin droneTwin = new DroneTwin("drone-1");
    Instant instant = Instant.parse("2026-04-23T00:00:10Z");

    droneTwin.setCreatedAt(instant);
    droneTwin.setLastSeenAt(instant);
    droneTwin.setIdentityUpdatedAt(instant);
    droneTwin.setNavigationUpdatedAt(instant);
    droneTwin.setMotionUpdatedAt(instant);
    droneTwin.setPowerUpdatedAt(instant);
    droneTwin.setConnectivityUpdatedAt(instant);
    droneTwin.setRelationshipsUpdatedAt(instant);

    assertEquals(instant.getEpochSecond(), droneTwin.getCreatedAtSeconds());
    assertEquals(instant.getEpochSecond(), droneTwin.getLastSeenAtSeconds());
    assertEquals(instant.getEpochSecond(), droneTwin.getIdentityUpdatedAtSeconds());
    assertEquals(instant.getEpochSecond(), droneTwin.getNavigationUpdatedAtSeconds());
    assertEquals(instant.getEpochSecond(), droneTwin.getMotionUpdatedAtSeconds());
    assertEquals(instant.getEpochSecond(), droneTwin.getPowerUpdatedAtSeconds());
    assertEquals(instant.getEpochSecond(), droneTwin.getConnectivityUpdatedAtSeconds());
    assertEquals(instant.getEpochSecond(), droneTwin.getRelationshipsUpdatedAtSeconds());
  }

  @Test
  void protocolSourceId_returnsNullUntilSystemAndComponentPresent() {
    DroneTwin droneTwin = new DroneTwin("drone-1");

    assertNull(droneTwin.getProtocolSourceId());

    droneTwin.setSystemId(1);
    assertNull(droneTwin.getProtocolSourceId());

    droneTwin.setComponentId(42);
    assertEquals("mavlink:1:42", droneTwin.getProtocolSourceId());
  }

  @Test
  void registerTwin_createsAndRetrieves() {
    TwinManager twinManager = new TwinManager();
    TwinUpdateContext context = new TwinUpdateContext();
    DroneTwin droneTwin = new DroneTwin("drone-1");

    twinManager.registerTwin(droneTwin, context);

    Optional<EntityTwin> result = twinManager.getTwin("drone-1");

    assertTrue(result.isPresent());
    assertEquals("drone-1", result.get().getTwinId());
    assertNotNull(result.get().getUuid());
  }

  @Test
  void registerTwin_setsCreatedAtLastSeenStatusAndConnectedLink() {
    TwinManager twinManager = new TwinManager();
    Instant now = Instant.parse("2026-04-23T01:02:03Z");
    TwinUpdateContext context = new TwinUpdateContext();
    context.setReceivedTime(now);

    DroneTwin droneTwin = new DroneTwin("drone-1");

    EntityTwin registered = twinManager.registerTwin(droneTwin, context);

    assertEquals(now, registered.getCreatedAt());
    assertEquals(now, registered.getLastSeenAt());
    assertEquals(TwinLifecycleStatus.ACTIVE, registered.getLifecycleStatus());
    assertNotNull(registered.getLinkState());
    assertTrue(registered.getLinkState().getConnected());
    assertEquals("CONNECTED", registered.getLinkState().getState());
  }

  @Test
  void registerTwin_existingTwin_preservesOriginalCreatedAtAndReturnsExisting() {
    TwinManager twinManager = new TwinManager();

    TwinUpdateContext firstContext = new TwinUpdateContext();
    Instant firstTime = Instant.parse("2026-04-23T01:00:00Z");
    firstContext.setReceivedTime(firstTime);

    DroneTwin firstTwin = new DroneTwin("drone-1");
    EntityTwin firstRegistered = twinManager.registerTwin(firstTwin, firstContext);

    UUID originalUuid = firstRegistered.getUuid();
    Instant originalCreatedAt = firstRegistered.getCreatedAt();

    TwinUpdateContext secondContext = new TwinUpdateContext();
    Instant secondTime = Instant.parse("2026-04-23T01:05:00Z");
    secondContext.setReceivedTime(secondTime);

    DroneTwin secondTwin = new DroneTwin("drone-1");
    secondTwin.setDisplayName("ignored-new-instance");

    EntityTwin returned = twinManager.registerTwin(secondTwin, secondContext);

    assertSame(firstRegistered, returned);
    assertEquals(originalUuid, returned.getUuid());
    assertEquals(originalCreatedAt, returned.getCreatedAt());
    assertEquals(secondTime, returned.getLastSeenAt());
    assertNull(returned.getDisplayName());
  }

  @Test
  void updateTwin_updatesStateAndRefreshesLastSeen() {
    TwinManager twinManager = new TwinManager();
    DroneTwin droneTwin = new DroneTwin("drone-1");

    TwinUpdateContext registerContext = new TwinUpdateContext();
    registerContext.setReceivedTime(Instant.parse("2026-04-23T01:00:00Z"));
    twinManager.registerTwin(droneTwin, registerContext);

    TwinUpdateContext updateContext = new TwinUpdateContext();
    Instant updateTime = Instant.parse("2026-04-23T01:10:00Z");
    updateContext.setReceivedTime(updateTime);

    twinManager.updateTwin("drone-1", twin -> ((DroneTwin) twin).setArmed(true), updateContext);

    DroneTwin updated = (DroneTwin) twinManager.getTwin("drone-1").orElseThrow();

    assertTrue(updated.getArmed());
    assertEquals(updateTime, updated.getLastSeenAt());
    assertEquals(TwinLifecycleStatus.ACTIVE, updated.getLifecycleStatus());
    assertTrue(updated.getLinkState().getConnected());
  }

  @Test
  void observer_receivesUpdateEvent() {
    TwinManager twinManager = new TwinManager();
    AtomicBoolean called = new AtomicBoolean(false);

    twinManager.addObserver(new TwinObserver() {
      @Override
      public void onTwinUpdated(String twinId, EntityTwin current, TwinUpdateContext context) {
        called.set(true);
        assertEquals("drone-1", twinId);
        assertEquals("AUTO", ((DroneTwin) current).getFlightMode());
      }
    });

    DroneTwin droneTwin = new DroneTwin("drone-1");
    twinManager.registerTwin(droneTwin, new TwinUpdateContext());

    twinManager.updateTwin("drone-1", twin -> ((DroneTwin) twin).setFlightMode("AUTO"), new TwinUpdateContext());

    assertTrue(called.get());
  }

  @Test
  void observerException_doesNotBreakManagerOrOtherObservers() {
    TwinManager twinManager = new TwinManager();
    AtomicBoolean healthyObserverCalled = new AtomicBoolean(false);

    twinManager.addObserver(new TwinObserver() {
      @Override
      public void onTwinUpdated(String twinId, EntityTwin current, TwinUpdateContext context) {
        throw new RuntimeException("boom");
      }
    });

    twinManager.addObserver(new TwinObserver() {
      @Override
      public void onTwinUpdated(String twinId, EntityTwin current, TwinUpdateContext context) {
        healthyObserverCalled.set(true);
      }
    });

    DroneTwin droneTwin = new DroneTwin("drone-1");
    twinManager.registerTwin(droneTwin, new TwinUpdateContext());

    assertDoesNotThrow(() ->
        twinManager.updateTwin("drone-1", twin -> ((DroneTwin) twin).setFlightMode("AUTO"), new TwinUpdateContext())
    );

    assertTrue(healthyObserverCalled.get());
    assertEquals("AUTO", ((DroneTwin) twinManager.getTwin("drone-1").orElseThrow()).getFlightMode());
  }

  @Test
  void multipleUpdates_accumulateState() {
    TwinManager twinManager = new TwinManager();
    DroneTwin droneTwin = new DroneTwin("drone-1");
    twinManager.registerTwin(droneTwin, new TwinUpdateContext());

    twinManager.updateTwin("drone-1", twin -> ((DroneTwin) twin).setArmed(true), new TwinUpdateContext());
    twinManager.updateTwin("drone-1", twin -> ((DroneTwin) twin).setFlightMode("AUTO"), new TwinUpdateContext());

    DroneTwin updated = (DroneTwin) twinManager.getTwin("drone-1").orElseThrow();

    assertTrue(updated.getArmed());
    assertEquals("AUTO", updated.getFlightMode());
  }

  @Test
  void relationship_upsert_replacesExistingMatchingRelationship() {
    TwinManager twinManager = new TwinManager();
    DroneTwin droneTwin = new DroneTwin("drone-1");
    twinManager.registerTwin(droneTwin, new TwinUpdateContext());

    TwinRelationship firstRelationship = new TwinRelationship("drone-1", "gs-1", "CONTROLLED_BY", true, Instant.now());
    TwinRelationship secondRelationship = new TwinRelationship("drone-1", "gs-1", "CONTROLLED_BY", false, Instant.now().plusSeconds(10));

    twinManager.upsertRelationship("drone-1", firstRelationship, new TwinUpdateContext());
    twinManager.upsertRelationship("drone-1", secondRelationship, new TwinUpdateContext());

    DroneTwin updated = (DroneTwin) twinManager.getTwin("drone-1").orElseThrow();

    assertEquals(1, updated.getRelationships().size());
    TwinRelationship stored = updated.getRelationships().iterator().next();
    assertFalse(stored.isActive());
  }

  @Test
  void relationship_upsert_setsUpdatedAtWhenMissing() {
    TwinManager twinManager = new TwinManager();
    DroneTwin droneTwin = new DroneTwin("drone-1");
    twinManager.registerTwin(droneTwin, new TwinUpdateContext());

    TwinRelationship relationship = new TwinRelationship("drone-1", "gs-1", "CONTROLLED_BY", true, null);

    TwinUpdateContext context = new TwinUpdateContext();
    Instant now = Instant.parse("2026-04-23T02:00:00Z");
    context.setReceivedTime(now);

    twinManager.upsertRelationship("drone-1", relationship, context);

    DroneTwin updated = (DroneTwin) twinManager.getTwin("drone-1").orElseThrow();
    TwinRelationship stored = updated.getRelationships().iterator().next();

    assertEquals(now, stored.getUpdatedAt());
    assertEquals(now, updated.getRelationshipsUpdatedAt());
    assertEquals(now, updated.getLastSeenAt());
  }

  @Test
  void relationship_update_notifiesObserver() {
    TwinManager twinManager = new TwinManager();
    AtomicBoolean called = new AtomicBoolean(false);

    twinManager.addObserver(new TwinObserver() {
      @Override
      public void onRelationshipUpdated(String twinId, TwinRelationship relationship, TwinUpdateContext context) {
        called.set(true);
        assertEquals("drone-1", twinId);
      }
    });

    DroneTwin droneTwin = new DroneTwin("drone-1");
    twinManager.registerTwin(droneTwin, new TwinUpdateContext());

    TwinRelationship relationship = new TwinRelationship("drone-1", "gs-1", "CONTROLLED_BY", true, Instant.now());

    twinManager.upsertRelationship("drone-1", relationship, new TwinUpdateContext());

    assertTrue(called.get());
  }

  @Test
  void removeRelationship_removesMatchingRelationship() {
    TwinManager twinManager = new TwinManager();
    DroneTwin droneTwin = new DroneTwin("drone-1");
    twinManager.registerTwin(droneTwin, new TwinUpdateContext());

    TwinRelationship relationship = new TwinRelationship("drone-1", "gs-1", "CONTROLLED_BY", true, Instant.now());
    twinManager.upsertRelationship("drone-1", relationship, new TwinUpdateContext());

    twinManager.removeRelationship("drone-1", "drone-1", "gs-1", "CONTROLLED_BY", new TwinUpdateContext());

    DroneTwin updated = (DroneTwin) twinManager.getTwin("drone-1").orElseThrow();
    assertTrue(updated.getRelationships().isEmpty());
  }

  @Test
  void updateTwin_missingTwin_returnsEmpty() {
    TwinManager twinManager = new TwinManager();

    Optional<EntityTwin> result = twinManager.updateTwin("missing", twin -> {
    }, new TwinUpdateContext());

    assertTrue(result.isEmpty());
  }

  @Test
  void removeTwin_nonExisting_returnsEmpty() {
    TwinManager twinManager = new TwinManager();

    Optional<EntityTwin> result = twinManager.removeTwin("missing", new TwinUpdateContext());

    assertTrue(result.isEmpty());
  }

  @Test
  void upsertRelationship_missingTwin_returnsEmpty() {
    TwinManager twinManager = new TwinManager();
    TwinRelationship relationship = new TwinRelationship("a", "b", "LINKED_TO", true, Instant.now());

    Optional<EntityTwin> result = twinManager.upsertRelationship("missing", relationship, new TwinUpdateContext());

    assertTrue(result.isEmpty());
  }

  @Test
  void registerTwin_nullTwin_throws() {
    TwinManager twinManager = new TwinManager();

    assertThrows(NullPointerException.class, () -> twinManager.registerTwin(null, new TwinUpdateContext()));
  }

  @Test
  void registerTwin_missingId_throws() {
    TwinManager twinManager = new TwinManager();
    DroneTwin droneTwin = new DroneTwin();

    assertThrows(NullPointerException.class, () -> twinManager.registerTwin(droneTwin, new TwinUpdateContext()));
  }

  @Test
  void concurrentUpdates_doNotCorruptState() throws Exception {
    TwinManager twinManager = new TwinManager();
    DroneTwin droneTwin = new DroneTwin("drone-1");
    twinManager.registerTwin(droneTwin, new TwinUpdateContext());

    Runnable firstTask = () -> twinManager.updateTwin(
        "drone-1",
        twin -> ((DroneTwin) twin).setArmed(true),
        new TwinUpdateContext()
    );

    Runnable secondTask = () -> twinManager.updateTwin(
        "drone-1",
        twin -> ((DroneTwin) twin).setFlightMode("AUTO"),
        new TwinUpdateContext()
    );

    Thread firstThread = new Thread(firstTask);
    Thread secondThread = new Thread(secondTask);

    firstThread.start();
    secondThread.start();

    firstThread.join();
    secondThread.join();

    DroneTwin updated = (DroneTwin) twinManager.getTwin("drone-1").orElseThrow();

    assertTrue(updated.getArmed());
    assertEquals("AUTO", updated.getFlightMode());
  }

  @Test
  void scanTwinStates_marksDisconnectedAndThenStale() {
    TwinManager twinManager = new TwinManager();
    DroneTwin droneTwin = new DroneTwin("drone-1");

    TwinUpdateContext context = new TwinUpdateContext();
    Instant registeredAt = Instant.parse("2026-04-23T00:00:00Z");
    context.setReceivedTime(registeredAt);

    twinManager.registerTwin(droneTwin, context);

    Instant disconnectedScanTime = registeredAt.plusMillis(6_000);
    twinManager.scanTwinStates(disconnectedScanTime);

    DroneTwin disconnected = (DroneTwin) twinManager.getTwin("drone-1").orElseThrow();
    assertEquals(TwinLifecycleStatus.DISCONNECTED, disconnected.getLifecycleStatus());
    assertNotNull(disconnected.getLinkState());
    assertFalse(disconnected.getLinkState().getConnected());
    assertEquals("DISCONNECTED", disconnected.getLinkState().getState());

    Instant staleScanTime = registeredAt.plusMillis(11_000);
    twinManager.scanTwinStates(staleScanTime);

    DroneTwin stale = (DroneTwin) twinManager.getTwin("drone-1").orElseThrow();
    assertEquals(TwinLifecycleStatus.STALE, stale.getLifecycleStatus());
    assertFalse(stale.getLinkState().getConnected());
    assertEquals("STALE", stale.getLinkState().getState());
  }

  @Test
  void purgeExpiredTwins_removesOnlyExpiredOnes() {
    TwinManager twinManager = new TwinManager();

    TwinUpdateContext oldContext = new TwinUpdateContext();
    Instant oldTime = Instant.parse("2026-04-23T00:00:00Z");
    oldContext.setReceivedTime(oldTime);

    TwinUpdateContext freshContext = new TwinUpdateContext();
    Instant freshTime = Instant.parse("2026-04-23T00:01:30Z");
    freshContext.setReceivedTime(freshTime);

    twinManager.registerTwin(new DroneTwin("old-drone"), oldContext);
    twinManager.registerTwin(new DroneTwin("fresh-drone"), freshContext);

    int removedCount = twinManager.purgeExpiredTwins(Instant.parse("2026-04-23T00:02:10Z"));

    assertEquals(1, removedCount);
    assertTrue(twinManager.getTwin("old-drone").isEmpty());
    assertTrue(twinManager.getTwin("fresh-drone").isPresent());
  }

  @Test
  void getTwinCountByStatus_countsStatuses() {
    TwinManager twinManager = new TwinManager();

    TwinUpdateContext context = new TwinUpdateContext();
    Instant registeredAt = Instant.parse("2026-04-23T00:00:00Z");
    context.setReceivedTime(registeredAt);

    twinManager.registerTwin(new DroneTwin("drone-1"), context);
    twinManager.registerTwin(new DroneTwin("drone-2"), context);

    twinManager.scanTwinStates(registeredAt.plusMillis(6_000));

    assertEquals(2, twinManager.getTwinCountByStatus(TwinLifecycleStatus.DISCONNECTED));
  }

  @Test
  void removeTwin_notifiesObserver() {
    TwinManager twinManager = new TwinManager();
    AtomicInteger removedCount = new AtomicInteger();

    twinManager.addObserver(new TwinObserver() {
      @Override
      public void onTwinRemoved(EntityTwin removed, TwinUpdateContext context) {
        removedCount.incrementAndGet();
        assertEquals("drone-1", removed.getTwinId());
      }
    });

    twinManager.registerTwin(new DroneTwin("drone-1"), new TwinUpdateContext());

    Optional<EntityTwin> removed = twinManager.removeTwin("drone-1", new TwinUpdateContext());

    assertTrue(removed.isPresent());
    assertEquals(1, removedCount.get());
  }
}