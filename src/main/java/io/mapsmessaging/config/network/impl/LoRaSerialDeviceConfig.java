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

package io.mapsmessaging.config.network.impl;

import io.mapsmessaging.config.Config;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.network.impl.LoRaSerialConfigDTO;

public class LoRaSerialDeviceConfig extends LoRaSerialConfigDTO implements Config {


  public LoRaSerialDeviceConfig(ConfigurationProperties properties) {
    super();
    this.name = properties.getProperty("name");
    this.address = properties.getIntProperty("address", 1);
    this.power = properties.getIntProperty("power", 14);
    this.frequency = properties.getFloatProperty("frequency", 0.0f);
    this.transmissionRate = properties.getIntProperty("transmissionRate", 2);
    this.hexKey = properties.getProperty("hexKey", "0x0 0x0 0x0 0x0 0x0 0x0 0x0 0x0 0x0 0x0 0x0 0x0 0x0 0x0 0x0 0x0");
    this.serialConfig = new SerialConfig(properties);
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("name", this.name);
    properties.put("power", this.power);
    properties.put("address", this.address);
    properties.put("transmissionRate", this.transmissionRate);
    properties.put("frequency", this.frequency);
    properties.put("hexKey", this.hexKey);
    properties.put("serial", ((SerialConfig) this.serialConfig).toConfigurationProperties());
    return properties;
  }

  @Override
  public boolean update(BaseConfigDTO config) {
    if (!(config instanceof LoRaChipDeviceConfig)) {
      return false;
    }

    LoRaChipDeviceConfig newConfig = (LoRaChipDeviceConfig) config;
    boolean hasChanged = false;

    if (!this.name.equals(newConfig.getName())) {
      this.name = newConfig.getName();
      hasChanged = true;
    }
    if (this.power != newConfig.getPower()) {
      this.power = newConfig.getPower();
      hasChanged = true;
    }
    if (this.frequency != newConfig.getFrequency()) {
      this.frequency = newConfig.getFrequency();
      hasChanged = true;
    }
    if(this.address != newConfig.getAddress()) {
      this.address = newConfig.getAddress();
      hasChanged = true;
    }
    if(this.transmissionRate != newConfig.getTransmissionRate()) {
      this.transmissionRate = newConfig.getTransmissionRate();
      hasChanged = true;
    }
    if(!this.hexKey.equalsIgnoreCase(newConfig.getHexKey())) {
      this.hexKey = newConfig.getHexKey();
      hasChanged = true;
    }
    hasChanged = hasChanged || ((SerialConfig) serialConfig).update(config);
    return hasChanged;
  }
}