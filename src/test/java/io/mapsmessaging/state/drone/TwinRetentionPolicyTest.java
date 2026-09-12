/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.state.drone;

import static org.junit.jupiter.api.Assertions.*;

import io.mapsmessaging.state.drone.core.EntityTwin;
import io.mapsmessaging.state.drone.core.TwinLifecycleStatus;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.core.TwinObserver;
import io.mapsmessaging.state.drone.core.TwinRetentionPolicy;
import io.mapsmessaging.state.drone.core.TwinType;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class TwinRetentionPolicyTest {

  private static final Instant REGISTERED_AT = Instant.parse("2026-09-12T10:00:00Z");

  @Test
  void defaultTwinIsPurgedAfterRetentionTimeout() {
    TwinManager manager = manager();
    manager.registerTwin(new DroneTwin("default"), context(REGISTERED_AT));

    int removed = manager.purgeExpiredTwins(REGISTERED_AT.plusSeconds(121));

    assertEquals(1, removed);
    assertTrue(manager.getTwin("default").isEmpty());
  }

  @Test
  void contactTwinSurvivesRetentionPurgeByType() {
    TwinManager manager = manager();
    ContactTwin contact = new ContactTwin("contact");
    manager.registerTwin(contact, context(REGISTERED_AT));

    manager.scanTwinStates(REGISTERED_AT.plusSeconds(20));
    int removed = manager.purgeExpiredTwins(REGISTERED_AT.plusSeconds(121));

    EntityTwin retained = manager.getTwin("contact").orElseThrow();
    assertEquals(0, removed);
    assertEquals(TwinLifecycleStatus.STALE, retained.getLifecycleStatus());
  }

  @Test
  void persistentTwinSurvivesRetentionPurgeButStillBecomesStale() {
    TwinManager manager = manager();
    DroneTwin persistent = new DroneTwin("persistent");
    persistent.setRetentionPolicy(TwinRetentionPolicy.PERSISTENT);
    manager.registerTwin(persistent, context(REGISTERED_AT));

    manager.scanTwinStates(REGISTERED_AT.plusSeconds(20));
    int removed = manager.purgeExpiredTwins(REGISTERED_AT.plusSeconds(121));

    EntityTwin retained = manager.getTwin("persistent").orElseThrow();
    assertEquals(0, removed);
    assertEquals(TwinLifecycleStatus.STALE, retained.getLifecycleStatus());
    assertFalse(retained.getLinkState().getConnected());
  }

  @Test
  void persistentTwinCanStillBeRemovedExplicitlyAndNotifiesObservers() {
    TwinManager manager = manager();
    AtomicInteger removals = new AtomicInteger();
    manager.addObserver(new TwinObserver() {
      @Override
      public void onTwinRemoved(EntityTwin removed, TwinUpdateContext context) {
        removals.incrementAndGet();
      }
    });
    DroneTwin persistent = new DroneTwin("persistent");
    persistent.setRetentionPolicy(TwinRetentionPolicy.PERSISTENT);
    manager.registerTwin(persistent, context(REGISTERED_AT));

    assertTrue(manager.removeTwin("persistent", new TwinUpdateContext()).isPresent());
    assertEquals(1, removals.get());
    assertTrue(manager.getTwin("persistent").isEmpty());
  }

  private TwinManager manager() {
    return new TwinManager(true, 10_000L, 5_000L, 120_000L, null);
  }

  private TwinUpdateContext context(Instant receivedAt) {
    TwinUpdateContext context = new TwinUpdateContext();
    context.setReceivedTime(receivedAt);
    return context;
  }

  private static final class ContactTwin extends EntityTwin {
    private ContactTwin(String twinId) {
      super(twinId, null);
      setTwinType(TwinType.CONTACT);
    }
  }
}
