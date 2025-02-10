package io.mapsmessaging.config.protocol.impl;

import io.mapsmessaging.config.Config;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.PluginConfigDTO;

import java.util.LinkedHashMap;
import java.util.Map;

public class PluginConfig extends PluginConfigDTO implements Config {

  public PluginConfig(ConfigurationProperties configuration) {
    setType(configuration.getProperty("protocol"));
    ProtocolConfigFactory.unpack(configuration, this);
    Object obj =  configuration.get("config");
    if(obj instanceof ConfigurationProperties){
      config = new LinkedHashMap<>();
      config.putAll(((ConfigurationProperties)obj).getMap());
    }
  }


  @Override
  public boolean update(BaseConfigDTO config) {
    boolean hasChanged = false;
    if (config instanceof PluginConfig) {
      PluginConfig newConfig = (PluginConfig) config;
      if (ProtocolConfigFactory.update(this, newConfig)) {
        hasChanged = true;
      }
    }
    return hasChanged;
  }


  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties config = new ConfigurationProperties();
    ProtocolConfigFactory.pack(config, this);
    return config;
  }

  public PluginConfig(){}
}
