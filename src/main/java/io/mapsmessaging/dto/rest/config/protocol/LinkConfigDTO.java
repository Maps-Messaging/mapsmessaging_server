/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2025 ] [Maps Messaging B.V.]
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

package io.mapsmessaging.dto.rest.config.protocol;

import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Schema(description = "Link Configuration DTO")
public class LinkConfigDTO extends BaseConfigDTO {

  @Schema(description = "Direction of the link", example = "inbound")
  protected String direction;

  @Schema(description = "Remote namespace", example = "remote_ns")
  protected String remoteNamespace;

  @Schema(description = "Local namespace", example = "local_ns")
  protected String localNamespace;

  @Schema(description = "Message selector", example = "selector_criteria")
  protected String selector;

  @Schema(description = "Include schema flag", example = "true")
  protected boolean includeSchema;

  @Schema(description = "Transformer configuration map")
  protected Map<String, Object> transformer;
}
