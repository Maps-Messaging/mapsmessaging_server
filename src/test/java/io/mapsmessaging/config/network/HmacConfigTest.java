package io.mapsmessaging.config.network;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.destination.FormatConfigDTO;
import io.mapsmessaging.dto.rest.config.network.HmacConfigDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HmacConfigTest {

  @Test
  void configuredHmacFieldsRoundTrip() {
    ConfigurationProperties props = new ConfigurationProperties();
    props.put("host", "example.org");
    props.put("port", 9000);
    props.put("HmacAlgorithm", "HmacSHA256");
    props.put("HmacManager", "Appender");
    props.put("HmacSharedKey", "shared-secret");

    HmacConfig source = new HmacConfig(props);
    HmacConfig restored = new HmacConfig(source.toConfigurationProperties());

    assertEquals(source.getHost(), restored.getHost());
    assertEquals(source.getPort(), restored.getPort());
    assertEquals(source.getHmacAlgorithm(), restored.getHmacAlgorithm());
    assertEquals(source.getHmacManager(), restored.getHmacManager());
    assertEquals(source.getHmacSharedKey(), restored.getHmacSharedKey());
  }

  @Test
  void algorithmEnablesDefaultManagerAndUpdateTracksChanges() {
    ConfigurationProperties props = new ConfigurationProperties();
    props.put("HmacAlgorithm", "HmacSHA256");
    HmacConfig config = new HmacConfig(props);

    assertEquals("Appender", config.getHmacManager());

    HmacConfigDTO update = new HmacConfigDTO();
    update.setHost("host");
    update.setPort(10000);
    update.setHmacAlgorithm("HmacSHA512");
    update.setHmacManager("Validator");
    update.setHmacSharedKey("new-key");

    assertTrue(config.update(update));
    assertEquals("HmacSHA512", config.getHmacAlgorithm());
    assertEquals("Validator", config.getHmacManager());
    assertEquals("new-key", config.getHmacSharedKey());
    assertFalse(config.update(update));
    assertFalse(config.update(new FormatConfigDTO()));
  }
}
