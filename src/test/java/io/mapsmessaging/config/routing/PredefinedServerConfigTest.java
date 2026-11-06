package io.mapsmessaging.config.routing;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.destination.FormatConfigDTO;
import io.mapsmessaging.dto.rest.config.routing.PredefinedServerConfigDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PredefinedServerConfigTest {

  @Test
  void configurationRoundTripsNameAndUrl() {
    ConfigurationProperties props = new ConfigurationProperties();
    props.put("name", "edge-a");
    props.put("url", "tcp://edge-a:1883/");

    PredefinedServerConfig source = new PredefinedServerConfig(props);
    PredefinedServerConfig restored =
        new PredefinedServerConfig(source.toConfigurationProperties());

    assertEquals(source.getName(), restored.getName());
    assertEquals(source.getUrl(), restored.getUrl());
  }

  @Test
  void updateAppliesChangesAndRejectsWrongDto() {
    PredefinedServerConfig config =
        new PredefinedServerConfig(new ConfigurationProperties());

    PredefinedServerConfigDTO update = new PredefinedServerConfigDTO();
    update.setName("edge-b");
    update.setUrl("mqtt://edge-b:1883/");

    assertTrue(config.update(update));
    assertEquals("edge-b", config.getName());
    assertEquals("mqtt://edge-b:1883/", config.getUrl());
    assertFalse(config.update(update));
    assertFalse(config.update(new FormatConfigDTO()));
  }
}
