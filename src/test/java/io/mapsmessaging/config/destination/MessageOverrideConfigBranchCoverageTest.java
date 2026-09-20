package io.mapsmessaging.config.destination;

import io.mapsmessaging.api.features.Priority;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.destination.MessageOverrideDTO;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MessageOverrideConfigBranchCoverageTest {

  @Test
  void legacyQualityOfServiceAndOptionalBooleanAbsenceAreParsed() {
    ConfigurationProperties props = new ConfigurationProperties();
    props.put("expiry", 100L);
    props.put("priority", Priority.HIGH.name());
    props.put("qualityOfService", QualityOfService.AT_LEAST_ONCE.name());

    MessageOverrideConfig config = new MessageOverrideConfig(props);

    assertEquals(100L, config.getExpiry());
    assertEquals(Priority.HIGH, config.getPriority());
    assertEquals(QualityOfService.AT_LEAST_ONCE, config.getQualityOfService());
    assertNull(config.getRetain());
    assertNull(config.getStoreOffline());
  }

  @Test
  void metaValuesAreConvertedToStringsAndDataMapIsRetained() {
    ConfigurationProperties meta = new ConfigurationProperties();
    meta.put("number", 42);
    meta.put("ignored", null);
    ConfigurationProperties data = new ConfigurationProperties();
    data.put("value", 7);

    ConfigurationProperties props = new ConfigurationProperties();
    props.put("meta", meta);
    props.put("dataMap", data);

    MessageOverrideConfig config = new MessageOverrideConfig(props);

    assertEquals("42", config.getMeta().get("number"));
    assertFalse(config.getMeta().containsKey("ignored"));
    assertEquals(7, config.getDataMap().get("value"));
  }

  @Test
  void serializationOmitsUnsetFieldsAndUpdateAppliesNullableOverrides() {
    MessageOverrideConfig config = new MessageOverrideConfig(new ConfigurationProperties());
    ConfigurationProperties packed = config.toConfigurationProperties();
    assertFalse(packed.containsKey("retain"));
    assertFalse(packed.containsKey("storeOffline"));

    MessageOverrideDTO update = new MessageOverrideDTO();
    update.setExpiry(500L);
    update.setPriority(Priority.LOW);
    update.setQualityOfService(QualityOfService.EXACTLY_ONCE);
    update.setResponseTopic("/reply");
    update.setContentType("application/json");
    update.setSchemaId("schema");
    update.setRetain(Boolean.TRUE);
    update.setStoreOffline(Boolean.TRUE);
    update.setMeta(Map.of("a", "b"));
    update.setDataMap(Map.of("x", 1));

    assertTrue(config.update(update));
    assertFalse(config.update(update));
    assertFalse(config.update(new BaseConfigDTO()));
  }
}