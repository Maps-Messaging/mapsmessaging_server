package io.mapsmessaging.config.device.triggers;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.device.triggers.CronTriggerConfigDTO;
import io.mapsmessaging.dto.rest.config.device.triggers.PeriodicTriggerConfigDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CronTriggerConfigTest {

  @Test
  void defaultsAndConfiguredValuesAreLoaded() {
    CronTriggerConfig defaults = new CronTriggerConfig(new ConfigurationProperties());
    assertEquals("cron", defaults.getType());
    assertEquals("", defaults.getName());
    assertEquals("", defaults.getCron());

    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("name", "midnight");
    properties.put("cron", "0 0 * * *");

    CronTriggerConfig configured = new CronTriggerConfig(properties);
    assertEquals("midnight", configured.getName());
    assertEquals("0 0 * * *", configured.getCron());
  }

  @Test
  void configurationRoundTripPreservesValues() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("name", "hourly");
    properties.put("cron", "0 * * * *");

    CronTriggerConfig source = new CronTriggerConfig(properties);
    CronTriggerConfig restored =
        new CronTriggerConfig(source.toConfigurationProperties());

    assertEquals(source.getType(), restored.getType());
    assertEquals(source.getName(), restored.getName());
    assertEquals(source.getCron(), restored.getCron());
  }

  @Test
  void updateReportsChangesAndIgnoresWrongDtoType() {
    CronTriggerConfig config = new CronTriggerConfig(new ConfigurationProperties());
    CronTriggerConfigDTO update = new CronTriggerConfigDTO();
    update.setName("every-five");
    update.setCron("*/5 * * * *");

    assertTrue(config.update(update));
    assertEquals("every-five", config.getName());
    assertEquals("*/5 * * * *", config.getCron());
    assertFalse(config.update(update));
    assertFalse(config.update(new PeriodicTriggerConfigDTO()));
  }
}
