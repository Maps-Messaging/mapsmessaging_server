package io.mapsmessaging.config.device;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.device.SpiDeviceBusConfigDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SpiDeviceBusConfigTest {

  @Test
  void triggerRoundTripsWithBusConfiguration() {
    ConfigurationProperties props = new ConfigurationProperties();
    props.put("name", "spi0");
    props.put("trigger", "ready");
    props.put("enabled", true);
    props.put("autoScan", false);
    props.put("scanTime", 1000);
    props.put("filter", "ALL");
    props.put("selector", "");
    props.put("config", List.of());

    SpiDeviceBusConfig source = new SpiDeviceBusConfig(props);
    SpiDeviceBusConfig restored =
        new SpiDeviceBusConfig(source.toConfigurationProperties());

    assertEquals("ready", source.getTrigger());
    assertEquals(source.getTrigger(), restored.getTrigger());
  }

  @Test
  void updateMustApplyChangedTrigger() {
    ConfigurationProperties props = new ConfigurationProperties();
    props.put("name", "spi0");
    props.put("trigger", "old");
    props.put("enabled", true);
    props.put("autoScan", false);
    props.put("scanTime", 1000);
    props.put("filter", "");
    props.put("selector", "");
    props.put("config", List.of());

    SpiDeviceBusConfig config = new SpiDeviceBusConfig(props);

    SpiDeviceBusConfigDTO update = new SpiDeviceBusConfigDTO();
    update.setName("spi0");
    update.setTrigger("new");
    update.setEnabled(true);
    update.setAutoScan(false);
    update.setTopicNameTemplate("");
    update.setScanTime(1000);
    update.setFilter("");
    update.setSelector("");
    update.setDevices(List.of());

    assertTrue(
        config.update(update),
        "Changing trigger is a configuration change and must be reported"
    );
    assertEquals(
        "new",
        config.getTrigger(),
        "Changed trigger must be applied to the live configuration"
    );
  }
}
