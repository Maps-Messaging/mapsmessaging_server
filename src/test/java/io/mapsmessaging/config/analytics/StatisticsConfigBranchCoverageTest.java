package io.mapsmessaging.config.analytics;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StatisticsConfigBranchCoverageTest {

  @Test
  void updateAppliesEventIgnoreAndAnalyserChanges() {
    StatisticsConfig current = config(100, "a,b", "x", "Base");
    StatisticsConfig changed = config(200, "c,d", "x", "Advanced");

    assertTrue(current.update(changed));
    assertEquals(200, current.getEventCount());
    assertEquals(java.util.List.of("c", "d"), current.getIgnoreList());
    assertEquals("Advanced", current.getStatisticName());
  }

  @Test
  void nonStatisticsDtoDoesNotChangeConfiguration() {
    StatisticsConfig current = config(100, "", "x", "Base");

    assertFalse(current.update(new BaseConfigDTO()));
  }

  @Test
  void emptyListsUseImmutableEmptyDefaults() {
    StatisticsConfig config = config(100, "", "", "Base");

    assertTrue(config.getIgnoreList().isEmpty());
    assertTrue(config.getKeyList().isEmpty());
  }

  private static StatisticsConfig config(
      int count,
      String ignore,
      String keys,
      String analyser) {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("eventCount", count);
    properties.put("ignoreList", ignore);
    properties.put("keyList", keys);
    properties.put("defaultAnalyser", analyser);
    return new StatisticsConfig(properties);
  }
}