/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.engine.destination.subscription.modes;

import io.mapsmessaging.api.SubscriptionContextBuilder;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.subscription.Subscription;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SharedGroupSessionIdentityTest {

  @Test
  void sameTopicDifferentGroupsHaveDistinctSessionKeys() {
    SubscriptionContext first = shared("workers-A");
    SubscriptionContext second = shared("workers-B");

    assertNotEquals(first.getKey(), second.getKey());
    assertEquals(first.getFilter(), second.getFilter());
    assertEquals(first.getKey(), shared("workers-A").getKey());
  }

  @Test
  void ordinaryDestinationKeepsItsPreviousSubscriptionKey() {
    SubscriptionContext ordinary = new SubscriptionContext("topic/orders");

    assertEquals("$normaltopic/orders", ordinary.getKey());
  }

  @Test
  void differentSharedGroupsHaveIndependentModeManagerEntries() {
    NormalSubscriptionModeManager manager = new NormalSubscriptionModeManager();
    DestinationImpl destination = mock(DestinationImpl.class);
    SubscriptionContext a = shared("workers-A");
    SubscriptionContext b = shared("workers-B");
    Subscription first = mock(Subscription.class);
    Subscription second = mock(Subscription.class);
    when(first.getContext()).thenReturn(a);
    when(second.getContext()).thenReturn(b);

    manager.add(first, destination);
    manager.add(second, destination);

    assertSame(first, manager.get(destination, a));
    assertSame(second, manager.get(destination, b));
    assertNull(manager.get(destination), "Shared groups must not overwrite the normal subscription entry");

    assertSame(first, manager.remove(destination, a));
    assertNull(manager.get(destination, a));
    assertSame(second, manager.get(destination, b));
  }

  @Test
  void distinctSelectorsWithinSameGroupHaveSeparateActiveEntries() {
    NormalSubscriptionModeManager manager = new NormalSubscriptionModeManager();
    DestinationImpl destination = mock(DestinationImpl.class);
    SubscriptionContext high = shared("workers-A");
    SubscriptionContext low = shared("workers-A");
    high.setSelector("temperature > 20");
    low.setSelector("temperature < 10");
    Subscription first = mock(Subscription.class);
    Subscription second = mock(Subscription.class);
    when(first.getContext()).thenReturn(high);
    when(second.getContext()).thenReturn(low);

    manager.add(first, destination);
    manager.add(second, destination);

    assertSame(first, manager.get(destination, high));
    assertSame(second, manager.get(destination, low));
  }

  @Test
  void removingSharedGroupDoesNotRemoveOrdinaryOrAnotherSharedGroup() {
    NormalSubscriptionModeManager manager = new NormalSubscriptionModeManager();
    DestinationImpl destination = mock(DestinationImpl.class);
    SubscriptionContext ordinaryContext = new SubscriptionContext("topic/orders");
    SubscriptionContext groupA = shared("workers-A");
    SubscriptionContext groupB = shared("workers-B");
    Subscription ordinary = mock(Subscription.class);
    Subscription first = mock(Subscription.class);
    Subscription second = mock(Subscription.class);
    when(ordinary.getContext()).thenReturn(ordinaryContext);
    when(first.getContext()).thenReturn(groupA);
    when(second.getContext()).thenReturn(groupB);

    manager.add(ordinary, destination);
    manager.add(first, destination);
    manager.add(second, destination);
    assertSame(first, manager.remove(destination, groupA));

    assertNull(manager.get(destination, groupA));
    assertSame(ordinary, manager.get(destination, ordinaryContext));
    assertSame(second, manager.get(destination, groupB));
  }

  @Test
  void shutdownClosesOrdinaryAndEachSharedGroupExactlyOnce() throws Exception {
    NormalSubscriptionModeManager manager = new NormalSubscriptionModeManager();
    DestinationImpl destination = mock(DestinationImpl.class);
    SubscriptionContext ordinaryContext = new SubscriptionContext("topic/orders");
    SubscriptionContext groupA = shared("workers-A");
    SubscriptionContext groupB = shared("workers-B");
    Subscription ordinary = mock(Subscription.class);
    Subscription first = mock(Subscription.class);
    Subscription second = mock(Subscription.class);
    when(ordinary.getContext()).thenReturn(ordinaryContext);
    when(first.getContext()).thenReturn(groupA);
    when(second.getContext()).thenReturn(groupB);

    manager.add(ordinary, destination);
    manager.add(first, destination);
    manager.add(second, destination);
    manager.shutdown();

    verify(ordinary, times(1)).close();
    verify(first, times(1)).close();
    verify(second, times(1)).close();
    assertNull(manager.get(destination, groupA));
    assertNull(manager.get(destination, groupB));
    assertNull(manager.get(destination, ordinaryContext));
  }

  private static SubscriptionContext shared(String group) {
    return new SubscriptionContextBuilder("topic/orders", ClientAcknowledgement.AUTO)
        .setSharedName(group).build();
  }
}
