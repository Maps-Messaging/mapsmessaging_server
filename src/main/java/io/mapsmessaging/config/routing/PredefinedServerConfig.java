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

package io.mapsmessaging.config.routing;

import io.mapsmessaging.config.Config;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.routing.PredefinedServerConfigDTO;

public class PredefinedServerConfig extends PredefinedServerConfigDTO implements Config {

  public PredefinedServerConfig(ConfigurationProperties properties) {
    super();
    this.name = properties.getProperty("name", "");
    this.url = properties.getProperty("url", "");
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("name", this.name);
    properties.put("url", this.url);
    return properties;
  }

  @Override
  public boolean update(BaseConfigDTO config) {
    boolean hasChanged = false;
    if (!(config instanceof PredefinedServerConfigDTO)) {
      return false;
    }

    PredefinedServerConfigDTO newConfig = (PredefinedServerConfigDTO) config;
    if (this.name == null || !this.name.equals(newConfig.getName())) {
      this.name = newConfig.getName();
      hasChanged = true;
    }
    if (this.url == null || !this.url.equals(newConfig.getUrl())) {
      this.url = newConfig.getUrl();
      hasChanged = true;
    }

    return hasChanged;
  }
}
