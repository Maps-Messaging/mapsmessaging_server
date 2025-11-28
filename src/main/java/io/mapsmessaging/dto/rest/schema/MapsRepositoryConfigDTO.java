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

package io.mapsmessaging.dto.rest.schema;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Schema(
    title = "Maps Repository configuration",
    description = "Configuration details for the MapsMessaging-based repository. " +
        "Used when repositoryType is set to 'Maps'.")
@Getter
@Setter
public class MapsRepositoryConfigDTO extends RepositoryConfigDTO {

  @Schema(
      description = "Local directory path for storing cached schemas " +
          "(used when local mirroring or fallback is enabled).",
      example = "{{MAPS_DATA}}/schemas")
  private String directoryPath;

  @Schema(
      description = "URL path of the remote MapsMessaging server used for schema synchronization.",
      example = "https://mapsmessaging.example.com/api/v1/schemas")
  private String urlPath;

  @Schema(
      description = "Username for authenticating to the remote MapsMessaging repository.",
      example = "admin")
  private String username;

  @Schema(
      description = "Password for authenticating to the remote MapsMessaging repository.",
      example = "secret-password")
  private String password;

  @Schema(
      description = "If true, newly created or updated schemas will be pushed to the remote repository.",
      example = "true")
  private boolean pushSchemas;

  @Schema(
      description = "If true, schemas will be periodically pulled from the remote repository " +
          "to keep the local cache up to date.",
      example = "false")
  private boolean pullSchemas;
}