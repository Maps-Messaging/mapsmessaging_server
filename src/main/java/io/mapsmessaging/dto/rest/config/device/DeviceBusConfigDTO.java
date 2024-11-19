/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2024 ] [Maps Messaging B.V.]
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

package io.mapsmessaging.dto.rest.config.device;

import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@ToString
@Schema(description = "DTO for DeviceBus configuration properties")
public class DeviceBusConfigDTO extends BaseConfigDTO {

  @Schema(description = "Indicates if the device bus is enabled")
  protected boolean enabled;

  @Schema(description = "Template for the topic name")
  protected String topicNameTemplate;

  @Schema(description = "Specifies if auto-scan is enabled")
  protected boolean autoScan;

  @Schema(description = "Scan time interval in milliseconds")
  protected int scanTime;

  @Schema(description = "Filter configuration for the device bus")
  protected String filter;

  @Schema(description = "Selector configuration for the device bus")
  protected String selector;
}
