package io.mapsmessaging.config;

import io.mapsmessaging.configuration.ConfigurationProperties;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DeviceManagerConfigBranchCoverageTest {

  @Test
  void loadsKnownTriggersAndIgnoresUnknownTriggerTypes() throws Exception {
    ConfigurationProperties periodic = new ConfigurationProperties();
    periodic.put("type", "periodic");
    periodic.put("name", "poll");
    periodic.put("period", 1000);

    ConfigurationProperties unknown = new ConfigurationProperties();
    unknown.put("type", "made-up");

    ConfigurationProperties triggers = new ConfigurationProperties();
    triggers.put("name", "triggers");
    triggers.put("config", List.of(periodic, unknown));

    DeviceManagerConfig config = create(root(List.of(triggers)));

    assertEquals(1, config.getTriggers().size());
  }

  @Test
  void loadsSingleI2cConfigAndRecognizesOneWireSpiAndSerialSections() throws Exception {
    ConfigurationProperties i2cBus = new ConfigurationProperties();
    i2cBus.put("bus", 1);
    ConfigurationProperties i2c = new ConfigurationProperties();
    i2c.put("name", "i2c");
    i2c.put("config", i2cBus);

    ConfigurationProperties oneWire = new ConfigurationProperties();
    oneWire.put("name", "oneWire");

    ConfigurationProperties spi = new ConfigurationProperties();
    spi.put("name", "spi");
    spi.put("config", List.of());

    ConfigurationProperties serial = new ConfigurationProperties();
    serial.put("name", "serial");
    serial.put("config", List.of());

    DeviceManagerConfig config = create(root(List.of(i2c, oneWire, spi, serial)));

    assertEquals(1, config.getI2cBuses().size());
    assertNotNull(config.getOneWireBus());
    assertNotNull(config.getSpiBus());
    assertNotNull(config.getSerialDeviceBusConfig());
  }

  @Test
  void nonDeviceManagerDtoIsRejectedByUpdate() {
    DeviceManagerConfig config = new DeviceManagerConfig();

    assertFalse(config.update(new io.mapsmessaging.dto.rest.config.BaseConfigDTO()));
  }

  private static ConfigurationProperties root(Object data) {
    ConfigurationProperties root = new ConfigurationProperties();
    root.put("enabled", true);
    root.put("demoEnabled", false);
    root.put("data", data);
    return root;
  }

  private static DeviceManagerConfig create(ConfigurationProperties properties) throws Exception {
    Constructor<DeviceManagerConfig> ctor =
        DeviceManagerConfig.class.getDeclaredConstructor(ConfigurationProperties.class);
    ctor.setAccessible(true);
    return ctor.newInstance(properties);
  }
}