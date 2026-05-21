package io.mapsmessaging.engine.destination.subscription.set;

import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class DestinationSetTest {

  private DestinationImpl destination(String name) {
    DestinationImpl d = Mockito.mock(DestinationImpl.class);
    Mockito.when(d.getFullyQualifiedNamespace()).thenReturn(name);
    return d;
  }

  private SubscriptionContext context(String filter, boolean wildcard) {
    SubscriptionContext ctx = Mockito.mock(SubscriptionContext.class);
    Mockito.when(ctx.getFilter()).thenReturn(filter);
    Mockito.when(ctx.containsWildcard()).thenReturn(wildcard);
    return ctx;
  }

  @Test
  void constructor_copiesProvidedMap_notBackedByIt() {
    Map<String, DestinationImpl> source = new LinkedHashMap<>();
    DestinationImpl d1 = destination("a/b");
    source.put("a/b", d1);

    DestinationSet set = new DestinationSet(context("a/#", true), source);

    source.clear();

    Assertions.assertEquals(1, set.size());
    Assertions.assertTrue(set.contains(d1));
  }

  @Test
  void interest_exactMatch_withoutWildcard() {
    DestinationSet set =
        new DestinationSet(context("a/b", false), Map.of());

    Assertions.assertTrue(set.interest("a/b"));
    Assertions.assertFalse(set.interest("a/c"));
  }

  @Test
  void interest_wildcardMatch() {
    DestinationSet set =
        new DestinationSet(context("a/+", true), Map.of());

    Assertions.assertTrue(set.interest("a/b"));
    Assertions.assertFalse(set.interest("a/b/c"));
  }

  @Test
  void add_onlyAddsWhenMatchingContext() {
    DestinationImpl match = destination("a/b");
    DestinationImpl noMatch = destination("x/y");

    DestinationSet set =
        new DestinationSet(context("a/#", true), Map.of());

    Assertions.assertTrue(set.add(match));
    Assertions.assertFalse(set.add(noMatch));

    Assertions.assertEquals(1, set.size());
    Assertions.assertTrue(set.contains("a/b"));
  }

  @Test
  void add_overwritesByNamespaceKey() {
    DestinationImpl d1 = destination("a/b");
    DestinationImpl d2 = destination("a/b");

    DestinationSet set =
        new DestinationSet(context("a/#", true), Map.of());

    set.add(d1);
    set.add(d2);

    Assertions.assertEquals(1, set.size());
    Assertions.assertTrue(set.contains(d2));
  }

  @Test
  void remove_byStringKey() {
    DestinationImpl d = destination("a/b");

    DestinationSet set =
        new DestinationSet(context("a/#", true), Map.of("a/b", d));

    Assertions.assertTrue(set.remove("a/b"));
    Assertions.assertTrue(set.isEmpty());
  }

  @Test
  void remove_byDestinationInstance() {
    DestinationImpl d = destination("a/b");

    DestinationSet set =
        new DestinationSet(context("a/#", true), Map.of("a/b", d));

    Assertions.assertTrue(set.remove(d));
    Assertions.assertFalse(set.contains(d));
  }

  @Test
  void addAll_addsOnlyMatchingDestinations() {
    DestinationImpl d1 = destination("a/1");
    DestinationImpl d2 = destination("a/2");
    DestinationImpl d3 = destination("b/1");

    DestinationSet set =
        new DestinationSet(context("a/#", true), Map.of());

    boolean changed = set.addAll(List.of(d1, d2, d3));

    Assertions.assertTrue(changed);
    Assertions.assertEquals(2, set.size());
    Assertions.assertFalse(set.contains(d3));
  }

  @Test
  void removeAll_removesMatchingEntries() {
    DestinationImpl d1 = destination("a/1");
    DestinationImpl d2 = destination("a/2");

    DestinationSet set =
        new DestinationSet(
            context("a/#", true),
            new LinkedHashMap<>(Map.of(
                "a/1", d1,
                "a/2", d2
            ))
        );

    boolean changed = set.removeAll(List.of("a/1", d2));

    Assertions.assertTrue(changed);
    Assertions.assertTrue(set.isEmpty());
  }

  @Test
  void removeIf_removesFirstMatchingOnly() {
    DestinationImpl d1 = destination("a/1");
    DestinationImpl d2 = destination("a/2");

    DestinationSet set =
        new DestinationSet(
            context("a/#", true),
            new LinkedHashMap<>(Map.of(
                "a/1", d1,
                "a/2", d2
            ))
        );

    boolean removed = set.removeIf(d -> d.getFullyQualifiedNamespace().endsWith("1"));

    Assertions.assertTrue(removed);
    Assertions.assertEquals(1, set.size());
  }

  @Test
  void clear_removesAll() {
    DestinationSet set =
        new DestinationSet(
            context("a/#", true),
            new LinkedHashMap<>(Map.of(
                "a/1", destination("a/1"),
                "a/2", destination("a/2")
            ))
        );

    set.clear();

    Assertions.assertTrue(set.isEmpty());
  }
}
