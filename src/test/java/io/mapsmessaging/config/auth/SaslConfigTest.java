package io.mapsmessaging.config.auth;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.auth.SaslConfigDTO;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.*;

class SaslConfigTest {

  @Test
  void configuredIdentityAndCustomEntriesRoundTrip() {
    ConfigurationProperties properties = properties();

    SaslConfig restored = new SaslConfig(new SaslConfig(properties).toConfigurationProperties());

    assertEquals("maps", restored.getIdentityProvider());
    assertEquals("edge", restored.getRealmName());
    assertEquals("SCRAM-SHA-256", restored.getMechanism());
    assertEquals("value", restored.getSaslEntries().get("custom"));
  }

  @Test
  void updateAppliesScalarChangesWithoutLosingSaslEntries() {
    SaslConfig config = new SaslConfig(properties());
    SaslConfigDTO updated = new SaslConfigDTO();
    updated.setIdentityProvider("new-provider");
    updated.setRealmName("new-realm");
    updated.setMechanism("PLAIN");
    updated.setSaslEntries(new LinkedHashMap<>(config.getSaslEntries()));

    assertTrue(config.update(updated));
    assertEquals("new-provider", config.getIdentityProvider());
    assertEquals("value", config.getSaslEntries().get("custom"));
    assertFalse(config.update(updated));
    assertFalse(config.update(new BaseConfigDTO()));
  }

  private static ConfigurationProperties properties() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("identityProvider", "maps");
    properties.put("realmName", "edge");
    properties.put("mechanism", "SCRAM-SHA-256");
    properties.put("custom", "value");
    return properties;
  }
}