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

package io.mapsmessaging.dto.rest.schema;

import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(
    title = "Schema Manager config",
    description = "Configures the schema manager on where it can find and store schemas")
@Data
public class SchemaManagerConfigDTO extends BaseConfigDTO {

  @Schema(
      description = "Repository-specific configuration object. " +
          "For example: SimpleRepositoryConfigDTO, FileRepositoryConfigDTO or MapsRepositoryConfigDTO " +
          "depending on the repositoryType.",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false,
      example = "file",
      oneOf = {
          SimpleRepositoryConfigDTO.class,
          FileRepositoryConfigDTO.class,
          MapsRepositoryConfigDTO.class
      },
      discriminatorProperty = "type",
      discriminatorMapping = {
          @DiscriminatorMapping(value = "Simple", schema = SimpleRepositoryConfigDTO.class),
          @DiscriminatorMapping(value = "File", schema = FileRepositoryConfigDTO.class),
          @DiscriminatorMapping(value = "Maps", schema = MapsRepositoryConfigDTO.class)
      }
  )
  private RepositoryConfigDTO repositoryConfig;

}