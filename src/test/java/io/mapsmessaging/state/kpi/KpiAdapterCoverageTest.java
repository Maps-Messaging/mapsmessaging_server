/*
 *
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *
 */

package io.mapsmessaging.state.kpi;

import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.state.drone.core.EntityTwin;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.core.TwinType;
import io.mapsmessaging.state.drone.tak.MtiStatusRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.Principal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class KpiAdapterCoverageTest {

  @TempDir
  Path tempDir;

  @Test
  void clientConnectionIdentityIsStable() {
    KpiAdapter adapter = adapter(mock(KpiConfig.class), mock(TwinManager.class));

    assertEquals("kpi", adapter.getName());
    assertEquals("1.0", adapter.getVersion());
    assertEquals(0L, adapter.getTimeOut());
    assertNull(adapter.getPrincipal());
    assertEquals("", adapter.getAuthenticationConfig());
    assertEquals("kpi-adapter", adapter.getUniqueName());
    assertEquals("internal", adapter.getProtocolName());
    assertEquals("", adapter.getRemoteIp());
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "{}",
      "{\"event\":\"note\"}",
      "{\"event\":\"inject\",\"failure_type\":\"FEED_LOSS\"}",
      "plain-text",
      "µ",
      " ",
      "[]",
      "null"
  })
  void nonEmptyControlPayloadIsForwardedExactly(String payload) throws Exception {
    KpiEvaluator evaluator = mock(KpiEvaluator.class);
    KpiAdapter adapter = adapter(mock(KpiConfig.class), mock(TwinManager.class));
    setField(adapter, "evaluator", evaluator);
    AtomicInteger completions = new AtomicInteger();

    adapter.sendMessage(event(payload.getBytes(StandardCharsets.UTF_8), completions::incrementAndGet));

    verify(evaluator).handleControl(payload);
    assertEquals(1, completions.get());
  }

  @Test
  void nullPayloadCompletesWithoutCallingEvaluator() throws Exception {
    KpiEvaluator evaluator = mock(KpiEvaluator.class);
    KpiAdapter adapter = adapter(mock(KpiConfig.class), mock(TwinManager.class));
    setField(adapter, "evaluator", evaluator);
    AtomicInteger completions = new AtomicInteger();

    adapter.sendMessage(event(null, completions::incrementAndGet));

    verifyNoInteractions(evaluator);
    assertEquals(1, completions.get());
  }

  @Test
  void emptyPayloadCompletesWithoutCallingEvaluator() throws Exception {
    KpiEvaluator evaluator = mock(KpiEvaluator.class);
    KpiAdapter adapter = adapter(mock(KpiConfig.class), mock(TwinManager.class));
    setField(adapter, "evaluator", evaluator);
    AtomicInteger completions = new AtomicInteger();

    adapter.sendMessage(event(new byte[0], completions::incrementAndGet));

    verifyNoInteractions(evaluator);
    assertEquals(1, completions.get());
  }

  @Test
  void payloadBeforeEvaluatorStartIsDroppedAndCompleted() {
    KpiAdapter adapter = adapter(mock(KpiConfig.class), mock(TwinManager.class));
    AtomicInteger completions = new AtomicInteger();

    adapter.sendMessage(event("early".getBytes(StandardCharsets.UTF_8), completions::incrementAndGet));

    assertEquals(1, completions.get());
  }

  @Test
  void evaluatorFailureIsContainedAndCompletionStillRuns() throws Exception {
    KpiEvaluator evaluator = mock(KpiEvaluator.class);
    doThrow(new IllegalArgumentException("bad control")).when(evaluator).handleControl(anyString());
    KpiAdapter adapter = adapter(mock(KpiConfig.class), mock(TwinManager.class));
    setField(adapter, "evaluator", evaluator);
    AtomicInteger completions = new AtomicInteger();

    assertDoesNotThrow(() ->
        adapter.sendMessage(event("bad".getBytes(StandardCharsets.UTF_8), completions::incrementAndGet)));

    assertEquals(1, completions.get());
  }

  @Test
  void nullMessageIsContainedAndCompletionStillRuns() {
    KpiAdapter adapter = adapter(mock(KpiConfig.class), mock(TwinManager.class));
    AtomicInteger completions = new AtomicInteger();
    MessageEvent event = new MessageEvent("/kpi/control", null, null, completions::incrementAndGet);

    assertDoesNotThrow(() -> adapter.sendMessage(event));

    assertEquals(1, completions.get());
  }

  @Test
  void nullCompletionTaskIsAccepted() throws Exception {
    KpiEvaluator evaluator = mock(KpiEvaluator.class);
    KpiAdapter adapter = adapter(mock(KpiConfig.class), mock(TwinManager.class));
    setField(adapter, "evaluator", evaluator);

    assertDoesNotThrow(() ->
        adapter.sendMessage(event("control".getBytes(StandardCharsets.UTF_8), null)));

    verify(evaluator).handleControl("control");
  }

  @Test
  void emptyTwinManagerProducesEmptyEvaluatorInput() throws Exception {
    TwinManager twins = mock(TwinManager.class);
    when(twins.listTwins()).thenReturn(List.of());
    KpiAdapter adapter = adapter(mock(KpiConfig.class), twins);

    Collection<KpiEvaluator.TwinInfo> result = invokeTwins(adapter);

    assertTrue(result.isEmpty());
  }

  @ParameterizedTest
  @EnumSource(TwinType.class)
  void twinSnapshotPreservesIdentityTypeAndLastSeen(TwinType type) throws Exception {
    Instant lastSeen = Instant.parse("2026-10-03T10:15:30Z");
    EntityTwin twin = mock(EntityTwin.class);
    when(twin.getTwinId()).thenReturn("twin-" + type.name());
    when(twin.getTwinType()).thenReturn(type);
    when(twin.getLastSeenAt()).thenReturn(lastSeen);
    TwinManager twins = mock(TwinManager.class);
    when(twins.listTwins()).thenReturn(List.of(twin));
    KpiAdapter adapter = adapter(mock(KpiConfig.class), twins);

    KpiEvaluator.TwinInfo info = invokeTwins(adapter).iterator().next();

    assertEquals("twin-" + type.name(), info.uid());
    assertEquals(type, info.twinType());
    assertEquals(lastSeen, info.lastSeenAt());
  }

  @Test
  void twinSnapshotPreservesNullLastSeen() throws Exception {
    EntityTwin twin = mock(EntityTwin.class);
    when(twin.getTwinId()).thenReturn("new");
    when(twin.getTwinType()).thenReturn(TwinType.DRONE);
    when(twin.getLastSeenAt()).thenReturn(null);
    TwinManager twins = mock(TwinManager.class);
    when(twins.listTwins()).thenReturn(List.of(twin));
    KpiAdapter adapter = adapter(mock(KpiConfig.class), twins);

    KpiEvaluator.TwinInfo info = invokeTwins(adapter).iterator().next();

    assertNull(info.lastSeenAt());
  }

  @Test
  void twinSnapshotIncludesEveryTwinReturnedByManager() throws Exception {
    TwinManager twins = mock(TwinManager.class);
    List<EntityTwin> source = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      EntityTwin twin = mock(EntityTwin.class);
      when(twin.getTwinId()).thenReturn("twin-" + i);
      when(twin.getTwinType()).thenReturn(TwinType.DRONE);
      when(twin.getLastSeenAt()).thenReturn(Instant.ofEpochSecond(i));
      source.add(twin);
    }
    when(twins.listTwins()).thenReturn(source);
    KpiAdapter adapter = adapter(mock(KpiConfig.class), twins);

    Collection<KpiEvaluator.TwinInfo> result = invokeTwins(adapter);

    assertEquals(5, result.size());
    assertEquals(
        Set.of("twin-0", "twin-1", "twin-2", "twin-3", "twin-4"),
        result.stream().map(KpiEvaluator.TwinInfo::uid).collect(java.util.stream.Collectors.toSet()));
  }

  @Test
  void tickEvaluatesAndChecksForDiscoveredBeans() throws Exception {
    KpiEvaluator evaluator = evaluatorWithNoDiscoveries();
    KpiAdapter adapter = adapter(mock(KpiConfig.class), mock(TwinManager.class));
    setField(adapter, "evaluator", evaluator);

    invoke(adapter, "tick");

    verify(evaluator).evaluate();
    verify(evaluator).getFeedMonitor();
    verify(evaluator).getRuns();
  }

  @Test
  void tickContainsEvaluationFailureAndSkipsDiscovery() throws Exception {
    KpiEvaluator evaluator = mock(KpiEvaluator.class);
    doThrow(new IllegalStateException("evaluate")).when(evaluator).evaluate();
    KpiAdapter adapter = adapter(mock(KpiConfig.class), mock(TwinManager.class));
    setField(adapter, "evaluator", evaluator);

    assertDoesNotThrow(() -> invoke(adapter, "tick"));

    verify(evaluator).evaluate();
    verify(evaluator, never()).getFeedMonitor();
    verify(evaluator, never()).getRuns();
  }

  @Test
  void discoveredFeedIsRegisteredOnce() throws Exception {
    FeedOutageMonitor.FeedStats feed = mock(FeedOutageMonitor.FeedStats.class);
    when(feed.getFeed()).thenReturn("mavlink:vehicle-1");
    FeedOutageMonitor monitor = mock(FeedOutageMonitor.class);
    when(monitor.getFeeds()).thenReturn(List.of(feed));
    KpiEvaluator evaluator = mock(KpiEvaluator.class);
    when(evaluator.getFeedMonitor()).thenReturn(monitor);
    when(evaluator.getClock()).thenReturn(Clock.systemUTC());
    FaultRunTracker runs = mock(FaultRunTracker.class);
    when(runs.getStats()).thenReturn(Map.of());
    when(evaluator.getRuns()).thenReturn(runs);

    KpiAdapter adapter = adapter(mock(KpiConfig.class), mock(TwinManager.class));
    setField(adapter, "evaluator", evaluator);

    try (MockedConstruction<FeedOutageJMX> constructed = mockConstruction(FeedOutageJMX.class)) {
      invoke(adapter, "registerDiscoveredBeans");
      invoke(adapter, "registerDiscoveredBeans");

      assertEquals(1, constructed.constructed().size());
      assertEquals(Set.of("mavlink:vehicle-1"), setFieldValue(adapter, "registeredFeeds"));
      assertEquals(1, beanList(adapter).size());
    }
  }

  @Test
  void multipleNewFeedsAreEachRegistered() throws Exception {
    FeedOutageMonitor.FeedStats first = mock(FeedOutageMonitor.FeedStats.class);
    FeedOutageMonitor.FeedStats second = mock(FeedOutageMonitor.FeedStats.class);
    when(first.getFeed()).thenReturn("mavlink:a");
    when(second.getFeed()).thenReturn("cot:b");
    FeedOutageMonitor monitor = mock(FeedOutageMonitor.class);
    when(monitor.getFeeds()).thenReturn(List.of(first, second));
    KpiEvaluator evaluator = mock(KpiEvaluator.class);
    when(evaluator.getFeedMonitor()).thenReturn(monitor);
    when(evaluator.getClock()).thenReturn(Clock.systemUTC());
    FaultRunTracker runs = mock(FaultRunTracker.class);
    when(runs.getStats()).thenReturn(Map.of());
    when(evaluator.getRuns()).thenReturn(runs);

    KpiAdapter adapter = adapter(mock(KpiConfig.class), mock(TwinManager.class));
    setField(adapter, "evaluator", evaluator);

    try (MockedConstruction<FeedOutageJMX> constructed = mockConstruction(FeedOutageJMX.class)) {
      invoke(adapter, "registerDiscoveredBeans");

      assertEquals(2, constructed.constructed().size());
      assertEquals(Set.of("mavlink:a", "cot:b"), setFieldValue(adapter, "registeredFeeds"));
    }
  }

  @Test
  void discoveredFailureTypeCreatesRunAndStageBeansOnce() throws Exception {
    FaultRunTracker runs = mock(FaultRunTracker.class);
    FaultRunTracker.RunTypeStats stats = mock(FaultRunTracker.RunTypeStats.class);
    when(runs.getStats()).thenReturn(Map.of("CUSTOM_FAILURE", stats));
    when(runs.statsFor("CUSTOM_FAILURE")).thenReturn(stats);
    for (FaultRunTracker.Stage stage : FaultRunTracker.Stage.values()) {
      when(stats.stage(stage)).thenReturn(mock(DurationStats.class));
    }
    FeedOutageMonitor monitor = mock(FeedOutageMonitor.class);
    when(monitor.getFeeds()).thenReturn(List.of());
    KpiEvaluator evaluator = mock(KpiEvaluator.class);
    when(evaluator.getFeedMonitor()).thenReturn(monitor);
    when(evaluator.getRuns()).thenReturn(runs);
    KpiAdapter adapter = adapter(mock(KpiConfig.class), mock(TwinManager.class));
    setField(adapter, "evaluator", evaluator);

    try (MockedConstruction<FaultRunsJMX> runBeans = mockConstruction(FaultRunsJMX.class);
         MockedConstruction<DurationJMX> durationBeans = mockConstruction(DurationJMX.class)) {
      invoke(adapter, "registerDiscoveredBeans");
      invoke(adapter, "registerDiscoveredBeans");

      assertEquals(1, runBeans.constructed().size());
      assertEquals(FaultRunTracker.Stage.values().length, durationBeans.constructed().size());
      assertEquals(Set.of("CUSTOM_FAILURE"), setFieldValue(adapter, "registeredFailureTypes"));
      assertEquals(1 + FaultRunTracker.Stage.values().length, beanList(adapter).size());
    }
  }

  @Test
  void alreadyRegisteredFailureTypeDoesNotCreateBeans() throws Exception {
    FaultRunTracker runs = mock(FaultRunTracker.class);
    FaultRunTracker.RunTypeStats stats = mock(FaultRunTracker.RunTypeStats.class);
    when(runs.getStats()).thenReturn(Map.of("KNOWN", stats));
    FeedOutageMonitor monitor = mock(FeedOutageMonitor.class);
    when(monitor.getFeeds()).thenReturn(List.of());
    KpiEvaluator evaluator = mock(KpiEvaluator.class);
    when(evaluator.getFeedMonitor()).thenReturn(monitor);
    when(evaluator.getRuns()).thenReturn(runs);
    KpiAdapter adapter = adapter(mock(KpiConfig.class), mock(TwinManager.class));
    setField(adapter, "evaluator", evaluator);
    setFieldValue(adapter, "registeredFailureTypes").add("KNOWN");

    try (MockedConstruction<FaultRunsJMX> runBeans = mockConstruction(FaultRunsJMX.class);
         MockedConstruction<DurationJMX> durationBeans = mockConstruction(DurationJMX.class)) {
      invoke(adapter, "registerDiscoveredBeans");

      assertTrue(runBeans.constructed().isEmpty());
      assertTrue(durationBeans.constructed().isEmpty());
      assertTrue(beanList(adapter).isEmpty());
    }
  }

  @Test
  void stopWithoutStartedResourcesIsIdempotent() {
    KpiAdapter adapter = adapter(mock(KpiConfig.class), mock(TwinManager.class));

    assertDoesNotThrow(adapter::stop);
    assertDoesNotThrow(adapter::stop);
  }

  @Test
  void stopShutsDownSchedulerAndClearsField() throws Exception {
    KpiAdapter adapter = adapter(mock(KpiConfig.class), mock(TwinManager.class));
    ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
    setField(adapter, "scheduler", scheduler);

    adapter.stop();

    verify(scheduler).shutdownNow();
    assertNull(getField(adapter, "scheduler"));
  }

  @Test
  void stopStopsEvaluatorAndRemovesStatusListener() throws Exception {
    KpiAdapter adapter = adapter(mock(KpiConfig.class), mock(TwinManager.class));
    KpiEvaluator evaluator = mock(KpiEvaluator.class);
    setField(adapter, "evaluator", evaluator);

    try (MockedStatic<MtiStatusRegistry> registry = mockStatic(MtiStatusRegistry.class)) {
      adapter.stop();

      registry.verify(() -> MtiStatusRegistry.removeStatusListener(evaluator));
      verify(evaluator).stop();
    }
  }

  @Test
  void stopClosesBeansAndClearsDiscoverySets() throws Exception {
    KpiAdapter adapter = adapter(mock(KpiConfig.class), mock(TwinManager.class));
    KpiBean first = mock(KpiBean.class);
    KpiBean second = mock(KpiBean.class);
    beanList(adapter).add(first);
    beanList(adapter).add(second);
    setFieldValue(adapter, "registeredFeeds").add("feed");
    setFieldValue(adapter, "registeredFailureTypes").add("failure");

    adapter.stop();

    verify(first).close();
    verify(second).close();
    assertTrue(beanList(adapter).isEmpty());
    assertTrue(setFieldValue(adapter, "registeredFeeds").isEmpty());
    assertTrue(setFieldValue(adapter, "registeredFailureTypes").isEmpty());
  }

  @Test
  void stopClosesSessionAndClearsField() throws Exception {
    KpiAdapter adapter = adapter(mock(KpiConfig.class), mock(TwinManager.class));
    Session session = mock(Session.class);
    setField(adapter, "session", session);
    SessionManager manager = mock(SessionManager.class);

    try (MockedStatic<SessionManager> managers = mockStatic(SessionManager.class)) {
      managers.when(SessionManager::getInstance).thenReturn(manager);

      adapter.stop();

      verify(manager).close(session, false);
      assertNull(getField(adapter, "session"));
    }
  }

  @Test
  void stopContainsSessionCloseFailureAndStillClearsField() throws Exception {
    KpiAdapter adapter = adapter(mock(KpiConfig.class), mock(TwinManager.class));
    Session session = mock(Session.class);
    setField(adapter, "session", session);
    SessionManager manager = mock(SessionManager.class);
    doThrow(new IOException("close")).when(manager).close(session, false);

    try (MockedStatic<SessionManager> managers = mockStatic(SessionManager.class)) {
      managers.when(SessionManager::getInstance).thenReturn(manager);

      assertDoesNotThrow(adapter::stop);

      assertNull(getField(adapter, "session"));
    }
  }

  @Test
  void stopClosesEventLog() throws Exception {
    KpiAdapter adapter = adapter(mock(KpiConfig.class), mock(TwinManager.class));
    KpiEventLog eventLog = mock(KpiEventLog.class);
    setField(adapter, "eventLog", eventLog);

    adapter.stop();

    verify(eventLog).close();
  }

  @Test
  void startContainsEarlyConfigurationFailure() {
    KpiConfig config = mock(KpiConfig.class);
    when(config.getDataDirectory()).thenThrow(new IllegalStateException("configuration"));
    KpiAdapter adapter = adapter(config, mock(TwinManager.class));

    assertDoesNotThrow(adapter::start);

    assertNull(getFieldUnchecked(adapter, "scheduler"));
    assertNull(getFieldUnchecked(adapter, "evaluator"));
  }

  @Test
  void startContainsEvaluatorStartFailure() {
    KpiConfig config = startConfig();
    KpiAdapter adapter = adapter(config, mock(TwinManager.class));

    try (MockedConstruction<KpiEventLog> eventLogs = mockConstruction(KpiEventLog.class);
         MockedConstruction<KpiStateStore> stores = mockConstruction(KpiStateStore.class);
         MockedConstruction<KpiEvaluator> evaluators = mockConstruction(
             KpiEvaluator.class,
             (mock, context) -> doThrow(new IllegalStateException("start")).when(mock).start())) {

      assertDoesNotThrow(adapter::start);

      assertEquals(1, evaluators.constructed().size());
      assertNull(getFieldUnchecked(adapter, "scheduler"));
    }
  }

  @Test
  void successfulStartBuildsEvaluatorBeansSubscriptionAndScheduler() throws Exception {
    KpiConfig config = startConfig();
    KpiAdapter adapter = adapter(config, mock(TwinManager.class));
    SessionManager manager = mock(SessionManager.class);
    Session session = mock(Session.class);
    when(manager.create(any(), same(adapter))).thenReturn(session);

    FaultRunTracker runs = mock(FaultRunTracker.class);
    FaultRunTracker.RunTypeStats runStats = mock(FaultRunTracker.RunTypeStats.class);
    when(runs.statsFor(anyString())).thenReturn(runStats);
    for (FaultRunTracker.Stage stage : FaultRunTracker.Stage.values()) {
      when(runStats.stage(stage)).thenReturn(mock(DurationStats.class));
    }
    CacheRebuildTracker rebuilds = mock(CacheRebuildTracker.class);
    when(rebuilds.stats(any(), any())).thenReturn(mock(DurationStats.class));
    FeedOutageMonitor monitor = mock(FeedOutageMonitor.class);
    when(monitor.getFeeds()).thenReturn(List.of());

    try (MockedStatic<SessionManager> managers = mockStatic(SessionManager.class);
         MockedStatic<MtiStatusRegistry> registry = mockStatic(MtiStatusRegistry.class);
         MockedConstruction<KpiEventLog> eventLogs = mockConstruction(KpiEventLog.class);
         MockedConstruction<KpiStateStore> stores = mockConstruction(KpiStateStore.class);
         MockedConstruction<KpiEvaluator> evaluators = mockConstruction(
             KpiEvaluator.class,
             (mock, context) -> {
               when(mock.getRuns()).thenReturn(runs);
               when(mock.getRebuilds()).thenReturn(rebuilds);
               when(mock.getFeedMonitor()).thenReturn(monitor);
               when(mock.getClock()).thenReturn(Clock.systemUTC());
             });
         MockedConstruction<KpiJMX> kpiBeans = mockConstruction(KpiJMX.class);
         MockedConstruction<KpiPictureJMX> pictureBeans = mockConstruction(KpiPictureJMX.class);
         MockedConstruction<DurationJMX> durationBeans = mockConstruction(DurationJMX.class);
         MockedConstruction<FaultRunsJMX> faultBeans = mockConstruction(FaultRunsJMX.class)) {

      managers.when(SessionManager::getInstance).thenReturn(manager);

      adapter.start();

      KpiEvaluator evaluator = evaluators.constructed().get(0);
      verify(evaluator).start();
      registry.verify(() -> MtiStatusRegistry.addStatusListener(evaluator));
      verify(manager).create(any(), same(adapter));
      verify(session).addSubscription(any());
      assertNotNull(getField(adapter, "scheduler"));
      assertEquals(KpiId.values().length, kpiBeans.constructed().size());
      assertEquals(1, pictureBeans.constructed().size());
      assertFalse(durationBeans.constructed().isEmpty());
      assertFalse(faultBeans.constructed().isEmpty());

      adapter.stop();

      verify(evaluator).stop();
      verify(manager).close(session, false);
      assertNull(getField(adapter, "scheduler"));
    }
  }

  @Test
  void subscriptionFailureIsContainedAndSchedulerIsNotStarted() throws Exception {
    KpiConfig config = startConfig();
    KpiAdapter adapter = adapter(config, mock(TwinManager.class));
    SessionManager manager = mock(SessionManager.class);
    when(manager.create(any(), same(adapter))).thenThrow(new IOException("session"));

    FaultRunTracker runs = mock(FaultRunTracker.class);
    FaultRunTracker.RunTypeStats runStats = mock(FaultRunTracker.RunTypeStats.class);
    when(runs.statsFor(anyString())).thenReturn(runStats);
    for (FaultRunTracker.Stage stage : FaultRunTracker.Stage.values()) {
      when(runStats.stage(stage)).thenReturn(mock(DurationStats.class));
    }
    CacheRebuildTracker rebuilds = mock(CacheRebuildTracker.class);
    when(rebuilds.stats(any(), any())).thenReturn(mock(DurationStats.class));

    try (MockedStatic<SessionManager> managers = mockStatic(SessionManager.class);
         MockedStatic<MtiStatusRegistry> registry = mockStatic(MtiStatusRegistry.class);
         MockedConstruction<KpiEventLog> eventLogs = mockConstruction(KpiEventLog.class);
         MockedConstruction<KpiStateStore> stores = mockConstruction(KpiStateStore.class);
         MockedConstruction<KpiEvaluator> evaluators = mockConstruction(
             KpiEvaluator.class,
             (mock, context) -> {
               when(mock.getRuns()).thenReturn(runs);
               when(mock.getRebuilds()).thenReturn(rebuilds);
             });
         MockedConstruction<KpiJMX> kpiBeans = mockConstruction(KpiJMX.class);
         MockedConstruction<KpiPictureJMX> pictureBeans = mockConstruction(KpiPictureJMX.class);
         MockedConstruction<DurationJMX> durationBeans = mockConstruction(DurationJMX.class);
         MockedConstruction<FaultRunsJMX> faultBeans = mockConstruction(FaultRunsJMX.class)) {

      managers.when(SessionManager::getInstance).thenReturn(manager);

      assertDoesNotThrow(adapter::start);

      assertNull(getField(adapter, "scheduler"));
      assertNull(getField(adapter, "session"));
      assertEquals(1, evaluators.constructed().size());
      assertFalse(kpiBeans.constructed().isEmpty());
    }
  }

  private KpiConfig startConfig() {
    KpiConfig config = mock(KpiConfig.class);
    when(config.getDataDirectory()).thenReturn(tempDir);
    when(config.getControlTopic()).thenReturn("/kpi/control");
    when(config.getEvaluationInterval()).thenReturn(Duration.ofHours(1));
    return config;
  }

  private KpiEvaluator evaluatorWithNoDiscoveries() {
    KpiEvaluator evaluator = mock(KpiEvaluator.class);
    FeedOutageMonitor monitor = mock(FeedOutageMonitor.class);
    when(monitor.getFeeds()).thenReturn(List.of());
    when(evaluator.getFeedMonitor()).thenReturn(monitor);
    FaultRunTracker runs = mock(FaultRunTracker.class);
    when(runs.getStats()).thenReturn(Map.of());
    when(evaluator.getRuns()).thenReturn(runs);
    return evaluator;
  }

  private MessageEvent event(byte[] payload, Runnable completion) {
    Message message = mock(Message.class);
    when(message.getOpaqueData()).thenReturn(payload);
    return new MessageEvent("/kpi/control", null, message, completion);
  }

  private KpiAdapter adapter(KpiConfig config, TwinManager twins) {
    return new KpiAdapter(config, twins);
  }

  @SuppressWarnings("unchecked")
  private Collection<KpiEvaluator.TwinInfo> invokeTwins(KpiAdapter adapter) throws Exception {
    return (Collection<KpiEvaluator.TwinInfo>) invoke(adapter, "twins");
  }

  private Object invoke(KpiAdapter adapter, String methodName) throws Exception {
    Method method = KpiAdapter.class.getDeclaredMethod(methodName);
    method.setAccessible(true);
    return method.invoke(adapter);
  }

  private void setField(KpiAdapter adapter, String name, Object value) throws Exception {
    Field field = KpiAdapter.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(adapter, value);
  }

  private Object getField(KpiAdapter adapter, String name) throws Exception {
    Field field = KpiAdapter.class.getDeclaredField(name);
    field.setAccessible(true);
    return field.get(adapter);
  }

  private Object getFieldUnchecked(KpiAdapter adapter, String name) {
    try {
      return getField(adapter, name);
    } catch (ReflectiveOperationException e) {
      throw new AssertionError(e);
    }
  }

  @SuppressWarnings("unchecked")
  private List<KpiBean> beanList(KpiAdapter adapter) throws Exception {
    return (List<KpiBean>) getField(adapter, "beans");
  }

  @SuppressWarnings("unchecked")
  private Set<String> setFieldValue(KpiAdapter adapter, String name) throws Exception {
    return (Set<String>) getField(adapter, name);
  }
}
