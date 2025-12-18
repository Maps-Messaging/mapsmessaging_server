package io.mapsmessaging.config.device;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.device.OneWireBusConfigDTO;
import io.mapsmessaging.dto.rest.config.device.SerialBusConfigDTO;
import io.mapsmessaging.dto.rest.config.device.SerialDeviceDTO;

import java.util.ArrayList;
import java.util.List;

public class SerialDeviceBusConfig extends SerialBusConfigDTO  implements DeviceBusConfig {

  public SerialDeviceBusConfig(ConfigurationProperties properties) {
    this.name = properties.getProperty("name");
    this.enabled =properties.getBooleanProperty("enabled", false);
    setScanTime(properties.getIntProperty("scanTime", 120000));
    setFilter(properties.getProperty("filter", "ON_CHANGE"));
    setSelector(properties.getProperty("selector", null));
    this.autoScan = properties.getBooleanProperty("autoScan", true);
    this.trigger = properties.getProperty("trigger");
    setTopicNameTemplate(properties.getProperty("topicNameTemplate", "/serial/[device_name]"));
    this.devices = new ArrayList<>();

    Object obj = properties.get("config");
    if (obj instanceof List) {
      List<ConfigurationProperties> configList = (List<ConfigurationProperties>) obj;
      for (ConfigurationProperties config : configList) {
        this.devices.add(new SerialDeviceConfig(config));
      }
    } else if (obj instanceof ConfigurationProperties config2) {
      this.devices.add(new SerialDeviceConfig(config2));
    }
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties props = new ConfigurationProperties();
    props.put("name", this.name);
    props.put("enabled", isEnabled());
    props.put("topicNameTemplate", getTopicNameTemplate());
    if (getFilter() != null) props.put("filter", getFilter());
    if (getSelector() != null) props.put("selector", getSelector());
    List<ConfigurationProperties> deviceList = new ArrayList<>();
    for (SerialDeviceDTO device : this.devices) {
      if (device instanceof SerialDeviceConfig serialDeviceConfig) {
        deviceList.add(serialDeviceConfig.toConfigurationProperties());
      }
    }
    props.put("config", deviceList);
    return props;
  }

  public boolean update(BaseConfigDTO config) {
    if (!(config instanceof OneWireBusConfig)) {
      return false;
    }

    OneWireBusConfigDTO newConfig = (OneWireBusConfigDTO) config;
    boolean hasChanged = false;

    if (this.name == null || !this.name.equals(newConfig.getName())) {
      this.name = newConfig.getName();
      hasChanged = true;
    }

    if (this.isEnabled() != newConfig.isEnabled()) {
      setEnabled(newConfig.isEnabled());
      hasChanged = true;
    }
    if (this.getTopicNameTemplate() == null
        || !this.getTopicNameTemplate().equals(newConfig.getTopicNameTemplate())) {
      setTopicNameTemplate(newConfig.getTopicNameTemplate());
      hasChanged = true;
    }

    if (this.getFilter() == null || !this.getFilter().equals(newConfig.getFilter())) {
      setFilter(newConfig.getFilter());
      hasChanged = true;
    }
    if (this.getSelector() == null || !this.getSelector().equals(newConfig.getSelector())) {
      setSelector(newConfig.getSelector());
      hasChanged = true;
    }
    return hasChanged;
  }
}
