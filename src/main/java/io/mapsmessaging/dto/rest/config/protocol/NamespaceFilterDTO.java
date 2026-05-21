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

package io.mapsmessaging.dto.rest.config.protocol;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(
    title = "Namespace Filter",
    description = "Defines filtering rules applied to a namespace, including depth and selector evaluation."
)
public class NamespaceFilterDTO {

  @Schema(
      description = "Namespace to which the filter applies",
      example = "root/system",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  protected String namespace;

  @Schema(
      description = "Depth to which the namespace filter applies",
      example = "3",
      minimum = "0",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  protected int depth;

  @Schema(
      description = "Selector expression applied to the namespace",
      example = "state = ACTIVE",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  protected String selector;

  @Schema(
      description = "Forces this filter to take priority over others",
      example = "false",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      defaultValue = "false",
      nullable = false
  )
  protected boolean forcePriority;
}
