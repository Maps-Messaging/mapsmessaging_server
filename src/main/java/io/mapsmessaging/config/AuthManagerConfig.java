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

package io.mapsmessaging.config;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.AuthManagerConfigDTO;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.license.FeatureManager;
import io.mapsmessaging.security.access.monitor.AuthenticationMonitorConfig;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.IOException;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@ToString
@Schema(description = "Auth Manager Configuration")
public class AuthManagerConfig extends AuthManagerConfigDTO implements ConfigManager {

  private AuthManagerConfig(ConfigurationProperties properties) {
    authenticationEnabled = properties.getBooleanProperty("authenticationEnabled", false);
    authorisationEnabled = properties.getBooleanProperty("authorizationEnabled", false) && authenticationEnabled;
    authConfig = ConfigHelper.buildMap((ConfigurationProperties) properties.get("config"));

    maxFailuresBeforeLock = properties.getIntProperty("maxFailuresBeforeLock", 5);
    initialLockSeconds = properties.getIntProperty("initialLockSeconds", 30);
    maxLockSeconds = properties.getIntProperty("maxLockSeconds", 900);
    failureDecaySeconds = properties.getIntProperty("failureDecaySeconds", 900);
    enableSoftDelay = properties.getBooleanProperty("enableSoftDelay", true);
    softDelayMillisPerFailure = properties.getIntProperty("softDelayMillisPerFailure", 200);
    maxSoftDelayMillis = properties.getIntProperty("maxSoftDelayMillis", 2000);
  }

  public AuthenticationMonitorConfig buildMonitorConfig(){
    AuthenticationMonitorConfig monitorConfig = new AuthenticationMonitorConfig();
    monitorConfig.setMaxFailuresBeforeLock(maxFailuresBeforeLock);
    monitorConfig.setInitialLockSeconds(initialLockSeconds);
    monitorConfig.setMaxLockSeconds(maxLockSeconds);
    monitorConfig.setFailureDecaySeconds(failureDecaySeconds);
    monitorConfig.setEnableSoftDelay(enableSoftDelay);
    monitorConfig.setSoftDelayMillisPerFailure(softDelayMillisPerFailure);
    monitorConfig.setMaxSoftDelayMillis(maxSoftDelayMillis);
    return monitorConfig;
  }

  public static AuthManagerConfig getInstance() {
    return ConfigurationManager.getInstance().getConfiguration(AuthManagerConfig.class);
  }

  public boolean update(BaseConfigDTO config) {
    AuthManagerConfig newConfig = (AuthManagerConfig) config;
    boolean hasChanged = false;

    if (this.authenticationEnabled != newConfig.isAuthenticationEnabled()) {
      this.authenticationEnabled = newConfig.isAuthenticationEnabled();
      hasChanged = true;
    }

    if (this.authorisationEnabled != newConfig.isAuthorisationEnabled()) {
      this.authorisationEnabled = newConfig.isAuthorisationEnabled();
      hasChanged = true;
    }
    if(maxFailuresBeforeLock != newConfig.maxFailuresBeforeLock){
      maxFailuresBeforeLock = newConfig.maxFailuresBeforeLock;
      hasChanged = true;
    }

    if(initialLockSeconds != newConfig.initialLockSeconds){
      initialLockSeconds = newConfig.initialLockSeconds;
      hasChanged = true;
    }

    if(maxLockSeconds != newConfig.maxLockSeconds){
      maxLockSeconds = newConfig.maxLockSeconds;
      hasChanged = true;
    }

    if(failureDecaySeconds != newConfig.failureDecaySeconds){
      failureDecaySeconds = newConfig.failureDecaySeconds;
      hasChanged = true;
    }

    if(enableSoftDelay != newConfig.enableSoftDelay){
      enableSoftDelay = newConfig.enableSoftDelay;
      hasChanged = true;
    }

    if(softDelayMillisPerFailure != newConfig.softDelayMillisPerFailure){
      softDelayMillisPerFailure = newConfig.softDelayMillisPerFailure;
      hasChanged = true;
    }

    if(maxSoftDelayMillis != newConfig.maxSoftDelayMillis){
      maxSoftDelayMillis = newConfig.maxSoftDelayMillis;
      hasChanged = true;
    }
    return hasChanged;
  }

  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("authenticationEnabled", authorisationEnabled);
    properties.put("authorisationEnabled", authorisationEnabled);
    properties.put("config", authConfig);
    properties.put("maxFailuresBeforeLock", maxFailuresBeforeLock);
    properties.put("initialLockSeconds", initialLockSeconds);
    properties.put("maxLockSeconds", maxLockSeconds);
    properties.put("failureDecaySeconds", failureDecaySeconds);
    properties.put("enableSoftDelay", enableSoftDelay);
    properties.put("softDelayMillisPerFailure", softDelayMillisPerFailure);
    properties.put("maxSoftDelayMillis", maxSoftDelayMillis);
    return properties;
  }

  @Override
  public ConfigManager load(FeatureManager featureManager) {
    return new AuthManagerConfig(ConfigurationManager.getInstance().getProperties(getName()));
  }

  @Override
  public void save() throws IOException {
    ConfigurationManager.getInstance().saveConfiguration(getName(), toConfigurationProperties());
  }

  @Override
  public String getName() {
    return "AuthManager";
  }
}
