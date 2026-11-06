package io.mapsmessaging.config.device;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.device.OneWireBusConfigDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OneWireBusConfigTest {

  @Test
  void configurationRoundTripsIncludingTrigger() {
    ConfigurationProperties props = new ConfigurationProperties();
    props.put("name", "onewire0");
    props.put("trigger", "temperature");
    props.put("enabled", true);
    props.put("autoScan", true);
    props.put("scanTime", 2000);
    props.put("filter", "ON_CHANGE");
    props.put("selector", "value > 1");

    OneWireBusConfig source = new OneWireBusConfig(props);
    OneWireBusConfig restored =
        new OneWireBusConfig(source.toConfigurationProperties());

    assertEquals(source.getName(), restored.getName());
    assertEquals(source.getTrigger(), restored.getTrigger());
    assertEquals(source.isEnabled(), restored.isEnabled());
    assertEquals(source.getSelector(), restored.getSelector());
  }

  @Test
  void updateAppliesTriggerAndOperationalFields() {
    OneWireBusConfig config =
        new OneWireBusConfig(new ConfigurationProperties());

    OneWireBusConfigDTO update = new OneWireBusConfigDTO();
    update.setName("bus");
    update.setTrigger("irq");
    update.setEnabled(true);
    update.setAutoScan(true);
    update.setTopicNameTemplate("/onewire");
    update.setScanTime(500);
    update.setFilter("ALL");
    update.setSelector("x = 1");

    assertTrue(config.update(update));
    assertEquals("irq", config.getTrigger());
    assertEquals("/onewire", config.getTopicNameTemplate());
    assertFalse(config.update(update));
  }
}
