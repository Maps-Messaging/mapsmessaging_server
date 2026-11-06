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

package io.mapsmessaging.state.drone.tak;

import com.udojava.jmx.wrapper.JMXBean;
import com.udojava.jmx.wrapper.JMXBeanAttribute;
import io.mapsmessaging.state.drone.tak.PictureRecoveryTracker.FailureType;
import io.mapsmessaging.state.drone.tak.PictureRecoveryTracker.Stage;
import io.mapsmessaging.utilities.admin.JMXManager;

import javax.management.ObjectInstance;
import java.util.List;

/**
 * Per-asset picture restoration delay for one failure type and stage, registered as
 * {@code io.mapsmessaging:type=Integration,name=PictureRecovery,failureType=..,stage=..}.
 * Buckets are cumulative (Prometheus histogram style); the exporter rule maps the number in the
 * attribute name onto the {@code le} label.
 */
@JMXBean(description = "Per-asset picture restoration delay by failure type and stage")
public class PictureRecoveryJMX {

  private final PictureRecoveryTracker tracker;
  private final FailureType failureType;
  private final RecoveryDurationStats stats;
  private final ObjectInstance mbean;

  PictureRecoveryJMX(PictureRecoveryTracker tracker, FailureType failureType, Stage stage) {
    this.tracker = tracker;
    this.failureType = failureType;
    this.stats = tracker.stats(failureType, stage);
    this.mbean = JMXManager.getInstance().register(this, List.of(
        "type=Integration",
        "name=PictureRecovery",
        "failureType=" + failureType.name(),
        "stage=" + stage.name()));
  }

  void close() {
    JMXManager.getInstance().unregister(mbean);
  }

  @JMXBeanAttribute(name = "Events Opened Count", description = "Recovery events started for this failure type")
  public long getEventsOpenedCount() {
    return tracker.getOpenedCount(failureType);
  }

  @JMXBeanAttribute(name = "Events Open Count", description = "Recovery events of this failure type still waiting to complete")
  public long getEventsOpenCount() {
    return tracker.getOpenCount(failureType);
  }

  @JMXBeanAttribute(name = "Not Restored Count", description = "Recovery events that timed out or were superseded before reaching this stage")
  public long getNotRestoredCount() {
    return stats.getNotRestoredCount();
  }

  @JMXBeanAttribute(name = "Last Restore Seconds", description = "Most recent restoration delay for this stage, -1 if none yet")
  public double getLastRestoreSeconds() {
    return stats.getLastSeconds();
  }

  @JMXBeanAttribute(name = "Restore Count", description = "Recoveries that reached this stage")
  public long getRestoreCount() {
    return stats.getCount();
  }

  @JMXBeanAttribute(name = "Restore Sum Seconds", description = "Total restoration delay across all recoveries that reached this stage")
  public double getRestoreSumSeconds() {
    return stats.getSumSeconds();
  }

  @JMXBeanAttribute(name = "Restore Bucket 0.1", description = "Restorations within 100ms")
  public long getRestoreBucket01() {
    return stats.getCumulativeCount(0);
  }

  @JMXBeanAttribute(name = "Restore Bucket 0.25", description = "Restorations within 250ms")
  public long getRestoreBucket025() {
    return stats.getCumulativeCount(1);
  }

  @JMXBeanAttribute(name = "Restore Bucket 0.5", description = "Restorations within 500ms")
  public long getRestoreBucket05() {
    return stats.getCumulativeCount(2);
  }

  @JMXBeanAttribute(name = "Restore Bucket 1", description = "Restorations within 1s")
  public long getRestoreBucket1() {
    return stats.getCumulativeCount(3);
  }

  @JMXBeanAttribute(name = "Restore Bucket 2.5", description = "Restorations within 2.5s")
  public long getRestoreBucket25() {
    return stats.getCumulativeCount(4);
  }

  @JMXBeanAttribute(name = "Restore Bucket 5", description = "Restorations within 5s")
  public long getRestoreBucket5() {
    return stats.getCumulativeCount(5);
  }

  @JMXBeanAttribute(name = "Restore Bucket 10", description = "Restorations within 10s")
  public long getRestoreBucket10() {
    return stats.getCumulativeCount(6);
  }

  @JMXBeanAttribute(name = "Restore Bucket 30", description = "Restorations within 30s")
  public long getRestoreBucket30() {
    return stats.getCumulativeCount(7);
  }

  @JMXBeanAttribute(name = "Restore Bucket 60", description = "Restorations within 60s")
  public long getRestoreBucket60() {
    return stats.getCumulativeCount(8);
  }

  @JMXBeanAttribute(name = "Restore Bucket 120", description = "Restorations within 120s")
  public long getRestoreBucket120() {
    return stats.getCumulativeCount(9);
  }

  @JMXBeanAttribute(name = "Restore Bucket 300", description = "Restorations within 300s")
  public long getRestoreBucket300() {
    return stats.getCumulativeCount(10);
  }

  // Same value as Restore Count; histogram_quantile needs an explicit +Inf bucket series.
  @JMXBeanAttribute(name = "Restore Bucket +Inf", description = "All restorations that reached this stage")
  public long getRestoreBucketInf() {
    return stats.getCount();
  }
}
