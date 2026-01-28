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

import io.mapsmessaging.api.DestinationInfo;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.rest.api.impl.destination.context.BrowseEntry;
import io.mapsmessaging.rest.api.impl.destination.context.NamespaceNode;
import io.mapsmessaging.rest.api.impl.destination.context.NamespaceTree;
import io.mapsmessaging.rest.api.impl.destination.context.Type;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NamespaceTreeTest {

  @Test
  void relativeAndAbsoluteNamespacesAreDifferent() {
    List<DestinationInfo> destinations = List.of(
        destination("COM3/1/1/topicA", DestinationType.TOPIC),
        destination("/COM3/1/1/topicB", DestinationType.TOPIC)
    );

    NamespaceTree tree = NamespaceTree.buildFromPaths(destinations);

    NamespaceNode relativeCom3 = tree.findNode("COM3");
    assertNotNull(relativeCom3);
    assertEquals("COM3", relativeCom3.getSegment());
    assertEquals("COM3", relativeCom3.getFullPath());

    NamespaceNode absoluteCom3 = tree.findNode("/COM3");
    assertNotNull(absoluteCom3);
    assertEquals("COM3", absoluteCom3.getSegment());
    assertEquals("/COM3", absoluteCom3.getFullPath());

    assertNotSame(relativeCom3, absoluteCom3);
  }

  @Test
  void findNodeReturnsNullWhenOnlyRelativeExistsButAbsoluteRequested() {
    List<DestinationInfo> destinations = List.of(
        destination("COM3/1/1/topicA", DestinationType.TOPIC)
    );

    NamespaceTree tree = NamespaceTree.buildFromPaths(destinations);

    assertNotNull(tree.findNode("COM3"));
    assertNull(tree.findNode("/COM3"));
  }

  @Test
  void findNodeReturnsNullWhenOnlyAbsoluteExistsButRelativeRequested() {
    List<DestinationInfo> destinations = List.of(
        destination("/COM3/1/1/topicA", DestinationType.TOPIC)
    );

    NamespaceTree tree = NamespaceTree.buildFromPaths(destinations);

    assertNotNull(tree.findNode("/COM3"));
    assertNull(tree.findNode("COM3"));
  }

  @Test
  void listingAtCom3ShowsChildFolderOneForBothNamespacesIndependently() {
    List<DestinationInfo> destinations = List.of(
        destination("COM3/1/1/topicA", DestinationType.TOPIC),
        destination("/COM3/1/1/topicB", DestinationType.TOPIC)
    );

    NamespaceTree tree = NamespaceTree.buildFromPaths(destinations);

    List<BrowseEntry> relative = tree.listAtNode("COM3", 0, 100);
    assertEquals(1, relative.size());
    assertEquals("1", relative.get(0).getName());
    assertEquals("COM3/1", relative.get(0).getFullPath());
    assertEquals(Type.FOLDER, relative.get(0).getDestinationType());
    assertTrue(relative.get(0).getChildCount() > 0);

    List<BrowseEntry> absolute = tree.listAtNode("/COM3", 0, 100);
    assertEquals(1, absolute.size());
    assertEquals("1", absolute.get(0).getName());
    assertEquals("/COM3/1", absolute.get(0).getFullPath());
    assertEquals(Type.FOLDER, absolute.get(0).getDestinationType());
    assertTrue(absolute.get(0).getChildCount() > 0);
  }

  @Test
  void listingAtLeafFolderShowsDestinations() {
    List<DestinationInfo> destinations = List.of(
        destination("COM3/1/1/topicA", DestinationType.TOPIC),
        destination("COM3/1/1/queueA", DestinationType.QUEUE),
        destination("/COM3/1/1/topicB", DestinationType.TOPIC)
    );

    NamespaceTree tree = NamespaceTree.buildFromPaths(destinations);

    List<BrowseEntry> relativeLeaf = tree.listAtNode("COM3/1/1", 0, 100);
    assertTrue(relativeLeaf.stream().anyMatch(e -> e.getName().equals("topicA") && e.getDestinationType() == Type.TOPIC));
    assertTrue(relativeLeaf.stream().anyMatch(e -> e.getName().equals("queueA") && e.getDestinationType() == Type.QUEUE));

    List<BrowseEntry> absoluteLeaf = tree.listAtNode("/COM3/1/1", 0, 100);
    assertEquals(1, absoluteLeaf.size());
    assertEquals("topicB", absoluteLeaf.get(0).getName());
    assertEquals("/COM3/1/1/topicB", absoluteLeaf.get(0).getFullPath());
    assertEquals(Type.TOPIC, absoluteLeaf.get(0).getDestinationType());
  }

  @Test
  void paginationWorksOnAStableSortedList() {
    List<DestinationInfo> destinations = List.of(
        destination("COM3/1/1/b", DestinationType.TOPIC),
        destination("COM3/1/1/a", DestinationType.TOPIC),
        destination("COM3/1/1/c", DestinationType.TOPIC),
        destination("COM3/1/1/d", DestinationType.TOPIC)
    );

    NamespaceTree tree = NamespaceTree.buildFromPaths(destinations);

    List<BrowseEntry> page0 = tree.listAtNode("COM3/1/1", 0, 2);
    assertEquals(2, page0.size());
    assertEquals("a", page0.get(0).getName());
    assertEquals("b", page0.get(1).getName());

    List<BrowseEntry> page1 = tree.listAtNode("COM3/1/1", 1, 2);
    assertEquals(2, page1.size());
    assertEquals("c", page1.get(0).getName());
    assertEquals("d", page1.get(1).getName());

    List<BrowseEntry> page2 = tree.listAtNode("COM3/1/1", 2, 2);
    assertTrue(page2.isEmpty());
  }


  @Test
  void findNodeForAbsoluteFirstSegmentMustWork() {
    List<DestinationInfo> destinations = List.of(
        destination("/COM3/1/1/HEARTBEAT", DestinationType.TOPIC)
    );

    NamespaceTree tree = NamespaceTree.buildFromPaths(destinations);

    NamespaceNode root = tree.getRoot();
    assertNotNull(root);

    NamespaceNode absoluteMarker = root.getChild("");
    assertNotNull(absoluteMarker, "Root must have an absolute marker child with segment \"\"");

    NamespaceNode slash = tree.findNode("/");
    assertNotNull(slash, "findNode(\"/\") must not be null");
    assertEquals("/", slash.getFullPath(), "findNode(\"/\") must return the absolute marker node, not root");

    NamespaceNode com3 = tree.findNode("/COM3");
    assertNotNull(com3, "findNode(\"/COM3\") must resolve when /COM3/... exists");
    assertEquals("/COM3", com3.getFullPath());
  }


  @Test
  void rootIsDistinctForAbsoluteMarkerPath() {
    List<DestinationInfo> destinations = List.of(
        destination("/COM3/1/1/topicB", DestinationType.TOPIC),
        destination("COM3/1/1/topicA", DestinationType.TOPIC)
    );

    NamespaceTree tree = NamespaceTree.buildFromPaths(destinations);

    NamespaceNode root = tree.getRoot();
    assertNotNull(root);
    assertEquals("", root.getFullPath());

    NamespaceNode absoluteMarker = tree.findNode("/");
    assertNotNull(absoluteMarker);
    assertEquals("/", absoluteMarker.getFullPath());

    List<BrowseEntry> rootEntries = tree.listAtNode("", 0, 100);
    assertTrue(rootEntries.stream().anyMatch(e -> e.getName().equals("COM3") && e.getFullPath().equals("COM3")));
    assertTrue(rootEntries.stream().anyMatch(e -> e.getName().equals("") && e.getFullPath().equals("/")));

    List<BrowseEntry> absoluteRootEntries = tree.listAtNode("/", 0, 100);
    assertTrue(absoluteRootEntries.stream().anyMatch(e -> e.getName().equals("COM3") && e.getFullPath().equals("/COM3")));
  }

  private static DestinationInfo destination(String name, DestinationType type) {
    return new DestinationInfo(name, type);
  }

}
