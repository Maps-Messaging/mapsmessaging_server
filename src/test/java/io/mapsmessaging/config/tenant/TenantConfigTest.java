package io.mapsmessaging.config.tenant;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.rest.StaticConfigDTO;
import io.mapsmessaging.dto.rest.config.tenant.TenantConfigDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TenantConfigTest {

  @Test
  void configurationRoundTripsTenantIdentity() {
    ConfigurationProperties props = new ConfigurationProperties();
    props.put("name", "ops");
    props.put("namespaceRoot", "/tenant/ops");
    props.put("scope", "regional");

    TenantConfig source = new TenantConfig(props);
    TenantConfig restored = new TenantConfig(source.toConfigurationProperties());

    assertEquals(source.getName(), restored.getName());
    assertEquals(source.getNamespaceRoot(), restored.getNamespaceRoot());
    assertEquals(source.getScope(), restored.getScope());
  }

  @Test
  void updateAppliesChangesAndThenBecomesStable() {
    TenantConfig config = new TenantConfig(new ConfigurationProperties());
    TenantConfigDTO update = new TenantConfigDTO();
    update.setName("tenant-a");
    update.setNamespaceRoot("/a");
    update.setScope("global");

    assertTrue(config.update(update));
    assertEquals("tenant-a", config.getName());
    assertEquals("/a", config.getNamespaceRoot());
    assertEquals("global", config.getScope());
    assertFalse(config.update(update));
    assertFalse(config.update(new StaticConfigDTO()));
  }
}
