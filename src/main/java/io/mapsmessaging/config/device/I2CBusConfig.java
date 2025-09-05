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
import io.mapsmessaging.dto.rest.config.device.I2CBusConfigDTO;
import io.mapsmessaging.dto.rest.config.device.I2CDeviceConfigDTO;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
public class I2CBusConfig extends I2CBusConfigDTO implements DeviceBusConfig {

  public I2CBusConfig(ConfigurationProperties config) {
    setEnabled(config.getBooleanProperty("enabled", false));
    setTopicNameTemplate(
        config.getProperty(
            "topicNameTemplate",
            "/device/[device_type]/[bus_name]/[bus_number]/[device_addr]/[device_name]"));
    setScanTime(config.getIntProperty("scanTime", 120000));
    setFilter(config.getProperty("filter", "ON_CHANGE"));
    setSelector(config.getProperty("selector", null));
    setBus(config.getIntProperty("bus", 0));
    setTrigger(config.getProperty("trigger", "default"));
    setAutoScan(config.getBooleanProperty("autoScan", false));

    List<I2CDeviceConfigDTO> deviceList = new ArrayList<>();
    Object obj = config.get("devices");
    if (obj instanceof List) {
      List<ConfigurationProperties> configList = (List<ConfigurationProperties>) obj;
      for (ConfigurationProperties configurationProperties : configList) {
        deviceList.add(new I2CDeviceConfig(configurationProperties));
      }
    } else if (obj instanceof ConfigurationProperties) {
      deviceList.add(new I2CDeviceConfig((ConfigurationProperties) obj));
    }
    setDevices(deviceList);
  }

  public boolean update(BaseConfigDTO config) {
    if (!(config instanceof I2CBusConfigDTO)) {
      return false;
    }
    I2CBusConfigDTO newConfig = (I2CBusConfigDTO) config;
    boolean hasChanged = false;

    if (this.isEnabled() != newConfig.isEnabled()) {
      setEnabled(newConfig.isEnabled());
      hasChanged = true;
    }
    if (this.getTopicNameTemplate() == null
        || !this.getTopicNameTemplate().equals(newConfig.getTopicNameTemplate())) {
      setTopicNameTemplate(newConfig.getTopicNameTemplate());
      hasChanged = true;
    }
    if (this.isAutoScan() != newConfig.isAutoScan()) {
      setAutoScan(newConfig.isAutoScan());
      hasChanged = true;
    }
    if (this.getScanTime() != newConfig.getScanTime()) {
      setScanTime(newConfig.getScanTime());
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
    if (this.getBus() != newConfig.getBus()) {
      setBus(newConfig.getBus());
      hasChanged = true;
    }
    if (this.getTrigger() == null || !this.getTrigger().equals(newConfig.getTrigger())) {
      setTrigger(newConfig.getTrigger());
      hasChanged = true;
    }
    if (this.getDevices() == null || !this.getDevices().equals(newConfig.getDevices())) {
      setDevices(newConfig.getDevices());
      hasChanged = true;
    }
    return hasChanged;
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties props = new ConfigurationProperties();
    props.put("enabled", isEnabled());
    props.put("topicNameTemplate", getTopicNameTemplate());
    props.put("scanTime", getScanTime());
    props.put("bus", getBus());
    props.put("autoScan", isAutoScan());
    if (getFilter() != null) props.put("filter", getFilter());
    if (getSelector() != null) props.put("selector", getSelector());
    if (getTrigger() != null && !getTrigger().isEmpty()) props.put("trigger", getTrigger());

    List<ConfigurationProperties> deviceList = new ArrayList<>();
    for (I2CDeviceConfigDTO device : getDevices()) {
      if (device instanceof Config) {
        deviceList.add(((Config) device).toConfigurationProperties());
      }
    }
    props.put("devices", deviceList);
    return props;
  }
}
