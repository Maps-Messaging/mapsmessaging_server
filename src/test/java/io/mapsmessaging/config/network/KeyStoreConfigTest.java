package io.mapsmessaging.config.network;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.network.KeyStoreConfigDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KeyStoreConfigTest {

  @Test
  void defaultsAreAppliedWhenOnlyAPathIsConfigured() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("path", "/tmp/store.jks");

    KeyStoreConfig config = new KeyStoreConfig(properties);

    assertEquals("JKS", config.getType());
    assertEquals("SunX509", config.getManagerFactory());
    assertEquals("/tmp/store.jks", config.getPath());
  }

  @Test
  void allFieldsRoundTripThroughConfigurationProperties() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("alias", "maps");
    properties.put("type", "PKCS12");
    properties.put("providerName", "BC");
    properties.put("managerFactory", "PKIX");
    properties.put("path", "/tmp/store.p12");
    properties.put("passphrase", "secret");
    properties.put("provider", "provider-id");

    KeyStoreConfig restored = new KeyStoreConfig(new KeyStoreConfig(properties).toConfigurationProperties());

    assertEquals("maps", restored.getAlias());
    assertEquals("PKCS12", restored.getType());
    assertEquals("BC", restored.getProviderName());
    assertEquals("PKIX", restored.getManagerFactory());
    assertEquals("provider-id", restored.getProvider());
  }

  @Test
  void updateAppliesChangedFieldsAndIgnoresUnrelatedDto() {
    KeyStoreConfig config = new KeyStoreConfig(new ConfigurationProperties());
    KeyStoreConfigDTO updated = new KeyStoreConfigDTO();
    updated.setAlias("alias");
    updated.setType("PKCS12");
    updated.setManagerFactory("PKIX");
    updated.setPath("/tmp/new.p12");
    updated.setPassphrase("new-secret");

    assertTrue(config.update(updated));
    assertEquals("/tmp/new.p12", config.getPath());
    assertFalse(config.update(updated));
    assertFalse(config.update(new BaseConfigDTO()));
  }
}