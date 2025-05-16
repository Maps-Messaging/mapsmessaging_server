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
import io.mapsmessaging.dto.rest.config.device.SpiDeviceConfigDTO;

public class SpiDeviceConfig extends SpiDeviceConfigDTO implements Config {

  public SpiDeviceConfig(ConfigurationProperties props) {
    this.address = props.getIntProperty("address", 0);
    this.name = props.getProperty("name");
    this.selector = props.getProperty("selector", "");
    this.spiBus = props.getIntProperty("spiBus", 0);
    this.spiMode = props.getIntProperty("spiMode", 0);
    this.spiChipSelect = props.getIntProperty("spiChipSelect", 0);
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties props = new ConfigurationProperties();
    props.put("address", this.address);
    props.put("name", this.name);
    props.put("spiBus", this.spiBus);
    props.put("spiMode", this.spiMode);
    props.put("spiChipSelect", this.spiChipSelect);
    if (!this.selector.isEmpty()) {
      props.put("selector", this.selector);
    }
    return props;
  }

  public boolean update(BaseConfigDTO config) {
    if (!(config instanceof SpiDeviceConfigDTO)) {
      return false;
    }

    SpiDeviceConfigDTO newConfig = (SpiDeviceConfigDTO) config;
    boolean hasChanged = false;

    if (this.address != newConfig.getAddress()) {
      this.address = newConfig.getAddress();
      hasChanged = true;
    }
    if (this.name == null || !this.name.equals(newConfig.getName())) {
      this.name = newConfig.getName();
      hasChanged = true;
    }
    if (this.selector == null || !this.selector.equals(newConfig.getSelector())) {
      this.selector = newConfig.getSelector();
      hasChanged = true;
    }
    if (this.spiBus != newConfig.getSpiBus()) {
      this.spiBus = newConfig.getSpiBus();
      hasChanged = true;
    }
    if (this.spiMode != newConfig.getSpiMode()) {
      this.spiMode = newConfig.getSpiMode();
      hasChanged = true;
    }
    if (this.spiChipSelect != newConfig.getSpiChipSelect()) {
      this.spiChipSelect = newConfig.getSpiChipSelect();
      hasChanged = true;
    }

    return hasChanged;
  }
}
