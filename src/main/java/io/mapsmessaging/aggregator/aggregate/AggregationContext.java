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
import io.mapsmessaging.dto.rest.config.aggregator.AggregatorInputConfigDTO;
import lombok.Getter;
import lombok.NonNull;

@Getter
public class AggregationContext {

  private final String aggregatorName;
  private final String outputTopic;
  private final long windowStartMillis;
  private final long windowEndMillis;
  private final boolean closedByAllInputs;

  private final AggregatorInputConfigDTO[] inputConfigs;

  private final MessageCodec messageCodec;
  private final MessageBuilder messageBuilder;

  public AggregationContext(
      @NonNull String aggregatorName,
      @NonNull String outputTopic,
      long windowStartMillis,
      long windowEndMillis,
      boolean closedByAllInputs,
      @NonNull AggregatorInputConfigDTO[] inputConfigs,
      @NonNull MessageCodec messageCodec,
      @NonNull MessageBuilder messageBuilder
  ) {
    this.aggregatorName = aggregatorName;
    this.outputTopic = outputTopic;
    this.windowStartMillis = windowStartMillis;
    this.windowEndMillis = windowEndMillis;
    this.closedByAllInputs = closedByAllInputs;
    this.inputConfigs = inputConfigs;
    this.messageCodec = messageCodec;
    this.messageBuilder = messageBuilder;
  }
}
