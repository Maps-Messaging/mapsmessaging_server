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

package io.mapsmessaging.dto.rest.config.protocol.impl;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Default transmit parameters for Semtech PULL_RESP (txpk) when outbound payload is not already Semtech JSON.")
public class SemtechTransmitDefaultsDTO {

  @Schema(description = "Transmit immediately (no scheduling)", example = "true")
  private boolean imme = true;

  @Schema(description = "Transmit frequency in MHz", example = "866.349812")
  private double freq = 866.349812;

  @Schema(description = "RF chain to use", example = "0")
  private int rfch = 0;

  @Schema(description = "Transmit power in dBm", example = "14")
  private int powe = 14;

  @Schema(description = "Modulation (LORA or FSK)", example = "LORA")
  private String modu = "LORA";

  @Schema(description = "LoRa datarate string (e.g., SF7BW125). For Semtech packet forwarder, datr is a string.", example = "SF7BW125")
  private String datr = "SF7BW125";

  @Schema(description = "LoRa coding rate (e.g., 4/5)", example = "4/5")
  private String codr = "4/5";

  @Schema(description = "Invert polarization (recommended true for LoRaWAN downlinks)", example = "true")
  private boolean ipol = true;
}