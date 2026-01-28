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
import io.mapsmessaging.rest.api.impl.destination.context.NamespaceNode;
import io.mapsmessaging.rest.api.impl.destination.context.NamespaceTree;
import io.mapsmessaging.rest.api.impl.destination.context.Type;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NamespaceTreeRealisticAbsoluteHierarchyWalkTest {

  @Test
  void absoluteHierarchySupportsWalkingAndFindingEveryFolder() {
    int rootFolders = 5;
    int level1Folders = 4;
    int level2Folders = 3;
    int destinationsPerLeaf = 30;

    List<DestinationInfo> destinations = new ArrayList<>();

    for (int r = 1; r <= rootFolders; r++) {
      String root = "/root-" + r;

      for (int a = 1; a <= level1Folders; a++) {
        String level1 = root + "/group-" + a;

        for (int b = 1; b <= level2Folders; b++) {
          String leaf = level1 + "/node-" + b;

          for (int d = 0; d < destinationsPerLeaf; d++) {
            DestinationType type = (d % 2 == 0)
                ? DestinationType.TOPIC
                : DestinationType.QUEUE;

            String name = (type == DestinationType.TOPIC ? "topic-" : "queue-")
                + String.format("%02d", d);

            destinations.add(destination(leaf + "/" + name, type));
          }
        }
      }
    }

    NamespaceTree tree = NamespaceTree.buildFromPaths(destinations);

    NamespaceNode absoluteRootMarker = tree.findNode("/");
    assertNotNull(absoluteRootMarker);
    assertEquals("/", absoluteRootMarker.getFullPath());

    for (int r = 1; r <= rootFolders; r++) {
      String rootPath = "/root-" + r;

      NamespaceNode rootNode = tree.findNode(rootPath);
      assertNotNull(rootNode, "Missing root folder: " + rootPath);
      assertEquals(rootPath, rootNode.getFullPath());

      for (int a = 1; a <= level1Folders; a++) {
        String groupPath = rootPath + "/group-" + a;

        NamespaceNode groupNode = tree.findNode(groupPath);
        assertNotNull(groupNode, "Missing group folder: " + groupPath);
        assertEquals(groupPath, groupNode.getFullPath());

        for (int b = 1; b <= level2Folders; b++) {
          String nodePath = groupPath + "/node-" + b;

          NamespaceNode leafNode = tree.findNode(nodePath);
          assertNotNull(leafNode, "Missing leaf folder: " + nodePath);
          assertEquals(nodePath, leafNode.getFullPath());

          List<BrowseEntry> leafEntries = tree.listAtNode(nodePath, 0, 100);
          assertEquals(destinationsPerLeaf, leafEntries.size(), "Unexpected leaf entry count at " + nodePath);

          for (BrowseEntry entry : leafEntries) {
            assertNotNull(entry.getName());
            assertNotNull(entry.getFullPath());
            assertTrue(entry.getFullPath().startsWith(nodePath + "/"));
            assertTrue(entry.getDestinationType() == Type.TOPIC || entry.getDestinationType() == Type.QUEUE);
          }
        }
      }
    }
  }

  private static DestinationInfo destination(String name, DestinationType type) {
    return new DestinationInfo(name, type);
  }
}