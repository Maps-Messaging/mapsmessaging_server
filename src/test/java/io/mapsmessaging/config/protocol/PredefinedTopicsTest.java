package io.mapsmessaging.config.protocol;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.destination.FormatConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.PredefinedTopicsDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PredefinedTopicsTest {

  @Test
  void defaultsAndConfiguredValuesRoundTrip() {
    PredefinedTopics defaults = new PredefinedTopics(new ConfigurationProperties());
    assertEquals(0, defaults.getId());
    assertEquals("", defaults.getTopic());
    assertEquals("*", defaults.getAddress());

    ConfigurationProperties props = new ConfigurationProperties();
    props.put("id", 7);
    props.put("topic", "sensor/temperature");
    props.put("address", "device-1");

    PredefinedTopics source = new PredefinedTopics(props);
    PredefinedTopics restored =
        new PredefinedTopics(source.toConfigurationProperties());

    assertEquals(source.getId(), restored.getId());
    assertEquals(source.getTopic(), restored.getTopic());
    assertEquals(source.getAddress(), restored.getAddress());
  }

  @Test
  void updateAppliesTopicIdentityChangesOnce() {
    PredefinedTopics config = new PredefinedTopics(new ConfigurationProperties());
    PredefinedTopicsDTO update = new PredefinedTopicsDTO();
    update.setId(9);
    update.setTopic("new/topic");
    update.setAddress("node-a");

    assertTrue(config.update(update));
    assertEquals(9, config.getId());
    assertEquals("new/topic", config.getTopic());
    assertEquals("node-a", config.getAddress());
    assertFalse(config.update(update));
    assertFalse(config.update(new FormatConfigDTO()));
  }
}
