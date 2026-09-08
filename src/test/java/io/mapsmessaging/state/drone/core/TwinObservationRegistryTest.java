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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class TwinObservationRegistryTest {

  @Test
  void lateObservationFromSameSource_isRejected() {
    TwinObservationRegistry registry = new TwinObservationRegistry();
    Instant now = Instant.parse("2026-09-08T10:00:00Z");

    assertTrue(registry.record("alpha", observation("cot", "tak-a", 50, now, 2.0), now));
    assertFalse(registry.record("alpha", observation("cot", "tak-a", 50, now.minusSeconds(1), 1.0), now));
    assertEquals(2.0, registry.resolve("alpha", "position.latitude", now).orElseThrow().getValue());
  }

  @Test
  void priorityWinsUntilHigherPriorityObservationExpires() {
    TwinObservationRegistry registry = new TwinObservationRegistry();
    Instant now = Instant.parse("2026-09-08T10:00:00Z");
    TwinFieldObservation lower = observation("cot", "tak-a", 50, now, 1.0);
    TwinFieldObservation higher = observation("stanag", "catl", 100, now.minusSeconds(5), 2.0);
    higher = TwinFieldObservation.builder()
        .field(higher.getField())
        .value(higher.getValue())
        .sourceProtocol(higher.getSourceProtocol())
        .sourceEndpoint(higher.getSourceEndpoint())
        .sourceId(higher.getSourceId())
        .observedAt(higher.getObservedAt())
        .receivedAt(now)
        .validUntil(now.plusSeconds(5))
        .priority(higher.getPriority())
        .origin(higher.getOrigin())
        .build();

    registry.record("alpha", lower, now);
    registry.record("alpha", higher, now);

    assertEquals(2.0, registry.resolve("alpha", "position.latitude", now).orElseThrow().getValue());
    assertEquals(1.0, registry.resolve("alpha", "position.latitude", now.plusSeconds(6)).orElseThrow().getValue());
  }

  private TwinFieldObservation observation(
      String protocol, String endpoint, int priority, Instant observedAt, double value) {
    return TwinFieldObservation.builder()
        .field("position.latitude")
        .value(value)
        .sourceProtocol(protocol)
        .sourceEndpoint(endpoint)
        .sourceId("vehicle-1")
        .observedAt(observedAt)
        .receivedAt(observedAt)
        .validUntil(observedAt.plusSeconds(60))
        .priority(priority)
        .origin(TwinObservationOrigin.OBSERVED)
        .build();
  }
}
