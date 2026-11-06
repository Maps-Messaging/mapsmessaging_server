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

package io.mapsmessaging.state.kpi;

import io.mapsmessaging.state.drone.tak.ClassificationRegistry;

import java.util.EnumSet;
import java.util.Set;

/**
 * KPI view of one asset on the mission roster at one evaluation.
 *
 * @param uid the asset's twin id
 * @param eligible whether MTI is expected to cover the asset (clarification Q2)
 * @param positionFresh whether its position/telemetry is within the freshness threshold (Q5)
 * @param mti its MTI condition
 * @param classification its last classification outcome, {@code null} if no CoT was composed yet
 * @param classifiedAsExpected for scripted assets, whether the classification matched the expected
 *     one; {@code null} for assets with no expected classification or no classification yet
 */
public record AssetAssessment(
    String uid,
    boolean eligible,
    boolean positionFresh,
    MtiCondition mti,
    ClassificationRegistry.Outcome classification,
    Boolean classifiedAsExpected) {

  public enum MtiCondition {
    GO,
    MITIGATE,
    HOLD,
    UNKNOWN,
    /** A state outside MTI's go/mitigate/hold/unknown alphabet. */
    OTHER,
    /** MTI had a status for the asset, but it expired or is older than the freshness threshold. */
    STALE,
    /** MTI never sent a status for the asset (or withdrew it). */
    NONE;

    /** A fresh MTI record exists - counts as covered, including an explicit UNKNOWN (Q3). */
    boolean isFresh() {
      return this != STALE && this != NONE;
    }

    /** A fresh, actionable trust opinion (Q3). */
    boolean isActionable() {
      return this == GO || this == MITIGATE || this == HOLD;
    }
  }

  /** The four concrete reasons an asset counts as unknown (clarification Q4). */
  public enum UnknownReason {
    NO_MTI,
    MTI_UNKNOWN,
    MTI_STALE,
    UNRESOLVED_CLASSIFICATION
  }

  /** MTI reasons apply to MTI-eligible assets only; an asset can have more than one reason. */
  public Set<UnknownReason> unknownReasons() {
    Set<UnknownReason> reasons = EnumSet.noneOf(UnknownReason.class);
    if (eligible) {
      switch (mti) {
        case NONE -> reasons.add(UnknownReason.NO_MTI);
        case UNKNOWN -> reasons.add(UnknownReason.MTI_UNKNOWN);
        case STALE -> reasons.add(UnknownReason.MTI_STALE);
        default -> {
          // GO, MITIGATE, HOLD and OTHER are opinions, not unknowns
        }
      }
    }
    if (!isClassificationSpecific()) {
      reasons.add(UnknownReason.UNRESOLVED_CLASSIFICATION);
    }
    return reasons;
  }

  /** Fresh MITIGATE or HOLD from MTI. */
  public boolean isDegraded() {
    return eligible && (mti == MtiCondition.MITIGATE || mti == MtiCondition.HOLD);
  }

  /** Resolved, or kept a valid original CoT type - fallback is not a failure (Q19). */
  public boolean isClassificationSpecific() {
    return classification == ClassificationRegistry.Outcome.RESOLVED
        || classification == ClassificationRegistry.Outcome.FALLBACK;
  }
}
