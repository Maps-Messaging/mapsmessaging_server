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

package io.mapsmessaging.config.device;

import io.mapsmessaging.config.Config;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.device.I2CDeviceConfigDTO;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class I2CDeviceConfig extends I2CDeviceConfigDTO implements Config {

  public I2CDeviceConfig(ConfigurationProperties props) {
    String addrString = props.getProperty("address", "0");
    if(addrString.indexOf(".") > 0){
      addrString = addrString.substring(0, addrString.indexOf("."));
    }
    setAddress(Integer.parseInt(addrString, addrString.contains("x") ? 16 : 10));
    setName(props.getProperty("name"));
    setSelector(props.getProperty("selector", ""));
  }

  public boolean update(BaseConfigDTO config) {
    if (!(config instanceof I2CDeviceConfigDTO)) {
      return false;
    }
    I2CDeviceConfigDTO newConfig = (I2CDeviceConfigDTO) config;
    boolean hasChanged = false;

    if (this.getAddress() != newConfig.getAddress()) {
      setAddress(newConfig.getAddress());
      hasChanged = true;
    }
    if (this.getName() == null || !this.getName().equals(newConfig.getName())) {
      setName(newConfig.getName());
      hasChanged = true;
    }
    if (this.getSelector() == null || !this.getSelector().equals(newConfig.getSelector())) {
      setSelector(newConfig.getSelector());
      hasChanged = true;
    }
    return hasChanged;
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties props = new ConfigurationProperties();
    props.put("address", getAddress());
    props.put("name", getName());
    if (getSelector() != null && !getSelector().isEmpty()) props.put("selector", getSelector());
    return props;
  }
}
