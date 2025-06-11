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

import java.util.List;

@Data
@EqualsAndHashCode(callSuper=false)
@NoArgsConstructor
@Schema(description = "SPI Device Bus Configuration DTO")
public class SpiDeviceBusConfigDTO extends BaseConfigDTO {

  @Schema(description = "Name of the SPI bus", example = "spiBus1")
  protected String name;

  @Schema(description = "Enable or disable auto-scanning of SPI devices", example = "true")
  protected boolean autoScan;

  @Schema(description = "List of SPI devices on this bus")
  protected List<SpiDeviceConfigDTO> devices;

  @Schema(description = "Indicates if the device bus is enabled")
  protected boolean enabled;

  @Schema(description = "Template for the topic name")
  protected String topicNameTemplate;

  @Schema(description = "Scan time interval in milliseconds")
  protected int scanTime;

  @Schema(description = "Filter configuration for the device bus")
  protected String filter;

  @Schema(description = "Selector configuration for the device bus")
  protected String selector;
}
