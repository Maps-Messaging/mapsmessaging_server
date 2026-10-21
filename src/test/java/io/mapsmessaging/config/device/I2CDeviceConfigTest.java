package io.mapsmessaging.config.device;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.device.I2CDeviceConfigDTO;
import io.mapsmessaging.dto.rest.config.device.SpiDeviceConfigDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class I2CDeviceConfigTest {

  @Test
  void parsesDecimalFloatStyleAndHexAddresses() {
    ConfigurationProperties decimal = new ConfigurationProperties();
    decimal.put("address", "42.0");
    decimal.put("name", "decimal");

    ConfigurationProperties hex = new ConfigurationProperties();
    hex.put("address", "0x2A");
    hex.put("name", "hex");

    assertEquals(42, new I2CDeviceConfig(decimal).getAddress());
    assertEquals(42, new I2CDeviceConfig(hex).getAddress());
  }

  @Test
  void roundTripPreservesAddressNameAndSelector() {
    ConfigurationProperties props = new ConfigurationProperties();
    props.put("address", "0x2A");
    props.put("name", "sensor");
    props.put("selector", "temperature > 30");

    I2CDeviceConfig source = new I2CDeviceConfig(props);
    I2CDeviceConfig restored =
        new I2CDeviceConfig(source.toConfigurationProperties());

    assertEquals(source.getAddress(), restored.getAddress());
    assertEquals(source.getName(), restored.getName());
    assertEquals(source.getSelector(), restored.getSelector());
  }

  @Test
  void updateReportsRealChangesOnly() {
    I2CDeviceConfig config = new I2CDeviceConfig();
    config.setAddress(1);
    config.setName("old");
    config.setSelector("");

    I2CDeviceConfigDTO changed = new I2CDeviceConfigDTO();
    changed.setAddress(2);
    changed.setName("new");
    changed.setSelector("x = 1");

    assertTrue(config.update(changed));
    assertEquals(2, config.getAddress());
    assertEquals("new", config.getName());
    assertEquals("x = 1", config.getSelector());
    assertFalse(config.update(changed));
    assertFalse(config.update(new SpiDeviceConfigDTO()));
  }
}
