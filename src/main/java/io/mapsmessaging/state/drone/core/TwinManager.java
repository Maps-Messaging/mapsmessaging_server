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

package io.mapsmessaging.state.drone.core;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * In-memory twin registry + observer fan-out.
 * No protocol logic, no emit logic, only state lifecycle + updates.
 */
public class TwinManager {

  private final ConcurrentHashMap<String, EntityTwin> twins = new ConcurrentHashMap<>();
  private final CopyOnWriteArrayList<TwinObserver> observers = new CopyOnWriteArrayList<>();

  // --------------------------------------------------
  // Observer management
  // --------------------------------------------------

  public void addObserver(TwinObserver observer) {
    if (observer != null) {
      observers.addIfAbsent(observer);
    }
  }

  public void removeObserver(TwinObserver observer) {
    observers.remove(observer);
  }

  // --------------------------------------------------
  // Query
  // --------------------------------------------------

  public Optional<EntityTwin> getTwin(String twinId) {
    return Optional.ofNullable(twins.get(twinId));
  }

  public Collection<EntityTwin> listTwins() {
    return Collections.unmodifiableCollection(twins.values());
  }

  public int getTwinCount() {
    return twins.size();
  }

  // --------------------------------------------------
  // Lifecycle
  // --------------------------------------------------

  public EntityTwin registerTwin(EntityTwin twin, TwinUpdateContext context) {
    Objects.requireNonNull(twin, "twin must not be null");
    Objects.requireNonNull(twin.getTwinId(), "twin.twinId must not be null");

    Instant now = resolveNow(context);

    twin.setCreatedAt(now);
    twin.setLastSeenAt(now);

    EntityTwin existing = twins.putIfAbsent(twin.getTwinId(), twin);
    if (existing == null) {
      notifyAdded(twin, context);
      return twin;
    }

    // Already exists → treat as heartbeat only
    existing.setLastSeenAt(now);
    return existing;
  }

  public Optional<EntityTwin> removeTwin(String twinId, TwinUpdateContext context) {
    EntityTwin removed = twins.remove(twinId);
    if (removed != null) {
      notifyRemoved(removed, context);
    }
    return Optional.ofNullable(removed);
  }

  // --------------------------------------------------
  // Controlled update (THIS is the important one)
  // --------------------------------------------------

  public Optional<EntityTwin> updateTwin(
      String twinId,
      Consumer<EntityTwin> updater,
      TwinUpdateContext context) {

    Objects.requireNonNull(twinId, "twinId must not be null");
    Objects.requireNonNull(updater, "updater must not be null");

    EntityTwin twin = twins.get(twinId);
    if (twin == null) {
      return Optional.empty();
    }

    Instant now = resolveNow(context);

    EntityTwin previous;
    synchronized (twin) {
      previous = shallowCopy(twin); // minimal safe snapshot

      updater.accept(twin);

      twin.setLastSeenAt(now);
    }

    notifyUpdated(previous, twin, context);
    return Optional.of(twin);
  }

  // --------------------------------------------------
  // Relationship handling
  // --------------------------------------------------

  public Optional<EntityTwin> upsertRelationship(
      String twinId,
      TwinRelationship relationship,
      TwinUpdateContext context) {

    Objects.requireNonNull(twinId, "twinId must not be null");
    Objects.requireNonNull(relationship, "relationship must not be null");

    EntityTwin twin = twins.get(twinId);
    if (twin == null) {
      return Optional.empty();
    }

    Instant now = resolveNow(context);

    synchronized (twin) {
      twin.getRelationships().removeIf(existing ->
          Objects.equals(existing.getSourceTwinId(), relationship.getSourceTwinId()) &&
              Objects.equals(existing.getTargetTwinId(), relationship.getTargetTwinId()) &&
              Objects.equals(existing.getRelationshipType(), relationship.getRelationshipType())
      );

      if (relationship.getUpdatedAt() == null) {
        relationship.setUpdatedAt(now);
      }

      twin.getRelationships().add(relationship);
      twin.setRelationshipsUpdatedAt(now);
      twin.setLastSeenAt(now);
    }

    for (TwinObserver observer : observers) {
      observer.onRelationshipUpdated(twinId, relationship, context);
    }

    return Optional.of(twin);
  }

  // --------------------------------------------------
  // Helpers
  // --------------------------------------------------

  private Instant resolveNow(TwinUpdateContext context) {
    if (context != null && context.getReceivedTime() != null) {
      return context.getReceivedTime();
    }
    return Instant.now();
  }

  /**
   * Minimal shallow copy for observer diffing.
   * Enough for detecting field changes without full deep clone overhead.
   */
  private EntityTwin shallowCopy(EntityTwin twin) {
    // You can replace this later with a proper clone or mapper
    EntityTwin copy;
    try {
      copy = twin.getClass().getDeclaredConstructor().newInstance();
    } catch (Exception e) {
      throw new IllegalStateException("Failed to copy twin", e);
    }

    copy.setTwinId(twin.getTwinId());
    copy.setTwinType(twin.getTwinType());
    copy.setDisplayName(twin.getDisplayName());

    copy.setGeoPosition(twin.getGeoPosition());
    copy.setHomePosition(twin.getHomePosition());
    copy.setVelocityVector(twin.getVelocityVector());
    copy.setOrientation(twin.getOrientation());
    copy.setFixInfo(twin.getFixInfo());
    copy.setBatteryState(twin.getBatteryState());
    copy.setLinkState(twin.getLinkState());

    copy.setCreatedAt(twin.getCreatedAt());
    copy.setLastSeenAt(twin.getLastSeenAt());

    copy.setIdentityUpdatedAt(twin.getIdentityUpdatedAt());
    copy.setNavigationUpdatedAt(twin.getNavigationUpdatedAt());
    copy.setMotionUpdatedAt(twin.getMotionUpdatedAt());
    copy.setPowerUpdatedAt(twin.getPowerUpdatedAt());
    copy.setConnectivityUpdatedAt(twin.getConnectivityUpdatedAt());
    copy.setRelationshipsUpdatedAt(twin.getRelationshipsUpdatedAt());

    return copy;
  }

  // --------------------------------------------------
  // Notifications
  // --------------------------------------------------

  private void notifyAdded(EntityTwin twin, TwinUpdateContext context) {
    for (TwinObserver observer : observers) {
      observer.onTwinAdded(twin, context);
    }
  }

  private void notifyUpdated(EntityTwin previous, EntityTwin current, TwinUpdateContext context) {
    for (TwinObserver observer : observers) {
      observer.onTwinUpdated(previous, current, context);
    }
  }

  private void notifyRemoved(EntityTwin removed, TwinUpdateContext context) {
    for (TwinObserver observer : observers) {
      observer.onTwinRemoved(removed, context);
    }
  }
}