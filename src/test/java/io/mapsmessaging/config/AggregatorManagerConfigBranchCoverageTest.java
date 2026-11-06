package io.mapsmessaging.config;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AggregatorManagerConfigBranchCoverageTest {

  @Test
  void integerReaderAcceptsNumbersStringsAndFallsBackForInvalidValues() throws Exception {
    ConfigurationProperties data = new ConfigurationProperties();
    data.put("stripeCount", "7");
    data.put("maxBatchPerAggregator", 11L);
    data.put("idleSleepMs", "not-a-number");

    AggregatorManagerConfig config = create(data);

    assertEquals(7, config.getStripeCount());
    assertEquals(11, config.getMaxBatchPerAggregator());
  }

  @Test
  void singleAggregatorObjectUsesSingleEntryPath() throws Exception {
    ConfigurationProperties aggregator = new ConfigurationProperties();
    aggregator.put("name", "one");
    aggregator.put("enabled", true);
    aggregator.put("outputTopic", "/out");

    ConfigurationProperties data = new ConfigurationProperties();
    data.put("aggregatorConfigList", aggregator);

    AggregatorManagerConfig config = create(data);

    assertEquals(1, config.getAggregatorConfigList().size());
  }

  @Test
  void missingDataUsesDefaultsAndUnrelatedUpdateIsRejected() throws Exception {
    AggregatorManagerConfig config = create(null);

    assertNotNull(config.getAggregatorConfigList());
    assertTrue(config.getAggregatorConfigList().isEmpty());
    assertFalse(config.update(new BaseConfigDTO()));
  }

  private static AggregatorManagerConfig create(ConfigurationProperties data) throws Exception {
    ConfigurationProperties root = new ConfigurationProperties();
    if (data != null) {
      root.put("data", data);
    }
    Constructor<AggregatorManagerConfig> constructor =
        AggregatorManagerConfig.class.getDeclaredConstructor(ConfigurationProperties.class);
    constructor.setAccessible(true);
    return constructor.newInstance(root);
  }
}