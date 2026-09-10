/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     https://commonsclause.com/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.mapsmessaging.state.drone.core;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Stores competing protocol observations without putting protocol state on the twin model. */
public class TwinObservationRegistry {

  private static final Comparator<TwinFieldObservation> PRECEDENCE =
      Comparator.comparingInt(TwinFieldObservation::getPriority)
          .thenComparing(TwinFieldObservation::getObservedAt, Comparator.nullsFirst(Comparator.naturalOrder()))
          .thenComparing(TwinFieldObservation::getReceivedAt, Comparator.nullsFirst(Comparator.naturalOrder()));

  private final Map<String, Map<String, Map<String, TwinFieldObservation>>> observations =
      new ConcurrentHashMap<>();

  public boolean record(String twinId, TwinFieldObservation observation, Instant effectiveTime) {
    Map<String, TwinFieldObservation> sources = observations
        .computeIfAbsent(twinId, ignored -> new ConcurrentHashMap<>())
        .computeIfAbsent(observation.getField(), ignored -> new ConcurrentHashMap<>());
    synchronized (sources) {
      TwinFieldObservation previous = sources.get(observation.sourceKey());
      if (previous != null && compareEventOrder(observation, previous) <= 0) {
        return false;
      }
      Optional<TwinFieldObservation> before = resolve(sources, effectiveTime);
      sources.put(observation.sourceKey(), observation);
      Optional<TwinFieldObservation> after = resolve(sources, effectiveTime);
      return after.isPresent() && after.get() == observation
          && (before.isEmpty() || before.get() != observation);
    }
  }

  public Optional<TwinFieldObservation> resolve(String twinId, String field, Instant effectiveTime) {
    Map<String, Map<String, TwinFieldObservation>> fields = observations.get(twinId);
    if (fields == null) {
      return Optional.empty();
    }
    Map<String, TwinFieldObservation> sources = fields.get(field);
    return sources == null ? Optional.empty() : resolve(sources, effectiveTime);
  }

  public List<TwinFieldObservation> list(String twinId) {
    Map<String, Map<String, TwinFieldObservation>> fields = observations.get(twinId);
    if (fields == null) {
      return List.of();
    }
    List<TwinFieldObservation> result = new ArrayList<>();
    fields.values().forEach(values -> result.addAll(values.values()));
    return List.copyOf(result);
  }

  private Optional<TwinFieldObservation> resolve(
      Map<String, TwinFieldObservation> sources, Instant effectiveTime) {
    return sources.values().stream()
        .filter(observation -> observation.isValidAt(effectiveTime))
        .max(PRECEDENCE);
  }

  private int compareEventOrder(TwinFieldObservation candidate, TwinFieldObservation previous) {
    Instant candidateTime = candidate.getObservedAt();
    Instant previousTime = previous.getObservedAt();
    if (candidateTime == null && previousTime == null) {
      return 0;
    }
    if (candidateTime == null) {
      return -1;
    }
    if (previousTime == null) {
      return 1;
    }
    return candidateTime.compareTo(previousTime);
  }
}
