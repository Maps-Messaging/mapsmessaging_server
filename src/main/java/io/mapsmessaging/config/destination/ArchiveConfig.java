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
@Schema(description = "Archive Configuration")
public class ArchiveConfig {

  private String name;
  private long idleTime;
  private String digestAlgorithm;
  private S3ArchiveConfig s3;

  public ArchiveConfig(ConfigurationProperties properties) {
    name = properties.getProperty("name", "None");
    idleTime = properties.getLongProperty("idleTime", -1);
    digestAlgorithm = properties.getProperty("digestAlgorithm", "MD5");
    if (properties.containsKey("s3")) {
      s3 = new S3ArchiveConfig((ConfigurationProperties) properties.get("s3"));
    }
  }
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("name", name);
    properties.put("idleTime", idleTime);
    properties.put("digestAlgorithm", digestAlgorithm);
    if (s3 != null) {
      properties.put("s3", s3.toConfigurationProperties());
    }
    return properties;
  }
}

