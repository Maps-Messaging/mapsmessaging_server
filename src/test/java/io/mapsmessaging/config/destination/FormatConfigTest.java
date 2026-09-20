package io.mapsmessaging.config.destination;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.destination.FormatConfigDTO;
import io.mapsmessaging.dto.rest.config.rest.StaticConfigDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FormatConfigTest {

  @Test
  void defaultsToJsonAndRoundTripsConfiguredFormat() {
    assertEquals("json", new FormatConfig(new ConfigurationProperties()).getName());

    ConfigurationProperties props = new ConfigurationProperties();
    props.put("name", "protobuf");
    FormatConfig source = new FormatConfig(props);
    FormatConfig restored = new FormatConfig(source.toConfigurationProperties());

    assertEquals("protobuf", restored.getName());
  }

  @Test
  void updateReportsActualFormatChanges() {
    FormatConfig config = new FormatConfig(new ConfigurationProperties());
    FormatConfigDTO update = new FormatConfigDTO();
    update.setName("cbor");

    assertTrue(config.update(update));
    assertEquals("cbor", config.getName());
    assertFalse(config.update(update));
    assertFalse(config.update(new StaticConfigDTO()));
  }
}
