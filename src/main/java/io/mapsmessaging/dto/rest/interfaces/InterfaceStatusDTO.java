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

package io.mapsmessaging.dto.rest.interfaces;

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
    title = "Interface Status",
    description =
        "Represents detailed statistics about an interface, including bytes and messages sent/received, connection count, and error counts.")
public class InterfaceStatusDTO {

  @Schema(
      title = "Interface Name",
      description = "Name of the interface",
      example = "myInterface",
      nullable = false)
  private String interfaceName;

  @Schema(
      title = "Total Bytes Sent",
      description = "Total number of bytes sent by the interface.",
      example = "1024000",
      minimum = "0")
  private long totalBytesSent;

  @Schema(
      title = "Total Bytes Received",
      description = "Total number of bytes received by the interface.",
      example = "2048000",
      minimum = "0")
  private long totalBytesReceived;

  @Schema(
      title = "Total Messages Sent",
      description = "Total number of messages sent by the interface.",
      example = "500",
      minimum = "0")
  private long totalMessagesSent;

  @Schema(
      title = "Total Messages Received",
      description = "Total number of messages received by the interface.",
      example = "480",
      minimum = "0")
  private long totalMessagesReceived;

  @Schema(
      title = "Bytes Sent per Second",
      description = "Number of bytes sent per second.",
      example = "1000",
      minimum = "0")
  private float bytesSent;

  @Schema(
      title = "Bytes Received per Second",
      description = "Number of bytes received per second.",
      example = "2000",
      minimum = "0")
  private float bytesReceived;

  @Schema(
      title = "Messages Sent per Second",
      description = "Number of messages sent per second.",
      example = "5",
      minimum = "0")
  private float messagesSent;

  @Schema(
      title = "Messages Received per Second",
      description = "Number of messages received per second.",
      example = "4",
      minimum = "0")
  private float messagesReceived;

  @Schema(
      title = "Current Connections",
      description = "Number of current connections.",
      example = "10",
      minimum = "0")
  private long connections;

  @Schema(
      title = "Connection Errors",
      description = "Total number of connection errors.",
      example = "3",
      minimum = "0")
  private long errors;

  @Schema(
      title = "Statistics",
      description = "A map of moving averages for various metrics.",
      nullable = true)
  private Map<String, LinkedMovingAverageRecordDTO> statistics;
}
