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
    title = "LoRa Endpoint Connection Information",
    description =
        "Represents connection metrics and information for a LoRa endpoint connection, including signal strength and packet details.")
public class LoRaEndPointConnectionInfoDTO {

  @Schema(
      title = "RSSI",
      description = "Received Signal Strength Indicator (RSSI) for the connection.",
      example = "-70",
      minimum = "-200",
      maximum = "0")
  private long rssi;

  @Schema(
      title = "Missed Packets",
      description = "The number of packets that were missed or lost.",
      example = "3",
      minimum = "0")
  private long missedPackets;

  @Schema(
      title = "Received Packets",
      description = "The total number of packets successfully received.",
      example = "500",
      minimum = "0")
  private long receivedPackets;

  @Schema(
      title = "Remote Node ID",
      description = "The identifier of the remote node in the connection.",
      example = "2",
      minimum = "0")
  private int remoteNodeId;

  @Schema(
      title = "Last Packet ID",
      description = "The identifier of the last packet received.",
      example = "1000",
      minimum = "0")
  private long lastPacketId;

  @Schema(
      title = "Last Read Time",
      description = "The timestamp of the last read operation from this connection.",
      example = "1625812345678")
  private long lastReadTime;

  @Schema(
      title = "Last Write Time",
      description = "The timestamp of the last write operation to this connection.",
      example = "1625812345678")
  private long lastWriteTime;
}
