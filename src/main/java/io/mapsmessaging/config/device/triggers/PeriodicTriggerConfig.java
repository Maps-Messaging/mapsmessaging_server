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

package io.mapsmessaging.config.device.triggers;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.device.triggers.PeriodicTriggerConfigDTO;

public class PeriodicTriggerConfig extends PeriodicTriggerConfigDTO implements TriggerConfig {

  public PeriodicTriggerConfig(ConfigurationProperties config) {
    this.type = "periodic";
    this.interval = config.getIntProperty("interval", 0);
    this.name = config.getProperty("name", "");
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties config = new ConfigurationProperties();
    config.put("type", this.type);
    config.put("name", this.name);
    config.put("interval", this.interval);
    return config;
  }

  @Override
  public boolean update(BaseConfigDTO config) {
    if (!(config instanceof PeriodicTriggerConfigDTO)) {
      return false;
    }

    PeriodicTriggerConfigDTO newConfig = (PeriodicTriggerConfigDTO) config;
    boolean hasChanged = false;

    if (this.interval != newConfig.getInterval()) {
      this.interval = newConfig.getInterval();
      hasChanged = true;
    }
    if (this.type == null || !this.type.equals(newConfig.getType())) {
      this.type = newConfig.getType();
      hasChanged = true;
    }
    if (this.name == null || !this.name.equals(newConfig.getName())) {
      this.name = newConfig.getName();
      hasChanged = true;
    }

    return hasChanged;
  }
}
