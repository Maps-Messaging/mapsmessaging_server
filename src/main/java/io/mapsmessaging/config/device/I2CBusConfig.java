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
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@ToString
public class I2CBusConfig extends DeviceBusConfig {

  private int bus;
  private boolean autoScan;
  private List<I2CDeviceConfig> devices;
  private String trigger;


  public I2CBusConfig(ConfigurationProperties config) {
    super(config);
    bus = config.getIntProperty("bus", 0);
    trigger = config.getProperty("trigger", "default");
    autoScan = config.getBooleanProperty("autoScan", false);
    devices = new ArrayList<>();
    Object obj = config.get("devices");
    if(obj instanceof List) {
      List<ConfigurationProperties> configList = (List<ConfigurationProperties>) obj;
      for(ConfigurationProperties configurationProperties:configList){
        devices.add(new I2CDeviceConfig(configurationProperties));
      }
    }
    else if(obj instanceof ConfigurationProperties){
      devices.add(new I2CDeviceConfig((ConfigurationProperties) obj));
    }
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties props = super.toConfigurationProperties();
    props.put("bus", bus);
    props.put("autoScan", autoScan);
    if(!trigger.isEmpty())props.put("trigger", trigger);

    List<ConfigurationProperties> deviceList = new ArrayList<>();
    for(I2CDeviceConfig device:devices){
      deviceList.add(device.toConfigurationProperties());
    }
    props.put("devices", deviceList);
    return props;
  }
}
