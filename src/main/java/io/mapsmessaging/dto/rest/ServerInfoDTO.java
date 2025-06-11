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

package io.mapsmessaging.dto.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(
    title = "Status Message",
    description =
        "Provides detailed status information about the server, including memory usage, CPU statistics, and thread states.")
public class ServerInfoDTO {

  @Schema(description = "Server name", example = "maps-server")
  private String serverName;

  @Schema(description = "Build version of the server", example = "3.3.7")
  private String version;

  @Schema(description = "Build date of the server", example = "2024-10-13")
  private String buildDate;

  @Schema(description = "Total memory in bytes", example = "536870912")
  private long totalMemory;

  @Schema(description = "Maximum memory in bytes", example = "1073741824")
  private long maxMemory;

  @Schema(description = "Free memory in bytes", example = "268435456")
  private long freeMemory;

  @Schema(description = "Number of active threads", example = "120")
  private int numberOfThreads;

  @Schema(
      description = "Time taken to create the status message, in nanoseconds",
      example = "1000000")
  private long timeToCreateNano;

  @Schema(description = "Server uptime in milliseconds", example = "123456789")
  private long uptime;

  @Schema(description = "Total connections count", example = "150")
  private long connections;

  @Schema(description = "Total destinations count", example = "30")
  private long destinations;

  @Schema(description = "CPU time in nanoseconds", example = "1234567890")
  private long cpuTime;

  @Schema(description = "CPU usage percentage", example = "12.5")
  private float cpuPercent;

  @Schema(description = "Storage size in bytes", example = "104857600")
  private long storageSize;

  @Schema(
      description = "Map of thread states and their counts",
      example = "{\"RUNNABLE\": 50, \"WAITING\": 10}")
  private Map<String, Integer> threadState = new LinkedHashMap<>();
}
