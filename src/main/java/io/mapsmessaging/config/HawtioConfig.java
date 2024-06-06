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
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@ToString
@Schema(description = "Hawtio Manager Configuration")
public class HawtioConfig extends ManagementConfig {
  private boolean enable;
  private String warFileLocation;
  private boolean discoverable;
  private String host;
  private int port;
  private boolean authenticationEnabled;

  private ConfigurationProperties hawtioMapping;

  private HawtioConfig(ConfigurationProperties config) {
    this.enable = config.getBooleanProperty("enable", true);
    this.warFileLocation = config.getProperty("warFileLocation", "");
    this.discoverable = config.getBooleanProperty("discoverable", false);

    hawtioMapping = (ConfigurationProperties) config.get("config");
    this.host = hawtioMapping.getProperty("hawtio.host", "0.0.0.0");
    this.port = hawtioMapping.getIntProperty("hawtio.port", 8181);
    this.authenticationEnabled =
        hawtioMapping.getBooleanProperty("hawtio.authenticationEnabled", false);
  }

  public static HawtioConfig getInstance() {
    return new HawtioConfig(ConfigurationManager.getInstance().getProperties("hawtio"));
  }

  public boolean update(ManagementConfig config) {
    boolean hasChanged = false;
    HawtioConfig newConfig = (HawtioConfig) config;
    if (this.enable != newConfig.isEnable()) {
      this.enable = newConfig.isEnable();
      hasChanged = true;
    }
    if (!this.warFileLocation.equals(newConfig.getWarFileLocation())) {
      this.warFileLocation = newConfig.getWarFileLocation();
      hasChanged = true;
    }
    if (this.discoverable != newConfig.isDiscoverable()) {
      this.discoverable = newConfig.isDiscoverable();
      hasChanged = true;
    }
    if (!this.host.equals(newConfig.getHost())) {
      this.host = newConfig.getHost();
      hasChanged = true;
    }
    if (this.port != newConfig.getPort()) {
      this.port = newConfig.getPort();
      hasChanged = true;
    }
    if (this.authenticationEnabled != newConfig.isAuthenticationEnabled()) {
      this.authenticationEnabled = newConfig.isAuthenticationEnabled();
      hasChanged = true;
    }

    return hasChanged;
  }

  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties config = new ConfigurationProperties();
    config.put("hawtio.enable", this.enable);
    config.put("hawtio.warFileLocation", this.warFileLocation);
    config.put("hawtio.discoverable", this.discoverable);

    hawtioMapping.put("hawtio.host", this.host);
    hawtioMapping.put("hawtio.port", this.port);
    hawtioMapping.put("hawtio.authenticationEnabled", this.authenticationEnabled);
    config.put("hawtio.config", hawtioMapping);

    return config;
  }
}
