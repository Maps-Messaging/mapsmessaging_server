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

package io.mapsmessaging.dto.rest.config.destination;

import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper=false)
@NoArgsConstructor
@Schema(description = "S3 Archive Configuration DTO")
public class S3ArchiveConfigDTO extends BaseConfigDTO {

  @Schema(description = "Region name for S3 storage", example = "us-west-2")
  protected String regionName;

  @Schema(description = "Access key ID for S3", example = "your-access-key-id")
  protected String accessKeyId;

  @Schema(description = "Secret access key for S3", example = "your-secret-access-key")
  protected String secretAccessKey;

  @Schema(description = "Bucket name for S3 storage", example = "my-bucket")
  protected String bucketName;

  @Schema(description = "Enable or disable compression for S3 storage", example = "true")
  protected boolean compression;
}
