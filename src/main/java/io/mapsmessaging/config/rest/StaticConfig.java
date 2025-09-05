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

package io.mapsmessaging.config.rest;

import io.mapsmessaging.config.Config;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.rest.StaticConfigDTO;

public class StaticConfig extends StaticConfigDTO implements Config {

  public StaticConfig(ConfigurationProperties properties) {
    super();
    this.enabled = properties.getBooleanProperty("enabled", true);
    this.directory = properties.getProperty("directory", "{{MAPS_HOME}}/www");
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("enabled", this.enabled);
    properties.put("directory", this.directory);
    return properties;
  }

  @Override
  public boolean update(BaseConfigDTO config) {
    if (!(config instanceof StaticConfigDTO)) {
      return false;
    }

    StaticConfigDTO newConfig = (StaticConfigDTO) config;
    boolean hasChanged = false;

    if (this.enabled != newConfig.isEnabled()) {
      this.enabled = newConfig.isEnabled();
      hasChanged = true;
    }
    if (this.directory == null || !this.directory.equals(newConfig.getDirectory())) {
      this.directory = newConfig.getDirectory();
      hasChanged = true;
    }

    return hasChanged;
  }
}
