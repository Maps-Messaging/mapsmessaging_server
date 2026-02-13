/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commonsclause
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

package io.mapsmessaging.dto.rest.config.aggregator;

import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Schema(
    title = "Aggregator Configuration DTO (Stage 1)",
    description = "Stage 1 Aggregator configuration. Fan-in time-bucket aggregation only: no correlation, no DLQ, no policies, no persistence."
)
public class AggregatorConfigDTO extends BaseConfigDTO {

  @Schema(
      description = "Unique name of this aggregator instance",
      example = "sensor-aggregator-1",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false,
      minLength = 1,
      maxLength = 128,
      pattern = "^[A-Za-z0-9_.-]+$"
  )
  protected String name;

  @Schema(
      description = "Enable or disable this aggregator instance",
      example = "true",
      defaultValue = "true",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected boolean enabled = true;

  @Schema(
      description = "Input configurations (one per topic) for this aggregator",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false,
      minLength = 1
  )
  protected List<AggregatorInputConfigDTO> inputs;

  @Schema(
      description = "Output topic for the aggregated envelope",
      example = "maps/aggregated/out",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false,
      minLength = 1,
      maxLength = 2048
  )
  protected String outputTopic;

  @Schema(
      description = "Time bucket duration in milliseconds (arrival-time based)",
      example = "1000",
      minimum = "1",
      maximum = "3600000",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  protected long windowDurationMs;

  @Schema(
      description = "Maximum time to wait before closing a window, even if not all inputs have arrived (milliseconds)",
      example = "5000",
      minimum = "1",
      maximum = "3600000",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  protected long timeoutMs;

  @Schema(
      description = "Maximum number of events to buffer per input within a single window (Stage 1 default: 1)",
      example = "1",
      defaultValue = "1",
      minimum = "1",
      maximum = "1000",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected int maxEventsPerTopic = 1;

  @Schema(
      description = "Output transformer chain configuration (array of objects). Applied to the aggregated envelope before publish.",
      type = "array",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true,
      example = "[{\"name\":\"JsonQuery\",\"parameters\":{\"query\":\"[\\\"object\\\",{\\\"window\\\":[\\\"get\\\",\\\"windowId\\\"],\\\"payload\\\":[\\\"get\\\",\\\"payload\\\"]}]\"}}]"
  )
  protected List<Map<String, Object>> outputTransformer;

}
