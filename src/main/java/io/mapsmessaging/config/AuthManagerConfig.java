/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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
import java.util.Objects;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@ToString
@Schema(description = "Auth Manager Configuration")
public class AuthManagerConfig extends AuthManagerConfigDTO implements ConfigManager {

  private AuthManagerConfig(ConfigurationProperties properties) {
    authenticationEnabled = properties.getBooleanProperty("authenticationEnabled", authenticationEnabled);
    authorisationEnabled = properties.getBooleanProperty("authorizationEnabled", authorisationEnabled) && authenticationEnabled;
    authConfig = ConfigHelper.buildMap((ConfigurationProperties) properties.get("config"));
    minimumPasswordLength = properties.getIntProperty("minimumPasswordLength", minimumPasswordLength);
    maximumPasswordLength = properties.getIntProperty("maximumPasswordLength", maximumPasswordLength);
    minimumLowercase = properties.getIntProperty("minimumLowercase", minimumLowercase);
    minimumUppercase = properties.getIntProperty("minimumUppercase", minimumUppercase);
    minimumDigits = properties.getIntProperty("minimumDigits", minimumDigits);
    minimumSpecial = properties.getIntProperty("minimumSpecial", minimumSpecial);
    allowedSpecialCharacters = properties.getProperty("allowedSpecialCharacters", allowedSpecialCharacters);
    rejectWhitespace = properties.getBooleanProperty("rejectWhitespace", rejectWhitespace);
    rejectContainsUsername = properties.getBooleanProperty("rejectContainsUsername", rejectContainsUsername);
    maximumConsecutiveIdenticalCharacters = properties.getIntProperty("maximumConsecutiveIdenticalCharacters", maximumConsecutiveIdenticalCharacters);
    passwordRegex = properties.getProperty("passwordRegex", passwordRegex);
    passwordHistoryCount = properties.getIntProperty("passwordHistoryCount", passwordHistoryCount);
    passwordMaxAgeDays = properties.getIntProperty("passwordMaxAgeDays", passwordMaxAgeDays);
    maxFailuresBeforeLock = properties.getIntProperty("maxFailuresBeforeLock", maxFailuresBeforeLock);
    initialLockSeconds = properties.getIntProperty("initialLockSeconds", initialLockSeconds);
    maxLockSeconds = properties.getIntProperty("maxLockSeconds", maxLockSeconds);
    failureDecaySeconds = properties.getIntProperty("failureDecaySeconds", failureDecaySeconds);
    enableSoftDelay = properties.getBooleanProperty("enableSoftDelay", enableSoftDelay);
    softDelayMillisPerFailure = properties.getIntProperty("softDelayMillisPerFailure", softDelayMillisPerFailure);
    maxSoftDelayMillis = properties.getIntProperty("maxSoftDelayMillis", maxSoftDelayMillis);
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

    if (authenticationEnabled != newConfig.authenticationEnabled) {
      authenticationEnabled = newConfig.authenticationEnabled;
      hasChanged = true;
    }

    if (authorisationEnabled != newConfig.authorisationEnabled) {
      authorisationEnabled = newConfig.authorisationEnabled;
      hasChanged = true;
    }

    if (minimumPasswordLength != newConfig.minimumPasswordLength) {
      minimumPasswordLength = newConfig.minimumPasswordLength;
      hasChanged = true;
    }

    if (maximumPasswordLength != newConfig.maximumPasswordLength) {
      maximumPasswordLength = newConfig.maximumPasswordLength;
      hasChanged = true;
    }

    if (minimumLowercase != newConfig.minimumLowercase) {
      minimumLowercase = newConfig.minimumLowercase;
      hasChanged = true;
    }

    if (minimumUppercase != newConfig.minimumUppercase) {
      minimumUppercase = newConfig.minimumUppercase;
      hasChanged = true;
    }

    if (minimumDigits != newConfig.minimumDigits) {
      minimumDigits = newConfig.minimumDigits;
      hasChanged = true;
    }

    if (minimumSpecial != newConfig.minimumSpecial) {
      minimumSpecial = newConfig.minimumSpecial;
      hasChanged = true;
    }

    if (!Objects.equals(allowedSpecialCharacters, newConfig.allowedSpecialCharacters)) {
      allowedSpecialCharacters = newConfig.allowedSpecialCharacters;
      hasChanged = true;
    }

    if (rejectWhitespace != newConfig.rejectWhitespace) {
      rejectWhitespace = newConfig.rejectWhitespace;
      hasChanged = true;
    }

    if (rejectContainsUsername != newConfig.rejectContainsUsername) {
      rejectContainsUsername = newConfig.rejectContainsUsername;
      hasChanged = true;
    }

    if (maximumConsecutiveIdenticalCharacters != newConfig.maximumConsecutiveIdenticalCharacters) {
      maximumConsecutiveIdenticalCharacters = newConfig.maximumConsecutiveIdenticalCharacters;
      hasChanged = true;
    }

    if (!Objects.equals(passwordRegex, newConfig.passwordRegex)) {
      passwordRegex = newConfig.passwordRegex;
      hasChanged = true;
    }

    if (passwordHistoryCount != newConfig.passwordHistoryCount) {
      passwordHistoryCount = newConfig.passwordHistoryCount;
      hasChanged = true;
    }

    if (passwordMaxAgeDays != newConfig.passwordMaxAgeDays) {
      passwordMaxAgeDays = newConfig.passwordMaxAgeDays;
      hasChanged = true;
    }

    if (maxFailuresBeforeLock != newConfig.maxFailuresBeforeLock) {
      maxFailuresBeforeLock = newConfig.maxFailuresBeforeLock;
      hasChanged = true;
    }

    if (initialLockSeconds != newConfig.initialLockSeconds) {
      initialLockSeconds = newConfig.initialLockSeconds;
      hasChanged = true;
    }

    if (maxLockSeconds != newConfig.maxLockSeconds) {
      maxLockSeconds = newConfig.maxLockSeconds;
      hasChanged = true;
    }

    if (failureDecaySeconds != newConfig.failureDecaySeconds) {
      failureDecaySeconds = newConfig.failureDecaySeconds;
      hasChanged = true;
    }

    if (enableSoftDelay != newConfig.enableSoftDelay) {
      enableSoftDelay = newConfig.enableSoftDelay;
      hasChanged = true;
    }

    if (softDelayMillisPerFailure != newConfig.softDelayMillisPerFailure) {
      softDelayMillisPerFailure = newConfig.softDelayMillisPerFailure;
      hasChanged = true;
    }

    if (maxSoftDelayMillis != newConfig.maxSoftDelayMillis) {
      maxSoftDelayMillis = newConfig.maxSoftDelayMillis;
      hasChanged = true;
    }
    return hasChanged;
  }

  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("authenticationEnabled", authenticationEnabled);
    properties.put("authorizationEnabled", authorisationEnabled);
    properties.put("authorisationEnabled", authorisationEnabled);
    properties.put("config", authConfig);
    properties.put("minimumPasswordLength", minimumPasswordLength);
    properties.put("maximumPasswordLength", maximumPasswordLength);
    properties.put("minimumLowercase", minimumLowercase);
    properties.put("minimumUppercase", minimumUppercase);
    properties.put("minimumDigits", minimumDigits);
    properties.put("minimumSpecial", minimumSpecial);
    properties.put("allowedSpecialCharacters", allowedSpecialCharacters);
    properties.put("rejectWhitespace", rejectWhitespace);
    properties.put("rejectContainsUsername", rejectContainsUsername);
    properties.put("maximumConsecutiveIdenticalCharacters", maximumConsecutiveIdenticalCharacters);
    properties.put("passwordRegex", passwordRegex);
    properties.put("passwordHistoryCount", passwordHistoryCount);
    properties.put("passwordMaxAgeDays", passwordMaxAgeDays);
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
