/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
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

package io.mapsmessaging.network.route.link;

import java.time.Duration;
import java.time.Instant;
import java.util.OptionalDouble;

/**
 * Read-only snapshot of live metrics used by cost functions.
 * Implementations should be lightweight and updated periodically.
 */
public interface LinkMetrics {
  OptionalDouble getLatencyMillisEma();

  OptionalDouble getJitterMillisEma();

  double getLossRatio();

  double getErrorRate();

  int getOutboundQueueDepth();

  OptionalDouble getThroughputMibPerSecond();

  Instant getLastUpdated();

  Duration getWindow();
}
