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

package io.mapsmessaging.rest.api.impl.destination.context;

import io.mapsmessaging.api.DestinationInfo;
import io.mapsmessaging.api.features.DestinationType;
import lombok.Getter;
import lombok.Value;

import java.time.Clock;
import java.util.Collections;
import java.util.List;

public class NamespaceTree {


  private final Clock clock;
  @Getter
  private final NamespaceNode root;

  public NamespaceTree() {
    this(Clock.systemUTC());
  }

  public NamespaceTree(Clock clock) {
    this.clock = clock;
    this.root = new NamespaceNode(null, "", 0L);
  }

  public static NamespaceTree buildFromPaths(List<DestinationInfo> paths) {
    NamespaceTree tree = new NamespaceTree();
    if (paths == null || paths.isEmpty()) {
      return tree;
    }

    for (DestinationInfo path : paths) {
      tree.addPath(path);
    }

    return tree;
  }

  public void addPath(DestinationInfo path) {
    String normalizedPath = NamespaceNormalizer.normalize(path.getName());
    if (normalizedPath.isEmpty()) {
      return;
    }

    String[] segments = NamespaceNormalizer.splitNormalized(normalizedPath);
    if (segments.length == 0) {
      return;
    }

    long now = nowMillis();

    NamespaceNode currentNode = root;

    int lastIndex = segments.length - 1;
    for (int i = 0; i < lastIndex; i++) {
      currentNode = currentNode.getOrCreateFolder(segments[i], now);
    }

    String leafName = segments[lastIndex];

    Type type = computeType(path.getType());
    currentNode.addDestination(leafName, type, now);
  }

  public NamespaceNode findNode(String path) {
    String normalizedPath = NamespaceNormalizer.normalize(path);

    if (normalizedPath.isEmpty()) {
      return root;
    }

    String[] segments = NamespaceNormalizer.splitNormalized(normalizedPath);
    if (segments.length == 0) {
      return root;
    }

    NamespaceNode currentNode = root;
    for (String segment : segments) {
      NamespaceNode next = currentNode.getChild(segment);
      if (next == null) {
        return null;
      }
      currentNode = next;
    }

    return currentNode;
  }


  public List<BrowseEntry> listAtNode(String path, int pageNo, int pageSize) {
    NamespaceNode node = findNode(path);
    if (node == null) {
      return Collections.emptyList();
    }

    List<Entry> page = node.pageEntries(pageNo, pageSize);
    if (page.isEmpty()) {
      return Collections.emptyList();
    }

    return page.stream()
        .map(e -> new BrowseEntry(
            e.getName(),
            e.getFullPath(),
            e.getDestinationType(),
            e.getChildCount()
        ))
        .toList();
  }

  public int getEntryCount(String path) {
    NamespaceNode node = findNode(path);
    if (node == null) {
      return 0;
    }
    return node.getEntryCount();
  }

  public long getLastUpdateMillis(String path) {
    NamespaceNode node = findNode(path);
    if (node == null) {
      return 0L;
    }
    return node.getLastUpdateMillis();
  }

  private long nowMillis() {
    return clock.millis();
  }

  private Type computeType(DestinationType destinationType) {
    switch(destinationType) {
      case DestinationType.TOPIC: return Type.TOPIC;
      case DestinationType.QUEUE: return Type.QUEUE;
      case DestinationType.TEMPORARY_TOPIC: return Type.TEMP_TOPIC;
      case DestinationType.TEMPORARY_QUEUE: return Type.TEMP_QUEUE;
      default:
        return Type.FOLDER;
    }
  }
}
