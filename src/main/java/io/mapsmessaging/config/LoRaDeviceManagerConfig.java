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

import io.mapsmessaging.config.network.impl.LoRaChipDeviceConfig;
import io.mapsmessaging.config.network.impl.LoRaSerialDeviceConfig;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.LoRaDeviceManagerConfigDTO;
import io.mapsmessaging.dto.rest.config.network.impl.LoRaConfigDTO;
import io.mapsmessaging.license.FeatureManager;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
public class LoRaDeviceManagerConfig extends LoRaDeviceManagerConfigDTO implements Config, ConfigManager {

  public static LoRaDeviceManagerConfig getInstance() {
    return ConfigurationManager.getInstance().getConfiguration(LoRaDeviceManagerConfig.class);
  }

  private LoRaDeviceManagerConfig(ConfigurationProperties properties) {
    deviceConfigList = new ArrayList<>();
    Object configEntry = properties.get("data");
    if (configEntry instanceof List) {
      for (ConfigurationProperties entry : (List<ConfigurationProperties>) configEntry) {
        parseConfig(entry);
      }
    } else if (configEntry instanceof ConfigurationProperties) {
      parseConfig((ConfigurationProperties) configEntry);
    }
  }

  private void parseConfig(ConfigurationProperties properties) {
    LoRaConfigDTO loraDeviceConfig = null;
    if (properties.containsKey("device")) {
      loraDeviceConfig = new LoRaChipDeviceConfig(properties);
    } else if (properties.containsKey("serial")) {
      loraDeviceConfig = new LoRaSerialDeviceConfig(properties);
    }
    if (loraDeviceConfig != null) {
      deviceConfigList.add(loraDeviceConfig);
    }
  }

  @Override
  public boolean update(BaseConfigDTO config) {
    boolean hasChanged = false;
    if (config instanceof LoRaDeviceManagerConfigDTO) {
      LoRaDeviceManagerConfigDTO newConfig = (LoRaDeviceManagerConfigDTO) config;
      if (this.deviceConfigList.size() != newConfig.getDeviceConfigList().size()) {
        this.deviceConfigList = newConfig.getDeviceConfigList();
        hasChanged = true;
      } else {
        for (int i = 0; i < this.deviceConfigList.size(); i++) {
          if (!this.deviceConfigList.get(i).equals(newConfig.getDeviceConfigList().get(i))) {
            this.deviceConfigList.set(i, newConfig.getDeviceConfigList().get(i));
            hasChanged = true;
          }
        }
      }
    }
    return hasChanged;
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    List<ConfigurationProperties> configList = new ArrayList<>();
    for (LoRaConfigDTO config : deviceConfigList) {
      if (config instanceof Config) {
        configList.add(((Config) config).toConfigurationProperties());
      }
    }
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("data", configList);
    return properties;
  }

  @Override
  public ConfigManager load(FeatureManager featureManager) {
    return new LoRaDeviceManagerConfig(ConfigurationManager.getInstance().getProperties(getName()));
  }

  @Override
  public void save() throws IOException {
    ConfigurationManager.getInstance().saveConfiguration(getName(), toConfigurationProperties());
  }

  @Override
  public String getName() {
    return "LoRaDevice";
  }
}
