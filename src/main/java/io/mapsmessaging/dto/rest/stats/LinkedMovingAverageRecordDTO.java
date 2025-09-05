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

package io.mapsmessaging.dto.rest.stats;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Schema(
    title = "Linked Moving Average Record",
    description =
        "Represents a record of moving average statistics, tracking metrics over a defined timespan with specific units.")
public class LinkedMovingAverageRecordDTO {

  @Schema(
      title = "Metric Name",
      description = "The name of the metric being recorded (e.g., 'latency', 'throughput').",
      example = "latency",
      nullable = false)
  private String name;

  @Schema(
      title = "Unit Name",
      description = "The unit of measurement for the metric (e.g., 'ms' for milliseconds).",
      example = "ms",
      nullable = false)
  private String unitName;

  @Schema(
      title = "Timespan",
      description = "The timespan over which the moving average is calculated, in milliseconds.",
      example = "60000",
      minimum = "0")
  private long timeSpan;

  @Schema(
      title = "Current Value",
      description = "The current moving average value for the metric.",
      example = "150",
      minimum = "0")
  private long current;

  @Schema(
      title = "Statistics Map",
      description =
          "A map containing additional statistical values, where each key is a descriptive label and each value is a measurement.",
      example = "{\"min\": 100, \"max\": 200, \"average\": 150}")
  private Map<String, Long> stats;
}
