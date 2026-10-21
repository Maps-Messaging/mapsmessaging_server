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

/** Registered as {@code io.mapsmessaging:type=Integration,name=FaultRuns,failureType=<TYPE|ALL>}. */
@JMXBean(description = "Closed fault injection runs per failure type")
public class FaultRunsJMX implements KpiBean {

  private final FaultRunTracker.RunTypeStats stats;
  private final ObjectInstance mbean;

  FaultRunsJMX(String failureType, FaultRunTracker.RunTypeStats stats) {
    this.stats = stats;
    this.mbean = JMXManager.getInstance().register(this, List.of(
        "type=Integration", "name=FaultRuns", "failureType=" + FeedOutageJMX.sanitise(failureType)));
  }

  @Override
  public void close() {
    JMXManager.getInstance().unregister(mbean);
  }

  @JMXBeanAttribute(name = "Runs Count", description = "Closed runs")
  public long getRunsCount() {
    return stats.getRuns();
  }

  @JMXBeanAttribute(name = "Not Restored Count", description = "Runs that timed out before full restoration")
  public long getNotRestoredCount() {
    return stats.getNotRestored();
  }

  @JMXBeanAttribute(name = "No Effect Count", description = "Runs where nothing was detected or degraded")
  public long getNoEffectCount() {
    return stats.getNoEffect();
  }
}
