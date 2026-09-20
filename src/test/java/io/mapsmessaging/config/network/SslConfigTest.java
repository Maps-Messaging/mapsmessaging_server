package io.mapsmessaging.config.network;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.network.KeyStoreConfigDTO;
import io.mapsmessaging.dto.rest.config.network.SslConfigDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SslConfigTest {

  @Test
  void directTlsConfigurationLoadsAndRoundTrips() {
    SslConfig restored = new SslConfig(new SslConfig(directTls()).toConfigurationProperties());

    assertEquals("TLSv1.3", restored.getContext());
    assertTrue(restored.isClientCertificateRequired());
    assertFalse(restored.isHostnameVerificationEnabled());
    assertEquals("http://localhost/crl", restored.getCrlUrl());
    assertEquals("/tmp/key.p12", restored.getKeyStore().getPath());
    assertEquals("/tmp/trust.p12", restored.getTrustStore().getPath());
  }

  @Test
  void nestedSecuritySectionIsLocatedForRequestedTransport() {
    ConfigurationProperties security = new ConfigurationProperties();
    security.put("tls", directTls());
    ConfigurationProperties root = new ConfigurationProperties();
    root.put("security", security);

    SslConfig config = new SslConfig(root, "tls");

    assertEquals("TLSv1.3", config.getContext());
    assertEquals("/tmp/key.p12", config.getKeyStore().getPath());
  }

  @Test
  void missingSecurityConfigurationIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> new SslConfig(new ConfigurationProperties()));
  }

  @Test
  void updateAppliesTlsAndNestedKeyStoreChanges() {
    SslConfig config = new SslConfig(directTls());
    SslConfigDTO updated = new SslConfigDTO();
    updated.setContext("TLSv1.2");
    updated.setClientCertificateRequired(false);
    updated.setClientCertificateWanted(true);
    updated.setHostnameVerificationEnabled(true);
    updated.setCrlUrl("http://localhost/new.crl");
    updated.setCrlInterval(5000);
    KeyStoreConfigDTO keyStore = new KeyStoreConfigDTO();
    keyStore.setType("PKCS12");
    keyStore.setManagerFactory("SunX509");
    keyStore.setPath("/tmp/new-key.p12");
    updated.setKeyStore(keyStore);

    assertTrue(config.update(updated));
    assertEquals("TLSv1.2", config.getContext());
    assertEquals("/tmp/new-key.p12", config.getKeyStore().getPath());
    assertFalse(config.update(updated));
  }

  private static ConfigurationProperties directTls() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("context", "TLSv1.3");
    properties.put("clientCertificateRequired", true);
    properties.put("clientCertificateWanted", false);
    properties.put("hostnameVerificationEnabled", false);
    properties.put("crlUrl", "http://localhost/crl");
    properties.put("crlInterval", 4000L);
    properties.put("keyStore", keyStore("/tmp/key.p12"));
    properties.put("trustStore", keyStore("/tmp/trust.p12"));
    return properties;
  }

  private static ConfigurationProperties keyStore(String path) {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("type", "PKCS12");
    properties.put("managerFactory", "SunX509");
    properties.put("path", path);
    return properties;
  }
}