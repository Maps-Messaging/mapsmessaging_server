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

package io.mapsmessaging.dto.rest.config.ml;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class S3Config {

  @Schema(description = "AWS region or compatible region name", example = "ap-southeast-2")
  private String region;

  @Schema(description = "prefix to add to the model in the S3 bucket", example = "/maps_ml_store/")
  private String prefix;

  @Schema(description = "Access key for S3 authentication", example = "AKIAIOSFODNN7EXAMPLE")
  private String accessKey;

  @Schema(description = "Secret key for S3 authentication", example = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY")
  private String secretKey;

  @Schema(description = "S3 bucket name to store models", example = "maps-model-store")
  private String bucket;

  @Schema(description = "Optional custom endpoint for S3-compatible services like MinIO", example = "https://minio.local:9000")
  private String endpoint;
}