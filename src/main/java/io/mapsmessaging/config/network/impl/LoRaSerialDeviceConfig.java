package io.mapsmessaging.config.network.impl;

import io.mapsmessaging.config.Config;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.network.impl.LoRaSerialConfigDTO;

public class LoRaSerialDeviceConfig extends LoRaSerialConfigDTO implements Config {


  public LoRaSerialDeviceConfig(ConfigurationProperties properties) {
    super();
    this.name = properties.getProperty("name");
    this.power = properties.getIntProperty("power", 14);
    this.frequency = properties.getFloatProperty("frequency", 0.0f);
    this.transmissionRate = properties.getIntProperty("transmissionRate", 2);
    this.serialConfig = new SerialConfig(properties);
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("name", this.name);
    properties.put("power", this.power);
    properties.put("frequency", this.frequency);
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
    hasChanged = hasChanged || ((SerialConfig) serialConfig).update(config);
    return hasChanged;
  }
}