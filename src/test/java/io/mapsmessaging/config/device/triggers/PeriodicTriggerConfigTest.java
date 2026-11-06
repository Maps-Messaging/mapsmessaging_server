package io.mapsmessaging.config.device.triggers;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.device.triggers.CronTriggerConfigDTO;
import io.mapsmessaging.dto.rest.config.device.triggers.PeriodicTriggerConfigDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PeriodicTriggerConfigTest {

  @Test
  void defaultsAndConfiguredValuesAreLoaded() {
    PeriodicTriggerConfig defaults =
        new PeriodicTriggerConfig(new ConfigurationProperties());
    assertEquals("periodic", defaults.getType());
    assertEquals("", defaults.getName());
    assertEquals(0, defaults.getInterval());

    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("name", "poller");
    properties.put("interval", 5000);

    PeriodicTriggerConfig configured = new PeriodicTriggerConfig(properties);
    assertEquals("poller", configured.getName());
    assertEquals(5000, configured.getInterval());
  }

  @Test
  void configurationRoundTripPreservesValues() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("name", "poller");
    properties.put("interval", 250);

    PeriodicTriggerConfig source = new PeriodicTriggerConfig(properties);
    PeriodicTriggerConfig restored =
        new PeriodicTriggerConfig(source.toConfigurationProperties());

    assertEquals(source.getType(), restored.getType());
    assertEquals(source.getName(), restored.getName());
    assertEquals(source.getInterval(), restored.getInterval());
  }

  @Test
  void updateReportsChangesAndRejectsWrongDtoType() {
    PeriodicTriggerConfig config =
        new PeriodicTriggerConfig(new ConfigurationProperties());

    PeriodicTriggerConfigDTO update = new PeriodicTriggerConfigDTO();
    update.setName("fast");
    update.setInterval(100);
    update.setType("periodic");

    assertTrue(config.update(update));
    assertEquals("fast", config.getName());
    assertEquals(100, config.getInterval());
    assertFalse(config.update(update));
    assertFalse(config.update(new CronTriggerConfigDTO()));
  }
}
