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

package io.mapsmessaging.state.metrics;

import com.udojava.jmx.wrapper.JMXBean;
import com.udojava.jmx.wrapper.JMXBeanAttribute;
import io.mapsmessaging.state.drone.core.EntityTwin;
import io.mapsmessaging.state.drone.core.TwinLifecycleStatus;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.core.TwinType;
import io.mapsmessaging.state.drone.tak.MtiStatusRegistry;
import io.mapsmessaging.state.drone.tak.MtiStatusSnapshot;
import io.mapsmessaging.utilities.admin.JMXManager;

import javax.management.ObjectInstance;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Twin counts for one (twin type, lifecycle status) pair, broken down by MTI state and MTI age.
 * Registered as {@code io.mapsmessaging:type=Integration,name=Twins,twinType=..,lifecycle=..}.
 *
 * <p>Deliberately raw: coverage, unknown-asset and staleness KPIs are formulas over these counts,
 * so their definitions (which lifecycle states count as "active", staleness threshold, whether
 * MTI "unknown" counts as covered) can be decided in the dashboard without code changes. MTI age
 * and report age buckets are cumulative, like Prometheus histogram buckets.
 */
@JMXBean(description = "Digital twin counts by type and lifecycle, with MTI state and age")
public class TwinStatusJMX {

  private static final Set<String> MTI_STATES = Set.of("go", "mitigate", "hold", "unknown");

  private final TwinManager twinManager;
  private final TwinType twinType;
  private final TwinLifecycleStatus lifecycleStatus;
  private final ObjectInstance mbean;

  TwinStatusJMX(TwinManager twinManager, TwinType twinType, TwinLifecycleStatus lifecycleStatus) {
    this.twinManager = twinManager;
    this.twinType = twinType;
    this.lifecycleStatus = lifecycleStatus;
    this.mbean = JMXManager.getInstance().register(this, List.of(
        "type=Integration",
        "name=Twins",
        "twinType=" + twinType.name(),
        "lifecycle=" + lifecycleStatus.name()));
  }

  void close() {
    JMXManager.getInstance().unregister(mbean);
  }

  @JMXBeanAttribute(name = "Twin Count", description = "Twins of this type in this lifecycle state")
  public long getTwinCount() {
    return count(snapshot -> true);
  }

  @JMXBeanAttribute(name = "Mti None Count", description = "Twins with no current (unexpired) MTI status")
  public long getMtiNoneCount() {
    return count(Objects::isNull);
  }

  @JMXBeanAttribute(name = "Mti Go Count", description = "Twins whose MTI state is go")
  public long getMtiGoCount() {
    return count(snapshot -> hasState(snapshot, "go"));
  }

  @JMXBeanAttribute(name = "Mti Mitigate Count", description = "Twins whose MTI state is mitigate")
  public long getMtiMitigateCount() {
    return count(snapshot -> hasState(snapshot, "mitigate"));
  }

  @JMXBeanAttribute(name = "Mti Hold Count", description = "Twins whose MTI state is hold")
  public long getMtiHoldCount() {
    return count(snapshot -> hasState(snapshot, "hold"));
  }

  @JMXBeanAttribute(name = "Mti Unknown Count", description = "Twins whose MTI state is unknown")
  public long getMtiUnknownCount() {
    return count(snapshot -> hasState(snapshot, "unknown"));
  }

  @JMXBeanAttribute(name = "Mti Other Count", description = "Twins with an MTI status outside the go/mitigate/hold/unknown alphabet")
  public long getMtiOtherCount() {
    return count(snapshot -> snapshot != null && !MTI_STATES.contains(normalise(snapshot.state())));
  }

  @JMXBeanAttribute(name = "Mti Age Le 30s Count", description = "Twins whose MTI status was observed at most 30s ago")
  public long getMtiAgeLe30sCount() {
    return countAgeAtMost(Duration.ofSeconds(30));
  }

  @JMXBeanAttribute(name = "Mti Age Le 60s Count", description = "Twins whose MTI status was observed at most 60s ago")
  public long getMtiAgeLe60sCount() {
    return countAgeAtMost(Duration.ofSeconds(60));
  }

  @JMXBeanAttribute(name = "Mti Age Le 120s Count", description = "Twins whose MTI status was observed at most 120s ago")
  public long getMtiAgeLe120sCount() {
    return countAgeAtMost(Duration.ofSeconds(120));
  }

  @JMXBeanAttribute(name = "Mti Age Le 300s Count", description = "Twins whose MTI status was observed at most 300s ago")
  public long getMtiAgeLe300sCount() {
    return countAgeAtMost(Duration.ofSeconds(300));
  }

  @JMXBeanAttribute(name = "Report Age Le 10s Count", description = "Twins whose last report arrived at most 10s ago")
  public long getReportAgeLe10sCount() {
    return countReportAgeAtMost(Duration.ofSeconds(10));
  }

  @JMXBeanAttribute(name = "Report Age Le 30s Count", description = "Twins whose last report arrived at most 30s ago")
  public long getReportAgeLe30sCount() {
    return countReportAgeAtMost(Duration.ofSeconds(30));
  }

  @JMXBeanAttribute(name = "Report Age Le 60s Count", description = "Twins whose last report arrived at most 60s ago")
  public long getReportAgeLe60sCount() {
    return countReportAgeAtMost(Duration.ofSeconds(60));
  }

  @JMXBeanAttribute(name = "Report Age Le 120s Count", description = "Twins whose last report arrived at most 120s ago")
  public long getReportAgeLe120sCount() {
    return countReportAgeAtMost(Duration.ofSeconds(120));
  }

  @JMXBeanAttribute(name = "Report Age Le 10s Mti Known Count", description = "Twins reporting within 10s that also have a go/mitigate/hold MTI status")
  public long getReportAgeLe10sMtiKnownCount() {
    return countReportAgeAtMost(Duration.ofSeconds(10), TwinStatusJMX::isKnown);
  }

  @JMXBeanAttribute(name = "Report Age Le 30s Mti Known Count", description = "Twins reporting within 30s that also have a go/mitigate/hold MTI status")
  public long getReportAgeLe30sMtiKnownCount() {
    return countReportAgeAtMost(Duration.ofSeconds(30), TwinStatusJMX::isKnown);
  }

  @JMXBeanAttribute(name = "Report Age Le 60s Mti Known Count", description = "Twins reporting within 60s that also have a go/mitigate/hold MTI status")
  public long getReportAgeLe60sMtiKnownCount() {
    return countReportAgeAtMost(Duration.ofSeconds(60), TwinStatusJMX::isKnown);
  }

  @JMXBeanAttribute(name = "Report Age Le 120s Mti Known Count", description = "Twins reporting within 120s that also have a go/mitigate/hold MTI status")
  public long getReportAgeLe120sMtiKnownCount() {
    return countReportAgeAtMost(Duration.ofSeconds(120), TwinStatusJMX::isKnown);
  }

  private long countReportAgeAtMost(Duration maxAge) {
    return countReportAgeAtMost(maxAge, snapshot -> true);
  }

  // Report age is independent of the lifecycle thresholds configured in TwinManager, so the
  // dashboard can compare several "active asset" timeouts side by side without a restart. The
  // MTI-known variant is the joint count "fresh AND known" - the complement of "stale or unknown",
  // which can't be derived from separate stale and unknown counts without double-counting.
  private long countReportAgeAtMost(Duration maxAge, Predicate<MtiStatusSnapshot> mtiPredicate) {
    Instant cutoff = Instant.now().minus(maxAge);
    long matches = 0;
    for (EntityTwin twin : twinManager.listTwins()) {
      Instant lastSeenAt = twin.getLastSeenAt();
      if (twin.getTwinType() == twinType
          && twin.getLifecycleStatus() == lifecycleStatus
          && lastSeenAt != null
          && !lastSeenAt.isBefore(cutoff)
          && mtiPredicate.test(MtiStatusRegistry.snapshot(twin.getTwinId()))) {
        matches++;
      }
    }
    return matches;
  }

  private static boolean isKnown(MtiStatusSnapshot snapshot) {
    return hasState(snapshot, "go") || hasState(snapshot, "mitigate") || hasState(snapshot, "hold");
  }

  private long countAgeAtMost(Duration maxAge) {
    Instant cutoff = Instant.now().minus(maxAge);
    return count(snapshot -> snapshot != null && !snapshot.observedAt().isBefore(cutoff));
  }

  private long count(Predicate<MtiStatusSnapshot> predicate) {
    long matches = 0;
    for (EntityTwin twin : twinManager.listTwins()) {
      if (twin.getTwinType() == twinType
          && twin.getLifecycleStatus() == lifecycleStatus
          && predicate.test(MtiStatusRegistry.snapshot(twin.getTwinId()))) {
        matches++;
      }
    }
    return matches;
  }

  private static boolean hasState(MtiStatusSnapshot snapshot, String state) {
    return snapshot != null && state.equals(normalise(snapshot.state()));
  }

  private static String normalise(String state) {
    return state == null ? "" : state.toLowerCase(Locale.ROOT);
  }
}
