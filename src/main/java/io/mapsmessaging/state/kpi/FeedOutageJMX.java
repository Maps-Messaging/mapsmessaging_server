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
import java.time.Clock;
import java.util.List;

/**
 * Registered as {@code io.mapsmessaging:type=Integration,name=FeedOutage,kind=<mti|mavlink|cot|tak>,feed=<instance>}.
 * Outage duration stats: Mean = Total Outage Seconds / Outage Count.
 */
@JMXBean(description = "Outage count and duration of one input/output feed")
public class FeedOutageJMX implements KpiBean {

  private final FeedOutageMonitor.FeedStats stats;
  private final Clock clock;
  private final ObjectInstance mbean;

  FeedOutageJMX(FeedOutageMonitor.FeedStats stats, Clock clock) {
    this.stats = stats;
    this.clock = clock;
    String feed = stats.getFeed();
    int separator = feed.indexOf(':');
    String kind = separator < 0 ? "other" : feed.substring(0, separator);
    String instance = separator < 0 ? feed : feed.substring(separator + 1);
    this.mbean = JMXManager.getInstance().register(this, List.of(
        "type=Integration", "name=FeedOutage", "kind=" + sanitise(kind), "feed=" + sanitise(instance)));
  }

  /** ObjectName values may not contain ':', ',', '=', quotes or wildcards unquoted. */
  static String sanitise(String value) {
    String cleaned = value.replaceAll("[^A-Za-z0-9._-]", "_");
    return cleaned.isEmpty() ? "_" : cleaned;
  }

  @Override
  public void close() {
    JMXManager.getInstance().unregister(mbean);
  }

  @JMXBeanAttribute(name = "In Outage", description = "1 while the feed is silent beyond its threshold or disconnected")
  public int getInOutage() {
    return stats.isInOutage() ? 1 : 0;
  }

  @JMXBeanAttribute(name = "Current Outage Seconds", description = "Length of the ongoing outage, 0 if none")
  public double getCurrentOutageSeconds() {
    return stats.getCurrentOutageSeconds(clock.instant());
  }

  @JMXBeanAttribute(name = "Outage Count", description = "Ended outages since startup")
  public long getOutageCount() {
    return stats.getOutages().getCount();
  }

  @JMXBeanAttribute(name = "Total Outage Seconds", description = "Summed length of ended outages")
  public double getTotalOutageSeconds() {
    return stats.getTotalOutageSeconds();
  }

  @JMXBeanAttribute(name = "Last Outage Seconds", description = "Length of the most recent ended outage, -1 if none")
  public double getLastOutageSeconds() {
    return stats.getOutages().getLastSeconds();
  }

  @JMXBeanAttribute(name = "Max Outage Seconds", description = "Longest ended outage, -1 if none")
  public double getMaxOutageSeconds() {
    return stats.getOutages().getMaxSeconds();
  }

  @JMXBeanAttribute(name = "Expected Interval Seconds", description = "Expected report interval, -1 for connection feeds")
  public double getExpectedIntervalSeconds() {
    return stats.getExpectedInterval() == null ? -1.0 : stats.getExpectedInterval().toMillis() / 1000.0;
  }
}
