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

package io.mapsmessaging.dto.rest.config.network.impl;

import io.mapsmessaging.dto.rest.config.network.EndPointConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper=false)
@NoArgsConstructor
public class LoRaConfigDTO extends EndPointConfigDTO {

  @Schema(description = "Name of the LoRa device", example = "LoRaNode1")
  protected String name;

  @Schema(description = "Power setting for the device", example = "14")
  protected int power;

  @Schema(description = "Operating frequency of the device in MHz", example = "868.0")
  protected float frequency;

  @Schema(description = "Base address to register for, 1-254", example = "2")
  protected int address;

  @Schema(description = "Transmission rate to limit the number of packets/second, 0 - unlimited, else per second", example = "5")
  protected int transmissionRate;

  @Schema(description = "Optional hex based 16 byte key", example = "0x0 0x0 0x0 0x0 0x0 0x0 0x0 0x0 0x0 0x0 0x0 0x0 0x0 0x0 0x0 0x0")
  protected String hexKey;

}
