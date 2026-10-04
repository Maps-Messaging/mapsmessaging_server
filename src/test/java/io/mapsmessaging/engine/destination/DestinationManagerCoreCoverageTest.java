/*
 *
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *
 */

package io.mapsmessaging.engine.destination;

import io.mapsmessaging.api.auth.DestinationAuthorisationCheck;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.dto.rest.system.Status;
import io.mapsmessaging.license.FeatureManager;
import io.mapsmessaging.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DestinationManagerCoreCoverageTest {

  @ParameterizedTest
  @EnumSource(DestinationType.class)
  void licensedDestinationTypesDelegateCreation(DestinationType type) throws Exception {
    Harness h = harness(true, 0, 0);
    when(h.pipeline.count(type)).thenReturn(CompletableFuture.completedFuture(0));
    DestinationImpl created = mock(DestinationImpl.class);
    when(h.pipeline.create("name", type)).thenReturn(CompletableFuture.completedFuture(created));

    CompletableFuture<DestinationImpl> result = h.manager.create("name", type, null);

    assertNotNull(result);
    assertSame(created, result.get());
    verify(h.pipeline).create("name", type);
  }

  @ParameterizedTest
  @EnumSource(DestinationType.class)
  void unlicensedDestinationTypesAreRejected(DestinationType type) throws Exception {
    Harness h = harness(false, 0, 0);

    assertNull(h.manager.create("name", type, null));

    verify(h.pipeline, never()).create(anyString(), any());
  }

  @Test
  void systemTopicCreationIsRejectedBeforeOtherChecks() throws Exception {
    Harness h = harness(true, 0, 0);

    assertNull(h.manager.create("$SYS/internal", DestinationType.TOPIC, null));

    verifyNoInteractions(h.pipeline);
  }

  @Test
  void authorisationDenialPreventsCreation() throws Exception {
    Harness h = harness(true, 0, 0);
    when(h.pipeline.count(DestinationType.TOPIC)).thenReturn(CompletableFuture.completedFuture(0));
    DestinationAuthorisationCheck auth = mock(DestinationAuthorisationCheck.class);
    when(auth.check("/secure", DestinationType.TOPIC, true)).thenReturn(false);

    IOException error = assertThrows(
        IOException.class,
        () -> h.manager.create("/secure", DestinationType.TOPIC, auth));

    assertEquals("Not authorised to create destination", error.getMessage());
    verify(h.pipeline, never()).create(anyString(), any());
  }

  @Test
  void authorisationAllowanceDelegatesCreation() throws Exception {
    Harness h = harness(true, 0, 0);
    when(h.pipeline.count(DestinationType.TOPIC)).thenReturn(CompletableFuture.completedFuture(0));
    DestinationAuthorisationCheck auth = mock(DestinationAuthorisationCheck.class);
    when(auth.check("/secure", DestinationType.TOPIC, true)).thenReturn(true);
    when(h.pipeline.create("/secure", DestinationType.TOPIC))
        .thenReturn(CompletableFuture.completedFuture(mock(DestinationImpl.class)));

    assertNotNull(h.manager.create("/secure", DestinationType.TOPIC, auth));

    verify(auth).check("/secure", DestinationType.TOPIC, true);
  }

  @Test
  void exactlyAtConfiguredTopicLimitStillAllowsCreation() throws Exception {
    Harness h = harness(true, 2, 0);
    when(h.pipeline.count(DestinationType.TOPIC)).thenReturn(CompletableFuture.completedFuture(2));
    when(h.pipeline.create("/topic", DestinationType.TOPIC))
        .thenReturn(CompletableFuture.completedFuture(mock(DestinationImpl.class)));

    assertNotNull(h.manager.create("/topic", DestinationType.TOPIC, null));
  }

  @Test
  void exceedingConfiguredTopicLimitRejectsCreation() throws Exception {
    Harness h = harness(true, 2, 0);
    when(h.pipeline.count(DestinationType.TOPIC)).thenReturn(CompletableFuture.completedFuture(3));

    assertNull(h.manager.create("/topic", DestinationType.TOPIC, null));
    verify(h.pipeline, never()).create(anyString(), any());
  }

  @Test
  void exceedingConfiguredQueueLimitRejectsTemporaryQueueCreation() throws Exception {
    Harness h = harness(true, 0, 1);
    when(h.pipeline.count(DestinationType.TEMPORARY_QUEUE))
        .thenReturn(CompletableFuture.completedFuture(2));

    assertNull(h.manager.create("/queue", DestinationType.TEMPORARY_QUEUE, null));
  }

  @Test
  void findOrCreateReturnsExistingDestinationWithoutCreate() throws Exception {
    Harness h = harness(true, 0, 0);
    DestinationImpl existing = mock(DestinationImpl.class);
    when(h.pipeline.find("/existing")).thenReturn(CompletableFuture.completedFuture(existing));

    CompletableFuture<DestinationImpl> result =
        h.manager.findOrCreate("/existing", DestinationType.TOPIC, null);

    assertSame(existing, result.get());
    verify(h.pipeline, never()).create(anyString(), any());
  }

  @Test
  void findOrCreateCreatesWhenDestinationIsMissing() throws Exception {
    Harness h = harness(true, 0, 0);
    DestinationImpl created = mock(DestinationImpl.class);
    when(h.pipeline.find("/new")).thenReturn(CompletableFuture.completedFuture(null));
    when(h.pipeline.count(DestinationType.TOPIC)).thenReturn(CompletableFuture.completedFuture(0));
    when(h.pipeline.create("/new", DestinationType.TOPIC))
        .thenReturn(CompletableFuture.completedFuture(created));

    assertSame(created, h.manager.findOrCreate("/new", DestinationType.TOPIC, null).get());
  }

  @Test
  void deleteProtectsSystemDestination() throws Exception {
    Harness h = harness(true, 0, 0);
    DestinationImpl destination = mock(DestinationImpl.class);
    when(destination.getFullyQualifiedNamespace()).thenReturn("$SYS/state");

    assertNull(h.manager.delete(destination));
    verify(h.pipeline, never()).delete(any());
  }

  @Test
  void deleteDelegatesForNormalDestination() throws Exception {
    Harness h = harness(true, 0, 0);
    DestinationImpl destination = mock(DestinationImpl.class);
    when(destination.getFullyQualifiedNamespace()).thenReturn("/normal");
    when(h.pipeline.delete(destination)).thenReturn(CompletableFuture.completedFuture(destination));

    assertSame(destination, h.manager.delete(destination).get());
  }

  @Test
  void aggregateSizeCountAndStorageAreSummedAcrossPipelines() throws Exception {
    Harness h = harness(true, 0, 0);
    DestinationManagerPipeline second = mock(DestinationManagerPipeline.class);
    set(h.manager, "creatorPipelines", new DestinationManagerPipeline[]{h.pipeline, second});

    when(h.pipeline.size()).thenReturn(CompletableFuture.completedFuture(2));
    when(second.size()).thenReturn(CompletableFuture.completedFuture(3));
    when(h.pipeline.count(DestinationType.TOPIC)).thenReturn(CompletableFuture.completedFuture(4));
    when(second.count(DestinationType.TOPIC)).thenReturn(CompletableFuture.completedFuture(5));
    when(h.pipeline.getStorageSize()).thenReturn(10L);
    when(second.getStorageSize()).thenReturn(20L);

    assertEquals(5, h.manager.size());
    assertEquals(9, h.manager.count(DestinationType.TOPIC));
    assertEquals(30L, h.manager.getStorageSize());
  }

  @Test
  void getCombinesPipelineCopies() throws Exception {
    Harness h = harness(true, 0, 0);
    DestinationManagerPipeline second = mock(DestinationManagerPipeline.class);
    set(h.manager, "creatorPipelines", new DestinationManagerPipeline[]{h.pipeline, second});
    DestinationImpl first = mock(DestinationImpl.class);
    DestinationImpl other = mock(DestinationImpl.class);

    when(h.pipeline.copy(any(), anyMap())).thenAnswer(invocation -> {
      Map<String, DestinationImpl> target = invocation.getArgument(1);
      target.put("/a", first);
      return CompletableFuture.completedFuture(target);
    });
    when(second.copy(any(), anyMap())).thenAnswer(invocation -> {
      Map<String, DestinationImpl> target = invocation.getArgument(1);
      target.put("/b", other);
      return CompletableFuture.completedFuture(target);
    });

    Map<String, DestinationImpl> result = h.manager.get(name -> true);

    assertEquals(2, result.size());
    assertSame(first, result.get("/a"));
    assertSame(other, result.get("/b"));
  }

  @Test
  void listenerLifecycleAndStatusExposeManagerState() throws Exception {
    Harness h = harness(true, 0, 0);
    DestinationManagerListener listener = mock(DestinationManagerListener.class);

    h.manager.addListener(listener);
    assertTrue(h.manager.getListeners().contains(listener));
    assertTrue(h.manager.removeListener(listener));
    assertFalse(h.manager.getListeners().contains(listener));

    assertEquals("Destination Manager", h.manager.getName());
    assertEquals(
        "Manages life cycle of the destinations and manages access to them",
        h.manager.getDescription());
    assertEquals(Status.OK, h.manager.getStatus().getStatus());
    assertTrue(h.manager.getStatus().getComment().contains("Running Pipelines:1"));
  }

  @Test
  void addSystemTopicUsesItsNamespaceForPipelineSelection() throws Exception {
    Harness h = harness(true, 0, 0);
    io.mapsmessaging.engine.system.SystemTopic topic =
        mock(io.mapsmessaging.engine.system.SystemTopic.class);
    when(topic.getFullyQualifiedNamespace()).thenReturn("$SYS/test");

    h.manager.addSystemTopic(topic);

    verify(h.pipeline).put(topic);
  }

  private static Harness harness(boolean enabled, int maxTopics, int maxQueues) throws Exception {
    DestinationManager manager = mock(DestinationManager.class, CALLS_REAL_METHODS);
    DestinationManagerPipeline pipeline = mock(DestinationManagerPipeline.class);

    set(manager, "logger", mock(Logger.class));
    set(manager, "topicsSupported", enabled);
    set(manager, "queuesSupported", enabled);
    set(manager, "schemaSupported", enabled);
    set(manager, "tempTopicsSupported", enabled);
    set(manager, "tempQueuesSupported", enabled);
    set(manager, "maxTopics", maxTopics);
    set(manager, "maxQueues", maxQueues);
    set(manager, "properties", new java.util.LinkedHashMap<>());
    set(manager, "rootPath", null);
    set(manager, "destinationManagerListeners", new DestinationUpdateManager());
    set(manager, "creatorPipelines", new DestinationManagerPipeline[]{pipeline});

    return new Harness(manager, pipeline);
  }

  private static void set(Object target, String fieldName, Object value) throws Exception {
    Field field = DestinationManager.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }

  private record Harness(DestinationManager manager, DestinationManagerPipeline pipeline) {
  }
}
