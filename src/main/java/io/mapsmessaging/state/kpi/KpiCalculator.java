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
import io.mapsmessaging.state.drone.tak.MtiStatusSnapshot;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/** Stateless KPI arithmetic: per-asset assessment and the roster-wide KPI values. */
final class KpiCalculator {

  private KpiCalculator() {
  }

  /**
   * @param lastSeenAt when the asset last reported, {@code null} if it has no twin (any more)
   * @param mti MTI's current, unexpired status, {@code null} if none
   * @param announced whether MTI ever sent a status for the asset that it has not withdrawn
   * @param classification the last composed classification, {@code null} if none yet
   * @param expectedType the scripted asset's expected CoT type, {@code null} if not scripted
   */
  static AssetAssessment assess(
      String uid,
      boolean eligible,
      Instant lastSeenAt,
      MtiStatusSnapshot mti,
      boolean announced,
      ClassificationRegistry.Classification classification,
      String expectedType,
      KpiConfig config,
      Instant now) {
    boolean positionFresh = lastSeenAt != null && !lastSeenAt.isBefore(now.minus(config.getPositionMaxAge()));
    Boolean asExpected = expectedType == null || classification == null
        ? null
        : matchesExpected(classification.baseCotType(), expectedType);
    return new AssetAssessment(
        uid,
        eligible,
        positionFresh,
        mtiCondition(mti, announced, config.getMtiMaxAge(), now),
        classification == null ? null : classification.outcome(),
        asExpected);
  }

  static AssetAssessment.MtiCondition mtiCondition(MtiStatusSnapshot mti, boolean announced, Duration maxAge, Instant now) {
    if (mti == null) {
      return announced ? AssetAssessment.MtiCondition.STALE : AssetAssessment.MtiCondition.NONE;
    }
    if (mti.observedAt() == null || mti.observedAt().isBefore(now.minus(maxAge))) {
      return AssetAssessment.MtiCondition.STALE;
    }
    String state = mti.state() == null ? "" : mti.state().toLowerCase(Locale.ROOT);
    return switch (state) {
      case "go" -> AssetAssessment.MtiCondition.GO;
      case "mitigate" -> AssetAssessment.MtiCondition.MITIGATE;
      case "hold" -> AssetAssessment.MtiCondition.HOLD;
      case "unknown" -> AssetAssessment.MtiCondition.UNKNOWN;
      default -> AssetAssessment.MtiCondition.OTHER;
    };
  }

  /** Segment-wise comparison; {@code *} in the expected type matches any single segment. */
  static boolean matchesExpected(String actualType, String expectedType) {
    if (actualType == null) {
      return false;
    }
    String[] actual = actualType.split("-");
    String[] expected = expectedType.split("-");
    if (actual.length != expected.length) {
      return false;
    }
    for (int index = 0; index < expected.length; index++) {
      if (!"*".equals(expected[index]) && !expected[index].equalsIgnoreCase(actual[index])) {
        return false;
      }
    }
    return true;
  }

  static KpiSnapshot compute(List<AssetAssessment> assets) {
    int roster = assets.size();
    int eligible = 0;
    int go = 0;
    int covered = 0;
    int actionable = 0;
    int mtiStale = 0;
    int positionStale = 0;
    int unknown = 0;
    int degraded = 0;
    int unknownOrDegraded = 0;
    int specific = 0;
    int fallback = 0;
    int failure = 0;
    int scripted = 0;
    int asExpected = 0;
    Map<AssetAssessment.UnknownReason, Integer> reasons = new EnumMap<>(AssetAssessment.UnknownReason.class);
    for (AssetAssessment.UnknownReason reason : AssetAssessment.UnknownReason.values()) {
      reasons.put(reason, 0);
    }
    Set<String> freshMti = new TreeSet<>();

    for (AssetAssessment asset : assets) {
      if (asset.eligible()) {
        eligible++;
        if (asset.mti() == AssetAssessment.MtiCondition.GO) {
          go++;
        }
        if (asset.mti().isFresh()) {
          covered++;
          freshMti.add(asset.uid());
        } else {
          mtiStale++;
        }
        if (asset.mti().isActionable()) {
          actionable++;
        }
      }
      if (!asset.positionFresh()) {
        positionStale++;
      }
      Set<AssetAssessment.UnknownReason> assetReasons = asset.unknownReasons();
      assetReasons.forEach(reason -> reasons.merge(reason, 1, Integer::sum));
      boolean isUnknown = !assetReasons.isEmpty();
      if (isUnknown) {
        unknown++;
      }
      if (asset.isDegraded()) {
        degraded++;
      }
      if (isUnknown || asset.isDegraded()) {
        unknownOrDegraded++;
      }
      if (asset.isClassificationSpecific()) {
        specific++;
      } else {
        failure++;
      }
      if (asset.classification() == ClassificationRegistry.Outcome.FALLBACK) {
        fallback++;
      }
      if (asset.classifiedAsExpected() != null) {
        scripted++;
        if (asset.classifiedAsExpected()) {
          asExpected++;
        }
      }
    }

    Map<KpiId, KpiSnapshot.Value> values = new EnumMap<>(KpiId.class);
    values.put(KpiId.READINESS, new KpiSnapshot.Value(go, eligible));
    values.put(KpiId.COVERAGE, new KpiSnapshot.Value(covered, eligible));
    values.put(KpiId.UNKNOWN_DEGRADED, new KpiSnapshot.Value(unknownOrDegraded, roster));
    values.put(KpiId.POSITION_STALE, new KpiSnapshot.Value(positionStale, roster));
    values.put(KpiId.CLASSIFICATION_TRUST, new KpiSnapshot.Value(specific, roster));
    values.put(KpiId.ACTIONABLE_TRUST, new KpiSnapshot.Value(actionable, eligible));
    values.put(KpiId.MTI_STALE, new KpiSnapshot.Value(mtiStale, eligible));
    values.put(KpiId.UNKNOWN, new KpiSnapshot.Value(unknown, roster));
    values.put(KpiId.DEGRADED, new KpiSnapshot.Value(degraded, roster));
    values.put(KpiId.CLASSIFICATION_FALLBACK, new KpiSnapshot.Value(fallback, roster));
    values.put(KpiId.CLASSIFICATION_FAILURE, new KpiSnapshot.Value(failure, roster));
    values.put(KpiId.CLASSIFICATION_CORRECTNESS, new KpiSnapshot.Value(asExpected, scripted));
    return new KpiSnapshot(values, roster, eligible, reasons, freshMti);
  }
}
