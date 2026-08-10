/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.engine.destination.subscription.set;

import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

class DestinationSetUsageTest {

  private DestinationImpl destination(String fullyQualifiedNamespace) {
    DestinationImpl destination = Mockito.mock(DestinationImpl.class);
    Mockito.when(destination.getFullyQualifiedNamespace()).thenReturn(fullyQualifiedNamespace);
    return destination;
  }

  private SubscriptionContext wildcardContext(String filter) {
    SubscriptionContext context = Mockito.mock(SubscriptionContext.class);
    Mockito.when(context.containsWildcard()).thenReturn(true);
    Mockito.when(context.getFilter()).thenReturn(filter);
    return context;
  }

  @Test
  void constructedFromInitialWildcardMatches_thenIncrementallyMaintained() {
    SubscriptionContext context = wildcardContext("a/#");

    DestinationImpl a1 = destination("a/1");
    DestinationImpl a2 = destination("a/2");

    Map<String, DestinationImpl> initialMatches = new LinkedHashMap<>();
    initialMatches.put("a/1", a1);
    initialMatches.put("a/2", a2);

    DestinationSet set = new DestinationSet(context, initialMatches);

    Assertions.assertEquals(2, set.size());
    Assertions.assertTrue(set.contains("a/1"));
    Assertions.assertTrue(set.contains("a/2"));

    // Destination created later that matches
    DestinationImpl a3 = destination("a/3");
    Assertions.assertTrue(set.add(a3));
    Assertions.assertEquals(3, set.size());
    Assertions.assertTrue(set.contains("a/3"));

    // Destination created later that does NOT match
    DestinationImpl b9 = destination("b/9");
    Assertions.assertFalse(set.add(b9));
    Assertions.assertEquals(3, set.size());
    Assertions.assertFalse(set.contains("b/9"));

    // Destination removed later
    Assertions.assertTrue(set.remove("a/2"));
    Assertions.assertEquals(2, set.size());
    Assertions.assertFalse(set.contains("a/2"));

    Assertions.assertTrue(set.remove(a1));
    Assertions.assertEquals(1, set.size());
    Assertions.assertFalse(set.contains("a/1"));
    Assertions.assertTrue(set.contains("a/3"));
  }

  @Test
  void interest_reportsWouldMatch_evenIfNotPresentInSet() {
    SubscriptionContext context = wildcardContext("a/+");

    DestinationImpl a1 = destination("a/1");
    Map<String, DestinationImpl> initialMatches = new LinkedHashMap<>();
    initialMatches.put("a/1", a1);

    DestinationSet set = new DestinationSet(context, initialMatches);

    Assertions.assertTrue(set.interest("a/2"));
    Assertions.assertFalse(set.interest("a/2/x"));
    Assertions.assertFalse(set.contains("a/2"));
  }

  @Test
  void constructorTrustsInitialMap_evenIfItContainsNonMatchingEntries() {
    SubscriptionContext context = wildcardContext("a/+");

    DestinationImpl a1 = destination("a/1");
    DestinationImpl b1 = destination("b/1"); // does NOT match a/+

    Map<String, DestinationImpl> initialMatches = new LinkedHashMap<>();
    initialMatches.put("a/1", a1);
    initialMatches.put("b/1", b1);

    DestinationSet set = new DestinationSet(context, initialMatches);

    // Documenting current behaviour: constructor copies, does not filter.
    Assertions.assertEquals(2, set.size());
    Assertions.assertTrue(set.contains("a/1"));
    Assertions.assertTrue(set.contains("b/1"));
    Assertions.assertFalse(set.interest("b/1"));
  }

  @Test
  void iterationOrder_isInsertionOrder_fromInitialMap_thenAddsAppend() {
    SubscriptionContext context = wildcardContext("a/#");

    DestinationImpl a1 = destination("a/1");
    DestinationImpl a2 = destination("a/2");

    Map<String, DestinationImpl> initialMatches = new LinkedHashMap<>();
    initialMatches.put("a/1", a1);
    initialMatches.put("a/2", a2);

    DestinationSet set = new DestinationSet(context, initialMatches);

    DestinationImpl a3 = destination("a/3");
    set.add(a3);

    Iterator<DestinationImpl> iterator = set.iterator();

    Assertions.assertSame(a1, iterator.next());
    Assertions.assertSame(a2, iterator.next());
    Assertions.assertSame(a3, iterator.next());
    Assertions.assertFalse(iterator.hasNext());
  }

  @Test
  void add_overwritesSameNamespace_keyedByFullyQualifiedNamespace() {
    SubscriptionContext context = wildcardContext("a/#");

    DestinationImpl first = destination("a/1");
    DestinationImpl replacement = destination("a/1");

    DestinationSet set = new DestinationSet(context, new LinkedHashMap<>());

    Assertions.assertTrue(set.add(first));
    Assertions.assertEquals(1, set.size());
    Assertions.assertTrue(set.contains(first));

    Assertions.assertTrue(set.add(replacement));
    Assertions.assertEquals(1, set.size());
    Assertions.assertTrue(set.contains(replacement));
  }
}
