/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
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

package io.mapsmessaging.config.destination;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "S3 Archive Configuration")
public class S3ArchiveConfig {

  private String regionName;
  private String accessKeyId;
  private String secretAccessKey;
  private String bucketName;
  private boolean compression;

  public S3ArchiveConfig(ConfigurationProperties properties) {
    regionName = properties.getProperty("regionName", "");
    accessKeyId = properties.getProperty("accessKeyId", "");
    secretAccessKey = properties.getProperty("secretAccessKey", "");
    bucketName = properties.getProperty("bucketName", "");
    compression = properties.getBooleanProperty("compression", false);
  }

  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("regionName", regionName);
    properties.put("accessKeyId", accessKeyId);
    properties.put("secretAccessKey", secretAccessKey);
    properties.put("bucketName", bucketName);
    properties.put("compression", compression);
    return properties;
  }
}

