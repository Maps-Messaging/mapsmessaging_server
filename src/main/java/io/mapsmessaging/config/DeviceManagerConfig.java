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

package io.mapsmessaging.config;

import io.mapsmessaging.config.device.I2CBusConfig;
import io.mapsmessaging.config.device.OneWireBusConfig;
import io.mapsmessaging.config.device.SpiDeviceBusConfig;
import io.mapsmessaging.config.device.triggers.CronTriggerConfig;
import io.mapsmessaging.config.device.triggers.InterruptTriggerConfig;
import io.mapsmessaging.config.device.triggers.PeriodicTriggerConfig;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.DeviceManagerConfigDTO;
import io.mapsmessaging.dto.rest.config.device.I2CBusConfigDTO;
import io.mapsmessaging.dto.rest.config.device.triggers.BaseTriggerConfigDTO;
import io.mapsmessaging.license.FeatureManager;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
public class DeviceManagerConfig extends DeviceManagerConfigDTO implements Config, ConfigManager {

  private DeviceManagerConfig(ConfigurationProperties config) {
    this.enabled = config.getBooleanProperty("enabled", false);
    this.demoEnabled = config.getBooleanProperty("demoEnabled", false);
    this.triggers = new ArrayList<>();
    this.i2cBuses = new ArrayList<>();
    processConfig(config);
  }

  public static DeviceManagerConfig getInstance() {
    return ConfigurationManager.getInstance().getConfiguration(DeviceManagerConfig.class);
  }

  @Override
  public boolean update(BaseConfigDTO config) {
    if (!(config instanceof DeviceManagerConfigDTO)) {
      return false;
    }

    DeviceManagerConfigDTO newConfig = (DeviceManagerConfigDTO) config;
    boolean hasChanged = false;

    if (this.enabled != newConfig.isEnabled()) {
      this.enabled = newConfig.isEnabled();
      hasChanged = true;
    }

    if (this.demoEnabled != newConfig.isDemoEnabled()) {
      this.demoEnabled = newConfig.isDemoEnabled();
      hasChanged = true;
    }

    // ToDO: Fix to update, add, delete
    if (this.triggers.size() != newConfig.getTriggers().size()) {
      this.triggers = newConfig.getTriggers();
      hasChanged = true;
    } else {
      for (int i = 0; i < this.triggers.size(); i++) {
        if (!this.triggers.get(i).equals(newConfig.getTriggers().get(i))) {
          this.triggers.set(i, newConfig.getTriggers().get(i));
          hasChanged = true;
        }
      }
    }

    if (this.i2cBuses.size() != newConfig.getI2cBuses().size()) {
      this.i2cBuses = newConfig.getI2cBuses();
      hasChanged = true;
    } else {
      for (int i = 0; i < this.i2cBuses.size(); i++) {
        if (!this.i2cBuses.get(i).equals(newConfig.getI2cBuses().get(i))) {
          this.i2cBuses.set(i, newConfig.getI2cBuses().get(i));
          hasChanged = true;
        }
      }
    }

    if ((this.spiBus != null && !this.spiBus.equals(newConfig.getSpiBus()))
        || (this.spiBus == null && newConfig.getSpiBus() != null)) {
      this.spiBus = newConfig.getSpiBus();
      hasChanged = true;
    }

    if ((this.oneWireBus != null && !this.oneWireBus.equals(newConfig.getOneWireBus()))
        || (this.oneWireBus == null && newConfig.getOneWireBus() != null)) {
      this.oneWireBus = newConfig.getOneWireBus();
      hasChanged = true;
    }

    return hasChanged;
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties config = new ConfigurationProperties();
    config.put("enabled", this.enabled);
    config.put("demoEnabled", this.demoEnabled);

    List<ConfigurationProperties> triggerConfigs = new ArrayList<>();
    for (BaseTriggerConfigDTO trigger : this.triggers) {
      triggerConfigs.add(((Config)trigger).toConfigurationProperties());
    }
    config.put("triggers", triggerConfigs);

    List<ConfigurationProperties> i2cBusConfigs = new ArrayList<>();
    for (I2CBusConfigDTO i2cBus : this.i2cBuses) {
      i2cBusConfigs.add(((Config)i2cBus).toConfigurationProperties());
    }
    config.put("i2cBuses", i2cBusConfigs);

    if (this.spiBus != null) {
      config.put("spiBus", ((Config)this.spiBus).toConfigurationProperties());
    }

    if (this.oneWireBus != null) {
      config.put("oneWireBus", ((Config)this.oneWireBus).toConfigurationProperties());
    }

    return config;
  }

  private void processConfig(ConfigurationProperties properties) {
    Object obj = properties.get("data");
    List<ConfigurationProperties> deviceList = new ArrayList<>();

    if (obj instanceof List) {
      for (ConfigurationProperties props : (List<ConfigurationProperties>) obj) {
        if (props.getProperty("name").equalsIgnoreCase("triggers")) {
          loadTriggers(props);
        } else {
          deviceList.add(props);
        }
      }
    } else if (obj instanceof ConfigurationProperties) {
      deviceList.add((ConfigurationProperties) obj);
    }
    loadConfig(deviceList);
  }

  private void loadTriggers(ConfigurationProperties deviceConfig) {
    Object configList = deviceConfig.get("config");
    if (configList instanceof List) {
      for (Object triggerConfigObj : (List) configList) {
        if (triggerConfigObj instanceof ConfigurationProperties) {
          BaseTriggerConfigDTO triggerConfig = createTriggerConfig((ConfigurationProperties) triggerConfigObj);
          if (triggerConfig != null) {
            triggers.add(triggerConfig);
          }
        }
      }
    }
  }

  private BaseTriggerConfigDTO createTriggerConfig(ConfigurationProperties config) {
    String type = config.getProperty("type", "");
    if (type.equalsIgnoreCase("cron")) {
      return new CronTriggerConfig(config);
    } else if (type.equalsIgnoreCase("periodic")) {
      return new PeriodicTriggerConfig(config);
    } else if (type.equalsIgnoreCase("interrupt")) {
      return new InterruptTriggerConfig(config);
    }
    return null;
  }

  private void loadConfig(List<ConfigurationProperties> devices) {
    for (ConfigurationProperties deviceConfig : devices) {
      String deviceName = deviceConfig.getProperty("name", "");
      if (deviceName.equalsIgnoreCase("i2c")) {
        loadI2CBuses(deviceConfig);
      } else if (deviceName.equalsIgnoreCase("oneWire")) {
        oneWireBus = new OneWireBusConfig(deviceConfig);
      } else if (deviceName.equalsIgnoreCase("spi")) {
        spiBus = new SpiDeviceBusConfig(deviceConfig);
      }
    }
  }

  private void loadI2CBuses(ConfigurationProperties deviceConfig) {
    Object configList = deviceConfig.get("config");
    if (configList instanceof List) {
      for (Object i2cBusConfigObj : (List) configList) {
        if (i2cBusConfigObj instanceof ConfigurationProperties) {
          i2cBuses.add(new I2CBusConfig((ConfigurationProperties) i2cBusConfigObj));
        }
      }
    } else if (configList instanceof ConfigurationProperties) {
      i2cBuses.add(new I2CBusConfig((ConfigurationProperties) configList));
    }
  }


  @Override
  public ConfigManager load(FeatureManager featureManager) {
    return new DeviceManagerConfig(ConfigurationManager.getInstance().getProperties(getName()));
  }

  @Override
  public void save() throws IOException {
    ConfigurationManager.getInstance().saveConfiguration(getName(), toConfigurationProperties());
  }


  @Override
  public String getName() {
    return "DeviceManager";
  }
}
