/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

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
    this.enabled =properties.getBooleanProperty("enabled", enabled);
    setScanTime(properties.getIntProperty("scanTime", scanTime));
    setFilter(properties.getProperty("filter", "ON_CHANGE"));
    setSelector(properties.getProperty("selector", null));
    this.autoScan = properties.getBooleanProperty("autoScan", autoScan);
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
