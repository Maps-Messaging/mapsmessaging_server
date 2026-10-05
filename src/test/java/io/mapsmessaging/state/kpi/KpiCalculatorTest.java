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

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.state.drone.tak.ClassificationRegistry;
import io.mapsmessaging.state.drone.tak.MtiStatusSnapshot;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.mapsmessaging.state.kpi.AssetAssessment.MtiCondition.*;
import static io.mapsmessaging.state.kpi.AssetAssessment.UnknownReason.*;
import static org.junit.jupiter.api.Assertions.*;

class KpiCalculatorTest {

  private static final Instant NOW = Instant.parse("2026-10-01T10:00:00Z");
  private static final ClassificationRegistry.Outcome RESOLVED = ClassificationRegistry.Outcome.RESOLVED;

  private final KpiConfig config = KpiConfig.from(new ConfigurationProperties());

  private static AssetAssessment asset(String uid, boolean eligible, AssetAssessment.MtiCondition mti) {
    return new AssetAssessment(uid, eligible, true, mti, RESOLVED, null);
  }

  @Test
  void readiness_countsOnlyFreshGo_overAllEligibleAssets() {
    // Stakeholder correction: no status, stale status and UNKNOWN must not look ready.
    KpiSnapshot snapshot = KpiCalculator.compute(List.of(
        asset("go", true, GO),
        asset("none", true, NONE),
        asset("stale", true, STALE),
        asset("unknown", true, UNKNOWN)));

    assertEquals(0.25, snapshot.value(KpiId.READINESS).value());
    assertEquals(4, snapshot.value(KpiId.READINESS).denominator());
  }

  @Test
  void coverage_includesExplicitUnknown_actionableTrustDoesNot() {
    KpiSnapshot snapshot = KpiCalculator.compute(List.of(
        asset("go", true, GO),
        asset("hold", true, HOLD),
        asset("unknown", true, UNKNOWN),
        asset("stale", true, STALE)));

    assertEquals(0.75, snapshot.value(KpiId.COVERAGE).value());
    assertEquals(0.5, snapshot.value(KpiId.ACTIONABLE_TRUST).value());
    assertEquals(0.25, snapshot.value(KpiId.MTI_STALE).value());
    assertEquals(Set.of("go", "hold", "unknown"), snapshot.freshMtiUids());
  }

  @Test
  void assetsOutsideMtiScope_doNotPullCoverageDown() {
    KpiSnapshot snapshot = KpiCalculator.compute(List.of(
        asset("own-drone", true, GO),
        asset("partner-contact", false, NONE)));

    assertEquals(1.0, snapshot.value(KpiId.COVERAGE).value());
    assertEquals(1, snapshot.eligibleSize());
    assertEquals(2, snapshot.rosterSize());
    assertEquals(0, snapshot.unknownReasons().get(NO_MTI));
  }

  @Test
  void unknownAssets_countedOnceInTotal_butUnderEveryReason() {
    AssetAssessment staleAndUnclassified = new AssetAssessment("a", true, true, STALE, ClassificationRegistry.Outcome.UNRESOLVED, null);
    AssetAssessment noMti = asset("b", true, NONE);
    AssetAssessment healthy = asset("c", true, GO);

    KpiSnapshot snapshot = KpiCalculator.compute(List.of(staleAndUnclassified, noMti, healthy));

    assertEquals(2, snapshot.value(KpiId.UNKNOWN).numerator());
    assertEquals(Map.of(NO_MTI, 1, MTI_UNKNOWN, 0, MTI_STALE, 1, UNRESOLVED_CLASSIFICATION, 1), snapshot.unknownReasons());
  }

  @Test
  void unknownDegraded_combinesUnknownAndMitigateOrHold() {
    KpiSnapshot snapshot = KpiCalculator.compute(List.of(
        asset("mitigate", true, MITIGATE),
        asset("unknown", true, UNKNOWN),
        asset("go-1", true, GO),
        asset("go-2", true, GO)));

    assertEquals(0.5, snapshot.value(KpiId.UNKNOWN_DEGRADED).value());
    assertEquals(1, snapshot.value(KpiId.DEGRADED).numerator());
    assertEquals(1, snapshot.value(KpiId.UNKNOWN).numerator());
  }

  @Test
  void fallbackToOriginalType_isNotAClassificationFailure() {
    KpiSnapshot snapshot = KpiCalculator.compute(List.of(
        new AssetAssessment("fallback", false, true, NONE, ClassificationRegistry.Outcome.FALLBACK, null),
        new AssetAssessment("generic", false, true, NONE, ClassificationRegistry.Outcome.UNRESOLVED, null),
        new AssetAssessment("never-composed", false, true, NONE, null, null),
        new AssetAssessment("resolved", false, true, NONE, RESOLVED, null)));

    assertEquals(0.5, snapshot.value(KpiId.CLASSIFICATION_TRUST).value());
    assertEquals(1, snapshot.value(KpiId.CLASSIFICATION_FALLBACK).numerator());
    assertEquals(2, snapshot.value(KpiId.CLASSIFICATION_FAILURE).numerator());
  }

  @Test
  void positionStale_isIndependentOfMtiFreshness() {
    KpiSnapshot snapshot = KpiCalculator.compute(List.of(
        new AssetAssessment("fresh-location-stale-trust", true, true, STALE, RESOLVED, null),
        new AssetAssessment("stale-location-fresh-trust", true, false, GO, RESOLVED, null)));

    assertEquals(0.5, snapshot.value(KpiId.POSITION_STALE).value());
    assertEquals(0.5, snapshot.value(KpiId.MTI_STALE).value());
  }

  @Test
  void emptyRoster_givesNoDataInsteadOfAPerfectScore() {
    KpiSnapshot snapshot = KpiCalculator.compute(List.of());

    assertTrue(Double.isNaN(snapshot.value(KpiId.READINESS).value()));
    assertTrue(Double.isNaN(snapshot.value(KpiId.COVERAGE).value()));
  }

  @Test
  void mtiCondition_expiredButAnnounced_isStale_neverSeen_isNone() {
    assertEquals(STALE, KpiCalculator.mtiCondition(null, true, config.getMtiMaxAge(), NOW));
    assertEquals(NONE, KpiCalculator.mtiCondition(null, false, config.getMtiMaxAge(), NOW));
  }

  @Test
  void mtiCondition_olderThanFreshnessThreshold_isStale_evenIfStillValid() {
    MtiStatusSnapshot old = new MtiStatusSnapshot("go", NOW.minusSeconds(121), NOW.plusSeconds(600));
    MtiStatusSnapshot recent = new MtiStatusSnapshot("GO", NOW.minusSeconds(120), NOW.plusSeconds(600));

    assertEquals(STALE, KpiCalculator.mtiCondition(old, true, config.getMtiMaxAge(), NOW));
    assertEquals(GO, KpiCalculator.mtiCondition(recent, true, config.getMtiMaxAge(), NOW));
  }

  @Test
  void mtiCondition_stateOutsideAlphabet_isOther_andCountsAsCovered() {
    MtiStatusSnapshot odd = new MtiStatusSnapshot("quarantine", NOW, NOW.plusSeconds(60));
    assertEquals(OTHER, KpiCalculator.mtiCondition(odd, true, config.getMtiMaxAge(), NOW));
    assertTrue(OTHER.isFresh());
    assertFalse(OTHER.isActionable());
  }

  @Test
  void assess_positionFreshness_usesConfiguredMaxAge() {
    AssetAssessment fresh = KpiCalculator.assess("a", true, NOW.minusSeconds(30), null, false, null, null, config, NOW);
    AssetAssessment stale = KpiCalculator.assess("a", true, NOW.minusSeconds(31), null, false, null, null, config, NOW);
    AssetAssessment gone = KpiCalculator.assess("a", true, null, null, false, null, null, config, NOW);

    assertTrue(fresh.positionFresh());
    assertFalse(stale.positionFresh());
    assertFalse(gone.positionFresh());
  }

  @Test
  void classificationCorrectness_comparesBaseTypeWithWildcards() {
    assertTrue(KpiCalculator.matchesExpected("a-f-A-M-F-Q", "a-*-A-M-F-Q"));
    assertFalse(KpiCalculator.matchesExpected("a-f-A-M-F", "a-*-A-M-F-Q"));
    assertFalse(KpiCalculator.matchesExpected("a-f-S-C-U", "a-*-A-M-F-Q"));
    assertFalse(KpiCalculator.matchesExpected(null, "a-*-A-M-F-Q"));

    ClassificationRegistry.Classification uav = new ClassificationRegistry.Classification(RESOLVED, "a-f-A-M-F-Q");
    AssetAssessment scripted = KpiCalculator.assess("uas", true, NOW, null, false, uav, "a-*-A-M-F-Q", config, NOW);
    AssetAssessment notScripted = KpiCalculator.assess("uas", true, NOW, null, false, uav, null, config, NOW);
    KpiSnapshot snapshot = KpiCalculator.compute(List.of(scripted, notScripted));

    assertEquals(Boolean.TRUE, scripted.classifiedAsExpected());
    assertNull(notScripted.classifiedAsExpected());
    assertEquals(1, snapshot.value(KpiId.CLASSIFICATION_CORRECTNESS).denominator());
    assertEquals(1.0, snapshot.value(KpiId.CLASSIFICATION_CORRECTNESS).value());
  }

  @Test
  void friend_and_assumed_friend_are_friendly() {
    assertTrue(KpiCalculator.isFriendly(classified("a-f-A-M-F-Q")));
    assertTrue(KpiCalculator.isFriendly(classified("a-a-S-C-U")));
    assertTrue(KpiCalculator.isFriendly(classified("a-F-G")));
  }

  @Test
  void adversary_unknown_neutral_and_unclassified_assets_are_not_friendly() {
    for (String type : List.of("a-h-A-M-F-Q", "a-s-A", "a-j-G", "a-k-G", "a-u-U", "a-p-A", "a-n-S-C-U")) {
      assertFalse(KpiCalculator.isFriendly(classified(type)), type);
    }
    assertFalse(KpiCalculator.isFriendly(null));
    assertFalse(KpiCalculator.isFriendly(classified(null)));
    assertFalse(KpiCalculator.isFriendly(classified("")));
    assertFalse(KpiCalculator.isFriendly(classified("a")));
    assertFalse(KpiCalculator.isFriendly(classified("a-fx-A")));
  }

  private static ClassificationRegistry.Classification classified(String baseCotType) {
    return new ClassificationRegistry.Classification(ClassificationRegistry.Outcome.RESOLVED, baseCotType);
  }
}
