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

import io.mapsmessaging.config.routing.PredefinedServerConfig;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.RoutingManagerConfigDTO;
import io.mapsmessaging.license.FeatureManager;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
public class RoutingManagerConfig extends RoutingManagerConfigDTO implements Config, ConfigManager {

  public static RoutingManagerConfig getInstance() {
    return ConfigurationManager.getInstance().getConfiguration(RoutingManagerConfig.class);
  }

  private RoutingManagerConfig(ConfigurationProperties properties) {
    this.enabled = properties.getBooleanProperty("enabled", false);
    this.autoDiscovery = properties.getBooleanProperty("autoDiscovery", true);
    this.predefinedServers = new ArrayList<>();

    Object servers = properties.get("predefinedServers");
    if (servers instanceof List) {
      for (ConfigurationProperties serverProps : (List<ConfigurationProperties>) servers) {
        this.predefinedServers.add(new PredefinedServerConfig(serverProps));
      }
    }
  }

  @Override
  public boolean update(BaseConfigDTO config) {
    if (!(config instanceof RoutingManagerConfigDTO)) {
      return false;
    }

    RoutingManagerConfigDTO newConfig = (RoutingManagerConfigDTO) config;
    boolean hasChanged = false;

    if (this.enabled != newConfig.isEnabled()) {
      this.enabled = newConfig.isEnabled();
      hasChanged = true;
    }

    if (this.autoDiscovery != newConfig.isAutoDiscovery()) {
      this.autoDiscovery = newConfig.isAutoDiscovery();
      hasChanged = true;
    }

    if (this.predefinedServers.size() != newConfig.getPredefinedServers().size()) {
      this.predefinedServers = newConfig.getPredefinedServers();
      hasChanged = true;
    } else {
      for (int i = 0; i < this.predefinedServers.size(); i++) {
        if (!this.predefinedServers.get(i).equals(newConfig.getPredefinedServers().get(i))) {
          this.predefinedServers.set(i, newConfig.getPredefinedServers().get(i));
          hasChanged = true;
        }
      }
    }

    return hasChanged;
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("enabled", this.enabled);
    properties.put("autoDiscovery", this.autoDiscovery);

    List<ConfigurationProperties> serverPropertiesList = new ArrayList<>();
    for (PredefinedServerConfig server : this.predefinedServers) {
      serverPropertiesList.add(server.toConfigurationProperties());
    }
    properties.put("predefinedServers", serverPropertiesList);

    return properties;
  }

  @Override
  public ConfigManager load(FeatureManager featureManager) {
    return new RoutingManagerConfig(ConfigurationManager.getInstance().getProperties(getName()));
  }

  @Override
  public void save() throws IOException {
    ConfigurationManager.getInstance().saveConfiguration(getName(), toConfigurationProperties());
  }


  @Override
  public String getName() {
    return "routing";
  }
}
