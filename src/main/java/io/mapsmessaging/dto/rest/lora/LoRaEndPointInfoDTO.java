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

package io.mapsmessaging.dto.rest.lora;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(
    title = "LoRa Endpoint Information",
    description =
        "Provides information about a LoRa endpoint, including node ID, RSSI, and queue size.")
public class LoRaEndPointInfoDTO {

  @Schema(
      title = "Node ID",
      description = "Unique identifier for the LoRa node.",
      example = "1",
      minimum = "0")
  private int nodeId;

  @Schema(
      title = "Last RSSI",
      description =
          "The most recent Received Signal Strength Indicator (RSSI) value for this endpoint.",
      example = "-70",
      minimum = "-200",
      maximum = "0")
  private int lastRSSI;

  @Schema(
      title = "Incoming Queue Size",
      description = "The size of the incoming message queue for this endpoint.",
      example = "10",
      minimum = "0")
  private int incomingQueueSize;

  @Schema(
      title = "Connection Size",
      description = "The number of active connections for this endpoint.",
      example = "5",
      minimum = "0")
  private int connectionSize;

  @Schema(
      title = "Last read operation",
      description = "The last time a packet was received")
  private long lastRead;


  @Schema(
      title = "Last write operation",
      description = "The last time a packet was sent")
  private long lastWrite;

}
