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

package io.mapsmessaging.config.device;

import io.mapsmessaging.configuration.ConfigurationProperties;
import java.util.ArrayList;
import java.util.List;

public class SpiDeviceBusConfig extends DeviceBusConfig {

  private String name;
  private boolean autoScan;
  private List<SpiDeviceConfig> devices;

  public SpiDeviceBusConfig(ConfigurationProperties properties) {
    super(properties);
    name = properties.getProperty("name");
    autoScan = properties.getBooleanProperty("autoScan", false);
    devices = new ArrayList<>();
    Object obj = properties.get("devices");
    if(obj instanceof List) {
      List<ConfigurationProperties> configList = (List<ConfigurationProperties>) obj;
      for(ConfigurationProperties configurationProperties:configList){
        devices.add(new SpiDeviceConfig(configurationProperties));
      }
    }
    else if(obj instanceof ConfigurationProperties){
      devices.add(new SpiDeviceConfig((ConfigurationProperties) obj));
    }
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties props = super.toConfigurationProperties();
    props.put("name", name);
    props.put("autoScan", autoScan);
    List<ConfigurationProperties> deviceList = new ArrayList<>();
    for(SpiDeviceConfig device:devices){
      deviceList.add(device.toConfigurationProperties());
    }
    props.put("devices", deviceList);
    return props;
  }
}