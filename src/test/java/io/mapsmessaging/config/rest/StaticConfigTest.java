package io.mapsmessaging.config.rest;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.destination.FormatConfigDTO;
import io.mapsmessaging.dto.rest.config.rest.StaticConfigDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StaticConfigTest {

  @Test
  void defaultsRespectDirectoryTranslationAndRoundTrip() {
    StaticConfig config = new StaticConfig(new ConfigurationProperties());

    assertTrue(config.isEnabled());
    String expectedDirectory =
        new ConfigurationProperties()
            .getProperty("directory", "{{MAPS_HOME}}/www");
    assertEquals(expectedDirectory, config.getDirectory());

    StaticConfig restored = new StaticConfig(config.toConfigurationProperties());
    assertEquals(config.isEnabled(), restored.isEnabled());
    assertEquals(config.getDirectory(), restored.getDirectory());
  }

  @Test
  void updateAppliesEnableAndDirectoryChangesOnlyOnce() {
    StaticConfig config = new StaticConfig(new ConfigurationProperties());
    StaticConfigDTO update = new StaticConfigDTO();
    update.setEnabled(false);
    update.setDirectory("/srv/maps/www");

    assertTrue(config.update(update));
    assertFalse(config.isEnabled());
    assertEquals("/srv/maps/www", config.getDirectory());
    assertFalse(config.update(update));
    assertFalse(config.update(new FormatConfigDTO()));
  }
}
