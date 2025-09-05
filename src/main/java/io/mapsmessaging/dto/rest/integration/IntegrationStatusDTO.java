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

package io.mapsmessaging.dto.rest.integration;

import io.mapsmessaging.dto.rest.stats.LinkedMovingAverageRecordDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    title = "Integration Status",
    description =
        "Represents the status of an integration, including bytes and messages processed, connection state, errors, and performance statistics.")
public class IntegrationStatusDTO {

  @Schema(
      title = "Interface Name",
      description = "The name of the interface associated with this integration.",
      example = "myInterface",
      nullable = false)
  private String interfaceName;

  @Schema(
      title = "Bytes Sent",
      description = "The total number of bytes sent by the interface.",
      example = "123456",
      minimum = "0")
  private long bytesSent;

  @Schema(
      title = "Bytes Received",
      description = "The total number of bytes received by the interface.",
      example = "654321",
      minimum = "0")
  private long bytesReceived;

  @Schema(
      title = "Messages Sent",
      description = "The total number of messages sent by the interface.",
      example = "100",
      minimum = "0")
  private long messagesSent;

  @Schema(
      title = "Messages Received",
      description = "The total number of messages received by the interface.",
      example = "95",
      minimum = "0")
  private long messagesReceived;

  @Schema(
      title = "Connection Errors",
      description = "The total count of connection errors encountered.",
      example = "2",
      minimum = "0")
  private long errors;

  @Schema(
      title = "Last Read Time",
      description = "The timestamp of the last read operation.",
      example = "1625812345678")
  private long lastReadTime;

  @Schema(
      title = "Last Write Time",
      description = "The timestamp of the last write operation.",
      example = "1625812345678")
  private long lastWriteTime;

  @Schema(
      title = "Interface State",
      description = "The current state of the interface (e.g., active, inactive).",
      example = "active",
      nullable = false)
  private String state;

  @Schema(
      title = "Statistics",
      description = "A map of moving averages related to interface performance metrics.",
      example =
          "{\"averageRead\": {\"name\": \"averageRead\", \"unitName\": \"bytes\", \"current\": 50, ...}}",
      nullable = true)
  private Map<String, LinkedMovingAverageRecordDTO> statistics;
}
