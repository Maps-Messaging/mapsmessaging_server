package io.mapsmessaging.config.device;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.device.I2CDeviceConfigDTO;
import io.mapsmessaging.dto.rest.config.device.SpiDeviceConfigDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpiDeviceConfigTest {

  @Test
  void constructorAndRoundTripPreserveStructuredSpiFields() {
    ConfigurationProperties props = new ConfigurationProperties();
    props.put("address", 3);
    props.put("name", "adc");
    props.put("selector", "value > 10");
    props.put("spiBus", 1);
    props.put("spiMode", 2);
    props.put("spiChipSelect", 4);

    SpiDeviceConfig source = new SpiDeviceConfig(props);
    SpiDeviceConfig restored =
        new SpiDeviceConfig(source.toConfigurationProperties());

    assertEquals(source.getAddress(), restored.getAddress());
    assertEquals(source.getName(), restored.getName());
    assertEquals(source.getSelector(), restored.getSelector());
    assertEquals(source.getSpiBus(), restored.getSpiBus());
    assertEquals(source.getSpiMode(), restored.getSpiMode());
    assertEquals(source.getSpiChipSelect(), restored.getSpiChipSelect());
  }

  @Test
  void updateAppliesChangedSpiFieldsAndReportsStability() {
    SpiDeviceConfig config = new SpiDeviceConfig(new ConfigurationProperties());

    SpiDeviceConfigDTO update = new SpiDeviceConfigDTO();
    update.setAddress(9);
    update.setName("device");
    update.setSelector("ok = true");
    update.setSpiBus(2);
    update.setSpiMode(3);
    update.setSpiChipSelect(1);

    assertTrue(config.update(update));
    assertEquals(9, config.getAddress());
    assertEquals("device", config.getName());
    assertEquals(2, config.getSpiBus());
    assertFalse(config.update(update));
    assertFalse(config.update(new I2CDeviceConfigDTO()));
  }
}
