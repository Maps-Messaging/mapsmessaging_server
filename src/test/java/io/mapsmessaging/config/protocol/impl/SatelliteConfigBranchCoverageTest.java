package io.mapsmessaging.config.protocol.impl;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.SatelliteConfigDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SatelliteConfigBranchCoverageTest {

  @Test
  void pollAndDeviceRefreshIntervalsAreClampedToMinimums() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("incomingMessagePollInterval", 1);
    properties.put("outgoingMessagePollInterval", 2);
    properties.put("deviceInfoUpdateMinutes", 3);

    SatelliteConfig config = new SatelliteConfig(properties);

    assertEquals(10, config.getIncomingMessagePollInterval());
    assertEquals(15, config.getOutgoingMessagePollInterval());
    assertEquals(15, config.getDeviceInfoUpdateMinutes());
  }

  @Test
  void updateAppliesProtocolSpecificFields() {
    SatelliteConfig config = new SatelliteConfig(new ConfigurationProperties());
    SatelliteConfigDTO update = new SatelliteConfigDTO();
    update.setBaseUrl("https://sat.example");
    update.setSendHighPriorityMessages(!config.isSendHighPriorityMessages());
    update.setIncomingMessagePollInterval(config.getIncomingMessagePollInterval() + 1);
    update.setOutgoingMessagePollInterval(config.getOutgoingMessagePollInterval() + 1);
    update.setHttpRequestTimeout(config.getHttpRequestTimeout() + 1);
    update.setMaxInflightEventsPerDevice(config.getMaxInflightEventsPerDevice() + 1);
    update.setOutboundBroadcast("/broadcast");
    update.setMailboxId("mailbox");
    update.setMailboxPassword("secret");
    update.setCommonInboundPublishRoot("/ci");
    update.setCommonOutboundPublishRoot("/co");
    update.setMapsInboundPublishRoot("/mi");
    update.setMapsOutboundPublishRoot("/mo");
    update.setDeviceInfoUpdateMinutes(config.getDeviceInfoUpdateMinutes());
    update.setMaxBufferSize(config.getMaxBufferSize() + 1);
    update.setCompressionCutoffSize(config.getCompressionCutoffSize() + 1);
    update.setMessageLifeTimeInMinutes(config.getMessageLifeTimeInMinutes() + 1);
    update.setSharedSecret("shared");
    update.setSinNumber(config.getSinNumber() + 1);

    assertTrue(config.update(update));
    assertEquals("https://sat.example", config.getBaseUrl());
    assertEquals("mailbox", config.getMailboxId());
    assertEquals("shared", config.getSharedSecret());
  }

  @Test
  void unrelatedDtoIsRejected() {
    assertFalse(new SatelliteConfig(new ConfigurationProperties()).update(new BaseConfigDTO()));
  }
}