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
import io.mapsmessaging.utilities.admin.JMXManager;

import javax.management.ObjectInstance;
import java.util.List;

/**
 * TAK output leg metrics, registered as {@code io.mapsmessaging:type=Integration,name=TakOutput}.
 * Latency buckets are cumulative (Prometheus histogram style); the exporter rule maps the number
 * in the attribute name onto the {@code le} label.
 */
@JMXBean(description = "TAK output connection, drop and latency metrics")
public class TakOutputJMX {

  private final EventPublisher eventPublisher;
  private final ObjectInstance mbean;

  TakOutputJMX(EventPublisher eventPublisher) {
    this.eventPublisher = eventPublisher;
    this.mbean = JMXManager.getInstance().register(this, List.of("type=Integration", "name=TakOutput"));
  }

  void close() {
    JMXManager.getInstance().unregister(mbean);
  }

  @JMXBeanAttribute(name = "Socket Connect Count", description = "Successful TAK server connections established")
  public long getSocketConnectCount() {
    return TakOutputStats.CONNECT_COUNT.sum();
  }

  @JMXBeanAttribute(name = "Socket Connect Failure Count", description = "Failed TAK server connection attempts")
  public long getSocketConnectFailureCount() {
    return TakOutputStats.CONNECT_FAILURE_COUNT.sum();
  }

  @JMXBeanAttribute(name = "Socket Disconnect Count", description = "Established TAK server connections that were torn down")
  public long getSocketDisconnectCount() {
    return TakOutputStats.DISCONNECT_COUNT.sum();
  }

  @JMXBeanAttribute(name = "Socket Connected Count", description = "TAK server connections currently open")
  public long getSocketConnectedCount() {
    return TakOutputStats.CONNECT_COUNT.sum() - TakOutputStats.DISCONNECT_COUNT.sum();
  }

  @JMXBeanAttribute(name = "Socket Last Write Age Millis", description = "Milliseconds since CoT was last written to a TAK server, -1 if never")
  public long getSocketLastWriteAgeMillis() {
    return TakOutputStats.getLastWriteAgeMillis();
  }

  @JMXBeanAttribute(name = "Socket Dropped Count", description = "CoT events dropped because the TAK socket send queue was full")
  public long getSocketDroppedCount() {
    return TakOutputStats.SOCKET_DROPPED_COUNT.sum();
  }

  @JMXBeanAttribute(name = "Publisher Dropped Count", description = "CoT events dropped because the internal CoT topic publish queue was full")
  public long getPublisherDroppedCount() {
    return eventPublisher == null ? 0 : eventPublisher.getDroppedEventCount();
  }

  @JMXBeanAttribute(name = "Latency Count", description = "Twin updates timed from receipt to CoT composition")
  public long getLatencyCount() {
    return TakOutputStats.getLatencyCount();
  }

  @JMXBeanAttribute(name = "Latency Sum Seconds", description = "Total receipt-to-CoT latency across all timed updates")
  public double getLatencySumSeconds() {
    return TakOutputStats.getLatencySumSeconds();
  }

  @JMXBeanAttribute(name = "Latency Bucket 0.005", description = "Updates with latency <= 5ms")
  public long getLatencyBucket0005() {
    return TakOutputStats.getLatencyCumulativeCount(0);
  }

  @JMXBeanAttribute(name = "Latency Bucket 0.01", description = "Updates with latency <= 10ms")
  public long getLatencyBucket001() {
    return TakOutputStats.getLatencyCumulativeCount(1);
  }

  @JMXBeanAttribute(name = "Latency Bucket 0.025", description = "Updates with latency <= 25ms")
  public long getLatencyBucket0025() {
    return TakOutputStats.getLatencyCumulativeCount(2);
  }

  @JMXBeanAttribute(name = "Latency Bucket 0.05", description = "Updates with latency <= 50ms")
  public long getLatencyBucket005() {
    return TakOutputStats.getLatencyCumulativeCount(3);
  }

  @JMXBeanAttribute(name = "Latency Bucket 0.1", description = "Updates with latency <= 100ms")
  public long getLatencyBucket01() {
    return TakOutputStats.getLatencyCumulativeCount(4);
  }

  @JMXBeanAttribute(name = "Latency Bucket 0.25", description = "Updates with latency <= 250ms")
  public long getLatencyBucket025() {
    return TakOutputStats.getLatencyCumulativeCount(5);
  }

  @JMXBeanAttribute(name = "Latency Bucket 0.5", description = "Updates with latency <= 500ms")
  public long getLatencyBucket05() {
    return TakOutputStats.getLatencyCumulativeCount(6);
  }

  @JMXBeanAttribute(name = "Latency Bucket 1", description = "Updates with latency <= 1s")
  public long getLatencyBucket1() {
    return TakOutputStats.getLatencyCumulativeCount(7);
  }

  @JMXBeanAttribute(name = "Latency Bucket 2.5", description = "Updates with latency <= 2.5s")
  public long getLatencyBucket25() {
    return TakOutputStats.getLatencyCumulativeCount(8);
  }

  @JMXBeanAttribute(name = "Latency Bucket 5", description = "Updates with latency <= 5s")
  public long getLatencyBucket5() {
    return TakOutputStats.getLatencyCumulativeCount(9);
  }

  @JMXBeanAttribute(name = "Latency Bucket 10", description = "Updates with latency <= 10s")
  public long getLatencyBucket10() {
    return TakOutputStats.getLatencyCumulativeCount(10);
  }

  // Same value as Latency Count; histogram_quantile needs an explicit +Inf bucket series.
  @JMXBeanAttribute(name = "Latency Bucket +Inf", description = "All timed updates")
  public long getLatencyBucketInf() {
    return TakOutputStats.getLatencyCount();
  }
}
