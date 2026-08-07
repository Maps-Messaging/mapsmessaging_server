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

package io.mapsmessaging.config.network;

import io.mapsmessaging.config.Config;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.network.SslConfigDTO;
import java.util.Objects;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@ToString
public class SslConfig extends SslConfigDTO implements Config {

  public SslConfig(ConfigurationProperties config) {
    this(config, "tls");
  }

  public SslConfig(ConfigurationProperties config, String transport) {
    ConfigurationProperties securityProps = locateConfig(config, transport);
    if (securityProps == null) {
      throw new IllegalArgumentException("Missing security." + transport + " configuration");
    }

    String defaultContext = "dtls".equalsIgnoreCase(transport) ? "DTLSv1.2" : "TLS";
    this.context = securityProps.getProperty("context", defaultContext);
    this.clientCertificateRequired = securityProps.getBooleanProperty("clientCertificateRequired", false);
    this.clientCertificateWanted = securityProps.getBooleanProperty("clientCertificateWanted", false);
    this.hostnameVerificationEnabled = securityProps.getBooleanProperty("hostnameVerificationEnabled", true);
    this.crlUrl = securityProps.getProperty("crlUrl", null);
    this.crlInterval = securityProps.getLongProperty("crlInterval", 3600000L);
    this.keyStore = new KeyStoreConfig((ConfigurationProperties) securityProps.get("keyStore"));
    this.trustStore = new KeyStoreConfig((ConfigurationProperties) securityProps.get("trustStore"));
  }

  private ConfigurationProperties locateConfig(ConfigurationProperties config, String transport) {
    if (config.containsKey("keyStore") || config.containsKey("trustStore") || config.containsKey("clientCertificateRequired")) {
      return config;
    }

    ConfigurationProperties endPoint = (ConfigurationProperties) config.get("endPoint");
    if (endPoint != null) {
      ConfigurationProperties endPointSecurity = locateConfig(endPoint, transport);
      if (endPointSecurity != null) {
        return endPointSecurity;
      }
    }

    ConfigurationProperties security = (ConfigurationProperties) config.get("security");
    return security == null ? null : (ConfigurationProperties) security.get(transport);
  }

  public boolean update(BaseConfigDTO config) {
    if (!(config instanceof SslConfigDTO newConfig)) {
      return false;
    }

    boolean hasChanged = false;
    if (this.clientCertificateRequired != newConfig.isClientCertificateRequired()) {
      this.clientCertificateRequired = newConfig.isClientCertificateRequired();
      hasChanged = true;
    }
    if (this.clientCertificateWanted != newConfig.isClientCertificateWanted()) {
      this.clientCertificateWanted = newConfig.isClientCertificateWanted();
      hasChanged = true;
    }
    if (this.hostnameVerificationEnabled != newConfig.isHostnameVerificationEnabled()) {
      this.hostnameVerificationEnabled = newConfig.isHostnameVerificationEnabled();
      hasChanged = true;
    }
    if (!Objects.equals(this.context, newConfig.getContext())) {
      this.context = newConfig.getContext();
      hasChanged = true;
    }
    if (!Objects.equals(this.crlUrl, newConfig.getCrlUrl())) {
      this.crlUrl = newConfig.getCrlUrl();
      hasChanged = true;
    }
    if (this.crlInterval != newConfig.getCrlInterval()) {
      this.crlInterval = newConfig.getCrlInterval();
      hasChanged = true;
    }
    if (newConfig.getKeyStore() != null && ((KeyStoreConfig) this.keyStore).update(newConfig.getKeyStore())) {
      hasChanged = true;
    }
    if (newConfig.getTrustStore() != null && ((KeyStoreConfig) this.trustStore).update(newConfig.getTrustStore())) {
      hasChanged = true;
    }
    return hasChanged;
  }

  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties config = new ConfigurationProperties();
    config.put("context", this.context);
    config.put("clientCertificateRequired", this.clientCertificateRequired);
    config.put("clientCertificateWanted", this.clientCertificateWanted);
    config.put("hostnameVerificationEnabled", this.hostnameVerificationEnabled);
    config.put("crlUrl", this.crlUrl);
    config.put("crlInterval", this.crlInterval);
    config.put("keyStore", ((KeyStoreConfig) keyStore).toConfigurationProperties());
    config.put("trustStore", ((KeyStoreConfig) trustStore).toConfigurationProperties());
    return config;
  }
}
