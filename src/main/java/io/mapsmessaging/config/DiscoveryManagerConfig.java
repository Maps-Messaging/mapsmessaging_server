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

package io.mapsmessaging.config;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.DiscoveryManagerConfigDTO;
import io.mapsmessaging.license.FeatureManager;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import lombok.NoArgsConstructor;

import java.io.IOException;

@NoArgsConstructor
public class DiscoveryManagerConfig extends DiscoveryManagerConfigDTO implements Config, ConfigManager {

  private DiscoveryManagerConfig(ConfigurationProperties config) {
    this.enabled = config.getBooleanProperty("enabled", false);
    this.hostnames = config.getProperty("hostnames", "::");
    this.addTxtRecords = config.getBooleanProperty("addTxtRecords", true);
    this.domainName = config.getProperty("domainName", ".local");
  }

  public static DiscoveryManagerConfig getInstance() {
    return ConfigurationManager.getInstance().getConfiguration(DiscoveryManagerConfig.class);
  }

  @Override
  public boolean update(BaseConfigDTO config) {
    if (!(config instanceof DiscoveryManagerConfigDTO)) {
      return false;
    }

    DiscoveryManagerConfigDTO newConfig = (DiscoveryManagerConfigDTO) config;
    boolean hasChanged = false;

    if (this.enabled != newConfig.isEnabled()) {
      this.enabled = newConfig.isEnabled();
      hasChanged = true;
    }
    if (!this.hostnames.equals(newConfig.getHostnames())) {
      this.hostnames = newConfig.getHostnames();
      hasChanged = true;
    }
    if (this.addTxtRecords != newConfig.isAddTxtRecords()) {
      this.addTxtRecords = newConfig.isAddTxtRecords();
      hasChanged = true;
    }
    if (!this.domainName.equals(newConfig.getDomainName())) {
      this.domainName = newConfig.getDomainName();
      hasChanged = true;
    }

    return hasChanged;
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties config = new ConfigurationProperties();
    config.put("enabled", this.enabled);
    config.put("hostnames", this.hostnames);
    config.put("addTxtRecords", this.addTxtRecords);
    config.put("domainName", this.domainName);
    return config;
  }



  @Override
  public ConfigManager load(FeatureManager featureManager) {
    return new DiscoveryManagerConfig(ConfigurationManager.getInstance().getProperties(getName()));
  }

  @Override
  public void save() throws IOException {
    ConfigurationManager.getInstance().saveConfiguration(getName(), toConfigurationProperties());
  }

  @Override
  public String getName() {
    return "DiscoveryManager";
  }
}
