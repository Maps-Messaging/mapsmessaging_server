/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 */

package io.mapsmessaging.state.drone.tak;

import io.mapsmessaging.state.config.CotConfigDTO;
import io.mapsmessaging.state.drone.core.EntityTwin;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.model.BatteryState;
import io.mapsmessaging.state.drone.model.GeoPosition;
import io.mapsmessaging.utilities.admin.JMXManager;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TakTwinObserverResolutionCoverageTest {

  @ParameterizedTest
  @MethodSource("batteryStatsCases")
  void batteryStatsCoverFiniteClampRoundingAndCharging(
      Double percentage,
      Double temperature,
      Boolean charging,
      String expected) throws Exception {

    withObserver(observer -> {
      BatteryState state = new BatteryState();
      state.setPercentage(percentage);
      state.setTemperatureCelsius(temperature);
      state.setCharging(charging);

      assertEquals(expected, invoke(observer, "buildStats",
          new Class<?>[]{BatteryState.class}, state));
    });
  }

  @Test
  void statsReturnNullForNullOrEmptyBatteryState() throws Exception {
    withObserver(observer -> {
      assertNull(invoke(observer, "buildStats",
          new Class<?>[]{BatteryState.class}, new Object[]{null}));
      assertNull(invoke(observer, "buildStats",
          new Class<?>[]{BatteryState.class}, new BatteryState()));
    });
  }

  @ParameterizedTest
  @MethodSource("unusableStatsInputs")
  void appendStatsLeavesUnusableInputsUntouched(String xml) throws Exception {
    withObserver(observer -> {
      DroneTwin twin = new DroneTwin("asset");
      BatteryState battery = new BatteryState();
      battery.setPercentage(50.0);
      twin.setBatteryState(battery);

      assertEquals(xml, invoke(observer, "appendStatsIfDue",
          new Class<?>[]{EntityTwin.class, String.class}, twin, xml));
    });
  }

  @Test
  void appendStatsRequiresBatteryDataAndDetailElement() throws Exception {
    withObserver(observer -> {
      DroneTwin twin = new DroneTwin("asset");

      String xml = "<event><detail/></event>";
      assertEquals(xml, invoke(observer, "appendStatsIfDue",
          new Class<?>[]{EntityTwin.class, String.class}, twin, xml));

      BatteryState battery = new BatteryState();
      battery.setPercentage(25.0);
      twin.setBatteryState(battery);
      assertEquals("<event/>", invoke(observer, "appendStatsIfDue",
          new Class<?>[]{EntityTwin.class, String.class}, twin, "<event/>"));
    });
  }

  @Test
  void appendStatsPublishesOnceThenRateLimitsUntilIntervalExpires() throws Exception {
    withObserver(observer -> {
      DroneTwin twin = new DroneTwin("asset");
      BatteryState battery = new BatteryState();
      battery.setPercentage(25.4);
      twin.setBatteryState(battery);
      String xml = "<event><detail><contact/></detail></event>";

      String first = invoke(observer, "appendStatsIfDue",
          new Class<?>[]{EntityTwin.class, String.class}, twin, xml);
      assertEquals(
          "<event><detail><contact/><stats battery=\"25\"/></detail></event>",
          first);

      String second = invoke(observer, "appendStatsIfDue",
          new Class<?>[]{EntityTwin.class, String.class}, twin, xml);
      assertEquals(xml, second);

      @SuppressWarnings("unchecked")
      Map<String, Long> times = (Map<String, Long>) field(observer, "lastStatsPublishTimes");
      times.put("asset", System.currentTimeMillis() - 31_000L);

      String third = invoke(observer, "appendStatsIfDue",
          new Class<?>[]{EntityTwin.class, String.class}, twin, xml);
      assertEquals(first, third);
    });
  }

  @Test
  void sourceNamespaceMatchIsCachedInTwinContext() throws Exception {
    withObserver(observer -> {
      CotConfigResolver resolver = mock(CotConfigResolver.class);
      CotConfigDTO match = new CotConfigDTO();
      when(resolver.resolve("/source/one")).thenReturn(match);
      setField(observer, "cotConfigResolver", resolver);

      TakTwinContext twinContext = new TakTwinContext();
      TwinUpdateContext context = new TwinUpdateContext();
      context.setSourceNamespace("/source/one");
      context.setUpdateSource("fallback");

      assertSame(match, resolve(observer, context, twinContext));
      assertSame(match, twinContext.getCotConfig());
      verify(resolver).resolve("/source/one");
      verify(resolver, never()).resolve("fallback");
    });
  }

  @Test
  void sourceNamespaceMissDoesNotFallBackToCachedOrUpdateSource() throws Exception {
    withObserver(observer -> {
      CotConfigResolver resolver = mock(CotConfigResolver.class);
      CotConfigDTO cached = new CotConfigDTO();
      setField(observer, "cotConfigResolver", resolver);

      TakTwinContext twinContext = new TakTwinContext();
      twinContext.setCotConfig(cached);
      TwinUpdateContext context = new TwinUpdateContext();
      context.setSourceNamespace("/unknown");
      context.setUpdateSource("fallback");

      assertNull(resolve(observer, context, twinContext));
      assertSame(cached, twinContext.getCotConfig());
      verify(resolver).resolve("/unknown");
      verify(resolver, never()).resolve("fallback");
    });
  }

  @Test
  void cachedCotConfigWinsWhenNoSourceNamespaceIsPresent() throws Exception {
    withObserver(observer -> {
      CotConfigResolver resolver = mock(CotConfigResolver.class);
      CotConfigDTO cached = new CotConfigDTO();
      setField(observer, "cotConfigResolver", resolver);

      TakTwinContext twinContext = new TakTwinContext();
      twinContext.setCotConfig(cached);
      TwinUpdateContext context = new TwinUpdateContext();
      context.setUpdateSource("fallback");

      assertSame(cached, resolve(observer, context, twinContext));
      verifyNoInteractions(resolver);
    });
  }

  @Test
  void updateSourceMatchIsCachedWhenNoNamespaceOrCachedConfigExists() throws Exception {
    withObserver(observer -> {
      CotConfigResolver resolver = mock(CotConfigResolver.class);
      CotConfigDTO match = new CotConfigDTO();
      when(resolver.resolve("mavlink-updater")).thenReturn(match);
      setField(observer, "cotConfigResolver", resolver);

      TakTwinContext twinContext = new TakTwinContext();
      TwinUpdateContext context = new TwinUpdateContext();
      context.setUpdateSource("mavlink-updater");

      assertSame(match, resolve(observer, context, twinContext));
      assertSame(match, twinContext.getCotConfig());
    });
  }

  @ParameterizedTest
  @MethodSource("emptyUpdateSourceContexts")
  void emptyUpdateSourceCannotResolveCotConfig(TwinUpdateContext context) throws Exception {
    withObserver(observer -> {
      CotConfigResolver resolver = mock(CotConfigResolver.class);
      setField(observer, "cotConfigResolver", resolver);

      assertNull(resolve(observer, context, new TakTwinContext()));
      verifyNoInteractions(resolver);
    });
  }

  @ParameterizedTest
  @MethodSource("receivedTimeCases")
  void receivedTimeHandlesNullAndPresentContexts(TwinUpdateContext context, Instant expected)
      throws Exception {
    Method method = TakTwinObserver.class.getDeclaredMethod("receivedTime", TwinUpdateContext.class);
    method.setAccessible(true);

    assertEquals(expected, method.invoke(null, context));
  }

  @Test
  void updateWithExplicitIdCreatesContextAndImmediateRepeatIsRateLimited() throws Exception {
    withObserver(observer -> {
      EventPublisher publisher = mock(EventPublisher.class);
      setField(observer, "eventPublisher", publisher);

      DroneTwin twin = positionedTwin("payload-id");
      TwinUpdateContext context = new TwinUpdateContext();
      context.setReceivedTime(Instant.now());

      observer.onTwinUpdated("explicit-id", twin, context);
      observer.onTwinUpdated("explicit-id", twin, context);

      @SuppressWarnings("unchecked")
      Map<String, TakTwinContext> contexts =
          (Map<String, TakTwinContext>) field(observer, "takContexts");
      assertTrue(contexts.containsKey("explicit-id"));
      verify(publisher, times(1)).publish(anyString());
    });
  }

  @Test
  void blankIdsAreIgnoredWithoutCreatingContext() throws Exception {
    withObserver(observer -> {
      DroneTwin twin = mock(DroneTwin.class);
      when(twin.getTwinId()).thenReturn(" ");

      observer.onTwinUpdated(null, twin, new TwinUpdateContext());
      observer.onTwinUpdated("", twin, new TwinUpdateContext());

      @SuppressWarnings("unchecked")
      Map<String, TakTwinContext> contexts =
          (Map<String, TakTwinContext>) field(observer, "takContexts");
      assertTrue(contexts.isEmpty());
    });
  }

  private static Stream<Arguments> batteryStatsCases() {
    return Stream.of(
        Arguments.of(-10.0, null, null, "<stats battery=\"0\"/>"),
        Arguments.of(0.4, null, null, "<stats battery=\"0\"/>"),
        Arguments.of(49.5, null, null, "<stats battery=\"50\"/>"),
        Arguments.of(100.4, null, null, "<stats battery=\"100\"/>"),
        Arguments.of(150.0, null, null, "<stats battery=\"100\"/>"),
        Arguments.of(Double.NaN, 20.6, null, "<stats battery_temp=\"21\"/>"),
        Arguments.of(Double.POSITIVE_INFINITY, -5.6, null, "<stats battery_temp=\"-6\"/>"),
        Arguments.of(null, Double.NEGATIVE_INFINITY, true, "<stats battery_status=\"Charging\"/>"),
        Arguments.of(null, null, false, "<stats battery_status=\"Discharging\"/>"),
        Arguments.of(78.6, 34.4, true,
            "<stats battery=\"79\" battery_temp=\"34\" battery_status=\"Charging\"/>")
    );
  }

  private static Stream<Arguments> unusableStatsInputs() {
    return Stream.of(
        Arguments.of((Object) null),
        Arguments.of(""),
        Arguments.of(" ")
    );
  }

  private static Stream<Arguments> emptyUpdateSourceContexts() {
    TwinUpdateContext blank = new TwinUpdateContext();
    blank.setUpdateSource(" ");
    TwinUpdateContext empty = new TwinUpdateContext();
    empty.setUpdateSource("");
    return Stream.of(
        Arguments.of((Object) null),
        Arguments.of(new TwinUpdateContext()),
        Arguments.of(blank),
        Arguments.of(empty)
    );
  }

  private static Stream<Arguments> receivedTimeCases() {
    Instant timestamp = Instant.parse("2026-10-03T01:02:03Z");
    TwinUpdateContext present = new TwinUpdateContext();
    present.setReceivedTime(timestamp);
    TwinUpdateContext absent = new TwinUpdateContext();
    return Stream.of(
        Arguments.of((Object) null, null),
        Arguments.of(absent, null),
        Arguments.of(present, timestamp)
    );
  }

  private static DroneTwin positionedTwin(String id) {
    DroneTwin twin = new DroneTwin(id);
    twin.setGeoPosition(new GeoPosition(38.4, -9.1, 125.0, null, null));
    twin.setLastSeenAt(Instant.now());
    return twin;
  }

  private static CotConfigDTO resolve(
      TakTwinObserver observer, TwinUpdateContext context, TakTwinContext twinContext)
      throws Exception {
    return invoke(observer, "resolveCotConfig",
        new Class<?>[]{TwinUpdateContext.class, TakTwinContext.class}, context, twinContext);
  }

  @SuppressWarnings("unchecked")
  private static <T> T invoke(
      Object target, String name, Class<?>[] parameterTypes, Object... arguments) throws Exception {
    Method method = TakTwinObserver.class.getDeclaredMethod(name, parameterTypes);
    method.setAccessible(true);
    return (T) method.invoke(target, arguments);
  }

  private static Object field(Object target, String name) throws Exception {
    Field field = target.getClass().getDeclaredField(name);
    field.setAccessible(true);
    return field.get(target);
  }

  private static void setField(Object target, String name, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }

  private static void withObserver(ObserverAction action) throws Exception {
    boolean enabled = JMXManager.isEnableJMX();
    JMXManager.setEnableJMX(false);
    ConfigurationManager configurationManager = mock(ConfigurationManager.class);

    try (MockedStatic<ConfigurationManager> mocked = mockStatic(ConfigurationManager.class)) {
      mocked.when(ConfigurationManager::getInstance).thenReturn(configurationManager);
      TakTwinObserver observer = new TakTwinObserver(new TwinManager());
      try {
        action.run(observer);
      } finally {
        observer.shutdown();
      }
    } finally {
      JMXManager.setEnableJMX(enabled);
    }
  }

  private interface ObserverAction {
    void run(TakTwinObserver observer) throws Exception;
  }
}
