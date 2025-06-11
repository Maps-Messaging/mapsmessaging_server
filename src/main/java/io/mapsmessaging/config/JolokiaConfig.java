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
import io.mapsmessaging.dto.rest.config.JolokiaConfigDTO;
import io.mapsmessaging.license.FeatureManager;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import lombok.NoArgsConstructor;

import java.io.IOException;

//ToDo Convert configurations into map<String, String>
@NoArgsConstructor
public class JolokiaConfig extends JolokiaConfigDTO implements Config, ConfigManager {

  private JolokiaConfig(ConfigurationProperties properties) {
    setEnable(properties.getBooleanProperty("enable", false));
    setJolokiaMapping((ConfigurationProperties) properties.get("config"));
  }

  public static JolokiaConfig getInstance() {
    return ConfigurationManager.getInstance().getConfiguration(JolokiaConfig.class);
  }

  @Override
  public boolean update(BaseConfigDTO config) {
    boolean hasChanged = false;
    if (config instanceof JolokiaConfigDTO) {
      JolokiaConfigDTO newConfig = (JolokiaConfigDTO) config;

      if (this.isEnable() != newConfig.isEnable()) {
        this.setEnable(newConfig.isEnable());
        hasChanged = true;
      }
      if (!this.getJolokiaMapping().equals(newConfig.getJolokiaMapping())) {
        this.setJolokiaMapping(newConfig.getJolokiaMapping());
        hasChanged = true;
      }
    }
    return hasChanged;
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("enable", isEnable());
    properties.put("config", getJolokiaMapping());
    return properties;
  }

  @Override
  public ConfigManager load(FeatureManager featureManager) {
    return new JolokiaConfig(ConfigurationManager.getInstance().getProperties(getName()));
  }

  @Override
  public void save() throws IOException {
    ConfigurationManager.getInstance().saveConfiguration(getName(), toConfigurationProperties());
  }

  @Override
  public String getName() {
    return "jolokia";
  }
}
