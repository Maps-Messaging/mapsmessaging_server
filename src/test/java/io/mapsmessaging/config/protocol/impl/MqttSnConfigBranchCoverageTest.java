package io.mapsmessaging.config.protocol.impl;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.MqttSnConfigDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MqttSnConfigBranchCoverageTest {

  @Test
  void emptyPredefinedTopicListExercisesListConfigurationPath() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("preDefinedTopics", List.of());

    MqttSnConfig config = new MqttSnConfig(properties);

    assertNotNull(config.getPredefinedTopicsList());
    assertTrue(config.getPredefinedTopicsList().isEmpty());
  }

  @Test
  void updateAppliesMqttSnSpecificFieldsAndThenIsIdempotent() {
    MqttSnConfig config = new MqttSnConfig(new ConfigurationProperties());
    MqttSnConfigDTO update = new MqttSnConfigDTO();
    update.setGatewayId("gw");
    update.setReceiveMaximum(config.getReceiveMaximum() + 1);
    update.setIdleSessionTimeout(config.getIdleSessionTimeout() + 1);
    update.setMaximumSessionExpiry(config.getMaximumSessionExpiry() + 1);
    update.setEnablePortChanges(!config.isEnablePortChanges());
    update.setEnableAddressChanges(!config.isEnableAddressChanges());
    update.setAdvertiseGateway(!config.isAdvertiseGateway());
    update.setRegisteredTopics("registered");
    update.setAdvertiseInterval(config.getAdvertiseInterval() + 1);
    update.setMaxRegisteredSize(config.getMaxRegisteredSize() + 1);
    update.setMaxInFlightEvents(config.getMaxInFlightEvents() + 1);
    update.setDropQoS0(!config.isDropQoS0());
    update.setEventQueueTimeout(config.getEventQueueTimeout() + 1);
    update.setPredefinedTopicsList(List.of());

    assertTrue(config.update(update));
    assertEquals("gw", config.getGatewayId());
    assertEquals("registered", config.getRegisteredTopics());
    assertFalse(config.update(update));
  }

  @Test
  void unrelatedDtoIsRejected() {
    assertFalse(new MqttSnConfig(new ConfigurationProperties()).update(new BaseConfigDTO()));
  }
}