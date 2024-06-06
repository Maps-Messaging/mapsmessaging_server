/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.config;

import io.mapsmessaging.config.device.I2CBusConfig;
import io.mapsmessaging.config.device.OneWireBusConfig;
import io.mapsmessaging.config.device.SpiDeviceBusConfig;
import io.mapsmessaging.config.device.triggers.CronTriggerConfig;
import io.mapsmessaging.config.device.triggers.InterruptTriggerConfig;
import io.mapsmessaging.config.device.triggers.PeriodicTriggerConfig;
import io.mapsmessaging.config.device.triggers.TriggerConfig;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@ToString
@Schema(description = "Device Manager Configuration")
public class DeviceManagerConfig extends Config {
  private boolean enabled;
  private List<TriggerConfig> triggers;
  private List<I2CBusConfig> i2cBuses;
  private SpiDeviceBusConfig spiBus;
  private OneWireBusConfig oneWireBus;


  private DeviceManagerConfig(ConfigurationProperties config) {
    this.enabled = config.getBooleanProperty("enabled", false);
    triggers = new ArrayList<>();
    i2cBuses = new ArrayList<>();
    processConfig(config);
  }

  public static DeviceManagerConfig getInstance() {
    return new DeviceManagerConfig(ConfigurationManager.getInstance().getProperties("DeviceManager"));
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties config = new ConfigurationProperties();
    List<ConfigurationProperties> configList = new ArrayList<>();
    for(TriggerConfig triggerConfig : triggers) {
      configList.add(triggerConfig.toConfigurationProperties());
    }
    config.put("triggers", configList);
    return config;
  }

  private void processConfig(ConfigurationProperties properties){
    Object obj = properties.get("data");
    List<ConfigurationProperties> deviceList = new ArrayList<>();

    if (obj instanceof List) {
      for(ConfigurationProperties props:(List<ConfigurationProperties>) obj){
        if(props.getProperty("name").equalsIgnoreCase("triggers")){
          loadTriggers(props);
        }
        else{
          deviceList.add(props);
        }
      }
    } else if (obj instanceof ConfigurationProperties) {
      deviceList.add((ConfigurationProperties) obj);
    }
    loadConfig(deviceList);
  }

  private void loadTriggers(ConfigurationProperties deviceConfig){
    Object configList = deviceConfig.get("config");
    if (configList instanceof List) {
      for (Object triggerConfigObj : (List) configList) {
        if (triggerConfigObj instanceof ConfigurationProperties) {
          TriggerConfig triggerConfig = createTriggerConfig((ConfigurationProperties) triggerConfigObj);
          if(triggerConfig != null){
            triggers.add(triggerConfig);
          }
        }
      }
    }
  }

  private TriggerConfig createTriggerConfig(ConfigurationProperties config){
    String type = config.getProperty("type", "");
    if(type.equalsIgnoreCase("cron")){
      return new CronTriggerConfig(config);
    }
    else if(type.equalsIgnoreCase("periodic")){
      return new PeriodicTriggerConfig(config);
    }
    else if(type.equalsIgnoreCase("interrupt")){
      return new InterruptTriggerConfig(config);
    }
    return null;
  }

  private void loadConfig( List<ConfigurationProperties> devices) {
    for (ConfigurationProperties deviceConfig : devices) {
      String deviceName = deviceConfig.getProperty("name", "");
      if(deviceName.equalsIgnoreCase("i2c")){
        loadI2CBuses(deviceConfig);
      }
      else if (deviceName.equalsIgnoreCase("oneWire")){
        oneWireBus = new OneWireBusConfig(deviceConfig);
      }
      else if (deviceName.equalsIgnoreCase("spi")){
        spiBus = new SpiDeviceBusConfig(deviceConfig);
      }
    }
  }

  private void loadI2CBuses(ConfigurationProperties deviceConfig){
    Object configList = deviceConfig.get("config");
    if (configList instanceof List) {
      for (Object i2cBusConfigObj : (List) configList) {
        if (i2cBusConfigObj instanceof ConfigurationProperties) {
          i2cBuses.add(new I2CBusConfig((ConfigurationProperties) i2cBusConfigObj));
        }
      }
    }
    else if (configList instanceof ConfigurationProperties) {
      i2cBuses.add(new I2CBusConfig((ConfigurationProperties) configList));
    }
  }
}
