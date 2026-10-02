package io.mapsmessaging.aggregator;

import io.mapsmessaging.aggregator.worker.AggregatorWorkScheduler;
import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.dto.rest.config.aggregator.AggregatorConfigDTO;
import io.mapsmessaging.dto.rest.config.aggregator.AggregatorInputConfigDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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
  @SuppressWarnings("unchecked")
  void wildcardMatchingCoversMismatchLengthAndTerminalHashCases() throws Exception {
    manager = new DynamicAggregatorManager(new AggregatorWorkScheduler(1, 4, 1), config("/sensor/+/data/#"));
    Method method = DynamicAggregatorManager.class.getDeclaredMethod("matchAndExtract", String.class, String.class);
    method.setAccessible(true);

    assertNull(method.invoke(manager, "/sensor/+/data/#", "/sensor"));
    assertNull(method.invoke(manager, "/sensor/+/data/#", "/sensor/alpha/other"));
    assertEquals(
        List.of("alpha", ""),
        (List<String>) method.invoke(manager, "/sensor/+/data/#", "/sensor/alpha/data")
    );
    assertEquals(
        List.of("alpha", "one/two"),
        (List<String>) method.invoke(manager, "/sensor/+/data/#", "/sensor/alpha/data/one/two")
    );
  }

  @Test
  void helperMethodsResolveWildcardsKeysAndNames() throws Exception {
    manager = new DynamicAggregatorManager(new AggregatorWorkScheduler(1, 4, 1), config("/sensor/+/data/#"));

    Method applyWildcardValues =
        DynamicAggregatorManager.class.getDeclaredMethod("applyWildcardValues", String.class, List.class);
    applyWildcardValues.setAccessible(true);
    Method buildAggregatorKey =
        DynamicAggregatorManager.class.getDeclaredMethod("buildAggregatorKey", List.class, String.class);
    buildAggregatorKey.setAccessible(true);
    Method sanitiseKey = DynamicAggregatorManager.class.getDeclaredMethod("sanitiseKey", String.class);
    sanitiseKey.setAccessible(true);

    assertEquals(
        "/sensor/alpha/data/gps/raw",
        applyWildcardValues.invoke(manager, "/sensor/+/data/#", List.of("alpha", "gps/raw"))
    );
    assertEquals(
        "alpha/gps/raw",
        buildAggregatorKey.invoke(manager, List.of("alpha", "gps/raw"), "/sensor/alpha/data/gps/raw")
    );
    assertEquals(
        "/plain/topic",
        buildAggregatorKey.invoke(manager, List.of(), "/plain/topic")
    );
    assertEquals("alpha_beta___", sanitiseKey.invoke(manager, "alpha/beta/#+"));
  }

  @Test
  void connectionMetadataAndUnknownDestinationDeletionAreStable() {
    manager = new DynamicAggregatorManager(new AggregatorWorkScheduler(1, 4, 1), config("/sensor/+/value"));

    assertEquals(30000L, manager.getTimeOut());
    assertEquals("DynamicAggregatorManager-agg", manager.getName());
    assertEquals("1.0", manager.getVersion());
    assertNull(manager.getPrincipal());
    assertEquals("", manager.getAuthenticationConfig());
    assertEquals("agg", manager.getUniqueName());
    assertEquals("aggregator-dynamic", manager.getProtocolName());
    assertEquals("loop", manager.getRemoteIp());

    assertDoesNotThrow(manager::sendKeepAlive);
    assertDoesNotThrow(() -> manager.destinationDeleted("/not/mapped"));
  }

  @Test
  void completionTaskRunsOnceAndExceptionsAreContained() {
    manager = new DynamicAggregatorManager(new AggregatorWorkScheduler(1, 4, 1), config("/sensor/+/value"));
    AtomicInteger completions = new AtomicInteger();

    manager.sendMessage(new MessageEvent(null, null, null, completions::incrementAndGet));
    assertEquals(1, completions.get());

    assertDoesNotThrow(() -> manager.sendMessage(
        new MessageEvent(" ", null, null, () -> {
          throw new IllegalStateException("expected");
        })
    ));
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


  @Test
  void destinationDeletionKeepsSharedAggregatorUntilLastResolvedTopicIsRemoved() throws Exception {
    manager = new DynamicAggregatorManager(new AggregatorWorkScheduler(1, 4, 1), config("/sensor/+/value"));

    Map<String, String> topics = mapField("topicToAggregatorKey");
    Map<String, Long> lastSeen = mapField("aggregatorLastSeen");
    topics.put("/sensor/a/value", "a");
    topics.put("/sensor/a/secondary", "a");
    lastSeen.put("a", System.currentTimeMillis());

    manager.destinationDeleted("/sensor/a/value");

    assertFalse(topics.containsKey("/sensor/a/value"));
    assertEquals("a", topics.get("/sensor/a/secondary"));
    assertTrue(lastSeen.containsKey("a"));

    manager.destinationDeleted("/sensor/a/secondary");

    assertFalse(topics.containsValue("a"));
    assertFalse(lastSeen.containsKey("a"));
  }

  @Test
  void cleanupRemovesExpiredAggregatorStateAndKeepsRecentState() throws Exception {
    manager = new DynamicAggregatorManager(new AggregatorWorkScheduler(1, 4, 1), config("/sensor/+/value"));

    Map<String, String> topics = mapField("topicToAggregatorKey");
    Map<String, Long> lastSeen = mapField("aggregatorLastSeen");
    long now = System.currentTimeMillis();
    topics.put("/sensor/stale/value", "stale");
    topics.put("/sensor/fresh/value", "fresh");
    lastSeen.put("stale", now - 20_000L);
    lastSeen.put("fresh", now);

    Method cleanup = DynamicAggregatorManager.class.getDeclaredMethod("cleanupInactiveAggregators");
    cleanup.setAccessible(true);
    cleanup.invoke(manager);

    assertFalse(topics.containsKey("/sensor/stale/value"));
    assertFalse(lastSeen.containsKey("stale"));
    assertEquals("fresh", topics.get("/sensor/fresh/value"));
    assertTrue(lastSeen.containsKey("fresh"));
  }

  @SuppressWarnings("unchecked")
  private <K, V> Map<K, V> mapField(String name) throws Exception {
    Field field = DynamicAggregatorManager.class.getDeclaredField(name);
    field.setAccessible(true);
    return (Map<K, V>) field.get(manager);
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
