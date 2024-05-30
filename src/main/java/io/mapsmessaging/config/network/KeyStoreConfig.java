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

package io.mapsmessaging.config.network;

import io.mapsmessaging.configuration.ConfigurationProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class KeyStoreConfig {
  private String alias;
  private String type;
  private String providerName;
  private String managerFactory;
  private String path;
  private String passphrase;
  private String provider;
  private String config;

  public KeyStoreConfig(ConfigurationProperties config) {
    this.alias = config.getProperty("alias", null);
    this.type = config.getProperty("type", "JKS");
    this.providerName = config.getProperty("providerName", null);
    this.managerFactory = config.getProperty("managerFactory", "SunX509");
    this.path = config.getProperty("path", null);
    this.passphrase = config.getProperty("passphrase", null);
    this.provider = config.getProperty("provider", null);
    this.config = config.getProperty("config", null);
  }

  public boolean update(KeyStoreConfig newConfig) {
    boolean hasChanged = false;

    if (!this.alias.equals(newConfig.getAlias())) {
      this.alias = newConfig.getAlias();
      hasChanged = true;
    }
    if (!this.type.equals(newConfig.getType())) {
      this.type = newConfig.getType();
      hasChanged = true;
    }
    if (!this.providerName.equals(newConfig.getProviderName())) {
      this.providerName = newConfig.getProviderName();
      hasChanged = true;
    }
    if (!this.managerFactory.equals(newConfig.getManagerFactory())) {
      this.managerFactory = newConfig.getManagerFactory();
      hasChanged = true;
    }
    if (!this.path.equals(newConfig.getPath())) {
      this.path = newConfig.getPath();
      hasChanged = true;
    }
    if (!this.passphrase.equals(newConfig.getPassphrase())) {
      this.passphrase = newConfig.getPassphrase();
      hasChanged = true;
    }
    if (!this.provider.equals(newConfig.getProvider())) {
      this.provider = newConfig.getProvider();
      hasChanged = true;
    }
    if (!this.config.equals(newConfig.getConfig())) {
      this.config = newConfig.getConfig();
      hasChanged = true;
    }

    return hasChanged;
  }

  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties keyStoreConfig = new ConfigurationProperties();
    keyStoreConfig.put("alias", getAlias());
    keyStoreConfig.put("type", getType());
    keyStoreConfig.put("providerName", getProviderName());
    keyStoreConfig.put("managerFactory", getManagerFactory());
    keyStoreConfig.put("path", getPath());
    keyStoreConfig.put("passphrase", getPassphrase());
    keyStoreConfig.put("provider", getProvider());
    keyStoreConfig.put("config", getConfig());
    return keyStoreConfig;
  }
}
