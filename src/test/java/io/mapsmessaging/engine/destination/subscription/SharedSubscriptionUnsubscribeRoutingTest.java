/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.engine.destination.subscription;

import io.mapsmessaging.api.SubscriptionContextBuilder;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.api.features.DestinationMode;
import io.mapsmessaging.engine.destination.subscription.modes.NormalSubscriptionModeManager;
import io.mapsmessaging.engine.destination.subscription.modes.SubscriptionModeManager;
import io.mapsmessaging.engine.destination.subscription.set.DestinationSet;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SharedSubscriptionUnsubscribeRoutingTest {

  @Test
  void unsubscribingOneSharedGroupPreservesOtherGroupAndOrdinarySubscription() throws Exception {
    Fixture f = fixture();
    SubscriptionContext groupA = shared("workers-A");
    SubscriptionContext groupB = shared("workers-B");
    SubscriptionContext ordinary = new SubscriptionContext("topic/orders");
    f.add(groupA);
    f.add(groupB);
    f.add(ordinary);

    assertTrue(f.controller.delSubscription("$share/workers-A/topic/orders"));

    assertFalse(f.contexts.containsKey(groupA.getKey()));
    assertFalse(f.subscriptions.containsKey(groupA.getKey()));
    assertTrue(f.contexts.containsKey(groupB.getKey()));
    assertTrue(f.subscriptions.containsKey(groupB.getKey()));
    assertTrue(f.contexts.containsKey(ordinary.getKey()));
  }

  @Test
  void malformedOrUnknownSharedUnsubscribeCannotDeleteExistingGroup() throws Exception {
    Fixture f = fixture();
    SubscriptionContext groupA = shared("workers-A");
    f.add(groupA);

    assertFalse(f.controller.delSubscription("$share//topic/orders"));
    assertFalse(f.controller.delSubscription("$share/unknown/topic/orders"));
    assertFalse(f.controller.delSubscription("$share/workers-A/other/topic"));
    assertTrue(f.contexts.containsKey(groupA.getKey()));
  }

  private static SubscriptionContext shared(String group) {
    return new SubscriptionContextBuilder("topic/orders", ClientAcknowledgement.AUTO)
        .setSharedName(group).build();
  }

  private static Fixture fixture() throws Exception {
    SubscriptionController controller = mock(SubscriptionController.class, CALLS_REAL_METHODS);
    Map<String, SubscriptionContext> contexts = new LinkedHashMap<>();
    Map<String, DestinationSet> subscriptions = new LinkedHashMap<>();
    Map<DestinationMode, SubscriptionModeManager> managers = new LinkedHashMap<>();
    managers.put(DestinationMode.NORMAL, new NormalSubscriptionModeManager());

    set(controller, "contextMap", contexts);
    set(controller, "subscriptions", subscriptions);
    set(controller, "subscriptionModeManager", managers);
    return new Fixture(controller, contexts, subscriptions);
  }

  private static void set(Object target, String name, Object value) throws Exception {
    Field field = SubscriptionController.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }

  private record Fixture(SubscriptionController controller,
                         Map<String, SubscriptionContext> contexts,
                         Map<String, DestinationSet> subscriptions) {
    void add(SubscriptionContext context) {
      contexts.put(context.getKey(), context);
      subscriptions.put(context.getKey(), new DestinationSet(context, Map.of()));
    }
  }
}
