package io.mapsmessaging.config.device;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.device.SpiDeviceBusConfigDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SpiDeviceBusConfigBranchCoverageTest {

  @Test
  void singleDeviceConfigurationIsAcceptedAsObjectNotOnlyList() {
    ConfigurationProperties device = new ConfigurationProperties();
    device.put("name", "adc");

    ConfigurationProperties properties = base();
    properties.put("config", device);

    SpiDeviceBusConfig config = new SpiDeviceBusConfig(properties);

    assertEquals(1, config.getDevices().size());
  }

  @Test
  void updateAppliesScalarFieldsAndListSizeChange() {
    SpiDeviceBusConfig config = new SpiDeviceBusConfig(base());
    SpiDeviceBusConfigDTO update = new SpiDeviceBusConfigDTO();
    update.setName("spi-new");
    update.setAutoScan(true);
    update.setEnabled(false);
    update.setTopicNameTemplate("/spi");
    update.setScanTime(250);
    update.setFilter("ON_CHANGE");
    update.setSelector("x > 0");
    update.setDevices(List.of());

    assertTrue(config.update(update));
    assertEquals("spi-new", config.getName());
    assertFalse(config.isEnabled());
    assertEquals("/spi", config.getTopicNameTemplate());
    assertEquals(250, config.getScanTime());
  }

  @Test
  void unrelatedDtoIsRejected() {
    assertFalse(new SpiDeviceBusConfig(base()).update(new BaseConfigDTO()));
  }

  private static ConfigurationProperties base() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("name", "spi0");
    properties.put("autoScan", false);
    properties.put("enabled", true);
    properties.put("topicNameTemplate", "");
    properties.put("scanTime", 1000);
    properties.put("filter", "");
    properties.put("selector", "");
    properties.put("trigger", "");
    properties.put("config", List.of());
    return properties;
  }
}