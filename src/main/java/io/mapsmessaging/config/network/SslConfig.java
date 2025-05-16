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

package io.mapsmessaging.config.network;

import io.mapsmessaging.config.Config;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.network.SslConfigDTO;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@ToString
public class SslConfig extends SslConfigDTO implements Config  {

  public SslConfig(ConfigurationProperties config) {
    ConfigurationProperties securityProps = locateConfig(config);
    this.context = securityProps.getProperty("context", "tls");
    this.clientCertificateRequired = config.getBooleanProperty("clientCertificateRequired", false);
    this.clientCertificateWanted = config.getBooleanProperty("clientCertificateWanted", false);
    this.crlUrl = config.getProperty("crlUrl", null);
    this.crlInterval = config.getLongProperty("crlInterval", 0);

    this.keyStore = new KeyStoreConfig((ConfigurationProperties) securityProps.get("keyStore"));
    this.trustStore = new KeyStoreConfig((ConfigurationProperties) securityProps.get("trustStore"));
  }

  private ConfigurationProperties locateConfig(ConfigurationProperties config) {
    if (config.containsKey("clientCertificateRequired")) {
      return config;
    }
    ConfigurationProperties security = (ConfigurationProperties) config.get("security");
    if (security != null) {
      security = (ConfigurationProperties) security.get("tls");
    }

    ConfigurationProperties endPoint = (ConfigurationProperties) config.get("endPoint");
    if (endPoint != null) {
      return locateConfig(endPoint);
    }

    return security;
  }

  public boolean update(BaseConfigDTO config) {
    boolean hasChanged = false;
    if (config instanceof SslConfigDTO) {
      SslConfigDTO newConfig = (SslConfigDTO) config;

      if (this.clientCertificateRequired != newConfig.isClientCertificateRequired()) {
        this.clientCertificateRequired = newConfig.isClientCertificateRequired();
        hasChanged = true;
      }
      if (this.clientCertificateWanted != newConfig.isClientCertificateWanted()) {
        this.clientCertificateWanted = newConfig.isClientCertificateWanted();
        hasChanged = true;
      }
      if (!this.crlUrl.equals(newConfig.getCrlUrl())) {
        this.crlUrl = newConfig.getCrlUrl();
        hasChanged = true;
      }
      if (this.crlInterval != newConfig.getCrlInterval()) {
        this.crlInterval = newConfig.getCrlInterval();
        hasChanged = true;
      }
      if (((KeyStoreConfig)this.keyStore).update(newConfig.getKeyStore())) {
        hasChanged = true;
      }
      if (((KeyStoreConfig)this.trustStore).update(newConfig.getTrustStore())) {
        hasChanged = true;
      }
    }
    return hasChanged;
  }

  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties config = new ConfigurationProperties();
    config.put("clientCertificateRequired", this.clientCertificateRequired);
    config.put("clientCertificateWanted", this.clientCertificateWanted);
    config.put("crlUrl", this.crlUrl);
    config.put("crlInterval", this.crlInterval);
    config.put("keyStore", ((KeyStoreConfig) keyStore).toConfigurationProperties());
    config.put("trustStore", ((KeyStoreConfig) trustStore).toConfigurationProperties());
    return config;
  }
}
