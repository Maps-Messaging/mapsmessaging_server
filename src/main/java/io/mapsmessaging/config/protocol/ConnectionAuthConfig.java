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

package io.mapsmessaging.config.protocol;

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
public class ConnectionAuthConfig extends Config {
  private String username;
  private String password;
  private String clientId;
  private String tokenGenerator;

  public ConnectionAuthConfig(ConfigurationProperties config) {
    this.username = config.getProperty("username");
    this.password = config.getProperty("password");
    this.clientId = config.getProperty("clientId");
    this.tokenGenerator = config.getProperty("tokenGenerator", "");
  }

  public boolean update(ConnectionAuthConfig newConfig) {
    boolean hasChanged = false;
    if (username != null && !username.equals(newConfig.getUsername())) {
      username = newConfig.getUsername();
      hasChanged = true;
    }

    if (password != null && !password.equals(newConfig.getPassword())) {
      password = newConfig.getPassword();
      hasChanged = true;
    }

    if (clientId != null && !clientId.equals(newConfig.getClientId())) {
      clientId = newConfig.getClientId();
      hasChanged = true;
    }
    return hasChanged;
  }

  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties config = new ConfigurationProperties();
    config.put("username", this.username);
    config.put("password", this.password);
    config.put("clientId", this.clientId);
    config.put("tokenGenerator", this.tokenGenerator);
    return config;
  }
}
