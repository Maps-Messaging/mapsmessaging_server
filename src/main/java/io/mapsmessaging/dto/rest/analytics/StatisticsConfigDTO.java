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

package io.mapsmessaging.dto.rest.analytics;

import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    title = "Analytics",
    description = "Configures the event stream statistics analytics")
public class StatisticsConfigDTO extends BaseConfigDTO {
  @Schema(
      title = "name of the statistic engine to run",
      description = "The number of events to process before emitting an event containing the data",
      example = "Advanced",
      nullable = false)
  protected String statisticName;

  @Schema(
      title = "Number of events",
      description = "The number of events to process before emitting an event containing the data",
      example = "100",
      nullable = false)
  protected int eventCount;

  @Schema(
      title = "Ignore List",
      description = "Lists the keys that should be ignored from the event and not part of the resultant statistics, Comma seperated",
      example = "modelName,serialNumber",
      nullable = true)
  protected List<String> ignoreList;

  @Schema(
      title = "Key List",
      description = "Specific set of keys to use rather than auto discovery this is used to refine the keys used",
      example = "temperature, humidity",
      nullable = true)
  protected List<String> keyList;
}
