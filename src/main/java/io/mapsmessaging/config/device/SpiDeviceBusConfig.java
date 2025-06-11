/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
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

import io.mapsmessaging.config.Config;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.device.SpiDeviceBusConfigDTO;
import io.mapsmessaging.dto.rest.config.device.SpiDeviceConfigDTO;

import java.util.ArrayList;
import java.util.List;

public class SpiDeviceBusConfig extends SpiDeviceBusConfigDTO implements DeviceBusConfig {

  public SpiDeviceBusConfig(ConfigurationProperties properties) {
    super();
    this.name = properties.getProperty("name");
    this.autoScan = properties.getBooleanProperty("autoScan", false);
    this.enabled = properties.getBooleanProperty("enabled", true);
    this.topicNameTemplate = properties.getProperty("topicNameTemplate", "");
    this.scanTime = properties.getIntProperty("scanTime", 1000); // Default to 1000 ms
    this.filter = properties.getProperty("filter", "");
    this.selector = properties.getProperty("selector", "");

    this.devices = new ArrayList<>();
    Object obj = properties.get("devices");
    if (obj instanceof List) {
      List<ConfigurationProperties> configList = (List<ConfigurationProperties>) obj;
      for (ConfigurationProperties config : configList) {
        this.devices.add(new SpiDeviceConfig(config));
      }
    } else if (obj instanceof ConfigurationProperties) {
      this.devices.add(new SpiDeviceConfig((ConfigurationProperties) obj));
    }
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties props = new ConfigurationProperties();
    props.put("name", this.name);
    props.put("autoScan", this.autoScan);
    props.put("enabled", this.enabled);
    props.put("topicNameTemplate", this.topicNameTemplate);
    props.put("scanTime", this.scanTime);
    props.put("filter", this.filter);
    props.put("selector", this.selector);

    List<ConfigurationProperties> deviceList = new ArrayList<>();
    for (SpiDeviceConfigDTO device : this.devices) {
      if (device instanceof SpiDeviceConfig) {
        deviceList.add(((SpiDeviceConfig) device).toConfigurationProperties());
      }
    }
    props.put("devices", deviceList);

    return props;
  }

  public boolean update(BaseConfigDTO config) {
    if (!(config instanceof SpiDeviceBusConfigDTO)) {
      return false;
    }

    SpiDeviceBusConfigDTO newConfig = (SpiDeviceBusConfigDTO) config;
    boolean hasChanged = false;

    if (this.name == null || !this.name.equals(newConfig.getName())) {
      this.name = newConfig.getName();
      hasChanged = true;
    }
    if (this.autoScan != newConfig.isAutoScan()) {
      this.autoScan = newConfig.isAutoScan();
      hasChanged = true;
    }
    if (this.enabled != newConfig.isEnabled()) {
      this.enabled = newConfig.isEnabled();
      hasChanged = true;
    }
    if (this.topicNameTemplate == null
        || !this.topicNameTemplate.equals(newConfig.getTopicNameTemplate())) {
      this.topicNameTemplate = newConfig.getTopicNameTemplate();
      hasChanged = true;
    }
    if (this.scanTime != newConfig.getScanTime()) {
      this.scanTime = newConfig.getScanTime();
      hasChanged = true;
    }
    if (this.filter == null || !this.filter.equals(newConfig.getFilter())) {
      this.filter = newConfig.getFilter();
      hasChanged = true;
    }
    if (this.selector == null || !this.selector.equals(newConfig.getSelector())) {
      this.selector = newConfig.getSelector();
      hasChanged = true;
    }

    // Update devices list
    if (this.devices.size() != newConfig.getDevices().size()) {
      this.devices = newConfig.getDevices();
      hasChanged = true;
    } else {
      for (int i = 0; i < this.devices.size(); i++) {
        if (((Config) this.devices.get(i))
            .update(newConfig.getDevices().get(i))) {
          hasChanged = true;
        }
      }
    }

    return hasChanged;
  }
}
