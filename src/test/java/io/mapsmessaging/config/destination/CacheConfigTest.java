package io.mapsmessaging.config.destination;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.destination.CacheConfigDTO;
import io.mapsmessaging.dto.rest.config.rest.StaticConfigDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CacheConfigTest {

  @Test
  void defaultsAndWriteThroughStringEncodingRoundTrip() {
    CacheConfig defaults = new CacheConfig(new ConfigurationProperties());
    assertEquals("None", defaults.getType());
    assertFalse(defaults.isWriteThrough());

    ConfigurationProperties props = new ConfigurationProperties();
    props.put("type", "JCS");
    props.put("writeThrough", "enable");

    CacheConfig source = new CacheConfig(props);
    CacheConfig restored = new CacheConfig(source.toConfigurationProperties());

    assertEquals("JCS", restored.getType());
    assertTrue(restored.isWriteThrough());
  }

  @Test
  void updateAppliesTypeAndWriteThroughChangesOnce() {
    CacheConfig config = new CacheConfig(new ConfigurationProperties());
    CacheConfigDTO update = new CacheConfigDTO();
    update.setType("WeakReference");
    update.setWriteThrough(true);

    assertTrue(config.update(update));
    assertEquals("WeakReference", config.getType());
    assertTrue(config.isWriteThrough());
    assertFalse(config.update(update));
    assertFalse(config.update(new StaticConfigDTO()));
  }
}
