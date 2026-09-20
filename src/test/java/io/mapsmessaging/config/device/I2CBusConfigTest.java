package io.mapsmessaging.config.device;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.device.I2CBusConfigDTO;
import io.mapsmessaging.dto.rest.config.device.SpiDeviceBusConfigDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class I2CBusConfigTest {

  @Test
  void constructorLoadsDeviceListAndRoundTripsBusConfiguration() {
    ConfigurationProperties device = new ConfigurationProperties();
    device.put("address", "0x44");
    device.put("name", "bme");

    ConfigurationProperties props = new ConfigurationProperties();
    props.put("enabled", true);
    props.put("bus", 1);
    props.put("trigger", "poll");
    props.put("filter", "ON_CHANGE");
    props.put("selector", "temperature > 20");
    props.put("scanTime", 5000);
    props.put("autoScan", true);
    props.put("devices", List.of(device));

    I2CBusConfig source = new I2CBusConfig(props);
    assertEquals(1, source.getDevices().size());
    assertEquals(0x44, source.getDevices().getFirst().getAddress());

    I2CBusConfig restored =
        new I2CBusConfig(source.toConfigurationProperties());

    assertEquals(source.getBus(), restored.getBus());
    assertEquals(source.getTrigger(), restored.getTrigger());
    assertEquals(source.getScanTime(), restored.getScanTime());
    assertEquals(source.getDevices(), restored.getDevices());
  }

  @Test
  void updateAppliesBusAndDeviceChangesAndThenBecomesStable() {
    I2CBusConfig config = new I2CBusConfig(new ConfigurationProperties());

    I2CBusConfigDTO update = new I2CBusConfigDTO();
    update.setEnabled(true);
    update.setTopicNameTemplate("/i2c");
    update.setAutoScan(true);
    update.setScanTime(250);
    update.setFilter("ALL");
    update.setSelector("x = 1");
    update.setBus(2);
    update.setTrigger("irq");

    I2CDeviceConfig device = new I2CDeviceConfig();
    device.setAddress(7);
    device.setName("sensor");
    device.setSelector("");
    update.setDevices(List.of(device));

    assertTrue(config.update(update));
    assertEquals(2, config.getBus());
    assertEquals("irq", config.getTrigger());
    assertEquals(List.of(device), config.getDevices());
    assertFalse(config.update(update));
    assertFalse(config.update(new SpiDeviceBusConfigDTO()));
  }
}
