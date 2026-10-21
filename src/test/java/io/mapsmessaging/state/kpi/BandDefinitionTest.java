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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BandDefinitionTest {

  private final KpiConfig defaults = KpiConfig.from(new ConfigurationProperties());

  @Test
  void readiness_usesAgreedProvisionalBoundaries() {
    BandDefinition readiness = defaults.band(KpiId.READINESS);
    assertEquals(Band.GREEN, readiness.evaluate(0.90));
    assertEquals(Band.AMBER, readiness.evaluate(0.8999));
    assertEquals(Band.AMBER, readiness.evaluate(0.70));
    assertEquals(Band.RED, readiness.evaluate(0.6999));
  }

  @Test
  void coverage_greenFromNinetyFivePercent_redBelowEighty() {
    BandDefinition coverage = defaults.band(KpiId.COVERAGE);
    assertEquals(Band.GREEN, coverage.evaluate(19.0 / 20.0));
    assertEquals(Band.AMBER, coverage.evaluate(0.80));
    assertEquals(Band.RED, coverage.evaluate(0.7999));
  }

  @Test
  void classificationTrust_amberOnlyBetweenNinetyAndNinetyFive() {
    BandDefinition trust = defaults.band(KpiId.CLASSIFICATION_TRUST);
    assertEquals(Band.GREEN, trust.evaluate(0.95));
    assertEquals(Band.AMBER, trust.evaluate(0.90));
    assertEquals(Band.RED, trust.evaluate(0.8999));
  }

  @Test
  void staleAndUnknownShares_lowerIsBetter_boundariesInclusive() {
    for (KpiId kpi : new KpiId[]{KpiId.POSITION_STALE, KpiId.UNKNOWN_DEGRADED}) {
      BandDefinition band = defaults.band(kpi);
      assertEquals(Band.GREEN, band.evaluate(0.0));
      assertEquals(Band.GREEN, band.evaluate(1.0 / 20.0), kpi.name());
      assertEquals(Band.AMBER, band.evaluate(0.0501));
      assertEquals(Band.AMBER, band.evaluate(0.20));
      assertEquals(Band.RED, band.evaluate(0.2001));
    }
  }

  @Test
  void emptyPopulation_isNoData() {
    assertEquals(Band.NO_DATA, defaults.band(KpiId.READINESS).evaluate(Double.NaN));
  }

  @Test
  void amberBeyondGreen_isRejected() {
    assertThrows(IllegalArgumentException.class, () -> new BandDefinition(true, 0.8, 0.9));
    assertThrows(IllegalArgumentException.class, () -> new BandDefinition(false, 0.2, 0.1));
  }

  @Test
  void diagnosticKpis_haveNoBand() {
    assertEquals(null, defaults.band(KpiId.ACTIONABLE_TRUST));
    assertEquals(null, defaults.band(KpiId.CLASSIFICATION_CORRECTNESS));
  }
}
