package io.mapsmessaging.config.device;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.device.OneWireBusConfigDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OneWireBusConfigBranchCoverageTest {

  @Test
  void nullFilterAndSelectorAreOmittedWhenSerialized() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("name", "ow");
    OneWireBusConfig config = new OneWireBusConfig(properties);
    config.setFilter(null);
    config.setSelector(null);

    ConfigurationProperties packed = config.toConfigurationProperties();

    assertFalse(packed.containsKey("filter"));
    assertFalse(packed.containsKey("selector"));
  }

  @Test
  void updateHandlesNullCurrentStringsAndAllOperationalFields() {
    OneWireBusConfig config = new OneWireBusConfig(new ConfigurationProperties());
    config.setTopicNameTemplate(null);
    config.setFilter(null);
    config.setSelector(null);
    config.setTrigger(null);

    OneWireBusConfigDTO update = new OneWireBusConfigDTO();
    update.setName("ow");
    update.setTrigger("ready");
    update.setEnabled(true);
    update.setAutoScan(true);
    update.setTopicNameTemplate("/ow");
    update.setScanTime(42);
    update.setFilter("ALL");
    update.setSelector("x > 0");

    assertTrue(config.update(update));
    assertEquals("ready", config.getTrigger());
    assertEquals("/ow", config.getTopicNameTemplate());
    assertEquals("ALL", config.getFilter());
    assertEquals("x > 0", config.getSelector());
  }

  @Test
  void unrelatedDtoIsRejected() {
    assertFalse(new OneWireBusConfig(new ConfigurationProperties()).update(new BaseConfigDTO()));
  }
}