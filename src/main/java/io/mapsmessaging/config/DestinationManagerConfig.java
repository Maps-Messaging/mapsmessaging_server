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

import io.mapsmessaging.config.destination.DestinationConfig;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.DestinationManagerConfigDTO;
import io.mapsmessaging.dto.rest.config.destination.DestinationConfigDTO;
import io.mapsmessaging.license.FeatureManager;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
public class DestinationManagerConfig extends DestinationManagerConfigDTO implements Config, ConfigManager {

  private DestinationManagerConfig(ConfigurationProperties properties, FeatureManager featureManager) {
    this.data = new ArrayList<>();
    Object configEntry = properties.get("data");
    if (configEntry instanceof List) {
      for (ConfigurationProperties entry : (List<ConfigurationProperties>) configEntry) {
        this.data.add(new DestinationConfig(entry, featureManager));
      }
    } else if (configEntry instanceof ConfigurationProperties) {
      this.data.add(new DestinationConfig((ConfigurationProperties) configEntry, featureManager));
    }
  }

  public static DestinationManagerConfig getInstance() {
    return ConfigurationManager.getInstance().getConfiguration(DestinationManagerConfig.class);
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties properties = new ConfigurationProperties();
    List<ConfigurationProperties> dataProperties = new ArrayList<>();
    for (DestinationConfigDTO destinationConfig : this.data) {
      dataProperties.add(((Config)destinationConfig).toConfigurationProperties());
    }
    properties.put("data", dataProperties);
    return properties;
  }

  @Override
  public boolean update(BaseConfigDTO config) {
    if (!(config instanceof DestinationManagerConfigDTO)) {
      return false;
    }

    DestinationManagerConfigDTO newConfig = (DestinationManagerConfigDTO) config;
    boolean hasChanged = false;

    // ToDo : Fix this to an update, merge, delete
    if (this.data.size() != newConfig.getData().size()) {
      this.data = newConfig.getData();
      hasChanged = true;
    } else {
      for (int i = 0; i < this.data.size(); i++) {
        if (!this.data.get(i).equals(newConfig.getData().get(i))) {
          this.data.set(i, newConfig.getData().get(i));
          hasChanged = true;
        }
      }
    }

    return hasChanged;
  }


  @Override
  public ConfigManager load(FeatureManager featureManager) {
    return new DestinationManagerConfig(ConfigurationManager.getInstance().getProperties(getName()), featureManager);
  }

  @Override
  public void save() throws IOException {
    ConfigurationManager.getInstance().saveConfiguration(getName(), toConfigurationProperties());
  }

  @Override
  public String getName() {
    return "DestinationManager";
  }
}
