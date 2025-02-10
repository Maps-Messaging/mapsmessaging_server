package io.mapsmessaging.config.protocol.impl;

import io.mapsmessaging.config.Config;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.PluginConfigDTO;

import java.util.LinkedHashMap;
import java.util.Map;

public class PluginConfig extends PluginConfigDTO implements Config {

  public PluginConfig(ConfigurationProperties config) {
    setType(config.getProperty("protocol"));
    ProtocolConfigFactory.unpack(config, this);
    Object obj =  config.get("config");
    if(obj instanceof ConfigurationProperties){
      super.config = new LinkedHashMap<String, String>();
      for(Map.Entry<String, Object> entry : ((ConfigurationProperties)obj).entrySet()){
        super.config.put(entry.getKey(), String.valueOf(entry.getValue()));
      }
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
