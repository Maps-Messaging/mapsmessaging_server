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

import io.mapsmessaging.config.Config;
import io.mapsmessaging.configuration.ConfigurationProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@ToString
public class DeviceBusConfig extends Config {

  private boolean enabled;
  private String topicNameTemplate;
  private boolean autoScan;
  private int scanTime;
  private String filter;
  private String selector;

  public DeviceBusConfig(ConfigurationProperties config){
    enabled = config.getBooleanProperty("enabled", false);
    topicNameTemplate = config.getProperty("topicNameTemplate", "/device/[device_type]/[bus_name]/[bus_number]/[device_addr]/[device_name]");
    autoScan = config.getBooleanProperty("autoScan", false);
    scanTime = config.getIntProperty("scanTime", 120000);
    filter = config.getProperty("filter", "ON_CHANGE");
    selector = config.getProperty("selector", null);
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties props = new ConfigurationProperties();
    props.put("enabled", enabled);
    props.put("topicNameTemplate", topicNameTemplate);
    props.put("autoScan", autoScan);
    props.put("scanTime", scanTime);
    if(filter != null)props.put("filter", filter);
    if(selector != null)props.put("selector", selector);
    return props;
  }

}
