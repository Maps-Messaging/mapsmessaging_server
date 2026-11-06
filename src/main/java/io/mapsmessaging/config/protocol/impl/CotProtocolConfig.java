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

package io.mapsmessaging.config.protocol.impl;

import io.mapsmessaging.config.Config;
import io.mapsmessaging.config.network.KeyStoreConfig;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.CotProtocolConfigDTO;

public class CotProtocolConfig extends CotProtocolConfigDTO implements Config {

  public CotProtocolConfig(ConfigurationProperties config) {
    setType("cot");
    ProtocolConfigFactory.unpack(config, this);
    this.takHostname = config.getProperty("takHostname", takHostname);
    this.takPort = config.getIntProperty("takPort", takPort);
    this.takTlsEnabled = config.getBooleanProperty("takTlsEnabled", takTlsEnabled);
    this.takTlsContext = config.getProperty("takTlsContext", takTlsContext);

    ConfigurationProperties keyStoreProps = (ConfigurationProperties) config.get("takKeyStore");
    if (keyStoreProps != null) {
      this.takKeyStore = new KeyStoreConfig(keyStoreProps);
    }
    ConfigurationProperties trustStoreProps = (ConfigurationProperties) config.get("takTrustStore");
    if (trustStoreProps != null) {
      this.takTrustStore = new KeyStoreConfig(trustStoreProps);
    }
  }

  @Override
  public boolean update(BaseConfigDTO config) {
    boolean hasChanged = false;
    if (config instanceof CotProtocolConfigDTO newConfig) {
      if (!this.takHostname.equals(newConfig.getTakHostname())) {
        this.takHostname = newConfig.getTakHostname();
        hasChanged = true;
      }
      if (this.takPort != newConfig.getTakPort()) {
        this.takPort = newConfig.getTakPort();
        hasChanged = true;
      }
      if (this.takTlsEnabled != newConfig.isTakTlsEnabled()) {
        this.takTlsEnabled = newConfig.isTakTlsEnabled();
        hasChanged = true;
      }
      if (!this.takTlsContext.equals(newConfig.getTakTlsContext())) {
        this.takTlsContext = newConfig.getTakTlsContext();
        hasChanged = true;
      }
      if (this.takKeyStore != null && ((Config) this.takKeyStore).update(newConfig.getTakKeyStore())) {
        hasChanged = true;
      }
      if (this.takTrustStore != null && ((Config) this.takTrustStore).update(newConfig.getTakTrustStore())) {
        hasChanged = true;
      }
      if (ProtocolConfigFactory.update(this, newConfig)) {
        hasChanged = true;
      }
    }
    return hasChanged;
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties properties = new ConfigurationProperties();
    ProtocolConfigFactory.pack(properties, this);
    properties.put("takHostname", this.takHostname);
    properties.put("takPort", this.takPort);
    properties.put("takTlsEnabled", this.takTlsEnabled);
    properties.put("takTlsContext", this.takTlsContext);
    if (this.takKeyStore != null) {
      properties.put("takKeyStore", ((Config) this.takKeyStore).toConfigurationProperties());
    }
    if (this.takTrustStore != null) {
      properties.put("takTrustStore", ((Config) this.takTrustStore).toConfigurationProperties());
    }
    return properties;
  }
}
