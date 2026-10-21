package io.mapsmessaging.config.protocol.impl;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.SatelliteConfigDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SatelliteConfigFinalCoverageTest {

  @Test
  void exactMinimumIntervalsAreNotClamped() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("incomingMessagePollInterval", 10);
    properties.put("outgoingMessagePollInterval", 15);
    properties.put("deviceInfoUpdateMinutes", 10);

    SatelliteConfig config = new SatelliteConfig(properties);

    assertEquals(10, config.getIncomingMessagePollInterval());
    assertEquals(15, config.getOutgoingMessagePollInterval());
    assertEquals(10, config.getDeviceInfoUpdateMinutes());
  }

  @Test
  void protocolSpecificConfigurationRoundTrips() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("baseUrl", "https://sat.example");
    properties.put("incomingMessagePollInterval", 20);
    properties.put("outgoingMessagePollInterval", 25);
    properties.put("httpRequestTimeoutSec", 30);
    properties.put("maxInflightEventsPerDevice", 4);
    properties.put("mailboxId", "box");
    properties.put("mailboxPassword", "pass");
    properties.put("deviceInfoUpdateMinutes", 20);
    properties.put("maxBufferSize", 2048);
    properties.put("compressionCutoffSize", 512);
    properties.put("messageLifeTimeInMinutes", 60);
    properties.put("sharedSecret", "secret");
    properties.put("sendHighPriorityMessages", true);
    properties.put("sinNumber", 99);
    properties.put("outboundBroadcast", "/broadcast");
    properties.put("commonInboundPublishRoot", "/ci");
    properties.put("commonOutboundPublishRoot", "/co");
    properties.put("mapsInboundPublishRoot", "/mi");
    properties.put("mapsOutboundPublishRoot", "/mo");

    SatelliteConfig restored =
        new SatelliteConfig(new SatelliteConfig(properties).toConfigurationProperties());

    assertEquals("https://sat.example", restored.getBaseUrl());
    assertEquals("box", restored.getMailboxId());
    assertEquals(2048, restored.getMaxBufferSize());
    assertEquals("secret", restored.getSharedSecret());
    assertEquals("/mo", restored.getMapsOutboundPublishRoot());
  }

  @Test
  void changingOnlyDeviceInfoIntervalMustBeReportedAsAConfigurationChange() {
    SatelliteConfig config = new SatelliteConfig(new ConfigurationProperties());
    SatelliteConfigDTO update = matching(config);
    update.setDeviceInfoUpdateMinutes(config.getDeviceInfoUpdateMinutes() + 1);

    assertTrue(
        config.update(update),
        "Changing deviceInfoUpdateMinutes must be reported as a configuration change");
    assertEquals(
        update.getDeviceInfoUpdateMinutes(),
        config.getDeviceInfoUpdateMinutes());
  }

  @Test
  void unrelatedDtoDoesNotChangeSatelliteConfiguration() {
    SatelliteConfig config = new SatelliteConfig(new ConfigurationProperties());

    assertFalse(config.update(new BaseConfigDTO()));
  }

  private static SatelliteConfigDTO matching(SatelliteConfig config) {
    SatelliteConfigDTO dto = new SatelliteConfigDTO();
    dto.setBaseUrl(config.getBaseUrl());
    dto.setSendHighPriorityMessages(config.isSendHighPriorityMessages());
    dto.setIncomingMessagePollInterval(config.getIncomingMessagePollInterval());
    dto.setOutgoingMessagePollInterval(config.getOutgoingMessagePollInterval());
    dto.setHttpRequestTimeout(config.getHttpRequestTimeout());
    dto.setMaxInflightEventsPerDevice(config.getMaxInflightEventsPerDevice());
    dto.setOutboundBroadcast(config.getOutboundBroadcast());
    dto.setMailboxId(config.getMailboxId());
    dto.setMailboxPassword(config.getMailboxPassword());
    dto.setCommonInboundPublishRoot(config.getCommonInboundPublishRoot());
    dto.setCommonOutboundPublishRoot(config.getCommonOutboundPublishRoot());
    dto.setMapsInboundPublishRoot(config.getMapsInboundPublishRoot());
    dto.setMapsOutboundPublishRoot(config.getMapsOutboundPublishRoot());
    dto.setDeviceInfoUpdateMinutes(config.getDeviceInfoUpdateMinutes());
    dto.setMaxBufferSize(config.getMaxBufferSize());
    dto.setCompressionCutoffSize(config.getCompressionCutoffSize());
    dto.setMessageLifeTimeInMinutes(config.getMessageLifeTimeInMinutes());
    dto.setSharedSecret(config.getSharedSecret());
    dto.setSinNumber(config.getSinNumber());
    return dto;
  }
}