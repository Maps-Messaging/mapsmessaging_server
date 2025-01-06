/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2025 ] [Maps Messaging B.V.]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.dto.rest.config.lora;

import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper=false)
@NoArgsConstructor
@Schema(description = "LoRa Device Configuration DTO")
public class LoRaDeviceConfigDTO extends BaseConfigDTO {

  @Schema(description = "Name of the LoRa device", example = "LoRaNode1")
  protected String name;

  @Schema(description = "Radio type of the LoRa device", example = "SX1276")
  protected String radio;

  @Schema(description = "Chip Select (CS) pin number", example = "10")
  protected int cs;

  @Schema(description = "IRQ pin number", example = "7")
  protected int irq;

  @Schema(description = "Reset (RST) pin number", example = "3")
  protected int rst;

  @Schema(description = "Power setting for the device", example = "14")
  protected int power;

  @Schema(description = "CAD timeout setting", example = "500")
  protected int cadTimeout;

  @Schema(description = "Operating frequency of the device in MHz", example = "868.0")
  protected float frequency;
}
