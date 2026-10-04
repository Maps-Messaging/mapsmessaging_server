/*
 *
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *
 */

package io.mapsmessaging.engine.destination.subscription;

import io.mapsmessaging.admin.SubscriptionControllerJMX;
import io.mapsmessaging.api.features.DestinationMode;
import io.mapsmessaging.dto.rest.session.SubscriptionContextDTO;
import io.mapsmessaging.engine.destination.DestinationFactory;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.subscription.modes.SubscriptionModeManager;
import io.mapsmessaging.engine.destination.subscription.set.DestinationSet;
import io.mapsmessaging.engine.session.SessionImpl;
import io.mapsmessaging.security.access.Identity;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SubscriptionControllerCoreCoverageTest {

  @Test
  void shutdownDetachesListenerShutsManagersAndClearsSets() throws Exception {
    Harness h = harness();
    h.subscriptions.put("key", mock(DestinationSet.class));

    h.controller.shutdown();

    verify(h.destinationFactory).removeListener(h.controller);
    for (SubscriptionModeManager manager : h.managers.values()) {
      verify(manager).shutdown();
    }
    assertTrue(h.subscriptions.isEmpty());
    verify(h.jmx).close();
  }

  @Test
  void closeDetachesListenerClosesManagersAndClearsAllState() throws Exception {
    Harness h = harness();
    h.subscriptions.put("key", mock(DestinationSet.class));
    h.contextMap.put("context", mock(SubscriptionContext.class));

    h.controller.close(false);

    verify(h.destinationFactory).removeListener(h.controller);
    for (SubscriptionModeManager manager : h.managers.values()) {
      verify(manager).close(false);
    }
    assertTrue(h.subscriptions.isEmpty());
    assertTrue(h.contextMap.isEmpty());
    verify(h.jmx).close();
  }

  @Test
  void hibernateAllOnlyActsWhenSessionIsLive() throws Exception {
    Harness h = harness();
    SessionImpl session = mock(SessionImpl.class);
    set(h.controller, "sessionImpl", session);

    h.controller.hibernateAll();

    assertTrue(h.controller.isHibernating());
    for (SubscriptionModeManager manager : h.managers.values()) {
      verify(manager).hibernate();
    }

    h.controller.hibernateAll();
    for (SubscriptionModeManager manager : h.managers.values()) {
      verify(manager, times(1)).hibernate();
    }
  }

  @Test
  void createdFansOutToEveryModeWhenNoIdentityIsConfigured() throws Exception {
    Harness h = harness();
    DestinationImpl destination = mock(DestinationImpl.class);
    when(destination.getResourceType()).thenReturn(io.mapsmessaging.api.features.DestinationType.TOPIC);

    h.controller.created(destination);

    for (SubscriptionModeManager manager : h.managers.values()) {
      verify(manager).created(eq(h.controller), eq(destination), anyCollection());
    }
  }

  @Test
  void deletedFansOutToEveryMode() throws Exception {
    Harness h = harness();
    DestinationImpl destination = mock(DestinationImpl.class);

    h.controller.deleted(destination);

    for (SubscriptionModeManager manager : h.managers.values()) {
      verify(manager).deleted(destination, h.subscriptions);
    }
  }

  @Test
  void destinationModeAccessorsDelegateToCorrectManagers() throws Exception {
    Harness h = harness();
    DestinationImpl destination = mock(DestinationImpl.class);
    Subscription normal = mock(Subscription.class);
    Subscription schema = mock(Subscription.class);
    when(h.managers.get(DestinationMode.NORMAL).get(destination)).thenReturn(normal);
    when(h.managers.get(DestinationMode.SCHEMA).get(destination)).thenReturn(schema);

    assertSame(normal, h.controller.get(destination));
    assertSame(schema, h.controller.getSchema(destination));

    h.controller.remove(destination);
    h.controller.removeSchema(destination);

    verify(h.managers.get(DestinationMode.NORMAL)).remove(destination);
    verify(h.managers.get(DestinationMode.SCHEMA)).remove(destination);
  }

  @Test
  void hibernateSubscriptionFansOutAcrossModes() throws Exception {
    Harness h = harness();

    h.controller.hibernateSubscription("sub-id");

    for (SubscriptionModeManager manager : h.managers.values()) {
      verify(manager).hibernateSubscription("sub-id");
    }
  }

  @Test
  void wakeDestinationReturnsFirstNonNullManagerResult() throws Exception {
    Harness h = harness();
    SessionImpl session = mock(SessionImpl.class);
    DestinationImpl destination = mock(DestinationImpl.class);
    io.mapsmessaging.api.SubscribedEventManager expected =
        mock(io.mapsmessaging.api.SubscribedEventManager.class);

    SubscriptionModeManager normal = h.managers.get(DestinationMode.NORMAL);
    SubscriptionModeManager schema = h.managers.get(DestinationMode.SCHEMA);
    when(normal.wake(session, destination)).thenReturn(null);
    when(schema.wake(session, destination)).thenReturn(expected);

    assertSame(expected, h.controller.wake(session, destination));
  }

  @Test
  void timeoutReplacementCancelsPreviousFuture() throws Exception {
    Harness h = harness();
    Future<?> first = mock(Future.class);
    Future<?> second = mock(Future.class);
    when(first.isCancelled()).thenReturn(false);

    h.controller.setTimeout(first);
    h.controller.setTimeout(second);

    verify(first).cancel(true);
    assertSame(second, h.controller.getTimeout());
  }

  @Test
  void alreadyCancelledTimeoutIsNotCancelledAgain() throws Exception {
    Harness h = harness();
    Future<?> first = mock(Future.class);
    Future<?> second = mock(Future.class);
    when(first.isCancelled()).thenReturn(true);

    h.controller.setTimeout(first);
    h.controller.setTimeout(second);

    verify(first, never()).cancel(anyBoolean());
    assertSame(second, h.controller.getTimeout());
  }

  @Test
  void subscriptionMapIsExposedAsUnmodifiableView() throws Exception {
    Harness h = harness();
    SubscriptionContext context = mock(SubscriptionContext.class);
    h.contextMap.put("key", context);

    Map<String, SubscriptionContext> view = h.controller.getSubscriptions();

    assertSame(context, view.get("key"));
    assertThrows(UnsupportedOperationException.class, () -> view.put("x", context));
  }

  @Test
  void subscriptionInformationReflectsControllerState() throws Exception {
    Harness h = harness();
    SubscriptionContext context = mock(SubscriptionContext.class);
    SubscriptionContextDTO details = new SubscriptionContextDTO();
    when(context.getDetails()).thenReturn(details);
    h.contextMap.put("key", context);

    var info = h.controller.getSubscriptionInformation();

    assertTrue(info.isHibernated());
    assertTrue(info.isPersistent());
    assertEquals("session", info.getSessionId());
    assertEquals("unique", info.getUniqueId());
    assertEquals(1, info.getSubscriptionContextList().size());
  }

  @Test
  void updateSubscriptionManagerUsesContextDestinationMode() throws Exception {
    Harness h = harness();
    SubscriptionContext context = mock(SubscriptionContext.class);
    DestinationImpl destination = mock(DestinationImpl.class);
    Subscription subscription = mock(Subscription.class);
    when(context.getDestinationMode()).thenReturn(DestinationMode.NORMAL);

    h.controller.updateSubscriptionManager(context, destination, subscription);

    verify(h.managers.get(DestinationMode.NORMAL)).add(subscription, destination);
  }

  @Test
  void deleteSubscriptionRemovesContextForMatchingManager() throws Exception {
    Harness h = harness();
    SubscriptionContext context = mock(SubscriptionContext.class);
    h.contextMap.put(DestinationMode.NORMAL.getNamespace() + "id", context);

    SubscriptionModeManager normal = h.managers.get(DestinationMode.NORMAL);
    when(normal.delSubscription("id", h.subscriptions, h.controller)).thenReturn(true);
    when(normal.getMode()).thenReturn(DestinationMode.NORMAL);

    assertTrue(h.controller.delSubscription("id"));
    assertFalse(h.contextMap.containsKey(DestinationMode.NORMAL.getNamespace() + "id"));
  }

  @Test
  void deleteSubscriptionReturnsFalseWhenNoModeMatches() throws Exception {
    Harness h = harness();

    assertFalse(h.controller.delSubscription("missing"));
  }

  private static Harness harness() throws Exception {
    SubscriptionController controller = mock(SubscriptionController.class, CALLS_REAL_METHODS);
    DestinationFactory destinationFactory = mock(DestinationFactory.class);
    Map<String, SubscriptionContext> contextMap = new LinkedHashMap<>();
    Map<String, DestinationSet> subscriptions = new LinkedHashMap<>();
    Map<DestinationMode, SubscriptionModeManager> managers = new LinkedHashMap<>();

    for (DestinationMode mode : DestinationMode.values()) {
      SubscriptionModeManager manager = mock(SubscriptionModeManager.class);
      when(manager.getMode()).thenReturn(mode);
      managers.put(mode, manager);
    }

    SubscriptionControllerJMX jmx = mock(SubscriptionControllerJMX.class);

    set(controller, "destinationManager", destinationFactory);
    set(controller, "sessionId", "session");
    set(controller, "uniqueSessionId", "unique");
    set(controller, "isPersistent", true);
    set(controller, "subscriptionControllerJMX", jmx);
    set(controller, "contextMap", contextMap);
    set(controller, "subscriptions", subscriptions);
    set(controller, "subscriptionModeManager", managers);
    set(controller, "sessionImpl", null);
    set(controller, "identity", null);
    set(controller, "schedule", null);

    return new Harness(controller, destinationFactory, contextMap, subscriptions, managers, jmx);
  }

  private static void set(Object target, String fieldName, Object value) throws Exception {
    Field field = SubscriptionController.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }

  private record Harness(
      SubscriptionController controller,
      DestinationFactory destinationFactory,
      Map<String, SubscriptionContext> contextMap,
      Map<String, DestinationSet> subscriptions,
      Map<DestinationMode, SubscriptionModeManager> managers,
      SubscriptionControllerJMX jmx) {
  }
}
