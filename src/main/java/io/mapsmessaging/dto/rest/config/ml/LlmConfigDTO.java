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

package io.mapsmessaging.dto.rest.config.ml;

import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.tools.config.lint.OpenVocab;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class LlmConfigDTO extends BaseConfigDTO {

  @Schema(
      description = "API token used to authenticate with the LLM provider",
      example = "sk-proj-abc123...",
      minLength = 1
  )
  private String apiToken;

  @OpenVocab
  @Schema(
      description =
          "Model name to use (provider-specific). "
              + "This is an open vocabulary and must match a model supported by the configured LLM provider.",
      example = "gpt-4.1",
      minLength = 1,
      maxLength = 128,
      pattern = "^[A-Za-z0-9._:-]+$"
  )
  private String model;
}
