package io.mapsmessaging.config.protocol.impl;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.MavlinkConfigDTO;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MavlinkConfigFinalCoverageTest {

  @Test
  void unsupportedAndBlankMessageIdRepresentationsProduceEmptyLists() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("acceptedMessageIds", 42);
    properties.put("rejectedMessageIds", "   ");

    MavlinkConfig config = new MavlinkConfig(properties);

    assertTrue(config.getAcceptedMessageIds().isEmpty());
    assertTrue(config.getRejectedMessageIds().isEmpty());
  }

  @Test
  void mapAcceptedSourceDefaultsMissingIdentityValuesAndParsesMessageLists() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put(
        "acceptedSources",
        List.of(
            Map.of(
                "acceptedMessageIds", "1,2",
                "rejectedMessageIds", "3,4")));

    MavlinkConfig config = new MavlinkConfig(properties);

    assertEquals(1, config.getAcceptedSources().size());
    assertEquals(0, config.getAcceptedSources().getFirst().getSystemId());
    assertEquals(0, config.getAcceptedSources().getFirst().getComponentId());
    assertEquals(List.of(1, 2), config.getAcceptedSources().getFirst().getAcceptedMessageIds());
    assertEquals(List.of(3, 4), config.getAcceptedSources().getFirst().getRejectedMessageIds());
  }

  @Test
  void acceptedSourcesRoundTripThroughConfigurationProperties() {
    ConfigurationProperties source = new ConfigurationProperties();
    source.put("systemId", 12);
    source.put("componentId", 34);
    source.put("acceptedMessageIds", "1,2");
    source.put("rejectedMessageIds", "9");

    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("acceptedSources", List.of(source));
    properties.put("acceptedMessageIds", "10");
    properties.put("rejectedMessageIds", "11");

    MavlinkConfig restored =
        new MavlinkConfig(new MavlinkConfig(properties).toConfigurationProperties());

    assertEquals(List.of(10), restored.getAcceptedMessageIds());
    assertEquals(List.of(11), restored.getRejectedMessageIds());
    assertEquals(12, restored.getAcceptedSources().getFirst().getSystemId());
    assertEquals(34, restored.getAcceptedSources().getFirst().getComponentId());
  }

  @Test
  void updateConvertsNullCollectionsToEmptyCopiesAndRejectsUnrelatedDto() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("acceptedMessageIds", List.of(1));
    properties.put("rejectedMessageIds", List.of(2));
    MavlinkConfig config = new MavlinkConfig(properties);

    MavlinkConfigDTO update = new MavlinkConfigDTO();
    update.setAcceptedMessageIds(null);
    update.setRejectedMessageIds(null);
    update.setAcceptedSources(null);

    assertTrue(config.update(update));
    assertNotNull(config.getAcceptedMessageIds());
    assertNotNull(config.getRejectedMessageIds());
    assertNotNull(config.getAcceptedSources());
    assertTrue(config.getAcceptedMessageIds().isEmpty());
    assertTrue(config.getRejectedMessageIds().isEmpty());
    assertTrue(config.getAcceptedSources().isEmpty());
    assertFalse(config.update(new BaseConfigDTO()));
  }
}