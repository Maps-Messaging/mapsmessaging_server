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
import io.mapsmessaging.dto.rest.auth.SecurityManagerDTO;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.license.FeatureManager;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@NoArgsConstructor
public class SecurityManagerConfig extends SecurityManagerDTO implements Config, ConfigManager {

  public static SecurityManagerConfig getInstance() {
    return ConfigurationManager.getInstance().getConfiguration(SecurityManagerConfig.class);
  }

  private SecurityManagerConfig(ConfigurationProperties properties) {
    map = new ConcurrentHashMap<>();
    for(String key :properties.keySet()){
      map.put(key, properties.getProperty(key));
    }
  }

  @Override
  public ConfigManager load(FeatureManager featureManager) {
    return new SecurityManagerConfig(ConfigurationManager.getInstance().getProperties(getName()));
  }

  @Override
  public void save() throws IOException {
    ConfigurationManager.getInstance().saveConfiguration(getName(), toConfigurationProperties());
  }

  @Override
  public String getName() {
    return "SecurityManager";
  }


  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties props = new ConfigurationProperties();
    for(String key : map.keySet()){
      props.put(key, map.get(key));
    }
    return props;
  }

  @Override
  public boolean update(BaseConfigDTO config) {
    return false;
  }

}
