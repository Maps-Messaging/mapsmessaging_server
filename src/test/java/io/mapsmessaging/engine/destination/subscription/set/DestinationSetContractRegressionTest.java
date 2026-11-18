/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.engine.destination.subscription.set;

import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DestinationSetContractRegressionTest {

  @Test
  void removeIfRemovesEveryMatchingDestinationNotOnlyTheFirst() {
    DestinationSet set = set("sensors/#", "sensors/one", "sensors/two", "sensors/three");

    assertTrue(set.removeIf(d -> d.getFullyQualifiedNamespace().endsWith("one")
        || d.getFullyQualifiedNamespace().endsWith("two")));

    assertEquals(1, set.size());
    assertTrue(set.contains("sensors/three"));
    assertFalse(set.contains("sensors/one"));
    assertFalse(set.contains("sensors/two"));
  }

  @Test
  void retainAllKeepsOnlyRequestedDestinationsAndReturnsWhetherChanged() {
    DestinationSet set = set("sensors/#", "sensors/one", "sensors/two", "sensors/three");
    DestinationImpl keep = set.stream()
        .filter(d -> d.getFullyQualifiedNamespace().equals("sensors/two"))
        .findFirst().orElseThrow();

    assertTrue(set.retainAll(Set.of(keep)));
    assertEquals(1, set.size());
    assertTrue(set.contains(keep));
    assertFalse(set.contains("sensors/one"));
    assertFalse(set.contains("sensors/three"));
    assertFalse(set.retainAll(Set.of(keep)), "An unchanged set must report no change");
  }

  @Test
  void addingAnExistingDestinationDoesNotReportASetChange() {
    DestinationSet set = set("sensors/#", "sensors/one");
    DestinationImpl existing = set.iterator().next();

    assertFalse(set.add(existing));
    assertEquals(1, set.size());
  }

  @Test
  void removeAllSupportsMixedInstanceAndNamespaceKeys() {
    DestinationSet set = set("sensors/#", "sensors/one", "sensors/two", "sensors/three");
    DestinationImpl two = set.stream()
        .filter(d -> d.getFullyQualifiedNamespace().equals("sensors/two"))
        .findFirst().orElseThrow();

    assertTrue(set.removeAll(List.of("sensors/one", two)));
    assertEquals(1, set.size());
    assertTrue(set.contains("sensors/three"));
  }

  @Test
  void plusMatchesOneLevelIncludingEmptyButNotExtraLevels() {
    assertTrue(DestinationSet.matches("sensors/+", "sensors/"));
    assertTrue(DestinationSet.matches("sensors/+", "sensors/one"));
    assertFalse(DestinationSet.matches("sensors/+", "sensors"));
    assertFalse(DestinationSet.matches("sensors/+", "sensors/one/two"));
  }

  @Test
  void terminalHashMatchesZeroOrManyLevelsButNotSystemTopics() {
    assertTrue(DestinationSet.matches("sensors/#", "sensors"));
    assertTrue(DestinationSet.matches("sensors/#", "sensors/one/two"));
    assertFalse(DestinationSet.matches("#", "$SYS/uptime"));
    assertTrue(DestinationSet.matches("$SYS/#", "$SYS/uptime"));
  }

  private static DestinationSet set(String filter, String... names) {
    SubscriptionContext context = new SubscriptionContext(filter);
    Map<String, DestinationImpl> destinations = new LinkedHashMap<>();
    for (String name : names) {
      DestinationImpl destination = mock(DestinationImpl.class);
      when(destination.getFullyQualifiedNamespace()).thenReturn(name);
      destinations.put(name, destination);
    }
    return new DestinationSet(context, destinations);
  }
}
