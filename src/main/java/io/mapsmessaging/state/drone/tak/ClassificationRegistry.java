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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Last classification outcome per asset, recorded by {@link CotEventPolicy} each time it composes a
 * CoT event and read by the KPI evaluator. Per asset rather than per event, so "unresolved
 * classification" can be one of the reasons an asset counts as unknown (IC26 KPI clarification Q4).
 *
 * <p>Outcomes follow clarification Q19: keeping a valid original CoT type is a fallback, not a
 * failure; only a generic/unresolved classification is a hard failure.
 */
public final class ClassificationRegistry {

  public enum Outcome {
    /** Per-asset configured classification or a resolved vehicle class. */
    RESOLVED,
    /** Kept the specific CoT type the asset arrived with (CoT-ingested assets). */
    FALLBACK,
    /** Generic or unresolved classification - the hard failure. */
    UNRESOLVED
  }

  /** @param baseCotType the CoT type before any MTI affiliation override. */
  public record Classification(Outcome outcome, String baseCotType) {
  }

  private static final Map<String, Classification> LAST = new ConcurrentHashMap<>();

  private ClassificationRegistry() {
  }

  static void record(String twinId, Outcome outcome, String baseCotType) {
    if (twinId != null && outcome != null) {
      LAST.put(twinId, new Classification(outcome, baseCotType));
    }
  }

  /** @return the last classification composed for the asset, or {@code null} if none yet. */
  public static Classification get(String twinId) {
    return twinId == null ? null : LAST.get(twinId);
  }

  /** Called by the KPI evaluator for assets that have left the mission roster. */
  public static void forget(String twinId) {
    if (twinId != null) {
      LAST.remove(twinId);
    }
  }

  /**
   * A CoT type is generic when it stops at the battle dimension (e.g. {@code a-f-A}, what
   * {@code CotTypeResolver} produces when neither a vehicle class nor a 2525D symbol resolves) or
   * carries no dimension at all - it says nothing about what the asset is.
   */
  static boolean isGeneric(String cotType) {
    if (cotType == null || cotType.isBlank()) {
      return true;
    }
    return cotType.split("-").length < 4;
  }

  public static void clearForTest() {
    LAST.clear();
  }
}
