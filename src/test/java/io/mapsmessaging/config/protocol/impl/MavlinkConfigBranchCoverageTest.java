package io.mapsmessaging.config.protocol.impl;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.protocol.impl.MavlinkAcceptedSourceDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.MavlinkConfigDTO;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MavlinkConfigBranchCoverageTest {

  @Test
  void parsesOptionalIdentityListsAndMixedAcceptedSourceRepresentations() {
    ConfigurationProperties sourceProps = new ConfigurationProperties();
    sourceProps.put("systemId", "4");
    sourceProps.put("componentId", 5L);
    sourceProps.put("acceptedMessageIds", "1, 2, 3");
    sourceProps.put("rejectedMessageIds", List.of(9, "10"));

    MavlinkAcceptedSourceDTO dtoSource = new MavlinkAcceptedSourceDTO();
    dtoSource.setSystemId(6);
    dtoSource.setComponentId(7);
    dtoSource.setAcceptedMessageIds(List.of(11));
    dtoSource.setRejectedMessageIds(List.of(12));

    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("systemId", "42");
    properties.put("componentId", 24L);
    properties.put("acceptedMessageIds", "1, 2, ,3");
    properties.put("rejectedMessageIds", List.of(4, "5", 6L));
    properties.put(
        "acceptedSources",
        List.of(
            sourceProps,
            Map.of(
                "systemId", "8",
                "componentId", 9,
                "acceptedMessageIds", "13,14",
                "rejectedMessageIds", List.of(15)),
            dtoSource,
            "ignored"));

    MavlinkConfig config = new MavlinkConfig(properties);

    assertEquals(42, config.getSystemId());
    assertEquals(24, config.getComponentId());
    assertEquals(List.of(1, 2, 3), config.getAcceptedMessageIds());
    assertEquals(List.of(4, 5, 6), config.getRejectedMessageIds());
    assertEquals(3, config.getAcceptedSources().size());
    assertEquals(List.of(1, 2, 3), config.getAcceptedSources().get(0).getAcceptedMessageIds());
    assertEquals(8, config.getAcceptedSources().get(1).getSystemId());
    assertEquals(6, config.getAcceptedSources().get(2).getSystemId());
  }

  @Test
  void updateCopiesMutableListsInsteadOfAliasingCallerDto() {
    MavlinkConfig config = new MavlinkConfig(new ConfigurationProperties());
    MavlinkConfigDTO update = new MavlinkConfigDTO();
    update.setAcceptedMessageIds(new java.util.ArrayList<>(List.of(20)));
    update.setRejectedMessageIds(new java.util.ArrayList<>(List.of(21)));
    MavlinkAcceptedSourceDTO source = new MavlinkAcceptedSourceDTO();
    source.setSystemId(1);
    source.setComponentId(2);
    source.setAcceptedMessageIds(new java.util.ArrayList<>(List.of(22)));
    source.setRejectedMessageIds(new java.util.ArrayList<>(List.of(23)));
    update.setAcceptedSources(new java.util.ArrayList<>(List.of(source)));

    assertTrue(config.update(update));

    update.getAcceptedMessageIds().add(99);
    update.getRejectedMessageIds().add(98);
    source.getAcceptedMessageIds().add(97);

    assertEquals(List.of(20), config.getAcceptedMessageIds());
    assertEquals(List.of(21), config.getRejectedMessageIds());
    assertEquals(List.of(22), config.getAcceptedSources().getFirst().getAcceptedMessageIds());
  }

  @Test
  void nullOptionalValuesAreNotSerialized() {
    MavlinkConfig config = new MavlinkConfig(new ConfigurationProperties());
    config.setSystemId(null);
    config.setComponentId(null);
    config.setTlogDirectory(null);

    ConfigurationProperties packed = config.toConfigurationProperties();

    assertFalse(packed.containsKey("systemId"));
    assertFalse(packed.containsKey("componentId"));
    assertFalse(packed.containsKey("tlogDirectory"));
  }
}