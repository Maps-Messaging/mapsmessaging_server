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

package io.mapsmessaging.state.adapter.mti;

import com.udojava.jmx.wrapper.JMXBean;
import com.udojava.jmx.wrapper.JMXBeanAttribute;
import io.mapsmessaging.utilities.admin.JMXManager;

import javax.management.ObjectInstance;
import java.util.List;

/**
 * Registered under {@code io.mapsmessaging:type=Integration,name=MTI} - same
 * JMXManager/JMXBeanWrapper convention as the network endpoint beans (see
 * {@code network.admin.EndPointStatisticsJMX}), so these attributes are picked up by the same
 * jmx_prometheus_javaagent already scraping this process (see the repmus-estonia-demo compose
 * blueprint's {@code observability/maps/prometheus.yml}).
 */
@JMXBean(description = "MTI status feed integration metrics")
class MtiStatusAdapterJMX {

  private final MtiStatusAdapter adapter;
  private final ObjectInstance mbean;

  MtiStatusAdapterJMX(MtiStatusAdapter adapter) {
    this.adapter = adapter;
    this.mbean = JMXManager.getInstance().register(this, List.of("type=Integration", "name=MTI"));
  }

  void close() {
    JMXManager.getInstance().unregister(mbean);
  }

  @JMXBeanAttribute(name = "Cache Size", description = "Number of assets currently held in the MTI status cache")
  public int getCacheSize() {
    return adapter.getCacheSize();
  }

  @JMXBeanAttribute(name = "Upsert Count", description = "Total new/update messages processed from the MTI feed")
  public long getUpsertCount() {
    return adapter.getUpsertCount();
  }

  @JMXBeanAttribute(name = "Delete Count", description = "Total delete messages processed from the MTI feed")
  public long getDeleteCount() {
    return adapter.getDeleteCount();
  }

  @JMXBeanAttribute(name = "Lookup Hit Count", description = "CoT-composition lookups that found a cached MTI status")
  public long getLookupHitCount() {
    return adapter.getLookupHitCount();
  }

  @JMXBeanAttribute(name = "Lookup Miss Count", description = "CoT-composition lookups that found no cached MTI status")
  public long getLookupMissCount() {
    return adapter.getLookupMissCount();
  }

  @JMXBeanAttribute(name = "Last Message Age Millis", description = "Milliseconds since the last MTI feed message, -1 if none received yet")
  public long getLastMessageAgeMillis() {
    return adapter.getLastMessageAgeMillis();
  }

  @JMXBeanAttribute(name = "Degraded Asset Count", description = "Assets currently reporting MTI state mitigate/hold")
  public int getDegradedAssetCount() {
    return adapter.getDegradedAssetCount();
  }

  @JMXBeanAttribute(name = "Readiness Rate", description = "Fraction (0.0-1.0) of the MTI-covered fleet not currently flagged mitigate/hold")
  public double getReadinessRate() {
    return adapter.getReadinessRate();
  }
}
