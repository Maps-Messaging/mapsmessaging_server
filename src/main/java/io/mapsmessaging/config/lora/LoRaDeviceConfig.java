/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2024 ] [Maps Messaging B.V.]
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

package io.mapsmessaging.config.lora;

import io.mapsmessaging.config.Config;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.lora.LoRaDeviceConfigDTO;

public class LoRaDeviceConfig extends LoRaDeviceConfigDTO implements Config {

  public LoRaDeviceConfig(ConfigurationProperties properties) {
    super();
    this.name = properties.getProperty("name");
    this.radio = properties.getProperty("radio");
    this.cs = properties.getIntProperty("cs", -1);
    this.irq = properties.getIntProperty("irq", -1);
    this.rst = properties.getIntProperty("rst", -1);
    this.power = properties.getIntProperty("power", 14);
    this.cadTimeout = properties.getIntProperty("CADTimeout", 0);
    this.frequency = properties.getFloatProperty("frequency", 0.0f);
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("name", this.name);
    properties.put("radio", this.radio);
    properties.put("cs", this.cs);
    properties.put("irq", this.irq);
    properties.put("rst", this.rst);
    properties.put("power", this.power);
    properties.put("cadTimeout", this.cadTimeout);
    properties.put("frequency", this.frequency);
    return properties;
  }

  @Override
  public boolean update(BaseConfigDTO config) {
    if (!(config instanceof LoRaDeviceConfigDTO)) {
      return false;
    }

    LoRaDeviceConfigDTO newConfig = (LoRaDeviceConfigDTO) config;
    boolean hasChanged = false;

    if (!this.name.equals(newConfig.getName())) {
      this.name = newConfig.getName();
      hasChanged = true;
    }
    if (!this.radio.equals(newConfig.getRadio())) {
      this.radio = newConfig.getRadio();
      hasChanged = true;
    }
    if (this.cs != newConfig.getCs()) {
      this.cs = newConfig.getCs();
      hasChanged = true;
    }
    if (this.irq != newConfig.getIrq()) {
      this.irq = newConfig.getIrq();
      hasChanged = true;
    }
    if (this.rst != newConfig.getRst()) {
      this.rst = newConfig.getRst();
      hasChanged = true;
    }
    if (this.power != newConfig.getPower()) {
      this.power = newConfig.getPower();
      hasChanged = true;
    }
    if (this.cadTimeout != newConfig.getCadTimeout()) {
      this.cadTimeout = newConfig.getCadTimeout();
      hasChanged = true;
    }
    if (this.frequency != newConfig.getFrequency()) {
      this.frequency = newConfig.getFrequency();
      hasChanged = true;
    }

    return hasChanged;
  }
}
