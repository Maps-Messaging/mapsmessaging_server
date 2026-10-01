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

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MissionRosterTest {

  private static final Instant T0 = Instant.parse("2026-10-01T10:00:00Z");

  private static MissionRoster roster(Map<String, Object> config) {
    return new MissionRoster(KpiConfig.from(new ConfigurationProperties(config)));
  }

  @Test
  void silentAsset_staysOnTheRoster_untilTheAdminTimeout() {
    MissionRoster roster = roster(Map.of("rosterAdminTimeoutSeconds", 600));
    roster.observe("uas-1", T0);

    assertEquals(List.of(), roster.expire(T0.plusSeconds(600)));
    assertEquals(List.of("uas-1"), roster.members());
    assertEquals(List.of("uas-1"), roster.expire(T0.plusSeconds(601)));
    assertEquals(List.of(), roster.members());
  }

  @Test
  void zeroAdminTimeout_neverExpires() {
    MissionRoster roster = roster(Map.of("rosterAdminTimeoutSeconds", 0));
    roster.observe("uas-1", T0);

    assertEquals(List.of(), roster.expire(T0.plusSeconds(1_000_000)));
  }

  @Test
  void configuredAssets_arePinned_andSurviveEndOfMission() {
    MissionRoster roster = roster(Map.of("rosterUids", "planned-1", "rosterAdminTimeoutSeconds", 1));
    roster.observe("seen-1", T0);

    assertEquals(List.of("planned-1", "seen-1"), roster.members());
    roster.expire(T0.plusSeconds(10));
    assertEquals(List.of("planned-1"), roster.members());
    roster.observe("seen-2", T0);
    assertEquals(List.of("seen-2"), roster.endMission());
    assertEquals(List.of("planned-1"), roster.members());
  }

  @Test
  void deactivate_removesOnlyThatAsset() {
    MissionRoster roster = roster(Map.of());
    roster.observe("a", T0);
    roster.observe("b", T0);

    assertTrue(roster.deactivate("a"));
    assertFalse(roster.deactivate("a"));
    assertEquals(List.of("b"), roster.members());
  }

  @Test
  void observe_keepsTheLatestReport() {
    MissionRoster roster = roster(Map.of("rosterAdminTimeoutSeconds", 60));
    roster.observe("a", T0.plusSeconds(100));
    roster.observe("a", T0);

    assertEquals(List.of(), roster.expire(T0.plusSeconds(150)));
  }

  @Test
  void eligibility_all_coversEveryAsset() {
    assertTrue(roster(Map.of()).isEligible("anything"));
  }

  @Test
  void eligibility_list_coversOnlyConfiguredUids_andPinsThem() {
    MissionRoster roster = roster(Map.of("eligibilityMode", "list", "eligibleUids", "UAS-1, UAS-2"));

    assertTrue(roster.isEligible("UAS-2"));
    assertFalse(roster.isEligible("partner"));
    assertEquals(List.of("UAS-1", "UAS-2"), roster.members());
  }

  @Test
  void eligibility_announced_isSticky_untilMtiWithdrawsIt() {
    MissionRoster roster = roster(Map.of("eligibilityMode", "announced"));
    assertFalse(roster.isEligible("uas-1"));

    roster.mtiAnnounced("uas-1");
    assertTrue(roster.isEligible("uas-1"));
    assertTrue(roster.wasAnnounced("uas-1"));

    roster.mtiWithdrawn("uas-1");
    assertFalse(roster.isEligible("uas-1"));
  }

  @Test
  void eligibility_listAndAnnounced_isTheUnion() {
    MissionRoster roster = roster(Map.of("eligibilityMode", "LIST_AND_ANNOUNCED", "eligibleUids", "UAS-1"));
    roster.mtiAnnounced("Aircraft 01");

    assertTrue(roster.isEligible("UAS-1"));
    assertTrue(roster.isEligible("Aircraft 01"));
    assertFalse(roster.isEligible("other"));
  }

  @Test
  void exportAndRestore_roundTripsRosterAndAnnouncements() {
    MissionRoster original = roster(Map.of());
    original.observe("a", T0);
    original.mtiAnnounced("a");

    MissionRoster restored = roster(Map.of());
    restored.restore(original.exportLastSeen(), original.exportAnnounced());

    assertEquals(List.of("a"), restored.members());
    assertTrue(restored.wasAnnounced("a"));
    assertEquals(Set.of("a"), restored.exportAnnounced());
  }
}
