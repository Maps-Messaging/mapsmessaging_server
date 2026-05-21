/*
 *
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

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(
    title = "Schema Import Location",
    description = "Defines a directory path and the schema format to load from that directory")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SchemaImportLocationDTO {

  @Schema(
      description = "Name to apply to the schema when loaded",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false,
      example = "protobuf_schema_1"
  )
  private String name;


  @Schema(
      description = "Directory path containing schema files",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false,
      example = "/opt/schema/protobuf"
  )
  private String path;

  @Schema(
      description = "Schema format contained in the directory",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false,
      allowableValues = {
          "protobuf",
          "json"
      },
      example = "protobuf"
  )
  private String format;
}