package io.mapsmessaging.config.protocol.impl;

import io.mapsmessaging.config.Config;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.LoraGatewayConfigDTO;

public class LoraGatewayConfig extends LoraGatewayConfigDTO implements Config {


  public LoraGatewayConfig(ConfigurationProperties config) {
    setType("lora_mqtt-sn");
    ProtocolConfigFactory.unpack(config, this);

    // Initialize LoRa-specific fields from config
    this.address = config.getIntProperty("address", address);
    this.power = config.getIntProperty("power", power);
    this.hexKeyString = config.getProperty("hexKeyString", "");
  }

  @Override
  public boolean update(BaseConfigDTO config) {
    boolean hasChanged = false;
    if (config instanceof LoraGatewayConfigDTO) {
      LoraGatewayConfigDTO newConfig = (LoraGatewayConfigDTO) config;

      // Check each field and update if necessary
      if (this.address != newConfig.getAddress()) {
        this.address = newConfig.getAddress();
        hasChanged = true;
      }

      if (this.power != newConfig.getPower()) {
        this.power = newConfig.getPower();
        hasChanged = true;
      }

      if (!hexKeyString.equals(newConfig.getHexKeyString())) {
        hexKeyString = newConfig.getHexKeyString();
        hasChanged = true;
      }

      // Update fields from ProtocolConfigFactory if needed
      if (ProtocolConfigFactory.update(this, newConfig)) {
        hasChanged = true;
      }
    }
    return hasChanged;
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties properties = new ConfigurationProperties();
    ProtocolConfigFactory.pack(properties, this);
    properties.put("address", this.address);
    properties.put("power", this.power);
    properties.put("hexKeyString", this.hexKeyString);
    return properties;
  }
}