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

package io.mapsmessaging.dto.rest.devices;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    title = "Device Information",
    description =
        "Represents detailed information about a device, including its name, type, state, and description.")
public class DeviceInfoDTO {

  @Schema(
      title = "Device Name",
      description = "The unique name or identifier for the device.",
      example = "temperatureSensor01",
      nullable = false)
  private String name;

  @Schema(
      title = "Device Description",
      description = "A brief description of the device’s purpose or functionality.",
      example = "Temperature sensor for monitoring room temperature",
      nullable = true)
  private String description;

  @Schema(
      title = "Device Type",
      description = "The type or category of the device, indicating its general function or use.",
      example = "sensor",
      nullable = false)
  private String type;

  @Schema(
      title = "Device State",
      description =
          "Retrieves any state registers, could be sensor data or device state, is dependent on the device.",
      example = "25.0C",
      nullable = false)
  private String state;
}
