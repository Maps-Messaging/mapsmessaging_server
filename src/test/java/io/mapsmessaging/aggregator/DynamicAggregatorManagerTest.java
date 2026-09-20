package io.mapsmessaging.aggregator;

import io.mapsmessaging.aggregator.worker.AggregatorWorkScheduler;
import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.dto.rest.config.aggregator.AggregatorConfigDTO;
import io.mapsmessaging.dto.rest.config.aggregator.AggregatorInputConfigDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class DynamicAggregatorManagerTest {

  private DynamicAggregatorManager manager;

  @AfterEach
  void stopManager() {
    if (manager != null) {
      manager.stop();
    }
  }

  @Test
  void blankAndUnmatchedDestinationsCompleteWithoutCreatingAnAggregator() {
    manager = new DynamicAggregatorManager(new AggregatorWorkScheduler(1, 4, 1), config("/sensor/+/value"));

    AtomicBoolean blankCompleted = new AtomicBoolean();
    manager.sendMessage(new MessageEvent(" ", null, null, () -> blankCompleted.set(true)));
    assertTrue(blankCompleted.get());

    AtomicBoolean unmatchedCompleted = new AtomicBoolean();
    manager.sendMessage(new MessageEvent("/other/device/value", null, null, () -> unmatchedCompleted.set(true)));
    assertTrue(unmatchedCompleted.get());
  }

  @Test
  @SuppressWarnings("unchecked")
  void wildcardMatchingExtractsSingleAndTailValues() throws Exception {
    manager = new DynamicAggregatorManager(new AggregatorWorkScheduler(1, 4, 1), config("/sensor/+/data/#"));
    Method method = DynamicAggregatorManager.class.getDeclaredMethod("matchAndExtract", String.class, String.class);
    method.setAccessible(true);

    assertEquals(
        List.of("alpha", "gps/raw"),
        (List<String>) method.invoke(manager, "/sensor/+/data/#", "/sensor/alpha/data/gps/raw")
    );
    assertEquals(
        List.of("alpha", ""),
        (List<String>) method.invoke(manager, "/sensor/+/data/#", "/sensor/alpha/data")
    );
    assertNull(method.invoke(manager, "/sensor/+/data/#", "/other/alpha/data"));
  }

  @Test
  void resolvedConfigAppliesWildcardToInputAndOutputWithoutMutatingTemplate() throws Exception {
    AggregatorConfigDTO template = config("/sensor/+/value");
    manager = new DynamicAggregatorManager(new AggregatorWorkScheduler(1, 4, 1), template);
    Method method = DynamicAggregatorManager.class.getDeclaredMethod("buildResolvedConfig", String.class, List.class);
    method.setAccessible(true);

    AggregatorConfigDTO resolved = (AggregatorConfigDTO) method.invoke(manager, "vehicle-7", List.of("vehicle-7"));

    assertEquals("agg-vehicle-7", resolved.getName());
    assertEquals("/out/vehicle-7", resolved.getOutputTopic());
    assertEquals("/sensor/vehicle-7/value", resolved.getInputs().getFirst().getTopicName());
    assertEquals("/sensor/+/value", template.getInputs().getFirst().getTopicName());
  }

  private static AggregatorConfigDTO config(String topic) {
    AggregatorInputConfigDTO input = new AggregatorInputConfigDTO();
    input.setTopicName(topic);

    AggregatorConfigDTO config = new AggregatorConfigDTO();
    config.setName("agg");
    config.setEnabled(true);
    config.setInputs(List.of(input));
    config.setOutputTopic("/out/{topicName}");
    config.setWindowDurationMs(1000);
    config.setTimeoutMs(1000);
    return config;
  }
}
