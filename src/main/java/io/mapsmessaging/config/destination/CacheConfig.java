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
import io.mapsmessaging.dto.rest.config.destination.CacheConfigDTO;

public class CacheConfig extends CacheConfigDTO implements Config {

  public CacheConfig(ConfigurationProperties properties) {
    this.type = properties.getProperty("type", "WeakReference");
    this.writeThrough =
        properties.getProperty("writeThrough", "disable").equalsIgnoreCase("enable");
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("type", this.type);
    properties.put("writeThrough", this.writeThrough ? "enable" : "disable");
    return properties;
  }

  public boolean update(BaseConfigDTO config) {
    if (!(config instanceof CacheConfigDTO)) {
      return false;
    }

    CacheConfigDTO newConfig = (CacheConfigDTO) config;
    boolean hasChanged = false;

    // Check and update type
    if (this.type == null || !this.type.equals(newConfig.getType())) {
      this.type = newConfig.getType();
      hasChanged = true;
    }

    // Check and update writeThrough
    if (this.writeThrough != newConfig.isWriteThrough()) {
      this.writeThrough = newConfig.isWriteThrough();
      hasChanged = true;
    }

    return hasChanged;
  }
}
