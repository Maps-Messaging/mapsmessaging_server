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
    this.keepAlive = config.getIntProperty("keepAlive", keepAlive);
    this.namespaceRoot = config.getProperty("namespaceRoot", namespaceRoot);
  }

  @Override
  public boolean update(BaseConfigDTO config) {
    boolean hasChanged = false;
    if (config instanceof NatsConfigDTO) {
      NatsConfigDTO newConfig = (NatsConfigDTO) config;

      if (this.keepAlive != newConfig.getKeepAlive()) {
        this.keepAlive = newConfig.getKeepAlive();
        hasChanged = true;
      }
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
      if(!namespaceRoot.equals(newConfig.getNamespaceRoot())){
        namespaceRoot = newConfig.getNamespaceRoot();
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
    properties.put("enableStreams", this.enableStreams);
    properties.put("enableKeyValues", this.enableKeyValues);
    properties.put("enableObjectStore", this.enableObjectStore);
    properties.put("keepAlive", this.keepAlive);
    properties.put("namespaceRoot", this.namespaceRoot);
    return properties;
  }
}
