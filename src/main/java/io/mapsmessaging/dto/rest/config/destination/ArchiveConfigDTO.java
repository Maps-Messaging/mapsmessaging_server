/*
 *
 *  Copyright [ 2020 - 2024 ] [Matthew Buckton]
 *  Copyright [ 2024 - 2025 ] [Maps Messaging B.V.]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package io.mapsmessaging.dto.rest.config.destination;

import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper=false)
@NoArgsConstructor
@Schema(description = "Archive Configuration DTO")
public class ArchiveConfigDTO extends BaseConfigDTO {

  @Schema(description = "Name of the archive", example = "defaultArchive")
  protected String name;

  @Schema(description = "Idle time before archiving in milliseconds", example = "60000")
  protected long idleTime;

  @Schema(description = "Digest algorithm used for archive integrity", example = "MD5")
  protected String digestAlgorithm;

  @Schema(description = "Configuration for S3 archiving")
  protected S3ArchiveConfigDTO s3;
}
