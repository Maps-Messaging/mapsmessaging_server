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

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@ToString
@Schema(description = "Discovery Manager Configuration")
public class DiscoveryManagerConfig extends Config {

  private boolean enabled;
  private String hostnames;
  private boolean addTxtRecords;
  private String domainName;

  // Constructor to load properties from ConfigurationProperties
  public DiscoveryManagerConfig(ConfigurationProperties config) {
    this.enabled = config.getBooleanProperty("DiscoveryManager.enabled", true);
    this.hostnames = config.getProperty("DiscoveryManager.hostnames", "::");
    this.addTxtRecords = config.getBooleanProperty("DiscoveryManager.addTxtRecords", true);
    this.domainName = config.getProperty("DiscoveryManager.domainName", ".local");
  }

  // Method to update properties from another DiscoveryManagerConfig object
  public boolean update(DiscoveryManagerConfig newConfig) {
    boolean hasChanged = false;

    if (this.enabled != newConfig.isEnabled()) {
      this.enabled = newConfig.isEnabled();
      hasChanged = true;
    }
    if (!this.hostnames.equals(newConfig.getHostnames())) {
      this.hostnames = newConfig.getHostnames();
      hasChanged = true;
    }
    if (this.addTxtRecords != newConfig.isAddTxtRecords()) {
      this.addTxtRecords = newConfig.isAddTxtRecords();
      hasChanged = true;
    }
    if (!this.domainName.equals(newConfig.getDomainName())) {
      this.domainName = newConfig.getDomainName();
      hasChanged = true;
    }

    return hasChanged;
  }

  // Method to push current values into a ConfigurationProperties object
  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties config = new ConfigurationProperties();
    config.put("DiscoveryManager.enabled", this.enabled);
    config.put("DiscoveryManager.hostnames", this.hostnames);
    config.put("DiscoveryManager.addTxtRecords", this.addTxtRecords);
    config.put("DiscoveryManager.domainName", this.domainName);
    return config;
  }
}
