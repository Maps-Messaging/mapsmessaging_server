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

package io.mapsmessaging.license.features;

import lombok.Data;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Hardware feature configuration for the license. All fields are required.")
public class Hardware {

  @Schema(
      description = "Enable I2C device support.",
      example = "true",
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  private boolean i2c;

  @Schema(
      description = "Enable SPI device support.",
      example = "true",
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  private boolean spi;

  @Schema(
      description = "Enable OneWire device support.",
      example = "false",
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  private boolean oneWire;

  @Schema(
      description = "Enable serial hardware support.",
      example = "true",
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  private boolean serial;
}