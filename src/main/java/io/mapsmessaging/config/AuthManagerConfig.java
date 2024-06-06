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
@Schema(description = "Auth Manager Configuration")
public class AuthManagerConfig  extends ManagementConfig {

  private boolean authenticationEnabled;
  private boolean authorisationEnabled;
  private ConfigurationProperties authConfig;

  private AuthManagerConfig(ConfigurationProperties properties) {
    authenticationEnabled = properties.getBooleanProperty("authenticationEnabled", false);
    authorisationEnabled = properties.getBooleanProperty("authorizationEnabled", false) && authenticationEnabled;
    authConfig = (ConfigurationProperties)properties.get("config");
  }

  public static AuthManagerConfig getInstance() {
    return new AuthManagerConfig(ConfigurationManager.getInstance().getProperties("AuthManager"));
  }

  public boolean update(ManagementConfig config) {
    AuthManagerConfig newConfig = (AuthManagerConfig)config;
    boolean hasChanged = false;

    if (this.authenticationEnabled != newConfig.isAuthenticationEnabled()) {
      this.authenticationEnabled = newConfig.isAuthenticationEnabled();
      hasChanged = true;
    }

    if (this.authorisationEnabled != newConfig.isAuthorisationEnabled()) {
      this.authorisationEnabled = newConfig.isAuthorisationEnabled();
      hasChanged = true;
    }
    if(updateMap(authConfig.getMap(), newConfig.authConfig.getMap())){
      hasChanged = true;
    }

    return hasChanged;
  }

  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("authenticationEnabled", authorisationEnabled);
    properties.put("authorisationEnabled", authorisationEnabled);
    properties.put("config", authConfig);
    return properties;
  }
}
