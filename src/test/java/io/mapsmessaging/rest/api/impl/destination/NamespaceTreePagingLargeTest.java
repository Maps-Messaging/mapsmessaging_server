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
import io.mapsmessaging.rest.api.impl.destination.context.Type;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class NamespaceTreePagingLargeTest {

  @Test
  void pagingOverOneThousandEntriesReturnsAllExactlyOnce() {
    int totalEntries = 1000;
    int pageSize = 37;

    List<DestinationInfo> destinations = new ArrayList<>(totalEntries);
    for (int i = 0; i < totalEntries; i++) {
      String name = "COM3/1/1/topic-" + String.format("%04d", i);
      destinations.add(destination(name, DestinationType.TOPIC));
    }

    NamespaceTree tree = NamespaceTree.buildFromPaths(destinations);

    int entryCount = tree.getEntryCount("COM3/1/1");
    assertEquals(totalEntries, entryCount);

    Set<String> seenNames = new HashSet<>();
    int pageNo = 0;

    while (true) {
      List<BrowseEntry> page = tree.listAtNode("COM3/1/1", pageNo, pageSize);
      if (page.isEmpty()) {
        break;
      }

      for (BrowseEntry entry : page) {
        assertEquals(Type.TOPIC, entry.getDestinationType());
        assertTrue(entry.getName().startsWith("topic-"));

        boolean added = seenNames.add(entry.getName());
        assertTrue(added, "Duplicate entry detected: " + entry.getName());
      }

      pageNo++;
    }

    assertEquals(totalEntries, seenNames.size());

    assertTrue(seenNames.contains("topic-0000"));
    assertTrue(seenNames.contains("topic-0999"));
  }

  @Test
  void pagingWithPageSizeOneWorks() {
    int totalEntries = 1000;

    List<DestinationInfo> destinations = new ArrayList<>(totalEntries);
    for (int i = 0; i < totalEntries; i++) {
      String name = "COM3/1/1/topic-" + String.format("%04d", i);
      destinations.add(destination(name, DestinationType.TOPIC));
    }

    NamespaceTree tree = NamespaceTree.buildFromPaths(destinations);

    Set<String> seenNames = new HashSet<>();

    for (int pageNo = 0; pageNo < totalEntries; pageNo++) {
      List<BrowseEntry> page = tree.listAtNode("COM3/1/1", pageNo, 1);
      assertEquals(1, page.size());
      boolean added = seenNames.add(page.get(0).getName());
      assertTrue(added);
    }

    List<BrowseEntry> beyond = tree.listAtNode("COM3/1/1", totalEntries, 1);
    assertTrue(beyond.isEmpty());

    assertEquals(totalEntries, seenNames.size());
  }

  private static DestinationInfo destination(String name, DestinationType type) {
    return new DestinationInfo(name, type);
  }


}
