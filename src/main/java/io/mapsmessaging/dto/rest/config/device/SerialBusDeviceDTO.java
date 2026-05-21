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

package io.mapsmessaging.dto.rest.config.device;

import io.mapsmessaging.dto.rest.config.network.SerialDeviceDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper=false)
@NoArgsConstructor
@Schema(description = "Serial Bus Configuration DTO")
public class SerialBusDeviceDTO extends DeviceBusConfigDTO {

  @Schema(
      description = "Name of the Serial Device",
      example = "SEN0640",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  protected String name;

  @Schema(
      description = "Serial port configuration",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  protected SerialDeviceDTO serialConfig;



  @Schema(
      description = "Read timeout in milliseconds",
      example = "60000",
      minimum = "1000",
      maximum = "600000",
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  protected int readTimeOut = 60000;

  @Schema(
      description = "Write timeout in milliseconds",
      example = "60000",
      minimum = "1000",
      maximum = "600000",
      defaultValue = "60000",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  protected int writeTimeOut = 60000;
}
