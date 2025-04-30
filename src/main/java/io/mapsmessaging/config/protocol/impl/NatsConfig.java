package io.mapsmessaging.config.protocol.impl;

import io.mapsmessaging.config.Config;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.NatsConfigDTO;

public class NatsConfig extends NatsConfigDTO implements Config {

  public NatsConfig(ConfigurationProperties config) {
    setType("nats");
    ProtocolConfigFactory.unpack(config, this);

    // Initialize Stomp-specific fields from config
    this.maxBufferSize = config.getIntProperty("maximumBufferSize", maxBufferSize);
    this.maxReceive = config.getIntProperty("maximumReceive", maxReceive);
    this.enableStreams = config.getBooleanProperty("enableStreams", enableStreams);
    this.enableObjectStore = config.getBooleanProperty("enableObjectStore", enableObjectStore);
    this.enableKeyValues = config.getBooleanProperty("enableKeyValues", enableKeyValues);
  }

  @Override
  public boolean update(BaseConfigDTO config) {
    boolean hasChanged = false;
    if (config instanceof NatsConfigDTO) {
      NatsConfigDTO newConfig = (NatsConfigDTO) config;

      // Check each field and update if necessary
      if (this.maxBufferSize != newConfig.getMaxBufferSize()) {
        this.maxBufferSize = newConfig.getMaxBufferSize();
        hasChanged = true;
      }
      if (this.maxReceive != newConfig.getMaxReceive()) {
        this.maxReceive = newConfig.getMaxReceive();
        hasChanged = true;
      }
      if(this.enableStreams != newConfig.isEnableStreams()){
        this.enableStreams = newConfig.isEnableStreams();
        hasChanged = true;
      }
      if(this.enableKeyValues != newConfig.isEnableKeyValues()){
        this.enableKeyValues = newConfig.isEnableKeyValues();
        hasChanged = true;
      }
      if(this.enableObjectStore != newConfig.isEnableObjectStore()){
        this.enableObjectStore = newConfig.isEnableObjectStore();
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
    properties.put("maximumBufferSize", this.maxBufferSize);
    properties.put("maximumReceive", this.maxReceive);
    return properties;
  }
}
