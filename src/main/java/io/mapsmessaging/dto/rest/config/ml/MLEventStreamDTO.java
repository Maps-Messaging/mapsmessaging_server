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

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MLEventStreamDTO {

  @Schema(description = "Unique ID for the model stream")
  private String id;

  @Schema(description = "Topic filter to match incoming events")
  private String topicFilter;

  @Schema(description = "Schema ID that the event must match")
  private String schemaId;

  @Schema(description = "Selector used to evaluate events")
  private String selector;

  @Schema(description = "Where to publish outliers")
  private String outlierTopic;

  @Schema(description = "Max number of events to train the model")
  private int maxTrainEvents;

  @Schema(description = "Max time in seconds to train the model, 0 disables")
  private int maxTrainTimeSeconds;

  @Schema(description = "Outlier rate threshold to trigger retraining")
  private double retrainThreshold;
}