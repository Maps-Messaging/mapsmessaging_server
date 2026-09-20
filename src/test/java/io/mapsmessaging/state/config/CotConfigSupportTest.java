package io.mapsmessaging.state.config;

import io.mapsmessaging.configuration.ConfigurationProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CotConfigSupportTest {

  @Test
  void singleEntryParsesConfiguredDefaultsAndAffiliationCaseInsensitively() {
    ConfigurationProperties properties = validEntry();
    properties.put("affiliation", "hostile");
    properties.put("uidPrefix", "edge-");
    properties.put("defaultCircularErrorMeters", 3.5);
    properties.put("defaultLinearErrorMeters", 4.5);

    CotConfigDTO config = CotConfigSupport.parse(properties).getFirst();

    assertEquals(CotAffiliation.HOSTILE, config.getAffiliation());
    assertEquals("edge-", config.getUidPrefix());
    assertEquals(3.5, config.getDefaultCircularErrorMeters(), 0.0);
    assertEquals(4.5, config.getDefaultLinearErrorMeters(), 0.0);
  }

  @Test
  void listParsingIgnoresUnsupportedEntries() {
    ConfigurationProperties first = validEntry();
    ConfigurationProperties second = validEntry();
    second.put("namespacePath", "/second/#");

    List<CotConfigDTO> configs = CotConfigSupport.parse(List.of(first, "ignored", second));

    assertEquals(List.of("/cot/#", "/second/#"), configs.stream().map(CotConfigDTO::getNamespacePath).toList());
  }

  @Test
  void roundTripPreservesPublishTakvPolicy() {
    CotConfigDTO source = CotConfigSupport.parse(validEntry()).getFirst();
    source.setPublishTakv(false);

    ConfigurationProperties packed = CotConfigSupport.toConfigurationProperties(List.of(source)).getFirst();
    CotConfigDTO restored = CotConfigSupport.parse(packed).getFirst();

    assertFalse(restored.isPublishTakv());
  }

  @Test
  void invalidNamespaceTimeoutAndErrorValuesAreRejected() {
    ConfigurationProperties blank = validEntry();
    blank.put("namespacePath", " ");
    assertThrows(IllegalArgumentException.class, () -> CotConfigSupport.parse(blank));

    ConfigurationProperties timeout = validEntry();
    timeout.put("staleTimeoutMillis", 0);
    assertThrows(IllegalArgumentException.class, () -> CotConfigSupport.parse(timeout));

    ConfigurationProperties error = validEntry();
    error.put("defaultCircularErrorMeters", -1.0);
    assertThrows(IllegalArgumentException.class, () -> CotConfigSupport.parse(error));
  }

  private static ConfigurationProperties validEntry() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("namespacePath", "/cot/#");
    properties.put("staleTimeoutMillis", 30000L);
    return properties;
  }
}