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

package io.mapsmessaging.config.network.impl;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.network.SslConfigDTO;
import io.mapsmessaging.dto.rest.config.network.impl.DtlsConfigDTO;
import io.mapsmessaging.dto.rest.config.network.impl.TlsConfigDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TlsSecurityConfigTest {

  @Test
  void tls_and_dtls_load_their_own_security_profiles() {
    ConfigurationProperties config = createConfig();

    TlsConfig tlsConfig = new TlsConfig(config);
    DtlsConfig dtlsConfig = new DtlsConfig(config);

    Assertions.assertFalse(tlsConfig.getSslConfig().isClientCertificateRequired());
    Assertions.assertEquals("TLSv1.3", tlsConfig.getSslConfig().getContext());
    Assertions.assertTrue(tlsConfig.getSslConfig().isHostnameVerificationEnabled());
    Assertions.assertTrue(dtlsConfig.getSslConfig().isClientCertificateRequired());
    Assertions.assertEquals("DTLSv1.2", dtlsConfig.getSslConfig().getContext());
  }

  @Test
  void tls_update_applies_nested_ssl_settings() {
    TlsConfig tlsConfig = new TlsConfig(createConfig());
    TlsConfigDTO update = new TlsConfigDTO();
    SslConfigDTO sslUpdate = createSslUpdate("TLSv1.2");
    update.setSslConfig(sslUpdate);

    Assertions.assertTrue(tlsConfig.update(update));
    Assertions.assertTrue(tlsConfig.getSslConfig().isClientCertificateRequired());
    Assertions.assertFalse(tlsConfig.getSslConfig().isHostnameVerificationEnabled());
    Assertions.assertEquals("TLSv1.2", tlsConfig.getSslConfig().getContext());
    Assertions.assertEquals("https://example.test/tls.crl", tlsConfig.getSslConfig().getCrlUrl());
  }

  @Test
  void dtls_update_applies_nested_ssl_settings() {
    DtlsConfig dtlsConfig = new DtlsConfig(createConfig());
    DtlsConfigDTO update = new DtlsConfigDTO();
    SslConfigDTO sslUpdate = createSslUpdate("DTLSv1.3");
    update.setSslConfig(sslUpdate);

    Assertions.assertTrue(dtlsConfig.update(update));
    Assertions.assertTrue(dtlsConfig.getSslConfig().isClientCertificateRequired());
    Assertions.assertEquals("DTLSv1.3", dtlsConfig.getSslConfig().getContext());
    Assertions.assertEquals(7200000L, dtlsConfig.getSslConfig().getCrlInterval());
  }

  private SslConfigDTO createSslUpdate(String context) {
    SslConfigDTO sslConfig = new SslConfigDTO();
    sslConfig.setClientCertificateRequired(true);
    sslConfig.setClientCertificateWanted(false);
    sslConfig.setHostnameVerificationEnabled(false);
    sslConfig.setContext(context);
    sslConfig.setCrlUrl("https://example.test/tls.crl");
    sslConfig.setCrlInterval(7200000L);
    return sslConfig;
  }

  private ConfigurationProperties createConfig() {
    ConfigurationProperties tls = createSecurityProfile("TLSv1.3", false);
    tls.put("hostnameVerificationEnabled", true);
    ConfigurationProperties dtls = createSecurityProfile("DTLSv1.2", true);

    ConfigurationProperties security = new ConfigurationProperties();
    security.put("tls", tls);
    security.put("dtls", dtls);

    ConfigurationProperties config = new ConfigurationProperties();
    config.put("security", security);
    return config;
  }

  private ConfigurationProperties createSecurityProfile(String context, boolean clientCertificateRequired) {
    ConfigurationProperties keyStore = new ConfigurationProperties();
    keyStore.put("type", "JKS");
    keyStore.put("managerFactory", "SunX509");
    keyStore.put("path", "test-keystore.jks");
    keyStore.put("passphrase", "password");

    ConfigurationProperties trustStore = new ConfigurationProperties();
    trustStore.put("type", "JKS");
    trustStore.put("managerFactory", "SunX509");
    trustStore.put("path", "test-truststore.jks");
    trustStore.put("passphrase", "password");

    ConfigurationProperties profile = new ConfigurationProperties();
    profile.put("context", context);
    profile.put("clientCertificateRequired", clientCertificateRequired);
    profile.put("clientCertificateWanted", false);
    profile.put("keyStore", keyStore);
    profile.put("trustStore", trustStore);
    return profile;
  }
}
