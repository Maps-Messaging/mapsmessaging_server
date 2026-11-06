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
import io.mapsmessaging.state.drone.core.TwinType;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class KpiConfigTest {

  /** The stanza deployed to central (ic26-demo-shine nodes/central/config/TwinManager.yaml.template). */
  private static final String CENTRAL_STANZA = """
      stateAdapters:
        kpi:
          positionMaxAgeSeconds: 30
          mtiMaxAgeSeconds: 120
          recoveryHoldSeconds: 15
          rosterAdminTimeoutSeconds: 3600
          eligibilityMode: list_and_announced
          eligibleUids: "UAS-1, Aircraft 01"
          controlTopic: "/kpi/control"
          faultInjection: false
          expectedClassifications:
            "UAS-1": "a-*-A-M-F-Q"
          feeds:
            "mti:status": 30
            "cot:": 5
          feedSilenceMultiplier: 3
          requiredFeeds: "mti:status"
          dataDirectory: "/tmp/kpi-test"
          bands:
            readiness: {green: 0.9, amber: 0.7}
            coverage: {green: 0.95, amber: 0.8}
            classificationTrust: {green: 0.95, amber: 0.9}
            positionStale: {green: 0.05, amber: 0.2}
            unknownDegraded: {green: 0.05, amber: 0.2}
            mtiStale: {green: 0.05, amber: 0.2}
      """;

  private static KpiConfig parse(String yaml) {
    Map<String, Object> root = new Yaml().load(yaml);
    ConfigurationProperties adapters = (ConfigurationProperties) new ConfigurationProperties(root).get("stateAdapters");
    return KpiConfig.from((ConfigurationProperties) adapters.get("kpi"));
  }

  @Test
  void centralStanza_parsesThroughTheConfigurationLibrary() {
    KpiConfig config = parse(CENTRAL_STANZA);

    assertEquals(Duration.ofSeconds(30), config.getPositionMaxAge());
    assertEquals(Duration.ofSeconds(120), config.getMtiMaxAge());
    assertEquals(Duration.ofSeconds(15), config.getRecoveryHold());
    assertEquals(KpiConfig.EligibilityMode.LIST_AND_ANNOUNCED, config.getEligibilityMode());
    assertEquals(Set.of("UAS-1", "Aircraft 01"), config.getEligibleUids());
    assertEquals(Map.of("UAS-1", "a-*-A-M-F-Q"), config.getExpectedClassifications());
    assertEquals(Duration.ofSeconds(30), config.feedInterval("mti:status"));
    assertEquals(Duration.ofSeconds(5), config.feedInterval("cot:basestation"));
    assertEquals(Set.of("mti:status"), config.getRequiredFeeds());
    assertEquals(new BandDefinition(true, 0.9, 0.7), config.band(KpiId.READINESS));
    assertEquals(new BandDefinition(false, 0.05, 0.2), config.band(KpiId.MTI_STALE));
    assertEquals(Path.of("/tmp/kpi-test"), config.getDataDirectory());
    assertFalse(config.isFaultInjection());
  }

  @Test
  void changedBand_keepsTheKpisDirection() {
    KpiConfig config = parse("""
        stateAdapters:
          kpi:
            bands:
              positionStale: {green: 0.1, amber: 0.3}
        """);

    assertEquals(new BandDefinition(false, 0.1, 0.3), config.band(KpiId.POSITION_STALE));
    assertEquals(new BandDefinition(true, 0.95, 0.8), config.band(KpiId.COVERAGE), "unchanged bands keep defaults");
  }

  @Test
  void emptyStanza_usesAgreedDefaults() {
    KpiConfig config = KpiConfig.from(null);

    assertEquals(KpiConfig.EligibilityMode.ALL, config.getEligibilityMode());
    assertEquals(Set.of(TwinType.DRONE), config.getTwinTypes());
    assertEquals(Duration.ofSeconds(3600), config.getRosterAdminTimeout());
    assertEquals(0.95, config.getRebuildTargetFraction());
    assertEquals("/kpi/control", config.getControlTopic());
    assertEquals(Duration.ofSeconds(1), config.feedInterval("mavlink:fleet"));
  }

  @Test
  void invalidValues_failLoudly() {
    assertThrows(IllegalArgumentException.class, () -> parse("""
        stateAdapters:
          kpi:
            eligibilityMode: sometimes
        """));
    assertThrows(IllegalArgumentException.class, () -> parse("""
        stateAdapters:
          kpi:
            bands:
              readiness: {green: 0.5, amber: 0.9}
        """));
  }
}
