/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

import io.mapsmessaging.config.network.SerialDeviceHelper;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.LoRaDeviceManagerConfigDTO;
import io.mapsmessaging.dto.rest.config.lora.LoRaDeviceConfigDTO;
import io.mapsmessaging.dto.rest.config.lora.LoRaHardwareConfigDTO;
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
    LoRaDeviceConfigDTO  loRaDeviceConfigDTO = new LoRaDeviceConfigDTO();
    if (properties.containsKey("device")) {
      ConfigurationProperties deviceProps  = (ConfigurationProperties) properties.get("device");
      LoRaHardwareConfigDTO hardwareConfigDTO = new LoRaHardwareConfigDTO();
      hardwareConfigDTO.setCs(deviceProps.getIntProperty("cs", 0));
      hardwareConfigDTO.setIrq(deviceProps.getIntProperty("irq", 0));
      hardwareConfigDTO.setRst(deviceProps.getIntProperty("rst", 0));
      hardwareConfigDTO.setRadio(deviceProps.getProperty("radio"));
      hardwareConfigDTO.setCadTimeout(deviceProps.getIntProperty("CADTimeout", 0));
      loRaDeviceConfigDTO.setHardware(hardwareConfigDTO);
      deviceConfigList.add(loRaDeviceConfigDTO);
    } else if (properties.containsKey("serial")) {
      ConfigurationProperties serialProps = (ConfigurationProperties) properties.get("serial");
      loRaDeviceConfigDTO.setSerialDevice(SerialDeviceHelper.getSerialDeviceDTO(serialProps));
      deviceConfigList.add(loRaDeviceConfigDTO);
    }
    loRaDeviceConfigDTO.setPower(properties.getIntProperty("power", 0));
    loRaDeviceConfigDTO.setFrequency(properties.getIntProperty("frequency", 0));
    loRaDeviceConfigDTO.setName(properties.getProperty("name"));
  }

  @Override
  public boolean update(BaseConfigDTO config) {
    boolean hasChanged = false;
    if (config instanceof LoRaDeviceManagerConfigDTO newConfig) {
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
    ConfigurationProperties properties = new ConfigurationProperties();
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
