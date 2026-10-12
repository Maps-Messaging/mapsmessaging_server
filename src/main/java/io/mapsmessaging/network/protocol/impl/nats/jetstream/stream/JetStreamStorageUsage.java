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

package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream;

import io.mapsmessaging.dto.rest.stats.LinkedMovingAverageRecordDTO;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.resources.ResourceStatistics;

import java.util.Map;

/**
 * Reads an existing MAPS storage metric without introducing a shared engine API.
 *
 * This is a sampled physical storage size, not JetStream's exact logical retained
 * message size. MAPS only publishes it when resource statistics are enabled, and
 * the memory storage implementation does not measure its resident bytes.
 * Zero therefore means either no measured bytes or that the metric is unavailable.
 * Accurate JetStream byte accounting remains a separate protocol-level requirement.
 */
public final class JetStreamStorageUsage {

  private static final String STORE = "store";
  private static final String TOTAL_SIZE = "Total Size";

  private JetStreamStorageUsage() {
  }

  public static long sampledPhysicalBytes(DestinationImpl destination) {
    ResourceStatistics statistics = destination.getResourceStatistics();
    if (statistics == null) {
      return 0;
    }
    Map<String, Map<String, LinkedMovingAverageRecordDTO>> groups = statistics.getStatistics();
    if (groups == null) {
      return 0;
    }
    Map<String, LinkedMovingAverageRecordDTO> storage = groups.get(STORE);
    if (storage == null) {
      return 0;
    }
    LinkedMovingAverageRecordDTO totalSize = storage.get(TOTAL_SIZE);
    return totalSize == null ? 0 : Math.max(0, totalSize.getCurrent());
  }
}
