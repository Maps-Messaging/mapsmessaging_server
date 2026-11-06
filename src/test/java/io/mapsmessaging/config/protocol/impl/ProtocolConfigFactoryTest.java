package io.mapsmessaging.config.protocol.impl;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.protocol.impl.MqttConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.MqttVersion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProtocolConfigFactoryTest {

  @Test
  void message_defaults_preserve_literal_keys_and_values_through_round_trip() {
    ConfigurationProperties defaults = new ConfigurationProperties();
    defaults.put("retain", true);
    defaults.put("storeOffline", false);
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("messageDefaults", defaults);

    MqttConfig config = new MqttConfig(properties);
    ConfigurationProperties packed = config.toConfigurationProperties();
    ConfigurationProperties packedDefaults =
        (ConfigurationProperties) packed.get("messageDefaults");

    assertEquals(true, packedDefaults.get("retain"));
    assertEquals(false, packedDefaults.get("storeOffline"));
    MqttConfig restored = new MqttConfig(packed);
    assertEquals(true, restored.getMessageDefaults().getRetain());
    assertEquals(false, restored.getMessageDefaults().getStoreOffline());
  }

  @Test
  void mqttFactoryFieldsRoundTripThroughConfigurationProperties() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("maximumSessionExpiry", 12345L);
    properties.put("maximumBufferSize", "2M");
    properties.put("serverReceiveMaximum", 17);
    properties.put("clientReceiveMaximum", 19);
    properties.put("clientMaximumTopicAlias", 21);
    properties.put("serverMaximumTopicAlias", 23);
    properties.put("strictClientId", true);
    properties.put("mqttVersion", "MQTT_5");

    MqttConfig restored = new MqttConfig(new MqttConfig(properties).toConfigurationProperties());

    assertEquals(12345L, restored.getMaximumSessionExpiry());
    assertEquals(2L * 1024 * 1024, restored.getMaximumBufferSize());
    assertEquals(17, restored.getServerReceiveMaximum());
    assertEquals(19, restored.getClientReceiveMaximum());
    assertEquals(21, restored.getClientMaximumTopicAlias());
    assertEquals(23, restored.getServerMaximumTopicAlias());
    assertTrue(restored.isStrictClientId());
    assertEquals(MqttVersion.MQTT_5, restored.getVersion());
  }

  @Test
  void updateAppliesMqttLimitsAndThenReportsNoFurtherChange() {
    MqttConfig original = new MqttConfig(new ConfigurationProperties());
    MqttConfigDTO updated = new MqttConfigDTO();
    updated.setMaximumSessionExpiry(99);
    updated.setMaximumBufferSize(4096);
    updated.setServerReceiveMaximum(3);
    updated.setClientReceiveMaximum(4);
    updated.setClientMaximumTopicAlias(5);
    updated.setServerMaximumTopicAlias(6);
    updated.setStrictClientId(true);

    assertTrue(ProtocolConfigFactory.update(original, updated));
    assertEquals(99, original.getMaximumSessionExpiry());
    assertEquals(4096, original.getMaximumBufferSize());
    assertTrue(original.isStrictClientId());
    assertFalse(ProtocolConfigFactory.update(original, updated));
  }

  @Test
  void invalidMqttVersionIsRejectedRatherThanSilentlyChanged() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("mqttVersion", "not-a-version");

    assertThrows(IllegalArgumentException.class, () -> new MqttConfig(properties));
  }
}