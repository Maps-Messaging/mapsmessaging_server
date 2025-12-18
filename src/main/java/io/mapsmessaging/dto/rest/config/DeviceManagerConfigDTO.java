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

package io.mapsmessaging.dto.rest.config;

import io.mapsmessaging.config.device.SerialDeviceBusConfig;
import io.mapsmessaging.dto.rest.config.device.I2CBusConfigDTO;
import io.mapsmessaging.dto.rest.config.device.OneWireBusConfigDTO;
import io.mapsmessaging.dto.rest.config.device.SpiDeviceBusConfigDTO;
import io.mapsmessaging.dto.rest.config.device.triggers.BaseTriggerConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Schema(description = "Device Manager Configuration DTO")
public class DeviceManagerConfigDTO extends BaseConfigDTO {

  @Schema(description = "Indicates if the device manager is enabled", example = "true")
  protected boolean enabled;

  @Schema(description = "Indicates if the device manager will load the demo devices", example = "false")
  protected boolean demoEnabled;

  @Schema(description = "List of trigger configurations")
  protected List<BaseTriggerConfigDTO> triggers;

  @Schema(description = "List of I2C bus configurations")
  protected List<I2CBusConfigDTO> i2cBuses;

  @Schema(description = "SPI bus configuration")
  protected SpiDeviceBusConfigDTO spiBus;

  @Schema(description = "OneWire bus configuration")
  protected OneWireBusConfigDTO oneWireBus;

  @Schema(description = "Serial device configuration")
  protected SerialDeviceBusConfig serialDeviceBusConfig;

}
