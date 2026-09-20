package io.mapsmessaging.config.network.impl;

import io.mapsmessaging.canbus.device.QueueFullPolicy;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.network.impl.CanbusConfigDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CanbusConfigTest {

  @Test
  void configuredQueueSettingsRoundTrip() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("deviceName", "can0");
    properties.put("queuedWritesEnabled", false);
    properties.put("queueDepth", 64);
    properties.put("bitrateBitsPerSecond", 500000);
    properties.put("maxBusUsagePercent", 35.5);
    properties.put("queueFullPolicy", "reject_new");
    properties.put("writeFailureBackoffMilliseconds", 250L);

    CanbusConfig restored = new CanbusConfig(new CanbusConfig(properties).toConfigurationProperties());

    assertEquals("can0", restored.getDeviceName());
    assertFalse(restored.isQueuedWritesEnabled());
    assertEquals(64, restored.getQueueDepth());
    assertEquals(500000, restored.getBitrateBitsPerSecond());
    assertEquals(35.5, restored.getMaxBusUsagePercent(), 0.0);
    assertEquals(QueueFullPolicy.REJECT_NEW, restored.getQueueFullPolicy());
    assertEquals(250L, restored.getWriteFailureBackoffMilliseconds());
  }

  @Test
  void updateAppliesAllQueueControlsAndThenBecomesIdempotent() {
    CanbusConfig config = new CanbusConfig(new ConfigurationProperties());
    CanbusConfigDTO updated = new CanbusConfigDTO();
    updated.setDeviceName("can1");
    updated.setQueuedWritesEnabled(false);
    updated.setQueueDepth(8);
    updated.setBitrateBitsPerSecond(125000);
    updated.setMaxBusUsagePercent(12.5);
    updated.setQueueFullPolicy(QueueFullPolicy.REJECT_NEW);
    updated.setWriteFailureBackoffMilliseconds(999);

    assertTrue(config.update(updated));
    assertEquals("can1", config.getDeviceName());
    assertEquals(8, config.getQueueDepth());
    assertEquals(QueueFullPolicy.REJECT_NEW, config.getQueueFullPolicy());
    assertFalse(config.update(updated));
  }

  @Test
  void invalidQueueFullPolicyIsRejected() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("queueFullPolicy", "throw-it-overboard");

    assertThrows(IllegalArgumentException.class, () -> new CanbusConfig(properties));
  }
}