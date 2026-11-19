/*
 *
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *
 */

package io.mapsmessaging.fusion;

import io.mapsmessaging.fusion.worker.FusionWorkScheduler;
import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.dto.rest.config.fusion.FusionConfigDTO;
import io.mapsmessaging.dto.rest.config.fusion.FusionInputConfigDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

class DynamicFusionManagerBranchCoverageTest {

  private DynamicFusionManager manager;

  @AfterEach
  void stopManager() {
    if (manager != null) {
      manager.stop();
    }
  }

  @ParameterizedTest
  @MethodSource("matchCases")
  void wildcardMatchingCoversTopicShapes(
      String template, String destination, List<String> expected) throws Exception {
    manager = manager(template);

    assertEquals(expected, invokeList("matchAndExtract", template, destination));
  }

  @ParameterizedTest
  @MethodSource("resolveCases")
  void wildcardReplacementCoversSingleMultiAndTailValues(
      String template, List<String> values, String expected) throws Exception {
    manager = manager("/sensor/+/value");

    assertEquals(expected, invoke("applyWildcardValues",
        new Class<?>[]{String.class, List.class}, template, values));
  }

  @ParameterizedTest
  @MethodSource("keyCases")
  void aggregatorKeyUsesWildcardValuesWhenPresent(
      List<String> values, String destination, String expected) throws Exception {
    manager = manager("/sensor/+/value");

    assertEquals(expected, invoke("buildFusionKey",
        new Class<?>[]{List.class, String.class}, values, destination));
  }

  @ParameterizedTest
  @MethodSource("sanitiseCases")
  void keySanitisingOnlyReplacesTopicWildcardCharacters(String input, String expected)
      throws Exception {
    manager = manager("/sensor/+/value");

    assertEquals(expected, invoke("sanitiseKey", new Class<?>[]{String.class}, input));
  }

  @Test
  void sendMessageCompletesWhenMappedFusionHasDisappeared() throws Exception {
    manager = manager("/sensor/+/value");
    mapField("topicToFusionKey").put("/sensor/a/value", "a");
    AtomicInteger completionCount = new AtomicInteger();
    MessageEvent event =
        new MessageEvent("/sensor/a/value", null, null, completionCount::incrementAndGet);

    manager.sendMessage(event);

    assertEquals(1, completionCount.get());
    assertFalse(mapField("aggregatorLastSeen").containsKey("a"));
  }

  @Test
  void sendMessageDelegatesToMappedFusionAndRefreshesLastSeen() throws Exception {
    manager = manager("/sensor/+/value");
    StaticFusion aggregator = mock(StaticFusion.class);
    mapField("topicToFusionKey").put("/sensor/a/value", "a");
    mapField("aggregatorsByKey").put("a", aggregator);
    mapField("aggregatorLastSeen").put("a", 1L);
    MessageEvent event = new MessageEvent("/sensor/a/value", null, null, () -> fail("delegated"));

    manager.sendMessage(event);

    verify(aggregator).acceptResolvedEvent("/sensor/a/value", event);
    assertTrue((Long) mapField("aggregatorLastSeen").get("a") > 1L);
  }

  @Test
  void createOrResolveReusesExistingFusionAndCachesNewTopic() throws Exception {
    manager = manager("/sensor/+/value");
    StaticFusion aggregator = mock(StaticFusion.class);
    mapField("aggregatorsByKey").put("a", aggregator);

    String key = (String) invoke("createOrResolveFusionKey",
        new Class<?>[]{String.class}, "/sensor/a/value");

    assertEquals("a", key);
    assertEquals("a", mapField("topicToFusionKey").get("/sensor/a/value"));
    assertTrue((Long) mapField("aggregatorLastSeen").get("a") > 0L);
    verifyNoInteractions(aggregator);
  }

  @Test
  void createOrResolveReturnsNullForEveryUnmatchedTemplate() throws Exception {
    FusionConfigDTO config = config("/sensor/+/value", "/status/+/state");
    manager = new DynamicFusionManager(new FusionWorkScheduler(1, 4, 1), config);

    assertNull(invoke("createOrResolveFusionKey",
        new Class<?>[]{String.class}, "/other/a/value"));
    assertTrue(mapField("topicToFusionKey").isEmpty());
    assertTrue(mapField("aggregatorsByKey").isEmpty());
  }

  @Test
  void resolvedConfigCopiesTemplateBehaviourAndResolvesEveryInput() throws Exception {
    FusionConfigDTO config = config("/sensor/+/value", "/status/+/state");
    config.setName("fleet");
    config.setEnabled(false);
    config.setOutputTopic("/combined/{topicName}");
    config.setWindowDurationMs(12_345L);
    config.setTimeoutMs(6_789L);
    config.setEmitFirstEventImmediately(true);
    config.setMaxEventsPerTopic(17);
    manager = new DynamicFusionManager(new FusionWorkScheduler(1, 4, 1), config);

    FusionConfigDTO resolved = (FusionConfigDTO) invoke(
        "buildResolvedConfig",
        new Class<?>[]{String.class, List.class},
        "alpha",
        List.of("alpha")
    );

    assertEquals("fleet-alpha", resolved.getName());
    assertFalse(resolved.isEnabled());
    assertEquals("/combined/alpha", resolved.getOutputTopic());
    assertEquals(12_345L, resolved.getWindowDurationMs());
    assertEquals(6_789L, resolved.getTimeoutMs());
    assertTrue(resolved.isEmitFirstEventImmediately());
    assertEquals(17, resolved.getMaxEventsPerTopic());
    assertEquals("/sensor/alpha/value", resolved.getInputs().get(0).getTopicName());
    assertEquals("/status/alpha/state", resolved.getInputs().get(1).getTopicName());
    assertEquals("/sensor/+/value", config.getInputs().get(0).getTopicName());
    assertEquals("/status/+/state", config.getInputs().get(1).getTopicName());
  }

  @Test
  void resolvedConfigLeavesOutputUntouchedWithoutPlaceholder() throws Exception {
    FusionConfigDTO config = config("/sensor/+/value");
    config.setOutputTopic("/combined/static");
    manager = new DynamicFusionManager(new FusionWorkScheduler(1, 4, 1), config);

    FusionConfigDTO resolved = (FusionConfigDTO) invoke(
        "buildResolvedConfig",
        new Class<?>[]{String.class, List.class},
        "alpha",
        List.of("alpha")
    );

    assertEquals("/combined/static", resolved.getOutputTopic());
  }

  @Test
  void destinationDeletionStopsFusionOnLastReference() throws Exception {
    manager = manager("/sensor/+/value");
    StaticFusion aggregator = mock(StaticFusion.class);
    mapField("aggregatorsByKey").put("a", aggregator);
    mapField("topicToFusionKey").put("/sensor/a/value", "a");
    mapField("aggregatorLastSeen").put("a", 10L);

    manager.destinationDeleted("/sensor/a/value");

    verify(aggregator).stop();
    assertFalse(mapField("aggregatorsByKey").containsKey("a"));
    assertFalse(mapField("aggregatorLastSeen").containsKey("a"));
  }

  @Test
  void destinationDeletionContainsFusionStopFailure() throws Exception {
    manager = manager("/sensor/+/value");
    StaticFusion aggregator = mock(StaticFusion.class);
    doThrow(new IllegalStateException("stop")).when(aggregator).stop();
    mapField("aggregatorsByKey").put("a", aggregator);
    mapField("topicToFusionKey").put("/sensor/a/value", "a");
    mapField("aggregatorLastSeen").put("a", 10L);

    assertDoesNotThrow(() -> manager.destinationDeleted("/sensor/a/value"));

    assertFalse(mapField("aggregatorsByKey").containsKey("a"));
    assertFalse(mapField("aggregatorLastSeen").containsKey("a"));
  }

  @Test
  void removeFusionClearsOnlyTopicsOwnedByThatKey() throws Exception {
    manager = manager("/sensor/+/value");
    StaticFusion a = mock(StaticFusion.class);
    StaticFusion b = mock(StaticFusion.class);
    mapField("aggregatorsByKey").put("a", a);
    mapField("aggregatorsByKey").put("b", b);
    mapField("topicToFusionKey").put("/a/1", "a");
    mapField("topicToFusionKey").put("/a/2", "a");
    mapField("topicToFusionKey").put("/b/1", "b");
    mapField("aggregatorLastSeen").put("a", 1L);
    mapField("aggregatorLastSeen").put("b", 2L);

    invoke("removeFusion", new Class<?>[]{String.class}, "a");

    verify(a).stop();
    verifyNoInteractions(b);
    assertEquals(Map.of("/b/1", "b"), mapField("topicToFusionKey"));
    assertEquals(Map.of("b", 2L), mapField("aggregatorLastSeen"));
  }

  @Test
  void removeUnknownFusionStillCleansDanglingMappings() throws Exception {
    manager = manager("/sensor/+/value");
    mapField("topicToFusionKey").put("/a/1", "a");
    mapField("topicToFusionKey").put("/b/1", "b");
    mapField("aggregatorLastSeen").put("a", 1L);

    invoke("removeFusion", new Class<?>[]{String.class}, "a");

    assertEquals(Map.of("/b/1", "b"), mapField("topicToFusionKey"));
    assertFalse(mapField("aggregatorLastSeen").containsKey("a"));
  }

  @Test
  void cleanupUsesFifteenSecondMinimumExpiry() throws Exception {
    FusionConfigDTO config = config("/sensor/+/value");
    config.setTimeoutMs(1_000L);
    manager = new DynamicFusionManager(new FusionWorkScheduler(1, 4, 1), config);
    StaticFusion stale = mock(StaticFusion.class);
    StaticFusion fresh = mock(StaticFusion.class);
    long now = System.currentTimeMillis();
    mapField("aggregatorsByKey").put("stale", stale);
    mapField("aggregatorsByKey").put("fresh", fresh);
    mapField("topicToFusionKey").put("/stale", "stale");
    mapField("topicToFusionKey").put("/fresh", "fresh");
    mapField("aggregatorLastSeen").put("stale", now - 16_000L);
    mapField("aggregatorLastSeen").put("fresh", now - 14_000L);

    invoke("cleanupInactiveFusions", new Class<?>[0]);

    verify(stale).stop();
    verifyNoInteractions(fresh);
    assertFalse(mapField("aggregatorLastSeen").containsKey("stale"));
    assertTrue(mapField("aggregatorLastSeen").containsKey("fresh"));
  }

  @Test
  void cleanupUsesThreeTimesConfiguredTimeoutAboveMinimum() throws Exception {
    FusionConfigDTO config = config("/sensor/+/value");
    config.setTimeoutMs(10_000L);
    manager = new DynamicFusionManager(new FusionWorkScheduler(1, 4, 1), config);
    StaticFusion stale = mock(StaticFusion.class);
    StaticFusion fresh = mock(StaticFusion.class);
    long now = System.currentTimeMillis();
    mapField("aggregatorsByKey").put("stale", stale);
    mapField("aggregatorsByKey").put("fresh", fresh);
    mapField("aggregatorLastSeen").put("stale", now - 31_000L);
    mapField("aggregatorLastSeen").put("fresh", now - 29_000L);

    invoke("cleanupInactiveFusions", new Class<?>[0]);

    verify(stale).stop();
    verifyNoInteractions(fresh);
  }

  @Test
  void nullCompletionIsAccepted() {
    manager = manager("/sensor/+/value");

    assertDoesNotThrow(() -> manager.sendMessage(new MessageEvent(null, null, null, null)));
  }

  private DynamicFusionManager manager(String topic) {
    return new DynamicFusionManager(new FusionWorkScheduler(1, 4, 1), config(topic));
  }

  @SuppressWarnings("unchecked")
  private List<String> invokeList(String methodName, String template, String destination)
      throws Exception {
    return (List<String>) invoke(
        methodName,
        new Class<?>[]{String.class, String.class},
        template,
        destination
    );
  }

  private Object invoke(String methodName, Class<?>[] parameterTypes, Object... args)
      throws Exception {
    Method method = DynamicFusionManager.class.getDeclaredMethod(methodName, parameterTypes);
    method.setAccessible(true);
    return method.invoke(manager, args);
  }

  @SuppressWarnings("unchecked")
  private Map<Object, Object> mapField(String name) throws Exception {
    Field field = DynamicFusionManager.class.getDeclaredField(name);
    field.setAccessible(true);
    return (Map<Object, Object>) field.get(manager);
  }

  private static FusionConfigDTO config(String... topics) {
    List<FusionInputConfigDTO> inputs = Stream.of(topics)
        .map(topic -> {
          FusionInputConfigDTO input = new FusionInputConfigDTO();
          input.setTopicName(topic);
          return input;
        })
        .toList();

    FusionConfigDTO config = new FusionConfigDTO();
    config.setName("agg");
    config.setEnabled(true);
    config.setInputs(inputs);
    config.setOutputTopic("/out/{topicName}");
    config.setWindowDurationMs(1_000L);
    config.setTimeoutMs(1_000L);
    return config;
  }

  private static Stream<Arguments> matchCases() {
    return Stream.of(
        Arguments.of("/sensor/+/value", "/sensor/a/value", List.of("a")),
        Arguments.of("/sensor/+/value", "/sensor//value", List.of("")),
        Arguments.of("/sensor/+/value", "/sensor/a/other", null),
        Arguments.of("/sensor/+/value", "/other/a/value", null),
        Arguments.of("/sensor/+/value", "/sensor/a", null),
        Arguments.of("/sensor/+/value", "/sensor/a/value/extra", null),
        Arguments.of("/sensor/#", "/sensor", List.of("")),
        Arguments.of("/sensor/#", "/sensor/", List.of("")),
        Arguments.of("/sensor/#", "/sensor/a", List.of("a")),
        Arguments.of("/sensor/#", "/sensor/a/b", List.of("a/b")),
        Arguments.of("/sensor/#", "/other/a", null),
        Arguments.of("/+/+/value", "/a/b/value", List.of("a", "b")),
        Arguments.of("/+/+/value", "//b/value", List.of("", "b")),
        Arguments.of("/+/+/value", "/a//value", List.of("a", "")),
        Arguments.of("/+/+/value", "/a/b/other", null),
        Arguments.of("/exact/topic", "/exact/topic", List.of()),
        Arguments.of("/exact/topic", "/exact/other", null),
        Arguments.of("/", "/", List.of()),
        Arguments.of("", "", List.of()),
        Arguments.of("#", "", List.of("")),
        Arguments.of("#", "a/b/c", List.of("a/b/c")),
        Arguments.of("+", "a", List.of("a")),
        Arguments.of("+", "", List.of("")),
        Arguments.of("a/+/c/#", "a/b/c/d/e", List.of("b", "d/e"))
    );
  }

  private static Stream<Arguments> resolveCases() {
    return Stream.of(
        Arguments.of("/sensor/+/value", List.of("a"), "/sensor/a/value"),
        Arguments.of("/+/+/value", List.of("a", "b"), "/a/b/value"),
        Arguments.of("/sensor/#", List.of("a/b"), "/sensor/a/b"),
        Arguments.of("/sensor/#", List.of(""), "/sensor/"),
        Arguments.of("#", List.of("a/b"), "a/b"),
        Arguments.of("+", List.of("a"), "a"),
        Arguments.of("/a/+/b/#", List.of("x", "y/z"), "/a/x/b/y/z"),
        Arguments.of("/exact/topic", List.of(), "/exact/topic")
    );
  }

  private static Stream<Arguments> keyCases() {
    return Stream.of(
        Arguments.of(null, "/sensor/a/value", "/sensor/a/value"),
        Arguments.of(List.of(), "/sensor/a/value", "/sensor/a/value"),
        Arguments.of(List.of("a"), "/sensor/a/value", "a"),
        Arguments.of(List.of("a", "b"), "/a/b/value", "a/b"),
        Arguments.of(List.of("", "tail"), "/value", "/tail")
    );
  }

  private static Stream<Arguments> sanitiseCases() {
    return Stream.of(
        Arguments.of("plain", "plain"),
        Arguments.of("a/b", "a_b"),
        Arguments.of("a#b", "a_b"),
        Arguments.of("a+b", "a_b"),
        Arguments.of("a/#+b", "a___b"),
        Arguments.of("", ""),
        Arguments.of("a b", "a b")
    );
  }
}
