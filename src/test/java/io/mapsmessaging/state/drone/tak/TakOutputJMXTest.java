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

import io.mapsmessaging.utilities.admin.JMXManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TakOutputJMXTest {

  @Test
  void topic_publisher_drops_and_composition_latency_remain_observable() {
    boolean enabled = JMXManager.isEnableJMX();
    JMXManager.setEnableJMX(false);
    EventPublisher publisher = mock(EventPublisher.class);
    when(publisher.getDroppedEventCount()).thenReturn(7L);
    TakOutputJMX output = new TakOutputJMX(publisher);
    try {
      long before = output.getLatencyCount();
      long bucketBefore = output.getLatencyBucket0025();
      double sumBefore = output.getLatencySumSeconds();

      TakOutputStats.recordLatencyMillis(25);

      assertEquals(7L, output.getPublisherDroppedCount());
      assertEquals(1L, output.getLatencyCount() - before);
      assertEquals(1L, output.getLatencyBucket0025() - bucketBefore);
      assertEquals(0.025, output.getLatencySumSeconds() - sumBefore, 1e-9);
      assertEquals(output.getLatencyCount(), output.getLatencyBucketInf());
    } finally {
      output.close();
      JMXManager.setEnableJMX(enabled);
    }
  }

  @Test
  void absent_publisher_reports_no_topic_queue_drops() {
    boolean enabled = JMXManager.isEnableJMX();
    JMXManager.setEnableJMX(false);
    TakOutputJMX output = new TakOutputJMX(null);
    try {
      assertEquals(0L, output.getPublisherDroppedCount());
    } finally {
      output.close();
      JMXManager.setEnableJMX(enabled);
    }
  }
}
