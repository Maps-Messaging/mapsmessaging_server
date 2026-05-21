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

package io.mapsmessaging.aggregator.aggregate;

import io.mapsmessaging.api.message.Message;
import lombok.NonNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AggregationEngine {

  private final Map<String, AggregationStrategy> strategyMap;

  public AggregationEngine() {
    this.strategyMap = new ConcurrentHashMap<>();
  }

  public void register(@NonNull AggregationStrategy strategy) {
    strategyMap.put(strategy.getName(), strategy);
  }

  public Message aggregate(@NonNull String strategyName, String[] topics, @NonNull Message[] contributions) {
    AggregationStrategy strategy = strategyMap.get(strategyName);
    if (strategy == null) {
      throw new IllegalArgumentException("Unknown aggregation strategy: " + strategyName);
    }
    return strategy.aggregate(topics, contributions);
  }
}
