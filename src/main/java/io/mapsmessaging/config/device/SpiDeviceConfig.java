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
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@ToString
public class SpiDeviceConfig extends DeviceConfig {

  private int address;
  private String name;
  private String selector;
  private int spiBus;
  private int spiMode;
  private int spiChipSelect;

  public SpiDeviceConfig(ConfigurationProperties props) {
    address = props.getIntProperty("address", 0);
    name = props.getProperty("name");
    selector = props.getProperty("selector", "");
    spiBus = props.getIntProperty("spiBus", 0);
    spiMode = props.getIntProperty("spiMode", 0);
    spiChipSelect = props.getIntProperty("spiChipSelect", 0);
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties props = new ConfigurationProperties();
    props.put("address", address);
    props.put("name", name);
    props.put("spiBus", spiBus);
    props.put("spiMode", spiMode);
    props.put("spiChipSelect", spiChipSelect);
    if(!selector.isEmpty())props.put("selector", selector);
    return props;
  }
}
