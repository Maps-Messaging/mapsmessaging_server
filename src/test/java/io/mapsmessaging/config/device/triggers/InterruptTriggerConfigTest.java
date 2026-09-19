package io.mapsmessaging.config.device.triggers;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.protocol.impl.NmeaConfigDTO;
import io.mapsmessaging.dto.rest.config.device.triggers.InterruptTriggerConfigDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InterruptTriggerConfigTest {

  @Test
  void defaultsAndConfiguredValuesAreLoaded() {
    InterruptTriggerConfig defaults = new InterruptTriggerConfig(new ConfigurationProperties());
    assertEquals("interrupt", defaults.getType());
    assertEquals(0, defaults.getAddress());
    assertEquals("UP", defaults.getPullDirection());
    assertEquals("", defaults.getId());
    assertEquals("", defaults.getName());

    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("address", 17);
    properties.put("pull", "DOWN");
    properties.put("id", "irq-17");
    properties.put("name", "PPS");

    InterruptTriggerConfig configured = new InterruptTriggerConfig(properties);
    assertEquals(17, configured.getAddress());
    assertEquals("DOWN", configured.getPullDirection());
    assertEquals("irq-17", configured.getId());
    assertEquals("PPS", configured.getName());
  }

  @Test
  void configurationRoundTripPreservesTriggerValues() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("address", 3);
    properties.put("pull", "DOWN");
    properties.put("id", "trigger-3");
    properties.put("name", "ready");

    InterruptTriggerConfig source = new InterruptTriggerConfig(properties);
    InterruptTriggerConfig restored =
        new InterruptTriggerConfig(source.toConfigurationProperties());

    assertEquals(source.getType(), restored.getType());
    assertEquals(source.getAddress(), restored.getAddress());
    assertEquals(source.getPullDirection(), restored.getPullDirection());
    assertEquals(source.getId(), restored.getId());
    assertEquals(source.getName(), restored.getName());
  }

  @Test
  void updateReportsActualChangesOnlyAndRejectsWrongDtoType() {
    InterruptTriggerConfig config = new InterruptTriggerConfig(new ConfigurationProperties());

    InterruptTriggerConfigDTO update = new InterruptTriggerConfigDTO();
    update.setAddress(8);
    update.setPullDirection("DOWN");
    update.setId("irq");
    update.setName("interrupt");
    update.setType("interrupt");

    assertTrue(config.update(update));
    assertEquals(8, config.getAddress());
    assertEquals("DOWN", config.getPullDirection());
    assertEquals("irq", config.getId());
    assertEquals("interrupt", config.getName());

    assertFalse(config.update(update));
    assertFalse(config.update(new NmeaConfigDTO()));
  }
}
