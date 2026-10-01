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

import java.time.Instant;
import java.util.*;

/**
 * The mission-active population the KPIs are measured against (IC26 KPI clarification Q1): once an
 * asset has entered the picture, silence makes it stale or unknown but does not remove it. It only
 * leaves through an explicit deactivation, the end of the mission, or the administrative timeout.
 * Assets listed in the configuration are pinned and never time out.
 *
 * <p>Also holds the MTI-announced assets (Q2): sticky, so an MTI outage shows up as falling
 * coverage instead of a shrinking denominator. Only an MTI delete removes an announcement.
 */
final class MissionRoster {

  private final KpiConfig config;
  private final Set<String> pinned;
  private final Map<String, Instant> lastSeen = new HashMap<>();
  private final Set<String> announced = new HashSet<>();

  MissionRoster(KpiConfig config) {
    this.config = config;
    this.pinned = new LinkedHashSet<>(config.getRosterUids());
    if (config.getEligibilityMode().usesList()) {
      pinned.addAll(config.getEligibleUids());
    }
  }

  /** Records that the asset is in the picture and when it last reported. */
  synchronized void observe(String uid, Instant reportedAt) {
    if (uid == null || reportedAt == null) {
      return;
    }
    lastSeen.merge(uid, reportedAt, (current, incoming) -> incoming.isAfter(current) ? incoming : current);
  }

  /** Drops silent, unpinned assets past the administrative timeout. @return the removed uids. */
  synchronized List<String> expire(Instant now) {
    if (config.getRosterAdminTimeout().isZero() || config.getRosterAdminTimeout().isNegative()) {
      return List.of();
    }
    Instant cutoff = now.minus(config.getRosterAdminTimeout());
    List<String> removed = new ArrayList<>();
    Iterator<Map.Entry<String, Instant>> iterator = lastSeen.entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry<String, Instant> entry = iterator.next();
      if (!pinned.contains(entry.getKey()) && entry.getValue().isBefore(cutoff)) {
        iterator.remove();
        removed.add(entry.getKey());
      }
    }
    return removed;
  }

  synchronized boolean deactivate(String uid) {
    return lastSeen.remove(uid) != null;
  }

  /** Clears every observed asset and MTI announcement; pinned assets stay. @return removed uids. */
  synchronized List<String> endMission() {
    List<String> removed = new ArrayList<>(lastSeen.keySet());
    removed.removeAll(pinned);
    lastSeen.clear();
    announced.clear();
    return removed;
  }

  synchronized void mtiAnnounced(String uid) {
    announced.add(uid);
  }

  synchronized void mtiWithdrawn(String uid) {
    announced.remove(uid);
  }

  /** Whether MTI ever sent a status for the asset that it has not withdrawn. */
  synchronized boolean wasAnnounced(String uid) {
    return announced.contains(uid);
  }

  synchronized boolean isEligible(String uid) {
    KpiConfig.EligibilityMode mode = config.getEligibilityMode();
    if (mode == KpiConfig.EligibilityMode.ALL) {
      return true;
    }
    return (mode.usesList() && config.getEligibleUids().contains(uid))
        || (mode.usesAnnounced() && announced.contains(uid));
  }

  /** @return every roster member, pinned ones included, in a stable order. */
  synchronized List<String> members() {
    Set<String> members = new TreeSet<>(lastSeen.keySet());
    members.addAll(pinned);
    return new ArrayList<>(members);
  }

  //<editor-fold desc="Persistence">
  synchronized Map<String, String> exportLastSeen() {
    Map<String, String> exported = new TreeMap<>();
    lastSeen.forEach((uid, at) -> exported.put(uid, at.toString()));
    return exported;
  }

  synchronized Set<String> exportAnnounced() {
    return new TreeSet<>(announced);
  }

  synchronized void restore(Map<String, String> savedLastSeen, Collection<String> savedAnnounced) {
    if (savedLastSeen != null) {
      savedLastSeen.forEach((uid, at) -> observe(uid, Instant.parse(at)));
    }
    if (savedAnnounced != null) {
      announced.addAll(savedAnnounced);
    }
  }
  //</editor-fold>
}
