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

package io.mapsmessaging.network.route.select;

import lombok.Builder;
import lombok.Getter;

/**
 * Immutable weights for the composite cost. All terms are linear; lower cost is better.
 */
@Getter
@Builder(toBuilder = true)
public class CostWeights {
  @Builder.Default
  private final double weightLatencyMillis = 1.0;
  @Builder.Default
  private final double weightLossRatio = 400.0;
  @Builder.Default
  private final double weightJitterMillis = 0.5;
  @Builder.Default
  private final double weightErrorRate = 300.0;
  @Builder.Default
  private final double weightOutboundQueuePenalty = 0.05;
  @Builder.Default
  private final double weightStaleMetricsPenalty = 200.0;

  /**
   * Clamp for queue depth to prevent runaway cost.
   */
  @Builder.Default
  private final int maxQueueDepthForPenalty = 10_000;
}

