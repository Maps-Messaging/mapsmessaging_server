package io.mapsmessaging.state.config.n2k;

import io.mapsmessaging.configuration.ConfigurationProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class N2KPgnTransmitConfigTest {

  @Test
  void configurationRoundTripsEnabledFlagAndInterval() {
    ConfigurationProperties props = new ConfigurationProperties();
    props.put("enabled", true);
    props.put("intervalMilliseconds", 2500L);

    N2KPgnTransmitConfig source = new N2KPgnTransmitConfig(props);
    N2KPgnTransmitConfig restored =
        new N2KPgnTransmitConfig(source.toConfigurationProperties());

    assertTrue(restored.isEnabled());
    assertEquals(2500L, restored.getIntervalMilliseconds());
  }

  @Test
  void updateReportsRealChangesOnly() {
    N2KPgnTransmitConfig current =
        new N2KPgnTransmitConfig(new ConfigurationProperties());

    ConfigurationProperties props = new ConfigurationProperties();
    props.put("enabled", true);
    props.put("intervalMilliseconds", 100L);
    N2KPgnTransmitConfig changed = new N2KPgnTransmitConfig(props);

    assertTrue(current.update(changed));
    assertTrue(current.isEnabled());
    assertEquals(100L, current.getIntervalMilliseconds());
    assertFalse(current.update(changed));
  }
}
