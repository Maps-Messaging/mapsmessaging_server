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

import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.hardware.device.filter.AlwaysSend;
import io.mapsmessaging.hardware.device.filter.OnChangeFilter;
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

  @Schema(
      description = "Indicates if the device bus is enabled",
      example = "true",
      requiredMode = Schema.RequiredMode.REQUIRED,
      defaultValue = "false",
      nullable = false
  )
  protected boolean enabled = false;

  @Schema(
      description = "Template for the topic name",
      requiredMode = Schema.RequiredMode.REQUIRED,
      pattern = "^/(?:[^/+#]+|\\+)(?:/(?:[^/+#]+|\\+))*?(?:/#)?$\n",
      example = "/folder/+/folder/topic",
      nullable = false
  )
  protected String topicNameTemplate;

  @Schema(
      description = "Specifies if auto-scan is enabled",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      defaultValue = "false",
      example = "false",
      nullable = true
  )
  protected boolean autoScan = false;

  @Schema(
      description = "1-wire bus Scan time interval in milliseconds",
      example = "30000",
      minimum = "1000",
      maximum = "600000",
      defaultValue = "60000",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected int scanTime = 60000;

  @Schema(
      description = "Filters raw value, depending on filter type will only send IF there is a change or every trigger",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      allowableValues = {"ALWAYS_SEND"," ON_CHANGE"},
      defaultValue = "ON_CHANGE",
      example = "ON_CHANGE",
      nullable = true
  )
  protected String filter;

  @Schema(
      description = "JMS selector configuration for the device bus",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      defaultValue = "",
      example = "temperature > 45 AND humidity > 30",
      nullable = true
  )
  protected String selector;
}
