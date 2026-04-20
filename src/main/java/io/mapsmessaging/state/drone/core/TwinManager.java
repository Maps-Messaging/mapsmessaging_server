package io.mapsmessaging.state.drone.core;

import io.mapsmessaging.config.TwinManagerConfig;
import io.mapsmessaging.dto.rest.config.TwinManagerConfigDTO;
import io.mapsmessaging.state.drone.model.LinkState;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class TwinManager {

  private final ConcurrentHashMap<String, EntityTwin> twins = new ConcurrentHashMap<>();
  private final CopyOnWriteArrayList<TwinObserver> observers = new CopyOnWriteArrayList<>();
  private final TwinManagerConfigDTO config;

  public TwinManager() {
    this.config = ConfigurationManager.getInstance().getConfiguration(TwinManagerConfig.class);
  }

  public int getTwinCount() {
    return twins.size();
  }

  public void addObserver(TwinObserver observer) {
    if (observer != null) {
      observers.addIfAbsent(observer);
    }
  }

  public void removeObserver(TwinObserver observer) {
    observers.remove(observer);
  }

  public Optional<EntityTwin> getTwin(String twinId) {
    return Optional.ofNullable(twins.get(twinId));
  }

  public Collection<EntityTwin> listTwins() {
    return Collections.unmodifiableCollection(twins.values());
  }

  public EntityTwin registerTwin(EntityTwin twin, TwinUpdateContext context) {
    Objects.requireNonNull(twin, "twin must not be null");
    Objects.requireNonNull(twin.getTwinId(), "twin.twinId must not be null");

    Instant now = resolveNow(context);

    twin.setCreatedAt(now);
    twin.setLastSeenAt(now);

    if (twin.getLifecycleStatus() == null) {
      twin.setLifecycleStatus(TwinLifecycleStatus.ACTIVE);
    }

    EntityTwin existing = twins.putIfAbsent(twin.getTwinId(), twin);
    if (existing == null) {
      ensureLinkConnected(twin);
      notifyAdded(twin, context);
      return twin;
    }

    synchronized (existing) {
      existing.setLastSeenAt(now);
      transitionStatus(existing, TwinLifecycleStatus.ACTIVE, context);
      ensureLinkConnected(existing);
    }
    return existing;
  }

  public Optional<EntityTwin> removeTwin(String twinId, TwinUpdateContext context) {
    EntityTwin removed = twins.remove(twinId);
    if (removed != null) {
      notifyRemoved(removed, context);
    }
    return Optional.ofNullable(removed);
  }

  public Optional<EntityTwin> updateTwin(String twinId,
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
      previous = shallowCopy(twin);
      updater.accept(twin);
      twin.setLastSeenAt(now);
      transitionStatus(twin, TwinLifecycleStatus.ACTIVE, context);
      ensureLinkConnected(twin);
    }

    notifyUpdated(previous, twin, context);
    return Optional.of(twin);
  }

  public Optional<EntityTwin> upsertRelationship(String twinId,
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

  public int getTwinCountByStatus(TwinLifecycleStatus status) {
    int count = 0;
    for (EntityTwin twin : twins.values()) {
      if (twin.getLifecycleStatus() == status) {
        count++;
      }
    }
    return count;
  }

  public Optional<EntityTwin> removeRelationship(String twinId,
                                                 String sourceTwinId,
                                                 String targetTwinId,
                                                 String relationshipType,
                                                 TwinUpdateContext context) {

    EntityTwin twin = twins.get(twinId);
    if (twin == null) {
      return Optional.empty();
    }

    synchronized (twin) {
      twin.getRelationships().removeIf(existing ->
          Objects.equals(existing.getSourceTwinId(), sourceTwinId) &&
              Objects.equals(existing.getTargetTwinId(), targetTwinId) &&
              Objects.equals(existing.getRelationshipType(), relationshipType)
      );
      twin.setRelationshipsUpdatedAt(resolveNow(context));
      twin.setLastSeenAt(resolveNow(context));
    }

    return Optional.of(twin);
  }

  public void scanTwinStates(Instant now) {
    Instant effectiveNow = now != null ? now : Instant.now();

    for (EntityTwin twin : twins.values()) {
      synchronized (twin) {
        long ageMillis = ageMillis(effectiveNow, twin.getLastSeenAt());

        if (ageMillis >= config.getStaleTimeoutMillis()) {
          transitionStatus(twin, TwinLifecycleStatus.STALE, null);
          ensureLinkDisconnected(twin, "STALE");
        } else if (ageMillis >= config.getHeartbeatTimeoutMillis()) {
          transitionStatus(twin, TwinLifecycleStatus.DISCONNECTED, null);
          ensureLinkDisconnected(twin, "DISCONNECTED");
        } else {
          transitionStatus(twin, TwinLifecycleStatus.ACTIVE, null);
          ensureLinkConnected(twin);
        }
      }
    }
  }

  public int purgeExpiredTwins(Instant now) {
    if (!config.isRemoveExpiredTwins()) {
      return 0;
    }

    Instant effectiveNow = now != null ? now : Instant.now();
    List<String> expiredIds = new ArrayList<>();

    for (EntityTwin twin : twins.values()) {
      long ageMillis = ageMillis(effectiveNow, twin.getLastSeenAt());
      if (ageMillis >= config.getRetentionTimeoutMillis()) {
        expiredIds.add(twin.getTwinId());
      }
    }

    int removedCount = 0;
    for (String twinId : expiredIds) {
      Optional<EntityTwin> removed = removeTwin(twinId, null);
      if (removed.isPresent()) {
        removedCount++;
      }
    }

    return removedCount;
  }

  private long ageMillis(Instant now, Instant then) {
    if (then == null) {
      return Long.MAX_VALUE;
    }
    return Math.max(0L, now.toEpochMilli() - then.toEpochMilli());
  }

  private void transitionStatus(EntityTwin twin,
                                TwinLifecycleStatus newStatus,
                                TwinUpdateContext context) {

    TwinLifecycleStatus previousStatus = twin.getLifecycleStatus();
    if (previousStatus == newStatus) {
      return;
    }

    twin.setLifecycleStatus(newStatus);

    for (TwinObserver observer : observers) {
      observer.onTwinStatusChanged(
          twin.getTwinId(),
          previousStatus,
          newStatus,
          twin,
          context
      );
    }
  }

  private void ensureLinkConnected(EntityTwin twin) {
    LinkState linkState = twin.getLinkState();
    if (linkState == null) {
      linkState = new LinkState();
      twin.setLinkState(linkState);
    }
    linkState.setConnected(true);
    linkState.setState("CONNECTED");
  }

  private void ensureLinkDisconnected(EntityTwin twin, String state) {
    LinkState linkState = twin.getLinkState();
    if (linkState == null) {
      linkState = new LinkState();
      twin.setLinkState(linkState);
    }
    linkState.setConnected(false);
    linkState.setState(state);
  }

  private Instant resolveNow(TwinUpdateContext context) {
    if (context != null && context.getReceivedTime() != null) {
      return context.getReceivedTime();
    }
    return Instant.now();
  }

  private EntityTwin shallowCopy(EntityTwin twin) {
    EntityTwin copy;
    try {
      copy = twin.getClass().getDeclaredConstructor().newInstance();
    } catch (Exception e) {
      throw new IllegalStateException("Failed to copy twin", e);
    }

    copy.setTwinId(twin.getTwinId());
    copy.setTwinType(twin.getTwinType());
    copy.setDisplayName(twin.getDisplayName());
    copy.setTwinPath(twin.getTwinPath());

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

    copy.setLifecycleStatus(twin.getLifecycleStatus());

    return copy;
  }

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