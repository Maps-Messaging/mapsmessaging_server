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

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(
    title = "LoRa Device Information",
    description =
        "Provides detailed information about a LoRa device, including sent and received data statistics and endpoint details.")
public class LoRaDeviceInfoDTO {

  @Schema(
      title = "Device Name",
      description = "The name of the LoRa device.",
      example = "LoRaDevice_01",
      nullable = false)
  private String name;

  @Schema(
      title = "Radio Type",
      description = "Type of radio module used by the LoRa device.",
      example = "SX1276",
      nullable = false)
  private String radio;

  @Schema(
      title = "Bytes Sent",
      description = "Total number of bytes sent by the LoRa device.",
      example = "1048576",
      minimum = "0")
  private long bytesSent;

  @Schema(
      title = "Bytes Received",
      description = "Total number of bytes received by the LoRa device.",
      example = "2048000",
      minimum = "0")
  private long bytesReceived;

  @Schema(
      title = "Packets Sent",
      description = "Total number of packets sent by the LoRa device.",
      example = "500",
      minimum = "0")
  private long packetsSent;

  @Schema(
      title = "Packets Received",
      description = "Total number of packets received by the LoRa device.",
      example = "480",
      minimum = "0")
  private long packetsReceived;

  @Schema(
      title = "Endpoint Information List",
      description =
          "A list of endpoint information for the device, detailing each endpoint’s status and metrics.",
      nullable = true)
  private List<LoRaEndPointInfoDTO> endPointInfoList;
}
