/*
 *
 *  Copyright [ 2026 ] Ralf Himmelein and Claude
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

import com.udojava.jmx.wrapper.JMXBean;
import com.udojava.jmx.wrapper.JMXBeanAttribute;
import io.mapsmessaging.utilities.admin.JMXManager;

import javax.management.ObjectInstance;
import java.util.List;

/**
 * Registered under {@code io.mapsmessaging:type=Integration,name=CoT}, owned by the single
 * {@code TakTwinObserver} instance (constructed once per server, see its constructor/shutdown()).
 */
@JMXBean(description = "CoT/TAK output integration metrics")
public class CotIntegrationJMX {

  private final CotEventPolicy policy;
  private final ObjectInstance mbean;

  CotIntegrationJMX(CotEventPolicy policy) {
    this.policy = policy;
    this.mbean = JMXManager.getInstance().register(this, List.of("type=Integration", "name=CoT"));
  }

  void close() {
    JMXManager.getInstance().unregister(mbean);
  }

  @JMXBeanAttribute(name = "Events Composed", description = "Total CoT events composed (creates, updates, and removals)")
  public long getEventsComposed() {
    return policy.getAppliedCount();
  }

  @JMXBeanAttribute(name = "Original Type Fallback Count", description = "Composed events that kept a round-tripped CoT-ingested twin's original type rather than a vehicle-class guess")
  public long getOriginalTypeFallbackCount() {
    return policy.getOriginalTypeFallbackCount();
  }

  @JMXBeanAttribute(name = "Classification Override Count", description = "Composed events that used a per-asset mavlink.knownSources[].cotClassification override")
  public long getClassificationOverrideCount() {
    return policy.getClassificationOverrideCount();
  }

  @JMXBeanAttribute(name = "Unknown Vehicle Class Count", description = "Composed events that fell back to the generic/unclassified CoT type because the twin had no resolvable vehicle class")
  public long getUnknownVehicleClassCount() {
    return policy.getUnknownVehicleClassCount();
  }

  @JMXBeanAttribute(name = "Mti Affiliation Override Count", description = "Composed events where MTI status overrode the twin's affiliation to unknown")
  public long getMtiAffiliationOverrideCount() {
    return policy.getMtiAffiliationOverrideCount();
  }

  @JMXBeanAttribute(name = "Mti Readiness Degraded Count", description = "Composed events flagged readiness=false by MTI mitigate/hold status")
  public long getMtiReadinessDegradedCount() {
    return policy.getMtiReadinessDegradedCount();
  }

  @JMXBeanAttribute(name = "Mti Cyber Icon Applied Count", description = "Composed events for a drone that got the custom cyber-compromise usericon override (WinTAK/ATAK only)")
  public long getMtiCyberIconAppliedCount() {
    return policy.getMtiCyberIconAppliedCount();
  }

  @JMXBeanAttribute(name = "Classification Trust Rate", description = "Fraction (0.0-1.0) of vehicle-class-derived compositions that resolved to a real classification, not the generic/unclassified fallback")
  public double getClassificationTrustRate() {
    long derived = policy.getVehicleClassDerivedCount();
    if (derived <= 0) {
      return 1.0d;
    }
    return 1.0d - ((double) policy.getUnknownVehicleClassCount() / derived);
  }
}
