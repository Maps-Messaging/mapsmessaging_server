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

package io.mapsmessaging.dto.rest.config.device;

import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper=false)
@NoArgsConstructor
@Schema(description = "SPI Device Configuration DTO")
public class SpiDeviceConfigDTO extends BaseConfigDTO {

  @Schema(description = "Device address on the SPI bus", example = "1")
  protected int address;

  @Schema(description = "Name of the SPI device", example = "TemperatureSensor")
  protected String name;

  @Schema(description = "Selector used for the device", example = "tempSelector")
  protected String selector;

  @Schema(description = "SPI bus number", example = "0")
  protected int spiBus;

  @Schema(description = "SPI mode for the device", example = "1")
  protected int spiMode;

  @Schema(description = "Chip select line for the SPI device", example = "0")
  protected int spiChipSelect;
}
