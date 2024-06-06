/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.config;

import io.mapsmessaging.config.network.LoRaDeviceConfig;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@ToString
@Schema(description = "LoRa Device Management Configuration")
public class LoRaDeviceManagerConfig extends ManagementConfig {

  private List<LoRaDeviceConfig> deviceConfigList;

  public static LoRaDeviceManagerConfig getInstance() {
    return new LoRaDeviceManagerConfig(ConfigurationManager.getInstance().getProperties("LoRaDevice"));
  }

  private LoRaDeviceManagerConfig(ConfigurationProperties properties) {
    deviceConfigList = new ArrayList<>();
    Object configEntry = properties.get("data");
    if (configEntry instanceof List) {
      for (ConfigurationProperties entry : (List<ConfigurationProperties>) configEntry) {
        deviceConfigList.add(new LoRaDeviceConfig(entry));
      }
    } else if (configEntry instanceof ConfigurationProperties) {
      deviceConfigList.add(new LoRaDeviceConfig((ConfigurationProperties) configEntry));
    }
  }

  @Override
  public boolean update(ManagementConfig config) {
    return false;
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    List<ConfigurationProperties> configList = new ArrayList<>();
    for(LoRaDeviceConfig config:deviceConfigList){
      configList.add(config.toConfigurationProperties());
    }
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("data", configList);
    return properties;
  }
}
