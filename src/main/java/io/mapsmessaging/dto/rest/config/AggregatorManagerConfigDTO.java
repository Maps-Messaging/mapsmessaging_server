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

package io.mapsmessaging.dto.rest.config;

import io.mapsmessaging.dto.rest.config.aggregator.AggregatorConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Aggregator Manager Configuration DTO (Stage 1)")
public class AggregatorManagerConfigDTO extends BaseManagerConfigDTO {

  @Schema(
      description = "Number of worker stripes (threads) used to run aggregators. 0 means auto (CPU heuristic).",
      example = "0",
      defaultValue = "0",
      minimum = "0",
      maximum = "128",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  protected int stripeCount = 0;

  @Schema(
      description = "Maximum number of events to drain per aggregator per scheduler pass (fairness limit).",
      example = "128",
      defaultValue = "128",
      minimum = "1",
      maximum = "8192",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  protected int maxBatchPerAggregator = 128;

  @Schema(
      description = "Scheduler idle sleep in milliseconds when no work is detected.",
      example = "1",
      defaultValue = "1",
      minimum = "0",
      maximum = "1000",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  protected int idleSleepMs = 1;

  @Schema(
      description = "Default mailbox capacity for each aggregator (bounded safety buffer).",
      example = "8192",
      defaultValue = "8192",
      minimum = "1",
      maximum = "1048576",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  protected int mailboxCapacity = 8192;

  @Schema(
      description = "Maximum number of aggregators allowed to be loaded (safety guardrail). 0 means unlimited.",
      example = "0",
      defaultValue = "0",
      minimum = "0",
      maximum = "1000000",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  protected int maxAggregators = 0;

  @Schema(
      description = "List of aggregator instance configurations",
      requiredMode = Schema.RequiredMode.REQUIRED,
      minLength = 1
  )
  protected List<AggregatorConfigDTO> aggregatorConfigList;

  public AggregatorManagerConfigDTO() {
    super("AggreagtorManagerConfigDTO");
  }

  @Override
  public String getSimpleName() {
    return "Aggregations";
  }
}
