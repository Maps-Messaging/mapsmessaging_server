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
    title = "Server Statistics",
    description =
        "Contains various metrics and statistics for server performance, including message rates, connection counts, and data throughput.")
public class ServerStatisticsDTO {

  @Schema(description = "Total packets sent", example = "1024")
  private long packetsSent;

  @Schema(description = "Total packets received", example = "2048")
  private long packetsReceived;

  @Schema(description = "Total read bytes", example = "5242880")
  private long totalReadBytes;

  @Schema(description = "Total write bytes", example = "4194304")
  private long totalWriteBytes;

  @Schema(description = "Total connections", example = "150")
  private long totalConnections;

  @Schema(description = "Total disconnections", example = "145")
  private long totalDisconnections;

  @Schema(description = "Total messages with no interest", example = "10")
  private long totalNoInterestMessages;

  @Schema(description = "Total subscribed messages", example = "5000")
  private long totalSubscribedMessages;

  @Schema(description = "Total published messages", example = "6000")
  private long totalPublishedMessages;

  @Schema(description = "Total retrieved messages", example = "2500")
  private long totalRetrievedMessages;

  @Schema(description = "Total expired messages", example = "20")
  private long totalExpiredMessages;

  @Schema(description = "Total delivered messages", example = "4000")
  private long totalDeliveredMessages;

  @Schema(description = "Published messages per second", example = "50")
  private float publishedPerSecond;

  @Schema(description = "Subscribed messages per second", example = "45")
  private float subscribedPerSecond;

  @Schema(description = "No interest messages per second", example = "5")
  private float noInterestPerSecond;

  @Schema(description = "Delivered messages per second", example = "60")
  private float deliveredPerSecond;

  @Schema(description = "Retrieved messages per second", example = "30")
  private float retrievedPerSecond;

  @Schema(
      description = "Statistics map",
      example =
          "{\"latency\": {\"name\": \"latency\", \"unitName\": \"ms\", \"current\": 10, ...}}")
  private Map<String, LinkedMovingAverageRecordDTO> stats;
}
