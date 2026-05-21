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

package io.mapsmessaging.rest.api.impl.destination;


import io.mapsmessaging.api.DestinationInfo;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.rest.api.impl.destination.context.BrowseEntry;
import io.mapsmessaging.rest.api.impl.destination.context.NamespaceTree;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class NamespaceTreeRestPagingSemanticsTest {

  @Test
  void restPagingClampAllowsForwardBackwardAndCorrectSlice() {
    int totalEntries = 1000;

    List<DestinationInfo> destinations = new ArrayList<>(totalEntries);
    for (int i = 0; i < totalEntries; i++) {
      destinations.add(destination("COM3/1/1/topic-" + String.format("%04d", i), DestinationType.TOPIC));
    }

    NamespaceTree tree = NamespaceTree.buildFromPaths(destinations);

    int requestedPageSize = 1;
    int effectivePageSize = clampRestPageSize(requestedPageSize);

    int pageNo = 5;

    List<BrowseEntry> page5 = tree.listAtNode("COM3/1/1", pageNo, effectivePageSize);
    assertEquals(effectivePageSize, page5.size());
    assertEquals("topic-0050", page5.get(0).getName());
    assertEquals("topic-0059", page5.get(page5.size() - 1).getName());

    List<BrowseEntry> page6 = tree.listAtNode("COM3/1/1", pageNo + 1, effectivePageSize);
    assertEquals(effectivePageSize, page6.size());
    assertEquals("topic-0060", page6.get(0).getName());
    assertEquals("topic-0069", page6.get(page6.size() - 1).getName());

    List<BrowseEntry> backTo5 = tree.listAtNode("COM3/1/1", pageNo, effectivePageSize);
    assertEquals(page5.size(), backTo5.size());
    assertEquals(page5.get(0).getName(), backTo5.get(0).getName());
    assertEquals(page5.get(page5.size() - 1).getName(), backTo5.get(backTo5.size() - 1).getName());

    List<BrowseEntry> page4 = tree.listAtNode("COM3/1/1", pageNo - 1, effectivePageSize);
    assertEquals(effectivePageSize, page4.size());
    assertEquals("topic-0040", page4.get(0).getName());
    assertEquals("topic-0049", page4.get(page4.size() - 1).getName());
  }

  @Test
  void restPagingClampStillReturnsAllEntriesExactlyOnce() {
    int totalEntries = 1000;

    List<DestinationInfo> destinations = new ArrayList<>(totalEntries);
    for (int i = 0; i < totalEntries; i++) {
      destinations.add(destination("COM3/1/1/topic-" + String.format("%04d", i), DestinationType.TOPIC));
    }

    NamespaceTree tree = NamespaceTree.buildFromPaths(destinations);

    int requestedPageSize = 3;
    int effectivePageSize = clampRestPageSize(requestedPageSize);

    Set<String> seen = new HashSet<>();
    int pageNo = 0;

    while (true) {
      List<BrowseEntry> page = tree.listAtNode("COM3/1/1", pageNo, effectivePageSize);
      if (page.isEmpty()) {
        break;
      }

      for (BrowseEntry entry : page) {
        boolean added = seen.add(entry.getName());
        assertTrue(added, "Duplicate entry: " + entry.getName());
      }

      pageNo++;
    }

    assertEquals(totalEntries, seen.size());
    assertTrue(seen.contains("topic-0000"));
    assertTrue(seen.contains("topic-0999"));
  }

  @Test
  void changingEffectivePageSizeChangesSliceAsExpected() {
    int totalEntries = 1000;

    List<DestinationInfo> destinations = new ArrayList<>(totalEntries);
    for (int i = 0; i < totalEntries; i++) {
      destinations.add(destination("COM3/1/1/topic-" + String.format("%04d", i), DestinationType.TOPIC));
    }

    NamespaceTree tree = NamespaceTree.buildFromPaths(destinations);

    int pageNo = 7;

    int effectivePageSize10 = clampRestPageSize(10);
    List<BrowseEntry> pageNo7Size10 = tree.listAtNode("COM3/1/1", pageNo, effectivePageSize10);
    assertEquals(10, pageNo7Size10.size());
    assertEquals("topic-0070", pageNo7Size10.get(0).getName());
    assertEquals("topic-0079", pageNo7Size10.get(9).getName());

    int effectivePageSize100 = clampRestPageSize(100);
    List<BrowseEntry> pageNo7Size100 = tree.listAtNode("COM3/1/1", pageNo, effectivePageSize100);
    assertEquals(100, pageNo7Size100.size());
    assertEquals("topic-0700", pageNo7Size100.get(0).getName());
    assertEquals("topic-0799", pageNo7Size100.get(99).getName());
  }

  private static int clampRestPageSize(int requestedPageSize) {
    int pageSize = requestedPageSize;
    if (pageSize <= 10) {
      pageSize = 10;
    }
    if (pageSize > 1000) {
      pageSize = 1000;
    }
    return pageSize;
  }

  private static DestinationInfo destination(String name, DestinationType type) {
    return new DestinationInfo(name, type);
  }
}
