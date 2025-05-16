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

package io.mapsmessaging.config.protocol;

import io.mapsmessaging.config.Config;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.ConnectionAuthConfigDTO;

public class ConnectionAuthConfig extends ConnectionAuthConfigDTO implements Config {

  public ConnectionAuthConfig(ConfigurationProperties config) {
    this.username = config.getProperty("username");
    this.password = config.getProperty("password");
    this.clientId = config.getProperty("clientId");
    this.tokenGenerator = config.getProperty("tokenGenerator", "");
  }

  @Override
  public boolean update(BaseConfigDTO config) {
    boolean hasChanged = false;

    if (config instanceof ConnectionAuthConfigDTO) {
      ConnectionAuthConfigDTO newConfig = (ConnectionAuthConfigDTO) config;
      if (this.username == null || !this.username.equals(newConfig.getUsername())) {
        this.username = newConfig.getUsername();
        hasChanged = true;
      }

      if (this.password == null || !this.password.equals(newConfig.getPassword())) {
        this.password = newConfig.getPassword();
        hasChanged = true;
      }

      if (this.clientId == null || !this.clientId.equals(newConfig.getClientId())) {
        this.clientId = newConfig.getClientId();
        hasChanged = true;
      }

      if (this.tokenGenerator == null
          || !this.tokenGenerator.equals(newConfig.getTokenGenerator())) {
        this.tokenGenerator = newConfig.getTokenGenerator();
        hasChanged = true;
      }
    }
    return hasChanged;
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties config = new ConfigurationProperties();
    config.put("username", this.username);
    config.put("password", this.password);
    config.put("clientId", this.clientId);
    config.put("tokenGenerator", this.tokenGenerator);
    return config;
  }
}
