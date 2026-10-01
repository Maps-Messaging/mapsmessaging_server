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
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Static bridge between {@link CotEventPolicy} (constructed by {@code TakTwinObserver} early in
 * server startup, inside {@code StateManagerAgent}'s constructor) and an MTI-feed
 * {@code StateMessageAdapter} (an external SPI jar, loaded later by
 * {@code StateManagerAgent.loadStateMessageAdapters()}). By the time adapters load, the
 * {@code CotEventPolicy} instance that will call {@link #lookup} already exists - there's no
 * constructor-injection path between them - so the adapter registers itself here at its own
 * {@code start()} instead.
 *
 * <p>No twin is ever required to have an MTI status; {@link #lookup} returns {@code null} both
 * before any adapter has registered and for any twin the registered adapter doesn't know about -
 * {@link CotEventPolicy} treats a {@code null} result as "render this twin exactly as it would
 * have without MTI in the picture at all."
 */
public final class MtiStatusRegistry {

  /** Implemented by whatever MTI-feed adapter is currently registered. */
  public interface Lookup {
    /**
     * @param twinId the twin's {@code EntityTwin.getTwinId()} - the MTI wire schema's own sample
     *     payloads use human-readable asset identifiers (e.g. {@code "UAS-ALPHA-07"}), not
     *     UUID-formatted strings, so this correlates on twinId, not twin.getUuid().
     * @return the current MTI view of that twin, or {@code null} if the adapter has none.
     */
    MtiLookupResult lookup(String twinId);
  }

  /**
   * Side-effect-free view of the adapter's cache for metrics. Must not touch lookup hit/miss
   * counters or evict entries, otherwise every metrics scrape would skew the CoT-composition KPIs.
   */
  public interface SnapshotSource {
    /** @return the current, unexpired MTI status of that twin, or {@code null} if none. */
    MtiStatusSnapshot snapshot(String twinId);
  }

  /**
   * Told when the adapter accepts or explicitly clears an MTI status, so recovery timing can tell
   * "MTI data is available again" apart from "MTI deliberately withdrew this asset's status".
   */
  public interface StatusListener {
    void onStatusAccepted(String twinId, Instant receivedAt);

    void onStatusCleared(String twinId);
  }

  private static volatile Lookup delegate;
  private static volatile SnapshotSource snapshotSource;
  private static final List<StatusListener> STATUS_LISTENERS = new CopyOnWriteArrayList<>();
  private static volatile Runnable cacheClearer;

  private MtiStatusRegistry() {
  }

  public static void setDelegate(Lookup lookup) {
    delegate = lookup;
  }

  public static void setSnapshotSource(SnapshotSource source) {
    snapshotSource = source;
  }

  public static void addStatusListener(StatusListener listener) {
    if (listener != null) {
      STATUS_LISTENERS.add(listener);
    }
  }

  public static void removeStatusListener(StatusListener listener) {
    STATUS_LISTENERS.remove(listener);
  }

  public static void statusAccepted(String twinId, Instant receivedAt) {
    if (twinId == null) {
      return;
    }
    for (StatusListener listener : STATUS_LISTENERS) {
      listener.onStatusAccepted(twinId, receivedAt);
    }
  }

  public static void statusCleared(String twinId) {
    if (twinId == null) {
      return;
    }
    for (StatusListener listener : STATUS_LISTENERS) {
      listener.onStatusCleared(twinId);
    }
  }

  /** Registered by the MTI adapter; lets a controlled cache-loss test empty the MTI cache. */
  public static void setCacheClearer(Runnable clearer) {
    cacheClearer = clearer;
  }

  /** @return true if an MTI adapter was registered and its cache was cleared. */
  public static boolean clearCache() {
    Runnable current = cacheClearer;
    if (current == null) {
      return false;
    }
    current.run();
    return true;
  }

  public static MtiLookupResult lookup(String twinId) {
    Lookup current = delegate;
    return current == null || twinId == null ? null : current.lookup(twinId);
  }

  public static MtiStatusSnapshot snapshot(String twinId) {
    SnapshotSource current = snapshotSource;
    return current == null || twinId == null ? null : current.snapshot(twinId);
  }
}
