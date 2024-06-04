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

package io.mapsmessaging.config.network;

import io.mapsmessaging.configuration.ConfigurationProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@ToString
public class LoRaDeviceConfig extends EndPointConfig {

  private String name;
  private String radio;

  private int cs;
  private int irq;
  private int rst;
  private int power;
  private int cadTimeout;
  private float frequency;

  public LoRaDeviceConfig(ConfigurationProperties properties) {
    name = properties.getProperty("name");
    radio =properties.getProperty("radio");
    cs = properties.getIntProperty("cs", -1);
    irq = properties.getIntProperty("irq", -1);
    rst =properties.getIntProperty("rst", -1);
    power = properties.getIntProperty("power", 14);
    cadTimeout = properties.getIntProperty("CADTimeout", 0);
    frequency = properties.getFloatProperty("frequency", 0.0f);
  }

  public boolean update(LoRaDeviceConfig newConfig) {
    super.update(newConfig);
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

    // Call the super class update method
    if (super.update(newConfig)) {
      hasChanged = true;
    }

    return hasChanged;
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("name", name);
    properties.put("radio", radio);
    properties.put("cs", cs);
    properties.put("irq", irq);
    properties.put("rst", rst);
    properties.put("power", power);
    properties.put("cadTimeout", cadTimeout);
    properties.put("frequency", frequency);
    return properties;
  }
}
