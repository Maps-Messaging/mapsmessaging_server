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

package io.mapsmessaging.dto.rest.config;

import io.mapsmessaging.dto.rest.config.device.I2CBusConfigDTO;
import io.mapsmessaging.dto.rest.config.device.OneWireBusConfigDTO;
import io.mapsmessaging.dto.rest.config.device.SerialBusConfigDTO;
import io.mapsmessaging.dto.rest.config.device.SpiDeviceBusConfigDTO;
import io.mapsmessaging.dto.rest.config.device.triggers.BaseTriggerConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Device Manager Configuration DTO")
@Getter
@Setter
public class DeviceManagerConfigDTO extends BaseConfigDTO implements ConfigurationManagerDTO {

  @Schema(
      description = "Indicates if the device manager is enabled",
      example = "true",
      defaultValue = "true",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  protected boolean enabled;

  @Schema(
      description = "Indicates if the device manager will load the demo devices",
      example = "false",
      defaultValue = "false",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected boolean demoEnabled;

  @Schema(
      description = "List of trigger configurations",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      defaultValue = "[]",
      nullable = true
  )
  protected List<BaseTriggerConfigDTO> triggers;

  @Schema(
      description = "List of I2C bus configurations",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected List<I2CBusConfigDTO> i2cBuses;

  @Schema(
      description = "SPI bus configuration",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected SpiDeviceBusConfigDTO spiBus;

  @Schema(
      description = "OneWire bus configuration",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected OneWireBusConfigDTO oneWireBus;

  @Schema(
      description = "Serial device configuration",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected SerialBusConfigDTO serialDeviceBusConfig;

}
