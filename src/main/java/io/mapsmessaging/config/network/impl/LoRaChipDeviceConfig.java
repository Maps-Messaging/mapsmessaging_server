package io.mapsmessaging.config.network.impl;

import io.mapsmessaging.config.Config;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.network.impl.LoRaChipConfigDTO;

public class LoRaChipDeviceConfig extends LoRaChipConfigDTO implements Config {


  public LoRaChipDeviceConfig(ConfigurationProperties properties) {
    super();
    setType("loraDevice");
    this.name = properties.getProperty("name");
    this.address = properties.getIntProperty("address", 1);
    this.power = properties.getIntProperty("power", 14);
    this.frequency = properties.getFloatProperty("frequency", 0.0f);
    this.transmissionRate = properties.getIntProperty("transmissionRate", 2);
    this.hexKey = properties.getProperty("hexKey", "0x00000000000000000000000000000000");
    this.radio = properties.getProperty("radio");
    this.cs = properties.getIntProperty("cs", -1);
    this.irq = properties.getIntProperty("irq", -1);
    this.rst = properties.getIntProperty("rst", -1);
    this.cadTimeout = properties.getIntProperty("CADTimeout", 0);
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("name", this.name);
    properties.put("power", this.power);
    properties.put("frequency", this.frequency);
    properties.put("address", this.address);
    properties.put("transmissionRate", this.transmissionRate);
    properties.put("radio", this.radio);
    properties.put("hexKey", this.hexKey);
    properties.put("cs", this.cs);
    properties.put("irq", this.irq);
    properties.put("rst", this.rst);
    properties.put("cadTimeout", this.cadTimeout);
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
    if (this.cadTimeout != newConfig.getCadTimeout()) {
      this.cadTimeout = newConfig.getCadTimeout();
      hasChanged = true;
    }

    return hasChanged;
  }
}
