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

package io.mapsmessaging.config.auth;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.config.Config;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.auth.SaslConfigDTO;

import java.util.LinkedHashMap;
import java.util.Map;

public class SaslConfig extends SaslConfigDTO implements Config {

  private static final String IDENTITY_PROVIDER = "identityProvider";

  public SaslConfig(ConfigurationProperties config) {
    identityProvider = config.getProperty(IDENTITY_PROVIDER);
    realmName = config.getProperty("realmName", MessageDaemon.getInstance().getId());
    mechanism = config.getProperty("mechanism");
    saslEntries = new LinkedHashMap<>();
    for (Map.Entry<String, Object> entry : config.entrySet()) {
      if (!entry.getKey().equalsIgnoreCase(IDENTITY_PROVIDER)) {
        saslEntries.put(entry.getKey(), entry.getValue());
      }
    }
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put(IDENTITY_PROVIDER, this.identityProvider);
    properties.put("realmName", this.realmName);
    properties.put("mechanism", this.mechanism);

    // Include saslEntries, excluding identityProvider
    if (saslEntries != null) {
      for (Map.Entry<String, Object> entry : saslEntries.entrySet()) {
        properties.put(entry.getKey(), entry.getValue());
      }
    }
    return properties;
  }

  public boolean update(BaseConfigDTO config) {
    if (!(config instanceof SaslConfigDTO)) {
      return false;
    }
    SaslConfigDTO newConfig = (SaslConfigDTO) config;
    boolean hasChanged = false;

    // Check and update realmName
    if (this.realmName == null || !this.realmName.equals(newConfig.getRealmName())) {
      this.realmName = newConfig.getRealmName();
      hasChanged = true;
    }

    // Check and update mechanism
    if (this.mechanism == null || !this.mechanism.equals(newConfig.getMechanism())) {
      this.mechanism = newConfig.getMechanism();
      hasChanged = true;
    }

    // Check and update identityProvider
    if (this.identityProvider == null
        || !this.identityProvider.equals(newConfig.getIdentityProvider())) {
      this.identityProvider = newConfig.getIdentityProvider();
      hasChanged = true;
    }

    // Check and update saslEntries map
    if (updateMap(this.saslEntries, newConfig.getSaslEntries())) {
      hasChanged = true;
    }

    return hasChanged;
  }
}
