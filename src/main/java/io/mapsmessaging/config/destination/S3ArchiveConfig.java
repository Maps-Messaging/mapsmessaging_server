/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2024 ] [Maps Messaging B.V.]
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

import io.mapsmessaging.config.Config;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.destination.S3ArchiveConfigDTO;

public class S3ArchiveConfig extends S3ArchiveConfigDTO implements Config {

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

  public boolean update(BaseConfigDTO config) {
    if (!(config instanceof S3ArchiveConfigDTO)) {
      return false;
    }
    S3ArchiveConfigDTO newConfig = (S3ArchiveConfigDTO) config;
    boolean hasChanged = false;

    // Check and update regionName
    if (this.regionName == null || !this.regionName.equals(newConfig.getRegionName())) {
      this.regionName = newConfig.getRegionName();
      hasChanged = true;
    }

    // Check and update accessKeyId
    if (this.accessKeyId == null || !this.accessKeyId.equals(newConfig.getAccessKeyId())) {
      this.accessKeyId = newConfig.getAccessKeyId();
      hasChanged = true;
    }

    // Check and update secretAccessKey
    if (this.secretAccessKey == null
        || !this.secretAccessKey.equals(newConfig.getSecretAccessKey())) {
      this.secretAccessKey = newConfig.getSecretAccessKey();
      hasChanged = true;
    }

    // Check and update bucketName
    if (this.bucketName == null || !this.bucketName.equals(newConfig.getBucketName())) {
      this.bucketName = newConfig.getBucketName();
      hasChanged = true;
    }

    // Check and update compression
    if (this.compression != newConfig.isCompression()) {
      this.compression = newConfig.isCompression();
      hasChanged = true;
    }

    return hasChanged;
  }
}
