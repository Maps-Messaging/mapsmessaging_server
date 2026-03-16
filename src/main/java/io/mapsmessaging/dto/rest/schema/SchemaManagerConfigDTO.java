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

import io.mapsmessaging.dto.rest.config.BaseManagerConfigDTO;
import io.mapsmessaging.schemas.formatters.ParseMode;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Schema(
    title = "Schema Manager config",
    description = "Configures the schema manager on where it can find and store schemas")
@Data
@EqualsAndHashCode(callSuper = true)
public class SchemaManagerConfigDTO extends BaseManagerConfigDTO {

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
          @DiscriminatorMapping(value = "simple", schema = SimpleRepositoryConfigDTO.class),
          @DiscriminatorMapping(value = "file", schema = FileRepositoryConfigDTO.class),
          @DiscriminatorMapping(value = "maps", schema = MapsRepositoryConfigDTO.class)
      }
  )
  private RepositoryConfigDTO repositoryConfig;


  @Schema(
      description = "Optional path to the protoc compiler executable. " +
          "If not supplied the system PATH will be used.",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true,
      example = "/usr/local/bin/protoc"
  )
  private String protocPath;

  @Schema(
      description = "List of directories that are scanned for schema source files such as protobuf or JSON schemas. " +
          "These schemas are loaded and exposed as built-in or well-known schemas.",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  private List<SchemaImportLocationDTO> importLocations = new ArrayList<>();

  @Schema(
      description = "Defines how schema parsing errors are handled. IGNORE keeps current behaviour and suppresses parse errors, STRICT throws an error.",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = false,
      example = "IGNORE",
      defaultValue = "IGNORE"
  )
  private ParseMode parseMode = ParseMode.IGNORE;


  public SchemaManagerConfigDTO() {
    super("SchemaManagerConfigDTO");
  }

}