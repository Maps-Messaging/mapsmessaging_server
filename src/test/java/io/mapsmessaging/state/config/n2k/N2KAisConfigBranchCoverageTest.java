package io.mapsmessaging.state.config.n2k;

import io.mapsmessaging.configuration.ConfigurationProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class N2KAisConfigBranchCoverageTest {

  @Test
  void allSupportedPgnBlocksRoundTrip() {
    ConfigurationProperties root = new ConfigurationProperties();
    root.put("pgn129039", pgn(true, 1000));
    root.put("pgn129040", pgn(false, 2000));
    root.put("pgn129809", pgn(true, 3000));
    root.put("pgn129810", pgn(false, 4000));

    N2KAisConfig config = new N2KAisConfig(root);
    N2KAisConfig restored = new N2KAisConfig(config.toConfigurationProperties());

    assertTrue(restored.getPgn129039().isEnabled());
    assertEquals(2000, restored.getPgn129040().getIntervalMilliseconds());
    assertEquals(3000, restored.getPgn129809().getIntervalMilliseconds());
    assertFalse(restored.getPgn129810().isEnabled());
  }

  @Test
  void updateAddsThenReplacesPgnConfigurationsAndBecomesIdempotent() {
    N2KAisConfig config = new N2KAisConfig(new ConfigurationProperties());
    N2KAisConfigDTO first = new N2KAisConfigDTO();
    N2KPgnTransmitConfigDTO pgn = new N2KPgnTransmitConfigDTO();
    pgn.setEnabled(true);
    pgn.setIntervalMilliseconds(500);
    first.setPgn129039(pgn);

    assertTrue(config.update(first));
    assertSame(pgn, config.getPgn129039());

    N2KAisConfigDTO replacement = new N2KAisConfigDTO();
    N2KPgnTransmitConfigDTO changed = new N2KPgnTransmitConfigDTO();
    changed.setEnabled(false);
    changed.setIntervalMilliseconds(750);
    replacement.setPgn129039(changed);

    assertTrue(config.update(replacement));
    assertSame(changed, config.getPgn129039());
    assertFalse(config.update(replacement));
  }

  private static ConfigurationProperties pgn(boolean enabled, long interval) {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("enabled", enabled);
    properties.put("intervalMilliseconds", interval);
    return properties;
  }
}