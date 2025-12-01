package io.mapsmessaging.config.device;

import io.mapsmessaging.config.network.impl.SerialConfig;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.device.SerialDeviceDTO;

import java.util.Map;

public class SerialDeviceConfig extends SerialDeviceDTO implements DeviceBusConfig {

  public SerialDeviceConfig(ConfigurationProperties props) {
    this.name = props.getProperty("name");
    this.selector = props.getProperty("selector", "");
    serialConfig = new SerialConfig(props);
  }


  @Override
  public ConfigurationProperties toConfigurationProperties() {
    return null;
  }

  @Override
  public boolean update(BaseConfigDTO config) {
    return false;
  }

  @Override
  public boolean updateMap(Map<String, Object> currentMap, Map<String, Object> newMap) {
    return DeviceBusConfig.super.updateMap(currentMap, newMap);
  }
}
