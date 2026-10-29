/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *
 */

package io.mapsmessaging.state.drone.tak;

import io.mapsmessaging.state.config.CotConfigDTO;
import io.mapsmessaging.state.drone.core.TwinLifecycleStatus;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.model.DetectionEvent;
import io.mapsmessaging.state.drone.model.GeoPosition;
import io.mapsmessaging.state.drone.tak.model.TakEvent;
import io.mapsmessaging.utilities.admin.JMXManager;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import java.io.IOException;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TakTwinObserverLifecycleCoverageTest {

  @ParameterizedTest
  @MethodSource("invalidDetectionSources")
  void detectionRejectsSourcesWithoutUsableIdentity(String twinId) throws Exception {
    withObserver(observer -> {
      TakEventMapper mapper = mock(TakEventMapper.class);
      DroneTwin source = mock(DroneTwin.class);
      when(source.getTwinId()).thenReturn(twinId);
      setField(observer, "takEventMapper", mapper);

      observer.onDetectionEvent(source, mock(DetectionEvent.class), context());

      verifyNoInteractions(mapper);
      assertTrue(contexts(observer).isEmpty());
    });
  }

  @Test
  void detectionMapperNullStopsBeforeSerialisationAndOutput() throws Exception {
    withObserver(observer -> {
      TakEventMapper mapper = mock(TakEventMapper.class);
      TakXmlSerialiser serialiser = mock(TakXmlSerialiser.class);
      EventPublisher publisher = mock(EventPublisher.class);
      DetectionEvent detection = mock(DetectionEvent.class);
      DroneTwin source = new DroneTwin("asset");
      TwinUpdateContext updateContext = context();
      when(mapper.mapDetection(source, detection, updateContext)).thenReturn(null);
      setField(observer, "takEventMapper", mapper);
      setField(observer, "takXmlSerialiser", serialiser);
      setField(observer, "eventPublisher", publisher);

      observer.onDetectionEvent(source, detection, updateContext);

      verify(mapper).mapDetection(source, detection, updateContext);
      verifyNoInteractions(serialiser, publisher);
    });
  }

  @Test
  void detectionSuccessPublishesSerialisedEvent() throws Exception {
    withObserver(observer -> {
      TakEventMapper mapper = mock(TakEventMapper.class);
      TakXmlSerialiser serialiser = mock(TakXmlSerialiser.class);
      EventPublisher publisher = mock(EventPublisher.class);
      DetectionEvent detection = mock(DetectionEvent.class);
      DroneTwin source = new DroneTwin("asset");
      TakEvent event = new TakEvent();
      TwinUpdateContext updateContext = context();
      when(mapper.mapDetection(source, detection, updateContext)).thenReturn(event);
      when(serialiser.toXml(event)).thenReturn("<event/>");
      setField(observer, "takEventMapper", mapper);
      setField(observer, "takXmlSerialiser", serialiser);
      setField(observer, "eventPublisher", publisher);

      observer.onDetectionEvent(source, detection, updateContext);

      verify(serialiser).toXml(event);
      verify(publisher).publish("<event/>");
    });
  }

  @Test
  void detectionPublisherFailureIsContained() throws Exception {
    withObserver(observer -> {
      TakEventMapper mapper = mock(TakEventMapper.class);
      TakXmlSerialiser serialiser = mock(TakXmlSerialiser.class);
      EventPublisher publisher = mock(EventPublisher.class);
      DetectionEvent detection = mock(DetectionEvent.class);
      DroneTwin source = new DroneTwin("asset");
      TakEvent event = new TakEvent();
      TwinUpdateContext updateContext = context();
      when(mapper.mapDetection(source, detection, updateContext)).thenReturn(event);
      when(serialiser.toXml(event)).thenReturn("<event/>");
      doThrow(new IOException("expected")).when(publisher).publish("<event/>");
      setField(observer, "takEventMapper", mapper);
      setField(observer, "takXmlSerialiser", serialiser);
      setField(observer, "eventPublisher", publisher);

      assertDoesNotThrow(() -> observer.onDetectionEvent(source, detection, updateContext));
      verify(publisher).publish("<event/>");
    });
  }

  @Test
  void detection_without_a_publisher_keeps_the_context_for_later_updates() throws Exception {
    withObserver(observer -> {
      DroneTwin source = positionedTwin("asset");

      assertDoesNotThrow(() -> observer.onDetectionEvent(source, mock(DetectionEvent.class), context()));

      assertTrue(contexts(observer).containsKey("asset"));
    });
  }

  @Test
  void detectionNamespaceMissStopsBeforeMapping() throws Exception {
    withObserver(observer -> {
      CotConfigResolver resolver = mock(CotConfigResolver.class);
      TakEventMapper mapper = mock(TakEventMapper.class);
      TwinUpdateContext updateContext = context();
      updateContext.setSourceNamespace("/unmapped/source");
      setField(observer, "namespaceFilteringEnabled", true);
      setField(observer, "cotConfigResolver", resolver);
      setField(observer, "takEventMapper", mapper);

      observer.onDetectionEvent(new DroneTwin("asset"), mock(DetectionEvent.class), updateContext);

      verify(resolver).resolve("/unmapped/source");
      verifyNoInteractions(mapper);
    });
  }

  @Test
  void detectionNamespaceMatchCachesConfigurationAndPublishes() throws Exception {
    withObserver(observer -> {
      CotConfigResolver resolver = mock(CotConfigResolver.class);
      TakEventMapper mapper = mock(TakEventMapper.class);
      TakXmlSerialiser serialiser = mock(TakXmlSerialiser.class);
      EventPublisher publisher = mock(EventPublisher.class);
      CotConfigDTO config = new CotConfigDTO();
      DetectionEvent detection = mock(DetectionEvent.class);
      DroneTwin source = new DroneTwin("asset");
      TakEvent event = new TakEvent();
      TwinUpdateContext updateContext = context();
      updateContext.setSourceNamespace("/mapped/source");
      when(resolver.resolve("/mapped/source")).thenReturn(config);
      when(mapper.mapDetection(source, detection, updateContext)).thenReturn(event);
      when(serialiser.toXml(event)).thenReturn("<event/>");
      setField(observer, "namespaceFilteringEnabled", true);
      setField(observer, "cotConfigResolver", resolver);
      setField(observer, "takEventMapper", mapper);
      setField(observer, "takXmlSerialiser", serialiser);
      setField(observer, "eventPublisher", publisher);

      observer.onDetectionEvent(source, detection, updateContext);

      assertSame(config, contexts(observer).get("asset").getCotConfig());
      verify(publisher).publish("<event/>");
    });
  }

  @ParameterizedTest
  @MethodSource("invalidStatusIdentities")
  void statusChangeRejectsUnusableResolvedIdentity(String explicitId, String twinId) throws Exception {
    withObserver(observer -> {
      TakEventMapper mapper = mock(TakEventMapper.class);
      DroneTwin twin = mock(DroneTwin.class);
      when(twin.getTwinId()).thenReturn(twinId);
      setField(observer, "takEventMapper", mapper);

      observer.onTwinStatusChanged(
          explicitId,
          TwinLifecycleStatus.ACTIVE,
          TwinLifecycleStatus.STALE,
          twin,
          context());

      assertTrue(contexts(observer).isEmpty());
      verifyNoInteractions(mapper);
    });
  }

  @Test
  void statusChangeFallsBackToTwinIdentityAndCreatesContext() throws Exception {
    withObserver(observer -> {
      TakEventMapper mapper = mock(TakEventMapper.class);
      DroneTwin twin = positionedTwin("asset");
      setField(observer, "takEventMapper", mapper);

      observer.onTwinStatusChanged(
          "",
          TwinLifecycleStatus.DISCONNECTED,
          TwinLifecycleStatus.ACTIVE,
          twin,
          context());

      assertTrue(contexts(observer).containsKey("asset"));
      verify(mapper).map(eq(twin), any(TwinUpdateContext.class));
    });
  }

  @Test
  void removingUnknownTwinClearsStatsState() throws Exception {
    withObserver(observer -> {
      statsTimes(observer).put("asset", 123L);

      observer.onTwinRemoved(new DroneTwin("asset"), context());

      assertFalse(statsTimes(observer).containsKey("asset"));
      assertFalse(contexts(observer).containsKey("asset"));
    });
  }

  @Test
  void removing_known_twin_clears_context_and_stats() throws Exception {
    withObserver(observer -> {
      TakEventMapper mapper = mock(TakEventMapper.class);
      contexts(observer).put("asset", new TakTwinContext());
      statsTimes(observer).put("asset", 123L);
      setField(observer, "takEventMapper", mapper);

      observer.onTwinRemoved(new DroneTwin("asset"), context());

      assertFalse(contexts(observer).containsKey("asset"));
      assertFalse(statsTimes(observer).containsKey("asset"));
    });
  }

  @Test
  void removing_twin_keeps_the_shared_topic_publisher_open() throws Exception {
    withObserver(observer -> {
      EventPublisher publisher = mock(EventPublisher.class);
      setField(observer, "eventPublisher", publisher);
      contexts(observer).put("asset", new TakTwinContext());

      observer.onTwinRemoved(positionedTwin("asset"), context());

      verify(publisher).publish(anyString());
      verify(publisher, never()).close();
    });
  }

  @Test
  void removal_publishes_mapped_event_to_topic_and_clears_state() throws Exception {
    withObserver(observer -> {
      TakEventMapper mapper = mock(TakEventMapper.class);
      TakXmlSerialiser serialiser = mock(TakXmlSerialiser.class);
      EventPublisher publisher = mock(EventPublisher.class);
      DroneTwin twin = positionedTwin("asset");
      TwinUpdateContext updateContext = context();
      TakEvent event = new TakEvent();
      when(mapper.mapRemoval(twin, updateContext)).thenReturn(event);
      when(serialiser.toXml(event)).thenReturn("<remove/>");
      contexts(observer).put("asset", new TakTwinContext());
      statsTimes(observer).put("asset", 123L);
      setField(observer, "takEventMapper", mapper);
      setField(observer, "takXmlSerialiser", serialiser);
      setField(observer, "eventPublisher", publisher);

      observer.onTwinRemoved(twin, updateContext);

      verify(publisher).publish("<remove/>");
      assertFalse(contexts(observer).containsKey("asset"));
      assertFalse(statsTimes(observer).containsKey("asset"));
    });
  }

  @Test
  void removal_mapper_null_skips_topic_publish_but_still_clears_state() throws Exception {
    withObserver(observer -> {
      TakEventMapper mapper = mock(TakEventMapper.class);
      EventPublisher publisher = mock(EventPublisher.class);
      contexts(observer).put("asset", new TakTwinContext());
      setField(observer, "takEventMapper", mapper);
      setField(observer, "eventPublisher", publisher);

      observer.onTwinRemoved(positionedTwin("asset"), context());

      verifyNoInteractions(publisher);
      assertFalse(contexts(observer).containsKey("asset"));
    });
  }

  @Test
  void removal_namespace_miss_skips_mapping_and_topic_publish() throws Exception {
    withObserver(observer -> {
      CotConfigResolver resolver = mock(CotConfigResolver.class);
      TakEventMapper mapper = mock(TakEventMapper.class);
      EventPublisher publisher = mock(EventPublisher.class);
      TwinUpdateContext updateContext = context();
      updateContext.setSourceNamespace("/unmapped/source");
      contexts(observer).put("asset", new TakTwinContext());
      setField(observer, "namespaceFilteringEnabled", true);
      setField(observer, "cotConfigResolver", resolver);
      setField(observer, "takEventMapper", mapper);
      setField(observer, "eventPublisher", publisher);

      observer.onTwinRemoved(positionedTwin("asset"), updateContext);

      verifyNoInteractions(mapper, publisher);
      assertFalse(contexts(observer).containsKey("asset"));
    });
  }

  @Test
  void twinPublishNamespaceMissStopsBeforeMapping() throws Exception {
    withObserver(observer -> {
      CotConfigResolver resolver = mock(CotConfigResolver.class);
      TakEventMapper mapper = mock(TakEventMapper.class);
      TwinUpdateContext updateContext = context();
      updateContext.setSourceNamespace("/unmapped/source");
      setField(observer, "namespaceFilteringEnabled", true);
      setField(observer, "cotConfigResolver", resolver);
      setField(observer, "takEventMapper", mapper);

      observer.onTwinAdded(positionedTwin("asset"), updateContext);

      verify(resolver).resolve("/unmapped/source");
      verifyNoInteractions(mapper);
    });
  }

  @Test
  void twinPublishMapperNullSkipsSerialisationAndOutput() throws Exception {
    withObserver(observer -> {
      TakEventMapper mapper = mock(TakEventMapper.class);
      TakXmlSerialiser serialiser = mock(TakXmlSerialiser.class);
      EventPublisher publisher = mock(EventPublisher.class);
      DroneTwin twin = positionedTwin("asset");
      TwinUpdateContext updateContext = context();
      when(mapper.map(twin, updateContext)).thenReturn(null);
      setField(observer, "takEventMapper", mapper);
      setField(observer, "takXmlSerialiser", serialiser);
      setField(observer, "eventPublisher", publisher);

      observer.onTwinAdded(twin, updateContext);

      verifyNoInteractions(serialiser, publisher);
    });
  }

  @Test
  void twin_publish_uses_the_topic_publisher() throws Exception {
    withObserver(observer -> {
      TakEventMapper mapper = mock(TakEventMapper.class);
      TakXmlSerialiser serialiser = mock(TakXmlSerialiser.class);
      EventPublisher publisher = mock(EventPublisher.class);
      DroneTwin twin = positionedTwin("asset");
      TwinUpdateContext updateContext = context();
      TakEvent event = new TakEvent();
      when(mapper.map(twin, updateContext)).thenReturn(event);
      when(serialiser.toXml(event)).thenReturn("<event/>");
      setField(observer, "takEventMapper", mapper);
      setField(observer, "takXmlSerialiser", serialiser);
      setField(observer, "eventPublisher", publisher);

      observer.onTwinAdded(twin, updateContext);

      verify(publisher).publish("<event/>");
    });
  }

  @Test
  void removal_publisher_failure_is_contained_and_state_is_cleared() throws Exception {
    withObserver(observer -> {
      EventPublisher publisher = mock(EventPublisher.class);
      doThrow(new IOException("expected")).when(publisher).publish(anyString());
      setField(observer, "eventPublisher", publisher);
      contexts(observer).put("asset", new TakTwinContext());
      statsTimes(observer).put("asset", 123L);

      assertDoesNotThrow(() -> observer.onTwinRemoved(positionedTwin("asset"), context()));

      verify(publisher).publish(anyString());
      assertFalse(contexts(observer).containsKey("asset"));
      assertFalse(statsTimes(observer).containsKey("asset"));
    });
  }

  @Test
  void shutdown_clears_observer_state() throws Exception {
    withObserverWithoutAutomaticShutdown(observer -> {
      contexts(observer).put("one", new TakTwinContext());
      contexts(observer).put("two", new TakTwinContext());
      statsTimes(observer).put("one", 1L);
      statsTimes(observer).put("two", 2L);

      observer.shutdown();

      assertTrue(contexts(observer).isEmpty());
      assertTrue(statsTimes(observer).isEmpty());
    });
  }

  @Test
  void shutdown_closes_the_topic_publisher() throws Exception {
    withObserverWithoutAutomaticShutdown(observer -> {
      EventPublisher publisher = mock(EventPublisher.class);
      setField(observer, "eventPublisher", publisher);

      observer.shutdown();

      verify(publisher).close();
    });
  }

  @Test
  void shutdownContainsPublisherCloseFailure() throws Exception {
    withObserverWithoutAutomaticShutdown(observer -> {
      EventPublisher publisher = mock(EventPublisher.class);
      doThrow(new IOException("expected")).when(publisher).close();
      setField(observer, "eventPublisher", publisher);

      assertDoesNotThrow(observer::shutdown);
      verify(publisher).close();
    });
  }

  private static Stream<Arguments> invalidDetectionSources() {
    return Stream.of(Arguments.of((Object) null), Arguments.of(""), Arguments.of(" "));
  }

  private static Stream<Arguments> invalidStatusIdentities() {
    return Stream.of(
        Arguments.of(null, null),
        Arguments.of(null, ""),
        Arguments.of("", " "),
        Arguments.of(" ", null));
  }

  private static DroneTwin positionedTwin(String id) {
    DroneTwin twin = new DroneTwin(id);
    twin.setGeoPosition(new GeoPosition(38.4, -9.1, 125.0, null, null));
    twin.setLastSeenAt(Instant.now());
    return twin;
  }

  private static TwinUpdateContext context() {
    TwinUpdateContext context = new TwinUpdateContext();
    context.setReceivedTime(Instant.now());
    return context;
  }

  @SuppressWarnings("unchecked")
  private static Map<String, TakTwinContext> contexts(TakTwinObserver observer) throws Exception {
    return (Map<String, TakTwinContext>) field(observer, "takContexts");
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Long> statsTimes(TakTwinObserver observer) throws Exception {
    return (Map<String, Long>) field(observer, "lastStatsPublishTimes");
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
    withObserverInternal(action, true);
  }

  private static void withObserverWithoutAutomaticShutdown(ObserverAction action) throws Exception {
    withObserverInternal(action, false);
  }

  private static void withObserverInternal(ObserverAction action, boolean automaticShutdown) throws Exception {
    boolean enabled = JMXManager.isEnableJMX();
    JMXManager.setEnableJMX(false);
    ConfigurationManager configurationManager = mock(ConfigurationManager.class);
    try (MockedStatic<ConfigurationManager> mocked = mockStatic(ConfigurationManager.class)) {
      mocked.when(ConfigurationManager::getInstance).thenReturn(configurationManager);
      TakTwinObserver observer = new TakTwinObserver(new TwinManager());
      try {
        action.run(observer);
      } finally {
        if (automaticShutdown) {
          observer.shutdown();
        }
      }
    } finally {
      MtiStatusRegistry.setSnapshotSource(null);
      JMXManager.setEnableJMX(enabled);
    }
  }

  private interface ObserverAction {
    void run(TakTwinObserver observer) throws Exception;
  }
}
