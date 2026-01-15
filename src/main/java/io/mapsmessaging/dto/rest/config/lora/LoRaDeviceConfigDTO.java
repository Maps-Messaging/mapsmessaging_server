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

package io.mapsmessaging.dto.rest.config.lora;

import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.network.SerialDeviceDTO;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper=false)
@NoArgsConstructor

@Schema(
    description = "LoRa Device Configuration DTO",
    extensions = {
        @Extension(
            name = "x-maps-oneOfRequired",
            properties = {
                @ExtensionProperty(name = "fields", value = "hardware,serialDevice")
            }
        )
    }
)
public class LoRaDeviceConfigDTO extends BaseConfigDTO {

  @Schema(
      description = "Name of the LoRa device",
      example = "LoRaNode1",
      nullable = false,
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  protected String name;

  @Schema(
      description = "Power setting for the device",
      example = "14",
      defaultValue = "1",
      minimum = "0",
      maximum = "16"
  )
  protected int power =1;

  @Schema(
      description = "Operating frequency of the device in MHz",
      example = "868.0",
      allowableValues = {"863", "902", "915", "470", "923", "865", "920"},
      minimum = "863",
      maximum = "923",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  protected float frequency;

  @Schema(
      description = "LoRa hardware configuration",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected LoRaHardwareConfigDTO hardware;

  @Schema(
      description = "Serial device configuration",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected SerialDeviceDTO serialDevice;
}
