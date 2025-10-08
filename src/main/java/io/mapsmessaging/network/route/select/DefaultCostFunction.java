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

import io.mapsmessaging.network.route.link.LinkMetrics;

import java.time.Duration;
import java.time.Instant;

/**
 * Default linear cost with light normalization and stale-metrics penalty.
 */
public final class DefaultCostFunction implements CostFunction {

  private static double clamp01(double value) {
    if (value < 0.0) return 0.0;
    if (value > 1.0) return 1.0;
    return value;
  }

  @Override
  public double compute(LinkMetrics linkMetrics, CostWeights costWeights) {
    double latencyMillis = linkMetrics.getLatencyMillisEma().orElse(1_000.0);
    double jitterMillis = linkMetrics.getJitterMillisEma().orElse(200.0);
    double lossRatio = clamp01(linkMetrics.getLossRatio());
    double errorRate = Math.max(0.0, linkMetrics.getErrorRate());

    int outboundQueueDepth = Math.max(0, linkMetrics.getOutboundQueueDepth());
    int cappedQueueDepth = Math.min(outboundQueueDepth, costWeights.getMaxQueueDepthForPenalty());

    double pricePerMebibyte = linkMetrics.getPricePerMebibyte().orElse(0.0);

    Instant lastUpdated = linkMetrics.getLastUpdated();
    Duration window = linkMetrics.getWindow();
    boolean metricsAreStale = lastUpdated.isBefore(Instant.now().minus(window.multipliedBy(2)));

    double cost = 0.0;
    cost += costWeights.getWeightLatencyMillis() * latencyMillis;
    cost += costWeights.getWeightLossRatio() * lossRatio;
    cost += costWeights.getWeightJitterMillis() * jitterMillis;
    cost += costWeights.getWeightErrorRate() * errorRate;
    cost += costWeights.getWeightOutboundQueuePenalty() * cappedQueueDepth;
    cost += costWeights.getWeightPricePerMebibyte() * pricePerMebibyte;

    if (metricsAreStale) {
      cost += costWeights.getWeightStaleMetricsPenalty();
    }
    return cost;
  }
}
