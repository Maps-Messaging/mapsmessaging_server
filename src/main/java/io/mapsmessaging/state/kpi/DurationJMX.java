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

/**
 * One timed stage, e.g. {@code name=FaultRun,failureType=FEED_LOSS,stage=USABLE_RESTORED} or
 * {@code name=CacheRebuild,trigger=RESTART,target=ALL}. Mean = Sum Seconds / Count.
 */
@JMXBean(description = "Count, sum, last and max of a recovery or detection duration")
public class DurationJMX implements KpiBean {

  private final DurationStats stats;
  private final ObjectInstance mbean;

  DurationJMX(List<String> nameParts, DurationStats stats) {
    this.stats = stats;
    this.mbean = JMXManager.getInstance().register(this, nameParts);
  }

  @Override
  public void close() {
    JMXManager.getInstance().unregister(mbean);
  }

  @JMXBeanAttribute(name = "Count", description = "Times this stage was reached")
  public long getCount() {
    return stats.getCount();
  }

  @JMXBeanAttribute(name = "Sum Seconds", description = "Total duration across all occurrences")
  public double getSumSeconds() {
    return stats.getSumSeconds();
  }

  @JMXBeanAttribute(name = "Last Seconds", description = "Most recent duration, -1 if none")
  public double getLastSeconds() {
    return stats.getLastSeconds();
  }

  @JMXBeanAttribute(name = "Max Seconds", description = "Longest duration, -1 if none")
  public double getMaxSeconds() {
    return stats.getMaxSeconds();
  }

  @JMXBeanAttribute(name = "Not Reached Count", description = "Occurrences that never reached this stage")
  public long getNotReachedCount() {
    return stats.getNotReachedCount();
  }
}
