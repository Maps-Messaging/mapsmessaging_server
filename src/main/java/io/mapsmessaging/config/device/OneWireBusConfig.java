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

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.device.OneWireBusConfigDTO;

public class OneWireBusConfig extends OneWireBusConfigDTO implements DeviceBusConfig {

  public OneWireBusConfig(ConfigurationProperties properties) {
    setEnabled(properties.getBooleanProperty("enabled", false));
    setTopicNameTemplate(
        properties.getProperty(
            "topicNameTemplate",
            "/device/[device_type]/[bus_name]/[bus_number]/[device_addr]/[device_name]"));
    setScanTime(properties.getIntProperty("scanTime", 120000));
    setFilter(properties.getProperty("filter", "ON_CHANGE"));
    setSelector(properties.getProperty("selector", null));
    this.name = properties.getProperty("name");
    this.autoScan = properties.getBooleanProperty("autoScan", false);
    this.trigger = properties.getProperty("trigger");
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties props = new ConfigurationProperties();
    props.put("name", this.name);
    props.put("trigger", this.trigger);
    props.put("enabled", isEnabled());
    props.put("topicNameTemplate", getTopicNameTemplate());
    props.put("autoScan", isAutoScan());
    props.put("scanTime", getScanTime());
    if (getFilter() != null) props.put("filter", getFilter());
    if (getSelector() != null) props.put("selector", getSelector());
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
    if (this.autoScan != newConfig.isAutoScan()) {
      this.autoScan = newConfig.isAutoScan();
      hasChanged = true;
    }
    if (this.trigger == null || !this.trigger.equals(newConfig.getTrigger())) {
      this.trigger = newConfig.getTrigger();
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
    if (this.getTrigger() == null || !this.getTrigger().equals(newConfig.getTrigger())) {
      setTrigger(newConfig.getTrigger());
      hasChanged = true;
    }
    return hasChanged;
  }
}
