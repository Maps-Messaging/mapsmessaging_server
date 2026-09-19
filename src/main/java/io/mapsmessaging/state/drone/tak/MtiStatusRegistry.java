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

  private static volatile Lookup delegate;

  private MtiStatusRegistry() {
  }

  public static void setDelegate(Lookup lookup) {
    delegate = lookup;
  }

  public static MtiLookupResult lookup(String twinId) {
    Lookup current = delegate;
    return current == null || twinId == null ? null : current.lookup(twinId);
  }
}
