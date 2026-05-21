package io.mapsmessaging.state.drone.core;


import io.mapsmessaging.state.drone.model.LinkState;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;

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

import static io.mapsmessaging.logging.ServerLogMessages.*;

public class TwinManager {

  private final ConcurrentHashMap<String, EntityTwin> twins = new ConcurrentHashMap<>();
  private final CopyOnWriteArrayList<TwinObserver> observers = new CopyOnWriteArrayList<>();
  private final Logger logger = LoggerFactory.getLogger(TwinManager.class);
  private final boolean removeExpiredTwins;
  private final long staleTimeoutMillis;
  private final long heartbeatTimeoutMillis;
  private final long retentionTimeoutMillis;

  public TwinManager() {
    this(true, 10000L, 5000L, 120000L);
  }

  public TwinManager(boolean removeExpiredTwins, long staleTimeoutMillis, long heartbeatTimeoutMillis, long retentionTimeoutMillis) {
    this.removeExpiredTwins = removeExpiredTwins;
    this.staleTimeoutMillis = staleTimeoutMillis;
    this.heartbeatTimeoutMillis = heartbeatTimeoutMillis;
    this.retentionTimeoutMillis = retentionTimeoutMillis;
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
    logger.log(TWIN_REGISTERED, twin.getTwinId(), twin.getTwinType().name());
    Instant now = resolveNow(context);

    EntityTwin existing = twins.putIfAbsent(twin.getTwinId(), twin);
    if (existing == null) {
      synchronized (twin) {
        if (twin.getCreatedAt() == null) {
          twin.setCreatedAt(now);
        }
        twin.setLastSeenAt(now);

        if (twin.getLifecycleStatus() == null) {
          twin.setLifecycleStatus(TwinLifecycleStatus.ACTIVE);
        }

        ensureLinkConnected(twin);
      }
      notifyAdded(twin, context);
      return twin;
    }
    else{
      logger.log(TWIN_REGISTER_EXISTING, twin.getTwinId());
    }

    synchronized (existing) {
      existing.setLastSeenAt(now);
      transitionStatus(existing, TwinLifecycleStatus.ACTIVE, context);
      ensureLinkConnected(existing);
    }

    return existing;
  }

  public Optional<EntityTwin> removeTwin(String twinId, TwinUpdateContext context) {
    Objects.requireNonNull(twinId, "twinId must not be null");

    EntityTwin removed = twins.remove(twinId);
    if (removed != null) {
      notifyRemoved(removed, context);
      logger.log(TWIN_REMOVED, twinId);
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

    synchronized (twin) {
      updater.accept(twin);
      twin.setLastSeenAt(now);
      transitionStatus(twin, TwinLifecycleStatus.ACTIVE, context);
      ensureLinkConnected(twin);
    }

    notifyUpdated(twin, context);
    logger.log(TWIN_UPDATED, twinId);
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
      transitionStatus(twin, TwinLifecycleStatus.ACTIVE, context);
      ensureLinkConnected(twin);
      logger.log(TWIN_RELATIONSHIP_UPSERTED, twinId, relationship.getSourceTwinId(), relationship.getTargetTwinId(), relationship.getRelationshipType());
    }

    notifyRelationshipUpdated(twinId, relationship, context);
    return Optional.of(twin);
  }

  public Optional<EntityTwin> removeRelationship(String twinId,
                                                 String sourceTwinId,
                                                 String targetTwinId,
                                                 String relationshipType,
                                                 TwinUpdateContext context) {

    Objects.requireNonNull(twinId, "twinId must not be null");

    EntityTwin twin = twins.get(twinId);
    if (twin == null) {
      return Optional.empty();
    }

    Instant now = resolveNow(context);
    TwinRelationship removedRelationship = null;

    synchronized (twin) {
      for (TwinRelationship existing : twin.getRelationships()) {
        if (Objects.equals(existing.getSourceTwinId(), sourceTwinId) &&
            Objects.equals(existing.getTargetTwinId(), targetTwinId) &&
            Objects.equals(existing.getRelationshipType(), relationshipType)) {
          removedRelationship = existing;
          break;
        }
      }

      if (removedRelationship != null) {
        twin.getRelationships().remove(removedRelationship);
        twin.setRelationshipsUpdatedAt(now);
        twin.setLastSeenAt(now);
      }
    }

    if (removedRelationship != null) {
      notifyRelationshipRemoved(twinId, removedRelationship, context);
    }
    logger.log(TWIN_RELATIONSHIP_REMOVED, twinId, sourceTwinId, targetTwinId, relationshipType);
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

  public void scanTwinStates(Instant now) {
    Instant effectiveNow = now != null ? now : Instant.now();

    for (EntityTwin twin : twins.values()) {
      synchronized (twin) {
        long ageMillis = ageMillis(effectiveNow, twin.getLastSeenAt());

        if (ageMillis >= staleTimeoutMillis) {
          transitionStatus(twin, TwinLifecycleStatus.STALE, null);
          ensureLinkDisconnected(twin, "STALE");
        } else if (ageMillis >= heartbeatTimeoutMillis) {
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
    if (!removeExpiredTwins) {
      return 0;
    }

    Instant effectiveNow = now != null ? now : Instant.now();
    List<String> expiredIds = new ArrayList<>();

    for (EntityTwin twin : twins.values()) {
      long ageMillis = ageMillis(effectiveNow, twin.getLastSeenAt());
      if (ageMillis >= retentionTimeoutMillis) {
        expiredIds.add(twin.getTwinId());
      }
    }

    int removedCount = 0;
    for (String twinId : expiredIds) {
      Optional<EntityTwin> removed = removeTwin(twinId, null);
      if (removed.isPresent()) {
        logger.log(TWIN_PURGED, twinId);
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
    logger.log(TWIN_STATUS_CHANGED, twin.getTwinId(), previousStatus.name(), newStatus.name());
    for (TwinObserver observer : observers) {
      try {
        observer.onTwinStatusChanged(
            twin.getTwinId(),
            previousStatus,
            newStatus,
            twin,
            context
        );
      } catch (Exception ignore) {
        logger.log(TWIN_OBSERVER_CALLBACK_FAILED, ignore);
      }
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

  private void notifyAdded(EntityTwin twin, TwinUpdateContext context) {
    for (TwinObserver observer : observers) {
      try {
        observer.onTwinAdded(twin, context);
      } catch (Exception ignore) {
        logger.log(TWIN_OBSERVER_CALLBACK_FAILED, ignore);
      }
    }
  }

  private void notifyUpdated(EntityTwin twin, TwinUpdateContext context) {
    for (TwinObserver observer : observers) {
      try {
        observer.onTwinUpdated(twin.getTwinId(), twin, context);
      } catch (Exception ignore) {
        logger.log(TWIN_OBSERVER_CALLBACK_FAILED, ignore);
      }
    }
  }

  private void notifyRemoved(EntityTwin removed, TwinUpdateContext context) {
    for (TwinObserver observer : observers) {
      try {
        observer.onTwinRemoved(removed, context);
      } catch (Exception ignore) {
        logger.log(TWIN_OBSERVER_CALLBACK_FAILED, ignore);
      }
    }
  }

  private void notifyRelationshipUpdated(String twinId,
                                         TwinRelationship relationship,
                                         TwinUpdateContext context) {
    for (TwinObserver observer : observers) {
      try {
        observer.onRelationshipUpdated(twinId, relationship, context);
      } catch (Exception ignore) {
        logger.log(TWIN_OBSERVER_CALLBACK_FAILED, ignore);
      }
    }
  }

  private void notifyRelationshipRemoved(String twinId,
                                         TwinRelationship relationship,
                                         TwinUpdateContext context) {
    for (TwinObserver observer : observers) {
      try {
        observer.onRelationshipRemoved(twinId, relationship, context);
      } catch (Exception ignore) {
        logger.log(TWIN_OBSERVER_CALLBACK_FAILED, ignore);
      }
    }
  }
}