package io.mapsmessaging.config.analytics;

import io.mapsmessaging.configuration.ConfigurationProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StatisticsConfigTest {

  @Test
  void eventCountIsClampedToDocumentedBounds() {
    ConfigurationProperties low = new ConfigurationProperties();
    low.put("eventCount", 1);

    ConfigurationProperties high = new ConfigurationProperties();
    high.put("eventCount", 2_000_000);

    assertEquals(10, new StatisticsConfig(low).getEventCount());
    assertEquals(1_000_000, new StatisticsConfig(high).getEventCount());
  }

  @Test
  void configurationPropertiesMustRoundTripInsteadOfReturningNull() {
    ConfigurationProperties props = new ConfigurationProperties();
    props.put("eventCount", 100);
    props.put("ignoreList", "serialNumber,modelName");
    props.put("keyList", "temperature, humidity");
    props.put("defaultAnalyser", "Advanced");

    StatisticsConfig config = new StatisticsConfig(props);
    ConfigurationProperties packed = config.toConfigurationProperties();

    assertNotNull(
        packed,
        "Config.toConfigurationProperties contract requires the populated properties to be returned"
    );
    assertEquals(100, packed.getIntProperty("eventCount", -1));
    assertEquals("Advanced", packed.getProperty("defaultAnalyser"));
  }

  @Test
  void applyingIdenticalConfigurationMustNotReportAChange() {
    StatisticsConfig current = config(List.of("temperature", "humidity"));
    StatisticsConfig identical = config(List.of("temperature", "humidity"));

    assertFalse(
        current.update(identical),
        "An identical key list is not a configuration change"
    );
  }

  @Test
  void changedKeyListMustBeAppliedAndReported() {
    StatisticsConfig current = config(List.of("temperature"));
    StatisticsConfig changed = config(List.of("temperature", "humidity"));

    assertTrue(
        current.update(changed),
        "Changing keyList must be reported as a configuration change"
    );
    assertEquals(List.of("temperature", "humidity"), current.getKeyList());
  }

  private static StatisticsConfig config(List<String> keys) {
    ConfigurationProperties props = new ConfigurationProperties();
    props.put("eventCount", 100);
    props.put("ignoreList", "");
    props.put("keyList", String.join(",", keys));
    props.put("defaultAnalyser", "Base");
    return new StatisticsConfig(props);
  }
}
