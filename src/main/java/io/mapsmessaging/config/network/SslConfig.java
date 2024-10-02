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
import lombok.ToString;

@Data
@NoArgsConstructor
@ToString
public class SslConfig {
  private boolean clientCertificateRequired;
  private boolean clientCertificateWanted;
  private String crlUrl;
  private long crlInterval;

  private String context;
  private KeyStoreConfig keyStore;
  private KeyStoreConfig trustStore;

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
    return security;
  }

  public boolean update(SslConfig newConfig) {
    boolean hasChanged = false;

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
    if (this.keyStore.update(newConfig.getKeyStore())) {
      hasChanged = true;
    }
    if (this.trustStore.update(newConfig.getTrustStore())) {
      hasChanged = true;
    }

    return hasChanged;
  }

  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties config = new ConfigurationProperties();
    config.put("clientCertificateRequired", this.clientCertificateRequired);
    config.put("clientCertificateWanted", this.clientCertificateWanted);
    config.put("crlUrl", this.crlUrl);
    config.put("crlInterval", this.crlInterval);
    config.put("keyStore", keyStore.toConfigurationProperties());
    config.put("trustStore", trustStore.toConfigurationProperties());
    return config;
  }
}
