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

import com.udojava.jmx.wrapper.JMXBean;
import com.udojava.jmx.wrapper.JMXBeanAttribute;
import io.mapsmessaging.utilities.admin.JMXManager;

import javax.management.ObjectInstance;
import java.util.List;

/** Registered as {@code io.mapsmessaging:type=Integration,name=KpiPicture}. */
@JMXBean(description = "Overall IC26 picture status, roster sizes, unknown reasons and recovery state")
public class KpiPictureJMX implements KpiBean {

  private final KpiEvaluator evaluator;
  private final ObjectInstance mbean;

  KpiPictureJMX(KpiEvaluator evaluator) {
    this.evaluator = evaluator;
    this.mbean = JMXManager.getInstance().register(this, List.of("type=Integration", "name=KpiPicture"));
  }

  @Override
  public void close() {
    JMXManager.getInstance().unregister(mbean);
  }

  @JMXBeanAttribute(name = "Overall Band", description = "Worst displayed critical KPI band: 0 green, 1 amber, 2 red, -1 no assets")
  public int getOverallBand() {
    return evaluator.getOverall().getCode();
  }

  @JMXBeanAttribute(name = "Usable", description = "1 while no critical KPI is red (MAPS-to-TAK usable data flow)")
  public int getUsable() {
    return evaluator.isUsable() ? 1 : 0;
  }

  @JMXBeanAttribute(name = "Fully Restored", description = "1 while all critical KPIs are green")
  public int getFullyRestored() {
    return evaluator.isFullyRestored() ? 1 : 0;
  }

  @JMXBeanAttribute(name = "Roster Size", description = "Mission-active assets (the KPI denominator)")
  public long getRosterSize() {
    KpiSnapshot latest = evaluator.getLatest();
    return latest == null ? 0 : latest.rosterSize();
  }

  @JMXBeanAttribute(name = "Eligible Size", description = "Mission-active assets MTI is expected to cover")
  public long getEligibleSize() {
    KpiSnapshot latest = evaluator.getLatest();
    return latest == null ? 0 : latest.eligibleSize();
  }

  @JMXBeanAttribute(name = "Unknown No Mti Count", description = "MTI-eligible assets MTI never reported on")
  public long getUnknownNoMtiCount() {
    return reason(AssetAssessment.UnknownReason.NO_MTI);
  }

  @JMXBeanAttribute(name = "Unknown Mti Unknown Count", description = "MTI-eligible assets MTI explicitly reports as unknown")
  public long getUnknownMtiUnknownCount() {
    return reason(AssetAssessment.UnknownReason.MTI_UNKNOWN);
  }

  @JMXBeanAttribute(name = "Unknown Mti Stale Count", description = "MTI-eligible assets whose MTI status expired or is older than the freshness threshold")
  public long getUnknownMtiStaleCount() {
    return reason(AssetAssessment.UnknownReason.MTI_STALE);
  }

  @JMXBeanAttribute(name = "Unknown Unresolved Classification Count", description = "Assets with a generic or no classification")
  public long getUnknownUnresolvedClassificationCount() {
    return reason(AssetAssessment.UnknownReason.UNRESOLVED_CLASSIFICATION);
  }

  @JMXBeanAttribute(name = "Open Fault Runs", description = "Failure injections not yet fully restored or ended")
  public long getOpenFaultRuns() {
    return evaluator.getRuns().getOpenCount();
  }

  @JMXBeanAttribute(name = "Cache Rebuild In Progress", description = "1 while the MTI cache is being rebuilt after a cache loss")
  public int getCacheRebuildInProgress() {
    return evaluator.getRebuilds().isInProgress() ? 1 : 0;
  }

  @JMXBeanAttribute(name = "Cache Rebuild Baseline", description = "Assets with fresh MTI opinions before the cache loss")
  public long getCacheRebuildBaseline() {
    return evaluator.getRebuilds().getBaselineSize();
  }

  @JMXBeanAttribute(name = "Cache Rebuild Restored", description = "Baseline assets with a fresh MTI opinion again")
  public long getCacheRebuildRestored() {
    return evaluator.getRebuilds().getRestoredCount();
  }

  @JMXBeanAttribute(name = "Position Max Age Seconds", description = "Position/telemetry freshness threshold")
  public long getPositionMaxAgeSeconds() {
    return evaluator.getConfig().getPositionMaxAge().toSeconds();
  }

  @JMXBeanAttribute(name = "Mti Max Age Seconds", description = "MTI status freshness threshold")
  public long getMtiMaxAgeSeconds() {
    return evaluator.getConfig().getMtiMaxAge().toSeconds();
  }

  @JMXBeanAttribute(name = "Recovery Hold Seconds", description = "How long a better band must hold before it is shown")
  public long getRecoveryHoldSeconds() {
    return evaluator.getConfig().getRecoveryHold().toSeconds();
  }

  private long reason(AssetAssessment.UnknownReason reason) {
    KpiSnapshot latest = evaluator.getLatest();
    return latest == null ? 0 : latest.unknownReasons().getOrDefault(reason, 0);
  }
}
