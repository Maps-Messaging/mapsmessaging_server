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

package io.mapsmessaging.dto.rest.config.ml;

import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@Schema(description = "Model event stream configuration")
public class MLEventStreamDTO extends BaseConfigDTO {

  @Schema(
      description = "Unique ID for the model stream",
      example = "weather-outliers",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  private String id;

  @Schema(
      description = "Topic filter to match incoming events",
      example = "/weather/#",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  private String topicFilter;

  @Schema(
      description = "Schema ID that the event must match",
      example = "weather.v1",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  private String schemaId;

  @Schema(
      description = "Selector used to evaluate events",
      example = "temperature > 35",
      defaultValue = "",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  private String selector = "";

  @Schema(
      description = "Where to publish outliers",
      example = "/ml/outliers/weather",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  private String outlierTopic;

  @Schema(
      description = "Max number of events to train the model",
      example = "1000",
      minimum = "100",
      maximum = "1000000",
      defaultValue = "1000",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  private int maxTrainEvents = 1000;

  @Schema(
      description = "Max time in seconds to train the model, 0 disables",
      defaultValue = "2400",
      minimum = "0",
      maximum = "86400",
      example = "600",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  private int maxTrainTimeSeconds = 2400;

  @Schema(
      description = "Outlier rate threshold to trigger retraining (0.0 to 1.0)",
      example = "0.05",
      minimum = "0",
      maximum = "1",
      defaultValue = "0.05",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  private double retrainThreshold = 0.05;
}
