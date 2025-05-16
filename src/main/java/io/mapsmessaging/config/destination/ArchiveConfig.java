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

package io.mapsmessaging.config.destination;

import io.mapsmessaging.config.Config;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.destination.ArchiveConfigDTO;

public class ArchiveConfig extends ArchiveConfigDTO implements Config {

  public ArchiveConfig(ConfigurationProperties properties) {
    this.name = properties.getProperty("name", "None");
    this.idleTime = properties.getLongProperty("idleTime", -1);
    this.digestAlgorithm = properties.getProperty("digestAlgorithm", "MD5");

    // Initialize S3 configuration if present
    if (properties.containsKey("s3")) {
      this.s3 = new S3ArchiveConfig((ConfigurationProperties) properties.get("s3"));
    }
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("name", this.name);
    properties.put("idleTime", this.idleTime);
    properties.put("digestAlgorithm", this.digestAlgorithm);

    // Add S3 config if present
    if (this.s3 != null) {
      properties.put("s3", ((S3ArchiveConfig) (this.s3)).toConfigurationProperties());
    }
    return properties;
  }

  public boolean update(BaseConfigDTO newConfig) {
    if (!(newConfig instanceof ArchiveConfig)) {
      return false;
    }

    ArchiveConfigDTO updatedConfig = (ArchiveConfigDTO) newConfig;
    boolean hasChanged = false;

    // Check and update name
    if (this.name == null || !this.name.equals(updatedConfig.getName())) {
      this.name = updatedConfig.getName();
      hasChanged = true;
    }

    // Check and update idleTime
    if (this.idleTime != updatedConfig.getIdleTime()) {
      this.idleTime = updatedConfig.getIdleTime();
      hasChanged = true;
    }

    // Check and update digestAlgorithm
    if (this.digestAlgorithm == null
        || !this.digestAlgorithm.equals(updatedConfig.getDigestAlgorithm())) {
      this.digestAlgorithm = updatedConfig.getDigestAlgorithm();
      hasChanged = true;
    }

    // Check and update S3 config
    if (this.s3 == null) {
      if (updatedConfig.getS3() != null) {
        this.s3 = updatedConfig.getS3();
        hasChanged = true;
      }
    } else if (((S3ArchiveConfig) (this.s3))
        .update((updatedConfig.getS3()))) { // Assuming S3ArchiveConfig has its own update method
      hasChanged = true;
    }

    return hasChanged;
  }
}
